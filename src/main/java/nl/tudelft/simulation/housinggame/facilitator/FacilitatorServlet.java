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

import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.PlayerState;
import nl.tudelft.simulation.housinggame.common.RoundState;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
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
        if (data == null || data.getScenario() == null)
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
            if (data.getCurrentRoundNumber() == 1)
                data.getCurrentGroupRound().setRoundState(RoundState.SHOW_HOUSES_BUY.toString());
            else
                data.getCurrentGroupRound().setRoundState(RoundState.SHOW_HOUSES_SELL.toString());
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
            data.getCurrentGroupRound().setRoundState(RoundState.SHOW_HOUSES_BUY.toString());
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
            data.putContentHtml("accordion/round", "in");
        }
        else if (data.getRoundState().eq(RoundState.ANNOUNCE_NEWS))
        {
            data.putContentHtml("button/show-houses", "btn btn-primary btn-active");
            data.putContentHtml("accordion/news", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_HOUSES_SELL))
        {
            data.putContentHtml("button/allow-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.ALLOW_SELLING))
        {
            data.putContentHtml("button/finish-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_HOUSES_BUY))
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
            data.putContentHtml("accordion/houses", "in");
        }
        else if (data.getRoundState().eq(RoundState.ALLOW_IMPROVEMENTS))
        {
            data.putContentHtml("button/show-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/improvements", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_SURVEY))
        {
            data.putContentHtml("button/complete-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
        }
        else if (data.getRoundState().eq(RoundState.SURVEY_COMPLETED))
        {
            data.putContentHtml("button/roll-dice", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
        }
        else if (data.getRoundState().eq(RoundState.ROLLED_DICE))
        {
            data.putContentHtml("button/show-summary", "btn btn-primary btn-active");
            data.putContentHtml("accordion/dice", "in");
        }
        else if (data.getRoundState().eq(RoundState.SHOW_SUMMARY))
        {
            if (data.getCurrentRoundNumber() < data.getScenario().getHighestRoundNumber())
            {
                data.putContentHtml("button/start-new-round", "btn btn-primary btn-active");
                data.putContentHtml("accordion/summary", "in");
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

        // make a new groupround
        GrouproundRecord groupRound = dslContext.newRecord(Tables.GROUPROUND);
        groupRound.setRoundState(RoundState.NEW_ROUND.toString());
        groupRound.setRoundNumber(data.getCurrentRoundNumber() + 1);
        groupRound.setGroupId(data.getGroup().getId());
        groupRound.setPluvialFloodIntensity(null);
        groupRound.setFluvialFloodIntensity(null);
        groupRound.store();

        // make houses that are not used unavailable for this round
        List<HousegroupRecord> currentHouseGroupList =
                dslContext.selectFrom(Tables.HOUSEGROUP).where(Tables.HOUSEGROUP.GROUP_ID.eq(data.getGroup().getId())).fetch();
        for (var houseGroup : currentHouseGroupList)
        {
            if (HouseGroupStatus.AVAILABLE.equals(houseGroup.getStatus()))
            {
                houseGroup.setStatus(HouseGroupStatus.NOT_AVAILABLE);
                houseGroup.store();
            }
        }

        // make the new houses available for this round
        List<Integer> houseIdList = dslContext
                .selectFrom(Tables.HOUSE.innerJoin(Tables.COMMUNITY).on(Tables.HOUSE.COMMUNITY_ID.eq(Tables.COMMUNITY.ID)))
                .where(Tables.COMMUNITY.GAMEVERSION_ID.eq(data.getGameVersion().getId()))
                .and(Tables.HOUSE.AVAILABLE_ROUND.eq(groupRound.getRoundNumber())).fetch(Tables.HOUSE.ID);
        for (int houseId : houseIdList)
        {
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseId);
            HousegroupRecord houseGroup = dslContext.newRecord(Tables.HOUSEGROUP);
            CommunityRecord community = SqlUtils.readRecordFromId(data, Tables.COMMUNITY, house.getCommunityId());
            houseGroup.setCode(house.getCode());
            houseGroup.setAdress(house.getAddress());
            houseGroup.setRating(house.getRating());
            houseGroup.setOriginalPrice(house.getPrice());
            houseGroup.setDamageReduction(0);
            houseGroup.setMarketValue(house.getPrice());
            houseGroup.setLastSoldPrice(null);
            houseGroup.setHouseSatisfaction(0);
            houseGroup.setStatus(HouseGroupStatus.AVAILABLE);
            houseGroup.setFluvialBaseProtection(community.getFluvialProtection());
            houseGroup.setPluvialBaseProtection(community.getPluvialProtection());
            houseGroup.setFluvialHouseProtection(0);
            houseGroup.setPluvialHouseProtection(0);
            houseGroup.setLastRoundCommFluvial(null);
            houseGroup.setLastRoundCommPluvial(null);
            houseGroup.setLastRoundHouseFluvial(null);
            houseGroup.setLastRoundHousePluvial(null);
            houseGroup.setHouseId(house.getId());
            houseGroup.setGroupId(data.getGroup().getId());
            houseGroup.setOwnerId(null);
            houseGroup.store();
            List<InitialhousemeasureRecord> initialMeasureList = dslContext.selectFrom(Tables.INITIALHOUSEMEASURE)
                    .where(Tables.INITIALHOUSEMEASURE.HOUSE_ID.eq(house.getId())).fetch();
            for (InitialhousemeasureRecord initialMeasure : initialMeasureList)
            {
                if (initialMeasure.getRoundNumber() <= groupRound.getRoundNumber())
                {
                    var measureType = SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, initialMeasure.getMeasuretypeId());
                    createMeasureForHouse(data, houseGroup, measureType, groupRound.getRoundNumber());
                }
            }
        }

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
                    if (playerRound.getStartHousegroupId() != null)
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
                    if (playerRound.getFinalHousegroupId() != null)
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

        ModalWindowUtils.make2ButtonModalWindow(data, "Move to dice rolls?", content, "YES", "complete-survey-ok", "NO", "",
                "");
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
                s.append("                    <td>" + data.k(welfareType.getInitialMoney()) + "</td>\n");
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
                PlayerroundRecord prrPrev = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                    {
                        prrPrev = prr;
                        prr = playerRoundList.get(i);
                        highestRound = i;
                    }
                }
                int currentHouseSatisfaction = 0;
                if (prr.getFinalHousegroupId() != null)
                {
                    HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getFinalHousegroupId());
                    currentHouseSatisfaction = hgr.getHouseSatisfaction();
                }
                else if (prr.getStartHousegroupId() != null)
                {
                    HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getStartHousegroupId());
                    currentHouseSatisfaction = hgr.getHouseSatisfaction();
                }

                s.append("                    <td>" + highestRound + "</td>\n");
                s.append("                    <td>" + prr.getPlayerState() + "</td>\n");

                HouseRecord house = data.getApprovedHouseForPlayerRound(prr);
                if (house == null)
                {
                    s.append("                    <td>--</td>\n");
                    s.append("                    <td>--</td>\n");
                }
                else
                {
                    s.append("                    <td>" + house.getCode() + "</td>\n");
                    s.append("                    <td>" + data.k(data.getExpectedTaxes(house)) + "</td>\n");
                }
                s.append("                    <td>" + data.k(prr.getSpendableIncome()) + "</td>\n");
                int netSatisfaction = prr.getPersonalSatisfaction() - prr.getSatisfactionFluvialPenalty()
                        - prr.getSatisfactionPluvialPenalty() - prr.getSatisfactionDebtPenalty();
                s.append("                    <td>" + netSatisfaction + "</td>\n");
                if (house == null)
                    s.append("                    <td>--</td>\n");
                else
                    s.append("                    <td>" + currentHouseSatisfaction + "</td>\n");
                if (prrPrev.getSpendableIncome() >= 0)
                    s.append("                    <td>-</td>\n");
                else
                    s.append("                    <td>-" + spr.getSatisfactionDebtPenalty() + "</td>\n");
                if (prr.getSatisfactionFluvialPenalty() + prr.getSatisfactionPluvialPenalty() == 0)
                    s.append("                    <td>--</td>\n");
                else
                    s.append("                    <td>"
                            + (prr.getSatisfactionFluvialPenalty() + prr.getSatisfactionPluvialPenalty()) + "</td>\n");
                s.append("                    <td>" + (prr.getPersonalSatisfaction() + currentHouseSatisfaction) + "</td>\n");
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
        s.append("                    <th>Start<br/>savings</th>\n");
        s.append("                    <th>Start<br/>debt</th>\n");
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
                PlayerroundRecord prrPrev = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                    {
                        prrPrev = prr;
                        prr = playerRoundList.get(i);
                    }
                }
                s.append("                    <td>" + data.k(prr.getRoundIncome()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getLivingCosts()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getMaximumMortgage()) + "</td>\n");
                s.append("                    <td>"
                        + data.k(prrPrev.getSpendableIncome() > 0 ? prrPrev.getSpendableIncome() : 0) + "</td>\n");
                s.append("                    <td>"
                        + data.k(prrPrev.getSpendableIncome() < 0 ? -prrPrev.getSpendableIncome() : 0) + "</td>\n");
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
        s.append("                    <th>Available<br/>round</th>\n");
        s.append("                    <th>Rating</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Buy<br/>price</th>\n");
        s.append("                    <th>Comment</th>\n");
        s.append("                    <th>House<br/>satisf.</th>\n");
        s.append("                    <th>Measures</th>\n");
        s.append("                    <th>Owner</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        var houseGroupList = data.getHouseGroupList();
        for (var houseGroup : houseGroupList)
        {
            if (HouseGroupStatus.isAvailableOrOccupied(houseGroup.getStatus()))
            {
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
                List<MeasureRecord> measureList = dslContext.selectFrom(Tables.MEASURE)
                        .where(Tables.MEASURE.HOUSEGROUP_ID.eq(houseGroup.getId())).fetch();
                s.append("                  <tr style=\"text-align:center;\">\n");
                s.append("                    <td>" + house.getCode() + "</td>\n");
                s.append("                    <td>" + house.getAvailableRound() + "</td>\n");
                s.append("                    <td>" + house.getRating() + "</td>\n");
                int marketPrice = houseGroup.getMarketValue();
                s.append("                    <td>" + data.k(marketPrice) + "</td>\n");
                Integer buyPrice = houseGroup.getLastSoldPrice();
                if (buyPrice == null)
                    s.append("                    <td>" + "--" + "</td>\n");
                else
                    s.append("                    <td>" + data.k(buyPrice) + "</td>\n");
                String comment = ""; // TODO
                s.append("                    <td>" + comment + "</td>\n");
                int houseSatisfaction = 0; // TODO
                s.append("                    <td>" + houseSatisfaction + "</td>\n");
                if (measureList.size() == 0)
                    s.append("                    <td>" + "--" + "</td>\n");
                else
                {
                    s.append("                    <td style=\"text-align:left;\">");
                    boolean first = true;
                    for (MeasureRecord measure : measureList)
                    {
                        MeasuretypeRecord measureType =
                                SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, measure.getMeasuretypeId());
                        if (!first)
                            s.append("<br />");
                        first = false;
                        s.append(measureType.getName());
                    }
                    s.append("</td>\n");
                }
                if (houseGroup.getOwnerId() != null)
                {
                    PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, houseGroup.getOwnerId());
                    s.append("                    <td>" + player.getCode() + "</td>\n");
                }
                else
                    s.append("                    <td>" + "--" + "</td>\n");
                s.append("                  </tr>\n");
            }
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");

        s.append(makeHouseBuyDecisionTable(data));
        s.append(makeHouseSellDecisionTable(data));
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static String makeHouseBuyDecisionTable(final FacilitatorData data)
    {
        // get the players with an unapproved buy for a house
        List<HousetransactionRecord> unapprovedBuyTransactions = getUnapprovedBuyTransactions(data);
        if (unapprovedBuyTransactions.size() == 0)
            return "";

        StringBuilder s = new StringBuilder();
        s.append("        <h1>Buying requests</h1>");
        s.append("        <p>Ensure all players have sufficient time to send their requests before handling them.</p>");
        s.append("        <p>This way, requests that need the facilitator's attention can be flagged in red.</p>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Maximum<br/>mortgage</th>\n");
        s.append("                    <th>Savings<br/>/ Debt</th>\n");
        s.append("                    <th>Maximum<br/>price</th>\n");
        s.append("                    <th>Selected<br/>house</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Player's buy<br/>/bid price</th>\n");
        s.append("                    <th>Comment</th>\n");
        s.append("                    <th>Approval</th>\n");
        s.append("                    <th>Rejection</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        for (HousetransactionRecord transaction : unapprovedBuyTransactions)
        {
            HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
            PlayerroundRecord playerRound = SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
            PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getMaximumMortgage()) + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getSpendableIncome()) + "</td>\n");
            int maxHousePrice = playerRound.getMaximumMortgage() + playerRound.getSpendableIncome();
            s.append("                    <td>" + data.k(maxHousePrice) + "</td>\n");
            s.append("                    <td>" + hgr.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(hgr.getMarketValue()) + "</td>\n");
            s.append("                    <td>" + data.k(transaction.getPrice()) + "</td>\n");
            s.append("                    <td><input type='text' class='buy-comment' name='comment-" + player.getCode()
                    + "' id='comment-" + player.getCode() + "' /></td>\n");
            s.append("                    <td><button name='approve-" + player.getCode() + "' id='approve-" + player.getCode()
                    + "' onclick='approveBuy(\"" + player.getCode() + "\", " + transaction.getId() + ")'>APPROVE</button></td>\n");
            s.append("                    <td><button name='reject-" + player.getCode() + "' id='reject-" + player.getCode()
                    + "' onclick='rejectBuy(\"" + player.getCode() + "\", " + transaction.getId() + ")'>REJECT</button></td>\n");
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        return s.toString();
    }

    public static String makeHouseSellDecisionTable(final FacilitatorData data)
    {
        // get the players with an unapproved buy for a house
        List<HousetransactionRecord> unapprovedSellStayTransactions = getUnapprovedSellStayTransactions(data);
        if (unapprovedSellStayTransactions.size() == 0)
            return "";

        StringBuilder s = new StringBuilder();
        s.append("        <h1>Selling / staying requests</h1>");
        s.append("        <p>Ensure all players have sufficient time to send their requests before handling them.</p>");
        s.append("        <p>This way, requests that need the facilitator's attention can be flagged in red.</p>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>House</th>\n");
        s.append("                    <th>Decision</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Bought<br/>price</th>\n");
        s.append("                    <th>Sell<br/>price</th>\n");
        s.append("                    <th>Moving<br/>reason</th>\n");
        s.append("                    <th>Comment</th>\n");
        s.append("                    <th>Approval</th>\n");
        s.append("                    <th>Rejection</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        for (HousetransactionRecord transaction : unapprovedSellStayTransactions)
        {
            HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
            PlayerroundRecord playerRound = SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
            PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getMaximumMortgage()) + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getSpendableIncome()) + "</td>\n");
            int maxHousePrice = playerRound.getMaximumMortgage() + playerRound.getSpendableIncome();
            s.append("                    <td>" + data.k(maxHousePrice) + "</td>\n");
            s.append("                    <td>" + hgr.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(hgr.getMarketValue()) + "</td>\n");
            s.append("                    <td>" + transaction.getPrice() + "</td>\n");
            s.append("                    <td><input type='text' name='comment-" + player.getCode() + "' id='comment-"
                    + player.getCode() + "' /></td>\n");
            s.append("                    <td><button name='approve-" + player.getCode() + "' id='approve-" + player.getCode()
                    + "' onclick='approveSell(\"" + player.getCode() + "\", " + transaction.getId() + ")'>APPROVE</button></td>\n");
            s.append("                    <td><button name='reject-" + player.getCode() + "' id='reject-" + player.getCode()
                    + "' onclick='rejectSell(\"" + player.getCode() + "\", " + transaction.getId() + ")'>REJECT</button></td>\n");
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        return s.toString();
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

    private static List<HousetransactionRecord> getUnapprovedBuyTransactions(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.eq(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_BUY)))
                .fetch();
    }

    private static List<HousetransactionRecord> getUnapprovedSellStayTransactions(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        var sellList = dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.eq(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_SELL)))
                .fetch();
        var stayList = dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.eq(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_STAY)))
                .fetch();
        List<HousetransactionRecord> sellStayList = new ArrayList<>();
        sellStayList.addAll(sellList);
        sellStayList.addAll(stayList);
        return sellStayList;
    }

    /**
     * Only return APPROVED houses for the final_houseround_id (COPIED, APPROVED_STAY and APPROVED_BUY). Note that we HAVE to
     * look at the player's latest round data -- if a player is a round behind the rest of the group, for whatever reason, we
     * still have to retrieve the house!
     * @param data PlayerData
     * @return Map of houseId to PlayerRecord
     */
    public static Map<Integer, PlayerRecord> getPlayersForApprovedHouseIds(final FacilitatorData data)
    {
        Map<Integer, PlayerRecord> playersForApprovedHouseIds = new HashMap<>();
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
                    HouseRecord house = data.getApprovedHouseForPlayerRound(prr);
                    if (prr != null && house != null)
                        playersForApprovedHouseIds.put(house.getId(), player);
                }
            }
        }
        return playersForApprovedHouseIds;
    }

    public static List<PlayerRecord> getPlayersWithoutHouse(final FacilitatorData data)
    {
        List<PlayerRecord> playersWithoutHouse = new ArrayList<>();
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (playerRoundList.isEmpty())
                playersWithoutHouse.add(player);
            else
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                if (prr != null)
                {
                    for (int i = 0; i < playerRoundList.size(); i++)
                    {
                        if (playerRoundList.get(i) != null)
                            prr = playerRoundList.get(i);
                    }
                    if (prr.getFinalHousegroupId() == null)
                        playersWithoutHouse.add(player);
                }
            }
        }
        return playersWithoutHouse;
    }

    public static List<PlayerroundRecord> getPlayerRoundsWithUnapprovedBuy(final FacilitatorData data)
    {
        List<PlayerroundRecord> playerRoundsUnapprovedBuy = new ArrayList<>();
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
                    if (prr.getFinalHousegroupId() != null)
                    {
                        HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getFinalHousegroupId());
                        if (hgr.getStatus().equals(TransactionStatus.UNAPPROVED_BUY))
                            playerRoundsUnapprovedBuy.add(prr);
                    }
                }
            }
        }
        return playerRoundsUnapprovedBuy;
    }

    public static List<PlayerroundRecord> getPlayerRoundsWithUnapprovedSellStay(final FacilitatorData data)
    {
        List<PlayerroundRecord> playerRoundsUnapprovedSellStay = new ArrayList<>();
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
                    if (prr.getFinalHousegroupId() != null)
                    {
                        HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getFinalHousegroupId());
                        if (hgr.getStatus().equals(TransactionStatus.UNAPPROVED_SELL)
                                || hgr.getStatus().equals(TransactionStatus.UNAPPROVED_STAY))
                            playerRoundsUnapprovedSellStay.add(prr);
                    }
                }
            }
        }
        return playerRoundsUnapprovedSellStay;
    }

    public static List<PlayerRecord> getActivePlayersWithoutHouse(final FacilitatorData data)
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
                    if (prr.getFinalHousegroupId() == null)
                        playersWithoutHouse.add(player);
                }
            }
        }
        return playersWithoutHouse;
    }

    public static List<HouseRecord> getAvailableHousesForRound(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        Map<Integer, PlayerRecord> playersForOwnedHouseIds = getPlayersForApprovedHouseIds(data);
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
                    HouseRecord house = data.getApprovedHouseForPlayerRound(prr);
                    if (house != null)
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
            HousegroupRecord houseRound =
                    dslContext.selectFrom(Tables.HOUSEGROUP).where(Tables.HOUSEGROUP.GROUP_ID.eq(data.getGroup().getId()))
                            .and(Tables.HOUSEGROUP.HOUSE_ID.eq(houseId)).fetchAny();
            if (houseRound != null)
            {
                List<MeasureRecord> mtrList = dslContext.selectFrom(Tables.MEASURE)
                        .where(Tables.MEASURE.HOUSEGROUP_ID.eq(houseRound.getId())).fetch();
                measureList.addAll(mtrList);
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
        int maxSpend = prr.getMaximumMortgage() + prr.getSpendableIncome();
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

    private static void createMeasureForHouse(final FacilitatorData data, final HousegroupRecord houseGroup,
            final MeasuretypeRecord measureType, final int roundNumber)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        MeasureRecord measure = dslContext.newRecord(Tables.MEASURE);
        measure.setRoundNumber(roundNumber);
        measure.setConsumedInRound(null);
        measure.setMeasuretypeId(measureType.getId());
        measure.setHousegroupId(houseGroup.getId());
        measure.store();

        houseGroup.setFluvialHouseProtection(houseGroup.getFluvialHouseProtection() + measureType.getFluvialProtectionDelta());
        houseGroup.setPluvialHouseProtection(houseGroup.getPluvialHouseProtection() + measureType.getPluvialProtectionDelta());
        houseGroup.setHouseSatisfaction(houseGroup.getHouseSatisfaction() + measureType.getSatisfactionDelta());
        houseGroup.store();
    }

    public static void createMeasureForHouse(final FacilitatorData data, final HousegroupRecord houseGroup,
            final InitialhousemeasureRecord ihmr)
    {
        MeasuretypeRecord measureType = SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, ihmr.getMeasuretypeId());
        createMeasureForHouse(data, houseGroup, measureType, ihmr.getRoundNumber());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
