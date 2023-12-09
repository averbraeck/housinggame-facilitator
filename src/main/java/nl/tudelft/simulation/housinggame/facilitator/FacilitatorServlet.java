package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import nl.tudelft.simulation.housinggame.common.PlayerState;
import nl.tudelft.simulation.housinggame.common.RoundState;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.InitialhousemeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasuretypeRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.NewsitemRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioparametersRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.WelfaretypeRecord;

@WebServlet("/facilitator")
public class FacilitatorServlet extends HttpServlet
{

    /** */
    private static final long serialVersionUID = 1L;

    /** button that has been pressed. */
    private String button = "";

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        FacilitatorData data = SessionUtils.getData(session);
        if (data == null)
        {
            response.sendRedirect("/housinggame-facilitator/login");
            return;
        }

        data.setShowModalWindow(0);
        data.setModalWindowHtml("");

        if (request.getParameter("menu") != null)
            data.setMenuState(request.getParameter("menu"));

        if (request.getParameter("button") != null)
            this.button = request.getParameter("button");
        else
            this.button = "";

        if (request.getParameter("house-management") != null)
        {
            if (request.getParameter("house-management").equals("buy-house"))
                houseBuyPopup(data, request);
        }

        if (this.button.equals("buy-house-ok"))
        {
            handleHouseBuy(data, request);
            this.button = "";
        }

        data.readDynamicData();
        handlePressedButton(data, this.button, request);
        prepareAccordionButtons(data, request); // dependent on NEW state
        handleTopMenu(data); // dependent on NEW state

        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    public void handleTopMenu(final FacilitatorData data)
    {
        data.getContentHtml().put("facilitator/house-allocation", "");
        if (data.getMenuState().equals("Player"))
        {
            makePlayerTables(data);
            data.getContentHtml().put("menuPlayer", "btn btn-primary");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("House"))
        {
            makeHouseTable(data);
            makeHouseSellingTable(data); // can be empty
            makeHouseBuyingTable(data); // can be empty
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn btn-primary");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("News"))
        {
            makeNewsTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn btn-primary");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("Flood"))
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
        if (button.equals("start-new-round"))
            popupNewRound(data);
        else if (button.equals("start-new-round-ok"))
        {
            newRound(data);
            data.getCurrentGroupRound().setRoundState(RoundState.NEW_ROUND.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("Player");
        }
        else if (button.equals("announce-news"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ANNOUNCE_NEWS.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("News");
        }
        else if (button.equals("show-houses"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_HOUSES.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("allow-selling"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ALLOW_SELLING.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("finish-selling"))
        {
            popupSellHouses(data);
        }
        else if (button.equals("finish-selling-ok"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SELLING_FINISHED.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("allow-buying"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ALLOW_BUYING.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("finish-buying"))
        {
            popupBuyHouses(data);
        }
        else if (button.equals("finish-buying-ok"))
        {
            calculateTaxes();
            data.getCurrentGroupRound().setRoundState(RoundState.BUYING_FINISHED.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("allow-improvements"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.ALLOW_IMPROVEMENTS.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("show-survey"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_SURVEY.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("Player");
        }
        else if (button.equals("complete-survey"))
        {
            popupSurvey(data);
        }
        else if (button.equals("complete-survey-ok"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SURVEY_COMPLETED.toString());
            data.getCurrentGroupRound().store();
        }
        else if (button.equals("roll-dice"))
        {
            // TODO: read dice values; check if dice values are valid. Popup if not -- ask to resubmit
            // TODO: calculate the damage
            data.getCurrentGroupRound().setRoundState(RoundState.ROLLED_DICE.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("Flood");
        }
        else if (button.equals("show-summary"))
        {
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_SUMMARY.toString());
            data.getCurrentGroupRound().store();
            data.setMenuState("Player");
        }
    }

    public void prepareAccordionButtons(final FacilitatorData data, final HttpServletRequest request)
    {
        for (String b : new String[] {"start-new-round", "announce-news", "show-houses", "allow-selling", "finish-selling",
                "allow-buying", "finish-buying", "allow-improvements", "show-survey", "complete-survey", "roll-dice",
                "show-summary"})
            data.putContentHtml("button/" + b, "btn btn-inactive");
        for (String a : new String[] {"round", "news", "houses", "improvements", "survey", "dice", "summary"})
            data.putContentHtml("accordion/" + a, "");

        if (data.getRoundState().eq(RoundState.LOGIN))
        {
            data.putContentHtml("button/start-new-round", "btn btn-primary btn-active");
            data.putContentHtml("accordion/round", "in");
        }
        else if (data.getRoundState().eq(RoundState.NEW_ROUND))
        {
            data.putContentHtml("button/announce-news", "btn btn-primary btn-active");
            data.putContentHtml("accordion/news", "in");
        }
        else if (data.getRoundState().eq(RoundState.ANNOUNCE_NEWS))
        {
            data.putContentHtml("button/show-houses", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_HOUSES))
        {
            if (data.getCurrentRoundNumber() == 1)
                data.putContentHtml("button/allow-buying", "btn btn-primary btn-active");
            else
                data.putContentHtml("button/allow-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.ALLOW_SELLING))
        {
            data.putContentHtml("button/finish-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.SELLING_FINISHED))
        {
            data.putContentHtml("button/allow-buying", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.ALLOW_BUYING))
        {
            data.putContentHtml("button/finish-buying", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.BUYING_FINISHED))
        {
            data.putContentHtml("button/allow-improvements", "btn btn-primary btn-active");
            data.putContentHtml("accordion/improvements", "in");
        }
        else if (data.getRoundState().eq(RoundState.ALLOW_IMPROVEMENTS))
        {
            data.putContentHtml("button/show-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_SURVEY))
        {
            data.putContentHtml("button/complete-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
        }
        else if (data.getRoundState().eq(RoundState.SURVEY_COMPLETED))
        {
            data.putContentHtml("button/roll-dice", "btn btn-primary btn-active");
            data.putContentHtml("accordion/dice", "in");
        }
        else if (data.getRoundState().eq(RoundState.ROLLED_DICE))
        {
            data.putContentHtml("button/show-summary", "btn btn-primary btn-active");
            data.putContentHtml("accordion/summary", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_SUMMARY))
        {
            if (data.getCurrentRoundNumber() < data.getScenario().getHighestRoundNumber())
            {
                data.putContentHtml("button/start-new-round", "btn btn-primary btn-active");
                data.putContentHtml("accordion/round", "in");
            }
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
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                {
                    PlayerState playerState = PlayerState.valueOf(playerRound.getPlayerState());
                    nrActivePlayers++;
                    if (playerState.equals(PlayerState.VIEW_SUMMARY))
                        nrReadyPlayers++;
                }
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active (in the same round)";
        content += "<br>There are " + nrReadyPlayers + " players who are at the summary screen<br>";
        if (data.getCurrentRoundNumber() == 0)
        {
            if (nrLoggedInPlayers < data.getScenario().getMinimumPlayers())
                content += "<br>This is LESS than the minimum number: " + data.getScenario().getMinimumPlayers();
            else
                content += "<br>This number would be sufficient to play the game.";
        }
        else
        {
            if (nrReadyPlayers < nrActivePlayers)
                content += "<br>NOT ALL PLAYERS are at the summary screen! (" + nrReadyPlayers + " < " + nrActivePlayers + ")";
            else if (nrActivePlayers == 0)
                content += "<br>NO PLAYERS HAVE CARRIED OUT ANY ACTIONS YET!";
            else
                content += "<br>All players are at the summary screen";
        }
        content += "<br>Do you really want to move to the next round?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Move to next round?", content, "YES", "start-new-round-ok", "NO", "",
                "");
        data.setShowModalWindow(1);
    }

    public void newRound(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        GrouproundRecord groupRound = dslContext.newRecord(Tables.GROUPROUND);
        groupRound.setRoundState(RoundState.NEW_ROUND.toString());
        groupRound.setRoundNumber(data.getCurrentRoundNumber() + 1);
        groupRound.setGroupId(data.getGroup().getId());
        groupRound.setPluvialFloodIntensity(null);
        groupRound.setFluvialFloodIntensity(null);
        groupRound.store();
        data.readDynamicData();
    }

    public void popupSellHouses(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrPlayersWithHouse = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = SqlUtils.getCurrentPlayerRound(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                if (playerRoundList.get(0) != null)
                    nrLoggedInPlayers++;
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                {
                    nrActivePlayers++;
                    if (playerRound.getStartHouseroundId() != null)
                        nrPlayersWithHouse++;
                }
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active (in the same round)";
        content += "<br>There are " + nrPlayersWithHouse + " players who have a house<br>";
        if (nrPlayersWithHouse < nrActivePlayers)
            content += "<br>NOT ALL ACTIVE PLAYERS HAVE A HOUSE! (" + nrPlayersWithHouse + " < " + nrActivePlayers + ")";
        else if (nrActivePlayers == 0)
            content += "<br>NO PLAYERS HAVE CARRIED OUT ANY ACTIONS YET!";
        else
            content += "<br>All active players have been allocated a house";
        content += "<br>Are you really ready with allocating houses?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Finish the selling / staying process?", content, "YES",
                "finish-selling-ok", "NO", "", "");
        data.setShowModalWindow(1);
    }

    public void popupBuyHouses(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrPlayersWithHouse = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = SqlUtils.getCurrentPlayerRound(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                if (playerRoundList.get(0) != null)
                    nrLoggedInPlayers++;
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                {
                    nrActivePlayers++;
                    if (playerRound.getFinalHouseroundId() != null)
                        nrPlayersWithHouse++;
                }
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active (in the same round)";
        content += "<br>There are " + nrPlayersWithHouse + " players who have a house<br>";
        if (nrPlayersWithHouse < nrActivePlayers)
            content += "<br>NOT ALL ACTIVE PLAYERS HAVE A HOUSE! (" + nrPlayersWithHouse + " < " + nrActivePlayers + ")";
        else if (nrActivePlayers == 0)
            content += "<br>NO PLAYERS HAVE CARRIED OUT ANY ACTIONS YET!";
        else
            content += "<br>All active players have been allocated a house";
        content += "<br>Are you really ready with allocating houses?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Finish the buying process?", content, "YES", "finish-buying-ok", "NO",
                "", "");
        data.setShowModalWindow(1);
    }

    public void popupSurvey(final FacilitatorData data)
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
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                {
                    PlayerState playerState = PlayerState.valueOf(playerRound.getPlayerState());
                    nrActivePlayers++;
                    if (playerState.equals(PlayerState.SURVEY_COMPLETED))
                        nrReadyPlayers++;
                }
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active (in the same round)";
        content += "<br>There are " + nrReadyPlayers + " players who completed the survey<br>";
        if (nrReadyPlayers < nrActivePlayers)
            content += "<br>NOT ALL PLAYERS have completed the survey! (" + nrReadyPlayers + " < " + nrActivePlayers + ")";
        else if (nrActivePlayers == 0)
            content += "<br>NO PLAYERS HAVE COMPLETED THE SURVEY!";
        else
            content += "<br>All players have completed the survey";
        content += "<br>Do you really want to move to dice rolls?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Move to dice rolls?", content, "YES", "complete-survey-ok", "NO", "", "");
        data.setShowModalWindow(1);
    }

    public void calculateTaxes()
    {
        // TODO make the tax calculations
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
        s.append("                    <th>House</th>\n");
        s.append("                    <th>Taxes</th>\n");
        s.append("                    <th>Spendable<br/>income</th>\n");
        s.append("                    <th>Player<br/>satisf</th>\n");
        s.append("                    <th>House<br/>satisf</th>\n");
        s.append("                    <th>Debt<br/>penalty</th>\n");
        s.append("                    <th>Flood<br/>penalty</th>\n");
        s.append("                    <th>Total<br/>satisf</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        ScenarioparametersRecord spr =
                SqlUtils.readRecordFromId(data, Tables.SCENARIOPARAMETERS, data.getScenario().getScenarioparametersId());

        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                int spendableIncome =
                        welfareType.getRoundIncome() + welfareType.getInitialMoney() - welfareType.getLivingCosts();
                s.append("                    <td>" + data.k(spendableIncome) + "</td>\n");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>\n");
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

                HouseRecord house = data.getHouseForPlayerRound(prr);
                int mortgage;
                int taxes;
                if (house == null)
                {
                    s.append("                    <td>--</td>\n");
                    s.append("                    <td>--</td>\n");
                    mortgage = 0;
                    taxes = 0;
                }
                else
                {
                    s.append("                    <td>" + house.getCode() + "</td>\n");
                    s.append("                    <td>" + data.k(data.getExpectedTaxes(house)) + "</td>\n");
                    mortgage = data.getExpectedMortgage(house);
                    taxes = data.getExpectedTaxes(house);
                }
                int improvements = prr.getCostMeasuresBought();
                int damageCost = prr.getCostFluvialDamage() + prr.getCostFluvialDamage();
                int spendableIncome = prr.getRoundIncome() + prr.getStartSavings() - prr.getLivingCosts() - mortgage - taxes
                        - improvements - damageCost;
                s.append("                    <td>" + data.k(spendableIncome) + "</td>\n");
                s.append("                    <td>" + prr.getStartPersonalSatisfaction() + "</td>\n");
                int totalSatisfaction = prr.getStartPersonalSatisfaction();
                if (house == null)
                {
                    s.append("                    <td>--</td>\n");
                }
                else
                {
                    s.append("                    <td>" + prr.getStartHouseSatisfaction() + "</td>\n");
                    totalSatisfaction += prr.getStartHouseSatisfaction();
                }
                if (prr.getStartDebt() == 0)
                    s.append("                    <td>-</td>\n");
                else
                {
                    s.append("                    <td>-" + spr.getSatisfactionDebtPenalty() + "</td>\n");
                    totalSatisfaction -= spr.getSatisfactionDebtPenalty();
                }
                s.append("                    <td>?</td>\n"); // TODO
                s.append("                    <td>" + totalSatisfaction + "</td>\n");
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
        s.append("                    <th>Living<br/>costs</th>\n");
        s.append("                    <th>Maximun<br/>mortgage</th>\n");
        s.append("                    <th>Savings</th>\n");
        s.append("                    <th>Debt</th>\n");
        s.append("                    <th>Increase<br/>satisf</th>\n");
        s.append("                    <th>Preferred<br/>rating</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                s.append("                    <td>" + data.k(welfareType.getRoundIncome()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getLivingCosts()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getMaximumMortgage()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getInitialMoney()) + "</td>\n");
                s.append("                    <td>" + data.k(0) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getSatisfactionCostPerPoint()) + "</td>\n");
                s.append("                    <td>" + welfareType.getPreferredHouseRating() + "</td>\n");
            }
            else
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                        prr = playerRoundList.get(i);
                }
                s.append("                    <td>" + data.k(prr.getRoundIncome()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getLivingCosts()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getMaximumMortgage()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getStartSavings()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getStartDebt()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getSatisfactionCostPerPoint()) + "</td>\n");
                s.append("                    <td>" + prr.getPreferredHouseRating() + "</td>\n");
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        return s.toString();
    }

    public static void makePlayerTables(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        s.append("        <h1>Players in the group</h1>");
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
        s.append("        <h1>Available houses in this round</h1>");
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

        // get the players with a house
        Map<Integer, PlayerRecord> ownedHouses = getPlayersForOwnedHouseIds(data);

        // get the initial and implemented measures for all houses
        Map<Integer, List<MeasuretypeRecord>> measureMap = getMeasuresPerHouse(data);

        // get the houses
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT house.id FROM house INNER JOIN community ON house.community_id=community.id "
                        + "WHERE community.gameversion_id=" + data.getGameVersion().getId()
                        + " ORDER BY available_round, code ASC;");
        for (org.jooq.Record record : resultList)
        {
            int id = Integer.valueOf(record.get(0).toString());
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, id);
            if (house.getAvailableRound() == data.getCurrentRoundNumber() || ownedHouses.containsKey(house.getId()))
            {
                s.append("                  <tr>\n");
                s.append("                    <td>" + house.getCode() + "</td>\n");
                s.append("                    <td>" + house.getAvailableRound() + "</td>\n");
                s.append("                    <td>" + data.k(house.getPrice()) + "</td>\n");
                s.append("                    <td>" + house.getRating() + "</td>\n");
                // TODO
                s.append("                    <td>" + "--" + "</td>\n");
                if (ownedHouses.containsKey(house.getId()))
                    s.append("                    <td>" + ownedHouses.get(house.getId()).getCode() + "</td>\n");
                else
                    s.append("                    <td>" + "--" + "</td>\n");
                if (measureMap.get(house.getId()).size() == 0)
                    s.append("                    <td>" + "--" + "</td>\n");
                else
                {
                    s.append("                    <td>");
                    boolean first = true;
                    for (MeasuretypeRecord measureType : measureMap.get(house.getId()))
                    {
                        if (!first)
                            s.append("<br />");
                        first = false;
                        s.append(measureType.getName());
                    }
                    s.append("</td>\n");
                }
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
        s.append("        <h1>News for this round and previous rounds</h1>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:left; width:90%;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Round</th>\n");
        s.append("                    <th>News Item</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        List<NewsitemRecord> newsItemList =
                dslContext.selectFrom(Tables.NEWSITEM).where(Tables.NEWSITEM.SCENARIO_ID.eq(data.getScenario().getId()))
                        .and(Tables.NEWSITEM.ROUND_NUMBER.le(data.getCurrentRoundNumber())).fetch()
                        .sortDesc(Tables.NEWSITEM.ROUND_NUMBER);
        for (NewsitemRecord news : newsItemList)
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + news.getRoundNumber() + "</td>\n");
            s.append("                    <td style=\"text-align:left;\">" + news.getContent() + "</td>\n");
            s.append("                  </tr>\n");
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
        s.append("        <h1>Information about floods up to this round</h1>");

        // TODO flood table.

        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static void makeHouseSellingTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();

        s.append("      <div class=\"hg-grid2-left\">\n");
        s.append("        <div>\n");
        s.append("          <b>Buying of house by player</b>\n");
        s.append("          <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("            <input type=\"hidden\" name=\"house-management\" value=\"buy-house\" />\n");
        s.append("            <div style=\"display: flex; flex-direction: column;\">\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-house\">House for player to buy</label> <select\n");
        s.append("                  name=\"house\" id=\"buy-house\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (HouseRecord house : getAvailableHousesForRound(data))
            s.append("                  <option value=\"" + house.getId() + "\">" + house.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-player\">Player who buys</label> <select\n");
        s.append("                  name=\"player\" id=\"buy-player\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (PlayerRecord player : getPlayersWithoutHouse(data))
            s.append("                  <option value=\"" + player.getId() + "\">" + player.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-price\">Adjusted buying price</label> <input\n");
        s.append("                  type=\"number\" id=\"buy-price\" name=\"buy-price\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <br />\n");
        s.append("              <div class=\"hg-button\">\n");
        s.append("                <input type=\"submit\" value='Sell house to player'\n");
        s.append("                  class=\"btn btn-primary\" />\n");
        s.append("              </div>\n");
        s.append("            </div>\n");
        s.append("          </form>\n");
        s.append("        </div>\n");
        s.append("        <div>\n");
        s.append("          <b>Selling of house to the bank</b>\n");
        s.append("          <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("            <input type=\"hidden\" name=\"house-management\" value=\"sell-house\" />\n");
        s.append("            <div style=\"display: flex; flex-direction: column;\">\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-house\">House player sells to bank</label> <select\n");
        s.append("                  name=\"house\" id=\"sell-house\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (HouseRecord house : getOwnedHouses(data))
            s.append("                  <option value=\"" + house.getId() + "\">" + house.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-price\">Adjusted selling price</label> <input\n");
        s.append("                  type=\"number\" id=\"sell-price\" name=\"sell-price\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-reason\">Reason for selling house</label> <input\n");
        s.append("                  type=\"text\" id=\"sell-reason\" name=\"sell-reason\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <br />\n");
        s.append("              <div class=\"hg-button\">\n");
        s.append("                <input type=\"submit\" value='Sell house to the bank'\n");
        s.append("                  class=\"btn btn-primary\" />\n");
        s.append("              </div>\n");
        s.append("            </div>\n");
        s.append("          </form>\n");
        s.append("        </div>\n");
        s.append("      </div>\n");

        data.getContentHtml().put("facilitator/house-allocation", s.toString());
    }

    public static void makeHouseBuyingTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();

        s.append("      <div class=\"hg-grid2-left\">\n");
        s.append("        <div>\n");
        s.append("          <b>Buying of house by player</b>\n");
        s.append("          <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("            <input type=\"hidden\" name=\"house-management\" value=\"buy-house\" />\n");
        s.append("            <div style=\"display: flex; flex-direction: column;\">\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-house\">House for player to buy</label> <select\n");
        s.append("                  name=\"house\" id=\"buy-house\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (HouseRecord house : getAvailableHousesForRound(data))
            s.append("                  <option value=\"" + house.getId() + "\">" + house.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-player\">Player who buys</label> <select\n");
        s.append("                  name=\"player\" id=\"buy-player\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (PlayerRecord player : getPlayersWithoutHouse(data))
            s.append("                  <option value=\"" + player.getId() + "\">" + player.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"buy-price\">Adjusted buying price</label> <input\n");
        s.append("                  type=\"number\" id=\"buy-price\" name=\"buy-price\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <br />\n");
        s.append("              <div class=\"hg-button\">\n");
        s.append("                <input type=\"submit\" value='Sell house to player'\n");
        s.append("                  class=\"btn btn-primary\" />\n");
        s.append("              </div>\n");
        s.append("            </div>\n");
        s.append("          </form>\n");
        s.append("        </div>\n");
        s.append("        <div>\n");
        s.append("          <b>Selling of house to the bank</b>\n");
        s.append("          <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("            <input type=\"hidden\" name=\"house-management\" value=\"sell-house\" />\n");
        s.append("            <div style=\"display: flex; flex-direction: column;\">\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-house\">House player sells to bank</label> <select\n");
        s.append("                  name=\"house\" id=\"sell-house\">\n");
        s.append("                  <option value=\"\"></option>\n");
        for (HouseRecord house : getOwnedHouses(data))
            s.append("                  <option value=\"" + house.getId() + "\">" + house.getCode() + "</option>\n");
        s.append("                </select>\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-price\">Adjusted selling price</label> <input\n");
        s.append("                  type=\"number\" id=\"sell-price\" name=\"sell-price\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <div>\n");
        s.append("                <label for=\"sell-reason\">Reason for selling house</label> <input\n");
        s.append("                  type=\"text\" id=\"sell-reason\" name=\"sell-reason\" value=\"\">\n");
        s.append("              </div>\n");
        s.append("              <br />\n");
        s.append("              <div class=\"hg-button\">\n");
        s.append("                <input type=\"submit\" value='Sell house to the bank'\n");
        s.append("                  class=\"btn btn-primary\" />\n");
        s.append("              </div>\n");
        s.append("            </div>\n");
        s.append("          </form>\n");
        s.append("        </div>\n");
        s.append("      </div>\n");

        data.getContentHtml().put("facilitator/house-allocation", s.toString());
    }

    public static Map<Integer, PlayerRecord> getPlayersForOwnedHouseIds(final FacilitatorData data)
    {
        Map<Integer, PlayerRecord> playersForOwnedHouseIds = new HashMap<>();
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                if (prr != null)
                {
                    for (int i = 0; i < playerRoundList.size(); i++)
                    {
                        if (playerRoundList.get(i) != null)
                            prr = playerRoundList.get(i);
                    }
                    HouseRecord house = data.getHouseForPlayerRound(prr);
                    if (prr != null && house != null)
                        playersForOwnedHouseIds.put(house.getId(), player);
                }
            }
        }
        return playersForOwnedHouseIds;
    }

    public static List<PlayerRecord> getPlayersWithoutHouse(final FacilitatorData data)
    {
        List<PlayerRecord> playersWithoutHouse = new ArrayList<>();
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                if (prr != null)
                {
                    for (int i = 0; i < playerRoundList.size(); i++)
                    {
                        if (playerRoundList.get(i) != null)
                            prr = playerRoundList.get(i);
                    }
                    HouseRecord house = data.getHouseForPlayerRound(prr);
                    if (prr != null && house == null)
                        playersWithoutHouse.add(player);
                }
            }
        }
        return playersWithoutHouse;
    }

    public static List<HouseRecord> getAvailableHousesForRound(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        Map<Integer, PlayerRecord> playersForOwnedHouseIds = getPlayersForOwnedHouseIds(data);
        List<HouseRecord> availableHouses = new ArrayList<>();
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT house.id FROM house INNER JOIN community ON house.community_id=community.id "
                        + "WHERE community.gameversion_id=" + data.getGameVersion().getId()
                        + " ORDER BY available_round, code ASC;");
        for (org.jooq.Record record : resultList)
        {
            int id = Integer.valueOf(record.get(0).toString());
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, id);
            if (house.getAvailableRound() == data.getCurrentRoundNumber())
            {
                if (!playersForOwnedHouseIds.keySet().contains(house.getId()))
                    availableHouses.add(house);
            }
        }
        return availableHouses;
    }

    public static List<HouseRecord> getOwnedHouses(final FacilitatorData data)
    {
        List<HouseRecord> ownedHouses = new ArrayList<>();
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                if (prr != null)
                {
                    for (int i = 0; i < playerRoundList.size(); i++)
                    {
                        if (playerRoundList.get(i) != null)
                            prr = playerRoundList.get(i);
                    }
                    HouseRecord house = data.getHouseForPlayerRound(prr);
                    if (prr != null && house != null)
                        ownedHouses.add(house);
                }
            }
        }
        return ownedHouses;
    }

    public static Map<Integer, List<MeasuretypeRecord>> getMeasuresPerHouse(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        Map<Integer, List<MeasuretypeRecord>> measureMap = new HashMap<>();
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT house.id FROM house INNER JOIN community ON house.community_id=community.id "
                        + "WHERE community.gameversion_id=" + data.getGameVersion().getId()
                        + " ORDER BY available_round, code ASC;");
        for (org.jooq.Record record : resultList)
        {
            int houseId = Integer.valueOf(record.get(0).toString());
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseId);
            List<MeasureRecord> measureList = new ArrayList<>();
            for (GrouproundRecord groupRound : data.getGroupRoundList())
            {
                HouseroundRecord houseRound =
                        dslContext.selectFrom(Tables.HOUSEROUND).where(Tables.HOUSEROUND.GROUPROUND_ID.eq(groupRound.getId()))
                                .and(Tables.HOUSEROUND.HOUSE_ID.eq(houseId)).fetchAny();
                if (houseRound != null)
                {
                    List<MeasureRecord> mtrList = dslContext.selectFrom(Tables.MEASURE)
                            .where(Tables.MEASURE.HOUSEROUND_ID.eq(houseRound.getId())).fetch();
                    measureList.addAll(mtrList);
                }
            }
            List<InitialhousemeasureRecord> initialMeasureList = dslContext.selectFrom(Tables.INITIALHOUSEMEASURE)
                    .where(Tables.INITIALHOUSEMEASURE.HOUSE_ID.eq(house.getId())).fetch();
            List<MeasuretypeRecord> measureTypeList = new ArrayList<>();
            for (InitialhousemeasureRecord initialMeasure : initialMeasureList)
                measureTypeList.add(SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, initialMeasure.getMeasuretypeId()));
            for (MeasureRecord measure : measureList)
                measureTypeList.add(SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, measure.getMeasuretypeId()));
            measureMap.put(house.getId(), measureTypeList);
        }
        return measureMap;
    }

    public static void houseBuyPopup(final FacilitatorData data, final HttpServletRequest request)
    {

        System.out.println("house = " + request.getParameter("house"));
        System.out.println("player = " + request.getParameter("player"));
        System.out.println("buy-price = " + request.getParameter("buy-price"));

        int houseId;
        int playerId;
        int buyPrice;
        try
        {
            houseId = Integer.valueOf(request.getParameter("house").trim());
            playerId = Integer.valueOf(request.getParameter("player").trim());
            buyPrice = request.getParameter("buy-price").trim().length() == 0 ? -1
                    : Integer.valueOf(request.getParameter("buy-price"));
        }
        catch (Exception e)
        {
            // e.g., one of the fields not filled, or illegal number for buy-price
            return;
        }

        HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseId);
        PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, playerId);
        PlayerroundRecord prr = SqlUtils.getLastPlayerRound(data, player.getId());
        if (buyPrice == -1)
            buyPrice = house.getPrice();
        int marketPrice = house.getPrice();
        // TODO: discount if area has been flooded

        String content = "";
        if (Math.abs(buyPrice - marketPrice) > marketPrice * 0.1)
            content += "Buying price " + buyPrice + " is more than 10% different from market price: " + marketPrice + "<br />";
        int maxSpend = prr.getMaximumMortgage() + prr.getStartSavings();
        if (buyPrice > maxSpend)
            content += "Buying price " + buyPrice + " is more than the max mortgage + savings of the player: " + maxSpend
                    + "<br />";
        if (content.length() == 0)
            content = "The player can afford the house, and the price is within reasonable bounds<br />";
        content += "<br />Are you sure you want to have the player buy the house?<br /><br />";
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("house", "" + houseId);
        parameterMap.put("player-round", "" + prr.getId());
        parameterMap.put("buy-price", "" + buyPrice);
        ModalWindowUtils.make2ButtonModalWindow(data, "Confirm house allocation?", content, "YES", "buy-house-ok", "NO", "", "",
                parameterMap);
        data.setShowModalWindow(1);
    }

    public static void handleHouseBuy(final FacilitatorData data, final HttpServletRequest request)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        int houseId;
        int playerRoundId;
        int buyPrice;
        try
        {
            System.out.println(request.getParameterMap().keySet());
            houseId = Integer.valueOf(request.getParameter("house").trim());
            playerRoundId = Integer.valueOf(request.getParameter("player-round").trim());
            buyPrice = request.getParameter("buy-price").trim().length() == 0 ? -1
                    : Integer.valueOf(request.getParameter("buy-price"));
        }
        catch (Exception e)
        {
            System.err.println("Error in processing houseBuy, message: " + e.getMessage());
            return;
        }

        System.out.println("handleHouseBuy");
        System.out.println("house = " + houseId);
        System.out.println("playerRound = " + playerRoundId);
        System.out.println("buyPrice = " + buyPrice);

        // TODO: handle house buy properly with new code

        /*-
        HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseId);
        PlayerroundRecord prr = SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, playerRoundId);

        prr.setFinalHouseId(house.getId());
        prr.setHousePriceBought(buyPrice);
        if (buyPrice > prr.getMaximumMortgage())
        {
            // use some savings
            int savings = prr.getSavings();
            savings -= (buyPrice - prr.getMaximumMortgage());
            prr.setSpentSavingsForBuyingHouse(buyPrice - prr.getMaximumMortgage());
            // if savings are negative, add to debt, and set savings to 0
            if (savings >= 0)
                prr.setSavings(savings);
            else
                prr.setDebt(prr.getDebt() - savings);
        }
        prr.store();

        if (buyPrice != house.getPrice())
        {
            // make bid record
            BidRecord bid = dslContext.newRecord(Tables.BID);
            bid.setPrice(buyPrice);
            bid.setHouseId(house.getId());
            bid.setGrouproundId(prr.getGrouproundId());
            bid.store();
        }

        */
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
