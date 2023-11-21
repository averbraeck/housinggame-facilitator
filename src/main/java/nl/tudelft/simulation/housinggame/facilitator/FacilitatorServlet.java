package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

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

import nl.tudelft.simulation.housinggame.common.RoundState;
import nl.tudelft.simulation.housinggame.data.Tables;
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
    }

    public void handleActivateButtons(final FacilitatorData data)
    {
        for (String b : new String[] {"new-round", "announce-news", "show-houses", "assign-houses", "calculate-taxes",
                "allow-improvements", "ask-perceptions", "roll-dice", "show-damage"})
            data.putContentHtml("button/" + b, "btn btn-inactive");
        switch (RoundState.valueOf(data.getGroupRound().getRoundState()))
        {
            case INIT -> data.putContentHtml("button/new-round", "btn btn-primary btn-active");
            case LOGIN -> data.putContentHtml("button/new-round", "btn btn-primary btn-active");
            case NEW_ROUND -> data.putContentHtml("button/announce-news", "btn btn-primary btn-active");
            case ANNOUNCE_NEWS -> data.putContentHtml("button/show-houses", "btn btn-primary btn-active");
            case SHOW_HOUSES -> data.putContentHtml("button/assign-houses", "btn btn-primary btn-active");
            case ASSIGN_HOUSES -> data.putContentHtml("button/calculate-taxes", "btn btn-primary btn-active");
            case CALCULATE_TAXES -> data.putContentHtml("button/allow-improvements", "btn btn-primary btn-active");
            case ALLOW_IMPROVEMENTS -> data.putContentHtml("button/ask-perceptions", "btn btn-primary btn-active");
            case ASK_PERCEPTIONS -> data.putContentHtml("button/roll-dice", "btn btn-primary btn-active");
            case ROLL_DICE -> data.putContentHtml("button/show-damage", "btn btn-primary btn-active");
            case SHOW_DAMAGE ->
            {
                if (data.getCurrentRound() < data.getHighestRound())
                    data.putContentHtml("button/new-round", "btn btn-primary btn-active");
            }
            default -> System.err.println("Unknown RoundState: " + data.getGroupRound().getRoundState());
        }
    }

    public void popupNewRound(final FacilitatorData data)
    {
        String content = "There are XX players logged in, <br>do you really want to move to the next round?";
        ModalWindowUtils.make2ButtonModalWindow(data, "Move to next round?", content, "YES", "new-round-ok", "NO", "", "");
        data.setShowModalWindow(1);
    }

    public void newRound(final FacilitatorData data)
    {
        System.out.println("Here, we would really move to the next round");
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
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
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
        List<PlayerRecord> playerList = dslContext.selectFrom(Tables.PLAYER)
                .where(Tables.PLAYER.GROUP_ID.eq(data.getGroup().getId())).fetch().sortAsc(Tables.PLAYER.CODE);
        for (PlayerRecord player : playerList)
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            SortedMap<Integer, PlayerroundRecord> playerRoundMap = SqlUtils.getPlayerRoundMap(data, player.getId());
            if (playerRoundMap.isEmpty())
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
                int highestRound = playerRoundMap.lastKey();
                PlayerroundRecord prr = playerRoundMap.get(highestRound);
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
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
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
        List<PlayerRecord> playerList = dslContext.selectFrom(Tables.PLAYER)
                .where(Tables.PLAYER.GROUP_ID.eq(data.getGroup().getId())).fetch().sortAsc(Tables.PLAYER.CODE);
        for (PlayerRecord player : playerList)
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            SortedMap<Integer, PlayerroundRecord> playerRoundMap = SqlUtils.getPlayerRoundMap(data, player.getId());
            if (playerRoundMap.isEmpty())
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
                int highestRound = playerRoundMap.lastKey();
                PlayerroundRecord prr = playerRoundMap.get(highestRound);
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
            if (house.getAvailableRound().intValue() <= data.getCurrentRound())
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
            if (round.getRoundNumber() <= data.getCurrentRound())
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
