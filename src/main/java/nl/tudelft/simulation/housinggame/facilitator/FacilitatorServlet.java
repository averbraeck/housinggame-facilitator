package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;

import nl.tudelft.simulation.housinggame.common.PlayerState;
import nl.tudelft.simulation.housinggame.common.RoundState;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.NewsitemRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.RoundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.WelfaretypeRecord;

@WebServlet("/facilitator")
public class FacilitatorServlet extends HttpServlet
{

    /** */
    private static final long serialVersionUID = 1L;

    /** can be Player, House, News or Flood. */
    private String menuState = "Player";

    /** button that has been pressed. */
    private String button = "";

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        if (request.getParameter("menu") != null)
            this.menuState = request.getParameter("menu");

        if (request.getParameter("button") != null)
            this.button = request.getParameter("button");
        else
            this.button = "";

        FacilitatorData data = SessionUtils.getData(session);
        if (data == null)
        {
            response.sendRedirect("/housinggame-facilitator/login");
            return;
        }

        data.setShowModalWindow(0);
        data.setModalWindowHtml("");
        handleTopMenu(data);
        handlePressedButton(data, this.button, request);
        handleActivateButtons(data);

        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    public void handleTopMenu(final FacilitatorData data)
    {
        if (this.menuState.equals("Player"))
        {
            makePlayerTables(data);
            data.getContentHtml().put("menuPlayer", "btn btn-primary");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (this.menuState.equals("House"))
        {
            makeHouseTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn btn-primary");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (this.menuState.equals("News"))
        {
            makeNewsTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn btn-primary");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (this.menuState.equals("Flood"))
        {
            makeFloodTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn btn-primary");
        }
    }

    public void handlePressedButton(final FacilitatorData data, final String button, final HttpServletRequest request)
    {
        if (button.equals("new-round"))
            popupNewRound(data);
        else if (button.equals("new-round-ok"))
            newRound(data);
        else if (button.equals("announce-news"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_HOUSES.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("assign-houses"))
        {
            // TODO: handle the house allocation first, before this button is pressed
            // TODO: popup to ask if all houses have eben allocated
            data.getCurrentGroupRound().setRoundState(RoundState.ASSIGN_HOUSES.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("calculate-taxes"))
        {
            // TODO: calculate taxes based on number of house owners per community
            data.getCurrentGroupRound().setRoundState(RoundState.CALCULATE_TAXES.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("allow-improvements"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ALLOW_IMPROVEMENTS.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("ask-perceptions"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ASK_PERCEPTIONS.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("roll-dice"))
        {
            // TODO: read dice values; check if dice values are valid. Popup if not -- ask to resubmit
            // TODO: calculate the damage
            data.getCurrentGroupRound().setRoundState(RoundState.ROLL_DICE.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("show-summary"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_SUMMARY.toString());
            data.getCurrentGroupRound().store();
        }
    }

    public void handleActivateButtons(final FacilitatorData data)
    {
        for (String b : new String[] {"new-round", "announce-news", "show-houses", "assign-houses", "calculate-taxes",
                "allow-improvements", "ask-perceptions", "roll-dice", "show-summary"})
            data.putContentHtml("button/" + b, "btn btn-inactive");
        for (String a : new String[] {"new-round", "announce-news", "houses-taxes",
                "allow-improvements", "ask-perceptions", "roll-dice", "show-summary"})
            data.putContentHtml("accordion/" + a, "");
        switch (RoundState.valueOf(data.getCurrentGroupRound().getRoundState()))
        {
            case LOGIN ->
            {
                data.putContentHtml("button/new-round", "btn btn-primary btn-active");
                data.putContentHtml("accordion/new-round", "in");
            }
            case NEW_ROUND ->
            {
                data.putContentHtml("button/announce-news", "btn btn-primary btn-active");
                data.putContentHtml("accordion/announce-news", "in");
            }
            case ANNOUNCE_NEWS ->
            {
                data.putContentHtml("button/show-houses", "btn btn-primary btn-active");
                data.putContentHtml("accordion/houses-taxes", "in");
            }
            case SHOW_HOUSES ->
            {
                data.putContentHtml("button/assign-houses", "btn btn-primary btn-active");
                data.putContentHtml("accordion/houses-taxes", "in");
            }
            case ASSIGN_HOUSES ->
            {
                data.putContentHtml("button/calculate-taxes", "btn btn-primary btn-active");
                data.putContentHtml("accordion/houses-taxes", "in");
            }
            case CALCULATE_TAXES ->
            {
                data.putContentHtml("button/allow-improvements", "btn btn-primary btn-active");
                data.putContentHtml("accordion/allow-improvements", "in");
            }
            case ALLOW_IMPROVEMENTS ->
            {
                data.putContentHtml("button/ask-perceptions", "btn btn-primary btn-active");
                data.putContentHtml("accordion/ask-perceptions", "in");
            }
            case ASK_PERCEPTIONS ->
            {
                data.putContentHtml("button/roll-dice", "btn btn-primary btn-active");
                data.putContentHtml("accordion/roll-dice", "in");
            }
            case ROLL_DICE ->
            {
                data.putContentHtml("button/show-summary", "btn btn-primary btn-active");
                data.putContentHtml("accordion/show-summary", "in");
            }
            case SHOW_SUMMARY ->
            {
                if (data.getCurrentRoundNumber() < data.getScenario().getHighestRoundNumber().intValue())
                    data.putContentHtml("button/new-round", "btn btn-primary btn-active");
                data.putContentHtml("accordion/new-round", "in");
            }
            default -> System.err.println("Unknown RoundState: " + data.getCurrentGroupRound().getRoundState());
        }
    }

    public void popupNewRound(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrReadyPlayers = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = SqlUtils.getCurrentPlayerRound(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                if (playerRoundList.get(0) != null)
                    nrLoggedInPlayers++;
                else
                {
                    if (playerRound != null && PlayerState.ge(playerRound.getPlayerState(), PlayerState.SUMMARY.toString()))
                        nrReadyPlayers++;
                }
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                    nrActivePlayers++;
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active";
        content += "<br>There are " + nrReadyPlayers + " players who are at the summary screen<br>";
        if (data.getCurrentRoundNumber() == 0)
        {
            if (nrLoggedInPlayers < data.getScenario().getMinimumPlayers().intValue())
                content += "<br>This is LESS than the minimum number: " + data.getScenario().getMinimumPlayers().intValue();
            else
                content += "<br>This number would be sufficient to play the game.";
        }
        else
        {
            if (nrReadyPlayers < nrActivePlayers)
                content += "<br>NOT ALL PLAYERS are at the summary screen! (" + nrReadyPlayers + " < " + nrActivePlayers + ")";
            else if (nrActivePlayers == 0)
                content += "<br>PLAYERS HAVE NOT CARRIED OUT ANY ACTIONS YET!";
            else
                content += "<br>All players are at the summary screen";
        }
        content += "<br>Do you really want to move to the next round?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Move to next round?", content, "YES", "new-round-ok", "NO", "", "");
        data.setShowModalWindow(1);
    }

    public void newRound(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        GrouproundRecord groupRound = dslContext.newRecord(Tables.GROUPROUND);
        groupRound.setRoundState(RoundState.NEW_ROUND.toString());
        groupRound.setRoundId(data.getRoundList().get(data.getCurrentRoundNumber() + 1).getId());
        groupRound.setGroupId(data.getGroup().getId());
        groupRound.setPluvialFloodIntensity(null);
        groupRound.setFluvialFloodIntensity(null);
        groupRound.store();
        data.readDynamicData();
    }

    public static String makePlayerStateTable(final FacilitatorData data)
    {
        /*-
        <thead>
          <tr>
            <th>Player</th>
            <th>Round</th>
            <th>State</th>
            <th>Satisfaction</th>
            <th>Income</th>
            <th>House</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>t1p1</td>
            <td>0</td>
            <td>Login</td>
            <td>5</td>
            <td>30k</td>
            <td>--</td>
          </tr>
          ...
        </tbody>
         */
        StringBuilder s = new StringBuilder();
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Round</th>\n");
        s.append("                    <th>State</th>\n");
        s.append("                    <th>Satisfaction</th>\n");
        s.append("                    <th>Income</th>\n");
        s.append("                    <th>House</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>-</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getIncomePerRound().intValue()) + "</td>\n");
                s.append("                    <td>--</td>\n");
            }
            else
            {
                int highestRound = 0;
                PlayerroundRecord prr = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                    {
                        prr = playerRoundList.get(i);
                        highestRound = i;
                    }
                }
                s.append("                    <td>" + highestRound + "</td>\n");
                s.append("                    <td>" + prr.getPlayerState() + "</td>\n");
                s.append("                    <td>" + prr.getSatisfaction() + "</td>\n");
                s.append("                    <td>" + data.k(prr.getIncomePerRound().intValue()) + "</td>\n");
                if (prr.getHouseId() == null)
                    s.append("                    <td>--</td>\n");
                else
                {
                    HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, prr.getHouseId());
                    s.append("                    <td>" + house.getAddress() + "</td>\n");
                }
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        return s.toString();
    }

    public static String makePlayerBudgetTable(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Income</th>\n");
        s.append("                    <th>Living costs</th>\n");
        s.append("                    <th>Maximum mortgage</th>\n");
        s.append("                    <th>Savings</th>\n");
        s.append("                    <th>Debt</th>\n");
        s.append("                    <th>Preferred rating</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>" + data.k(welfareType.getIncomePerRound().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getLivingCosts().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getMaximumMortgage().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getInitialMoney().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(0) + "</td>\n");
                s.append("                    <td>" + welfareType.getPreferredHouseRating().intValue() + "</td>\n");
            }
            else
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                        prr = playerRoundList.get(i);
                }
                s.append("                    <td>" + data.k(prr.getIncomePerRound().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getLivingCosts().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getMaximumMortgage().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getSavings().intValue()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getDebt().intValue()) + "</td>\n");
                s.append("                    <td>" + prr.getPreferredHouseRating().intValue() + "</td>\n");
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        return s.toString();
    }

    public static void makePlayerTables(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append(makePlayerStateTable(data));
        s.append("          </table>\n");
        s.append("        </div>\n");
        s.append("\n");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("           <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append(makePlayerBudgetTable(data));
        s.append("           </table>\n");
        s.append("        </div>\n");
        s.append("\n");
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static void makeHouseTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Address</th>\n");
        s.append("                    <th>Available round</th>\n");
        s.append("                    <th>Initial price</th>\n");
        s.append("                    <th>Rating</th>\n");
        s.append("                    <th>Adjusted price</th>\n");
        s.append("                    <th>Owner</th>\n");
        s.append("                    <th>Measures</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT house.id FROM house INNER JOIN community ON house.community_id=community.id "
                        + "WHERE community.gameversion_id=" + data.getGameVersion().getId()
                        + " ORDER BY available_round, address ASC;");
        for (org.jooq.Record record : resultList)
        {
            UInteger id = (UInteger) record.get(0);
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, id);
            if (house.getAvailableRound().intValue() <= data.getCurrentRoundNumber())
            {
                s.append("                  <tr>\n");
                s.append("                    <td>" + house.getAddress() + "</td>\n");
                s.append("                    <td>" + house.getAvailableRound() + "</td>\n");
                s.append("                    <td>" + data.k(house.getPrice().intValue()) + "</td>\n");
                s.append("                    <td>" + house.getRating() + "</td>\n");
                // TODO
                s.append("                    <td>" + "--" + "</td>\n");
                // TODO
                s.append("                    <td>" + "--" + "</td>\n");
                // TODO
                s.append("                    <td>" + "--" + "</td>\n");
                s.append("                  </tr>\n");
            }
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static void makeNewsTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:left; width:90%;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Round</th>\n");
        s.append("                    <th>News Item</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT newsitem.id FROM newsitem INNER JOIN round ON newsitem.round_id=round.id "
                        + "WHERE round.scenario_id=" + data.getScenario().getId() + " ORDER BY round_number DESC;");
        for (org.jooq.Record record : resultList)
        {
            UInteger id = (UInteger) record.get(0);
            NewsitemRecord news = SqlUtils.readRecordFromId(data, Tables.NEWSITEM, id);
            RoundRecord round = SqlUtils.readRecordFromId(data, Tables.ROUND, news.getRoundId());
            if (round.getRoundNumber() <= data.getCurrentRoundNumber())
            {
                s.append("                  <tr>\n");
                s.append("                    <td>" + round.getRoundNumber() + "</td>\n");
                s.append("                    <td style=\"text-align:left;\">" + news.getContent() + "</td>\n");
                s.append("                  </tr>\n");
            }
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static void makeFloodTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();

        // TODO flood table.

        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
