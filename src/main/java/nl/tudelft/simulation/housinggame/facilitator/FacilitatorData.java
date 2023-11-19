package nl.tudelft.simulation.housinggame.facilitator;

import java.time.LocalDateTime;
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

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GamesessionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GameversionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.RoundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.UserRecord;

public class FacilitatorData
{

    /**
     * the SQL datasource representing the database's connection pool.<br>
     * the datasource is shared among the servlets and stored as a ServletContext attribute.
     */
    private DataSource dataSource;

    /** the facilitator User record. */
    private UserRecord user;

    /** There is always a gamesession to which the player belongs. */
    private GamesessionRecord gameSession;

    /** There is always a group to which the player belongs. */
    private GroupRecord group;

    /** The game might not have started, but a groep ALWAYS has a highest group round (0 if not started). */
    private GrouproundRecord groupRound;

    /** the current round as a record. Always there. */
    private RoundRecord round;

    /** The scenario. Always there. */
    private ScenarioRecord scenario;

    /** The game version. Always there. */
    private GameversionRecord gameVersion;

    /* ================================= */
    /* FULLY DYNAMIC INFO IN THE SESSION */
    /* ================================= */

    /** Content that ready for the jsp page to display. */
    private Map<String, String> contentHtmlMap = new HashMap<>();

    /** which menu has been chosen, to maintain persistence after a POST. */
    private String menuChoice = "";

    /** when 0, do not show popup; when 1: show popup. filled and updated by RoundServlet. */
    private int showModalWindow = 0;

    /** client info (dynamic) for popup. */
    private String modalWindowHtml = "";

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

                setDataSource((DataSource) new InitialContext().lookup("/housinggame-facilitator_datasource"));
            }
            catch (NamingException e)
            {
                throw new RuntimeException(new ServletException(e));
            }
        }
        return this.dataSource;
    }

    public void setDataSource(final DataSource dataSource)
    {
        this.dataSource = dataSource;
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
                    s.append(gs.getId().intValue());
                    s.append("\">");
                    s.append(gs.getName());
                    s.append("</option>\n");
                }
            }
        }
        return s.toString();
    }

    public String getUsername()
    {
        return this.user == null ? null : this.user.getUsername();
    }

    public UserRecord getUser()
    {
        return this.user;
    }

    public void setUser(final UserRecord user)
    {
        this.user = user;
    }

    public GamesessionRecord getGameSession()
    {
        return this.gameSession;
    }

    public void setGameSession(final GamesessionRecord gameSession)
    {
        this.gameSession = gameSession;
    }

    public GroupRecord getGroup()
    {
        return this.group;
    }

    public void setGroup(final GroupRecord group)
    {
        this.group = group;
    }

    public GrouproundRecord getGroupRound()
    {
        return this.groupRound;
    }

    public void setGroupRound(final GrouproundRecord groupRound)
    {
        this.groupRound = groupRound;
    }

    public int getCurrentRound()
    {
        return this.round.getRoundNumber();
    }

    public RoundRecord getRound()
    {
        return this.round;
    }

    public void setRound(final RoundRecord round)
    {
        this.round = round;
    }

    public ScenarioRecord getScenario()
    {
        return this.scenario;
    }

    public void setScenario(final ScenarioRecord scenario)
    {
        this.scenario = scenario;
    }

    public GameversionRecord getGameVersion()
    {
        return this.gameVersion;
    }

    public void setGameVersion(final GameversionRecord gameVersion)
    {
        this.gameVersion = gameVersion;
    }

    public String getContentHtml(final String key)
    {
        return this.contentHtmlMap.get(key);
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

    public String getMenuChoice()
    {
        return this.menuChoice;
    }

    public void setMenuChoice(final String menuChoice)
    {
        this.menuChoice = menuChoice;
    }

    public String getModalWindowHtml()
    {
        return this.modalWindowHtml;
    }

    public void setModalWindowHtml(final String modalClientWindowHtml)
    {
        this.modalWindowHtml = modalClientWindowHtml;
    }

    /**
     * Express a number in thousands.
     * @param nr int; the number to display
     * @return String; the number if less than 1000, or the rounded number divided by 1000, followed by 'k'
     */
    public String k(final int nr)
    {
        if (nr < 1000)
            return Integer.toString(nr);
        else
            return Integer.toString(nr / 1000) + " k";
    }

}
