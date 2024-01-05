package nl.tudelft.simulation.housinggame.facilitator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import nl.tudelft.simulation.housinggame.common.GroupState;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GamesessionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GameversionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioparametersRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.UserRecord;

public class FacilitatorData
{

    /**
     * the SQL datasource representing the database's connection pool.<br>
     * the datasource is shared among the servlets and stored as a ServletContext attribute.
     */
    private DataSource dataSource;

    /** the facilitator User record (static during session). */
    private UserRecord user;

    /** The game version (static during session). */
    private GameversionRecord gameVersion;

    /** The scenario (static during session). */
    private ScenarioRecord scenario;

    /** The scenario parameters. */
    private ScenarioparametersRecord scenarioParameters;

    /** Gamesession for the facilitator (static during session). */
    private GamesessionRecord gameSession;

    /** Group that the facilitator is responsible for (static during session). */
    private GroupRecord group;

    /** List of the players of the group (static during session). */
    private List<PlayerRecord> playerList;

    /** The current round. This is DYNAMIC. */
    private int currentRoundNumber = -1;

    /** The list of GroupRounds until now. This is DYNAMIC. */
    private List<GrouproundRecord> groupRoundList = new ArrayList<>();

    /* ================================= */
    /* FULLY DYNAMIC INFO IN THE SESSION */
    /* ================================= */

    /** Content that ready for the jsp page to display. */
    private Map<String, String> contentHtmlMap = new HashMap<>();

    /** when 0, do not show popup; when 1: show popup; when 2: refresh screen INCLUDING modal window. See ModalWindowUtils. */
    private int showModalWindow = 0;

    /** client info (dynamic) for popup. */
    private String modalWindowHtml = "";

    /** the menu state of the facilitator app (right-hand side of the screen). */
    private String menuState = "Player";

    /** the round for which the flood information is shown. */
    private int floodInfoRoundNumber = -1;

    /* ******************* */
    /* GETTERS AND SETTERS */
    /* ******************* */

    public DataSource getDataSource()
    {
        if (this.dataSource == null)
        {
            try
            {
                // determine the connection pool, and create one if it does not yet exist (first use after server restart)
                try
                {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException(new ServletException(e));
                }

                try
                {
                    Context ctx = new InitialContext();
                    try
                    {
                        ctx.lookup("/housinggame-facilitator_datasource");
                    }
                    catch (NamingException ne)
                    {
                        final HikariConfig config = new HikariConfig();
                        config.setJdbcUrl("jdbc:mysql://localhost:3306/housinggame");
                        config.setUsername("housinggame");
                        config.setPassword("tudHouse#4");
                        config.setMaximumPoolSize(2);
                        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                        DataSource dataSource = new HikariDataSource(config);
                        ctx.bind("/housinggame-facilitator_datasource", dataSource);
                    }
                }
                catch (NamingException e)
                {
                    throw new RuntimeException(new ServletException(e));
                }

                this.dataSource = (DataSource) new InitialContext().lookup("/housinggame-facilitator_datasource");
            }
            catch (NamingException e)
            {
                throw new RuntimeException(new ServletException(e));
            }
        }
        return this.dataSource;
    }

    /*-
    <option value="3">Session 1</option>
    <option value="6">Session 2</option>
    */
    public String getValidSessionOptions()
    {
        LocalDateTime now = LocalDateTime.now();
        DSLContext dslContext = DSL.using(getDataSource(), SQLDialect.MYSQL);
        List<GamesessionRecord> gsList = dslContext.selectFrom(Tables.GAMESESSION).fetch();
        StringBuilder s = new StringBuilder();
        for (GamesessionRecord gs : gsList)
        {
            if (gs.getStartTime() == null || now.isAfter(gs.getStartTime()))
            {
                if (gs.getEndTime() == null || now.isBefore(gs.getEndTime()))
                {
                    s.append("<option value=\"");
                    s.append(gs.getId());
                    s.append("\">");
                    s.append(gs.getName());
                    s.append("</option>\n");
                }
            }
        }
        return s.toString();
    }

    public void readFacilitatorData(final UserRecord user, final GroupRecord group)
    {
        DSLContext dslContext = DSL.using(getDataSource(), SQLDialect.MYSQL);
        this.user = user;
        this.group = group;
        this.gameSession = SqlUtils.readRecordFromId(this, Tables.GAMESESSION, group.getGamesessionId());
        this.scenario = SqlUtils.readRecordFromId(this, Tables.SCENARIO, group.getScenarioId());
        this.gameVersion = SqlUtils.readRecordFromId(this, Tables.GAMEVERSION, this.scenario.getGameversionId());
        this.playerList = dslContext.selectFrom(Tables.PLAYER).where(Tables.PLAYER.GROUP_ID.eq(group.getId())).fetch()
                .sortAsc(Tables.PLAYER.CODE);
        this.scenarioParameters =
                SqlUtils.readRecordFromId(this, Tables.SCENARIOPARAMETERS, this.scenario.getScenarioparametersId());
        readDynamicData();
    }

    public void readDynamicData()
    {
        DSLContext dslContext = DSL.using(getDataSource(), SQLDialect.MYSQL);
        try
        {
            this.groupRoundList.clear();
            this.currentRoundNumber = -1;
            dslContext.execute("LOCK TABLES groupround WRITE WAIT 10;");
            for (int i = 0; i <= this.scenario.getHighestRoundNumber(); i++)
            {
                GrouproundRecord groupRound =
                        dslContext.selectFrom(Tables.GROUPROUND).where(Tables.GROUPROUND.ROUND_NUMBER.eq(i))
                                .and(Tables.GROUPROUND.GROUP_ID.eq(this.group.getId())).fetchAny();
                if (groupRound == null)
                    break;
                this.groupRoundList.add(groupRound);
                this.currentRoundNumber = i;
            }
            if (this.groupRoundList.isEmpty())
            {
                GrouproundRecord groupRound = dslContext.newRecord(Tables.GROUPROUND);
                groupRound.setGroupId(this.group.getId());
                groupRound.setRoundNumber(0);
                groupRound.setFluvialFloodIntensity(0);
                groupRound.setPluvialFloodIntensity(0);
                groupRound.setGroupState(GroupState.LOGIN.toString());
                groupRound.store();
                this.groupRoundList.add(groupRound);
                this.currentRoundNumber = 0;
            }
            if (this.floodInfoRoundNumber <= 0)
                this.floodInfoRoundNumber = this.currentRoundNumber;
            else if (!this.menuState.equals("Flood"))
                this.floodInfoRoundNumber = this.currentRoundNumber;
        }
        finally
        {
            dslContext.execute("UNLOCK TABLES;");
        }
    }

    public String getUsername()
    {
        return this.user == null ? null : this.user.getUsername();
    }

    public UserRecord getUser()
    {
        return this.user;
    }

    public GamesessionRecord getGameSession()
    {
        return this.gameSession;
    }

    public GroupRecord getGroup()
    {
        return this.group;
    }

    public GrouproundRecord getCurrentGroupRound()
    {
        return this.groupRoundList.isEmpty() ? null : this.groupRoundList.get(this.currentRoundNumber);
    }

    public GroupState getGroupState()
    {
        if (getCurrentGroupRound() == null)
            return GroupState.LOGIN;
        return GroupState.valueOf(getCurrentGroupRound().getGroupState());
    }

    public int getCurrentRoundNumber()
    {
        return this.currentRoundNumber;
    }

    /**
     * @return floodInfoRoundNumber
     */
    public int getFloodInfoRoundNumber()
    {
        return this.floodInfoRoundNumber;
    }

    /**
     * @param floodInfoRoundNumber set floodInfoRoundNumber
     */
    public void setFloodInfoRoundNumber(final int floodInfoRoundNumber)
    {
        this.floodInfoRoundNumber = floodInfoRoundNumber;
    }

    /**
     * Return a List of the players of the group (static during session).
     * @return List&lt;PlayerRecord&gt;>; a list of the players of the group (static during session).
     */
    public List<PlayerRecord> getPlayerList()
    {
        return this.playerList;
    }

    public List<PlayerroundRecord> getPlayerRoundList()
    {
        DSLContext dslContext = DSL.using(getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.PLAYERROUND)
                .where(Tables.PLAYERROUND.GROUPROUND_ID.eq(getCurrentGroupRound().getId())).fetch();
    }

    public List<GrouproundRecord> getGroupRoundList()
    {
        return this.groupRoundList;
    }

    public ScenarioRecord getScenario()
    {
        return this.scenario;
    }

    public GameversionRecord getGameVersion()
    {
        return this.gameVersion;
    }

    public ScenarioparametersRecord getScenarioParameters()
    {
        return this.scenarioParameters;
    }

    public String getContentHtml(final String key)
    {
        return this.contentHtmlMap.containsKey(key) ? this.contentHtmlMap.get(key) : "";
    }

    public void putContentHtml(final String key, final String value)
    {
        this.contentHtmlMap.put(key, value);
    }

    public Map<String, String> getContentHtml()
    {
        return this.contentHtmlMap;
    }

    public int getShowModalWindow()
    {
        return this.showModalWindow;
    }

    public void setShowModalWindow(final int showModalWindow)
    {
        this.showModalWindow = showModalWindow;
    }

    public String getModalWindowHtml()
    {
        return this.modalWindowHtml;
    }

    public void setModalWindowHtml(final String modalClientWindowHtml)
    {
        this.modalWindowHtml = modalClientWindowHtml;
    }

    public String getMenuState()
    {
        return this.menuState;
    }

    public void setMenuState(final String menuState)
    {
        this.menuState = menuState;
    }

    /**
     * Express a number in thousands.
     * @param nr int; the number to display
     * @return String; the number if less than 1000, or the rounded number divided by 1000, followed by 'k'
     */
    public String k(final int nr)
    {
        if (Math.abs(nr) < 1000)
            return Integer.toString(nr);
        else
            return Integer.toString(nr / 1000) + " k";
    }

    public boolean isState(final GroupState state)
    {
        return getGroupState().eq(state);
    }

    public HousegroupRecord getApprovedHouseGroupForPlayerRound(final PlayerroundRecord playerRound)
    {
        if (playerRound.getFinalHousegroupId() != null)
        {
            return SqlUtils.readRecordFromId(this, Tables.HOUSEGROUP, playerRound.getFinalHousegroupId());
        }
        if (playerRound.getActiveTransactionId() != null)
        {
            HousetransactionRecord transaction =
                    SqlUtils.readRecordFromId(this, Tables.HOUSETRANSACTION, playerRound.getActiveTransactionId());
            if (transaction.getTransactionStatus().equals(TransactionStatus.APPROVED_BUY))
                return SqlUtils.readRecordFromId(this, Tables.HOUSEGROUP, transaction.getHousegroupId());
        }
        return null;
    }

    public HouseRecord getApprovedHouseForPlayerRound(final PlayerroundRecord playerRound)
    {
        HousegroupRecord hgr = getApprovedHouseGroupForPlayerRound(playerRound);
        if (hgr != null)
            return SqlUtils.readRecordFromId(this, Tables.HOUSE, hgr.getHouseId());
        return null;
    }

    /**
     * When the house is known, the expected mortgage is the actual mortgage. Otherwise, take the expected round mortgage as the
     * mortgage percentage of 80% of the maximum mortgage.
     * @param playerRound the player to estimate the mortgage for
     * @return int; the expected mortgage payment
     */
    public int getExpectedMortgagePayment(final PlayerroundRecord playerRound)
    {
        if (playerRound.getFinalHousegroupId() != null)
        {
            HousegroupRecord houseGroup =
                    SqlUtils.readRecordFromId(this, Tables.HOUSEGROUP, playerRound.getFinalHousegroupId());
            return (int) (houseGroup.getLastSoldPrice() * getMortgagePercentage() / 100.0);
        }
        if (playerRound.getActiveTransactionId() != null)
        {
            HousetransactionRecord transaction =
                    SqlUtils.readRecordFromId(this, Tables.HOUSETRANSACTION, playerRound.getActiveTransactionId());
            if (TransactionStatus.isBuy(transaction.getTransactionStatus()))
                return (int) (transaction.getPrice() * getMortgagePercentage() / 100.0);
        }
        return (int) (0.8 * playerRound.getMaximumMortgage() * getMortgagePercentage() / 100.0);
    }

    public int getExpectedTaxes(final HouseRecord house)
    {
        if (house == null)
            return 0;
        // TODO: get mid score from database
        return 15000;
    }

    public double getMortgagePercentage()
    {
        return this.scenarioParameters.getMortgagePercentage();
    }

    public List<HousegroupRecord> getHouseGroupList()
    {
        DSLContext dslContext = DSL.using(getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.HOUSEGROUP).where(Tables.HOUSEGROUP.GROUP_ID.eq(this.group.getId())).fetch();
    }
}
