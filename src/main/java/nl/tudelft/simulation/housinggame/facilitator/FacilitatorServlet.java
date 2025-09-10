package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.FluvialPluvial;
import nl.tudelft.simulation.housinggame.common.GroupState;
import nl.tudelft.simulation.housinggame.common.PlayerState;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GroupstateRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

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

        // If showModalWindow has value 2, it means: refresh (once) INCLUDING modal window.
        if (data.getShowModalWindow() == 2)
        {
            data.setShowModalWindow(1); // wipe next time!
        }
        else
        {
            data.setShowModalWindow(0);
            data.setModalWindowHtml("");
        }

        if (request.getParameter("menu") != null)
            data.setMenuState(request.getParameter("menu"));

        if (request.getParameter("button") != null)
            this.button = request.getParameter("button");
        else
            this.button = "";

        if (request.getParameter("floodRound") != null)
        {
            int floodInfoRoundNumber = Integer.parseInt(request.getParameter("floodRound"));
            data.setFloodInfoRoundNumber(floodInfoRoundNumber);
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
            TablePlayer.makePlayerTables(data);
            data.getContentHtml().put("menuPlayer", "btn btn-primary");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("House"))
        {
            TableHouse.makeHouseTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn btn-primary");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("News"))
        {
            TableNews.makeNewsTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn btn-primary");
            data.getContentHtml().put("menuFlood", "btn");
        }
        else if (data.getMenuState().equals("Flood"))
        {
            TableFlood.makeFloodTable(data);
            data.getContentHtml().put("menuPlayer", "btn");
            data.getContentHtml().put("menuHouse", "btn");
            data.getContentHtml().put("menuNews", "btn");
            data.getContentHtml().put("menuFlood", "btn btn-primary");
        }
    }

    public void handlePressedButton(final FacilitatorData data, final String button, final HttpServletRequest request)
    {
        if (button.equals("start-new-round"))
            ContentNewRound.popupNewRound(data);
        else if (button.equals("start-new-round-ok"))
        {
            ContentNewRound.newRound(data);
            data.getCurrentGroupRound().setGroupState(GroupState.NEW_ROUND.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.NEW_ROUND, "Round=" + data.getCurrentRoundNumber());
            data.setMenuState("Player");
        }
        else if (button.equals("announce-news"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.ANNOUNCE_NEWS.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.ANNOUNCE_NEWS, "");
            data.setMenuState("News");
        }
        else if (button.equals("show-houses"))
        {
            if (data.getCurrentRoundNumber() == 1)
            {
                data.getCurrentGroupRound().setGroupState(GroupState.SHOW_HOUSES_BUY.toString());
                storeGroupState(data, GroupState.SHOW_HOUSES_BUY, "");
            }
            else
            {
                data.getCurrentGroupRound().setGroupState(GroupState.SHOW_HOUSES_SELL.toString());
                storeGroupState(data, GroupState.SHOW_HOUSES_SELL, "");
            }
            data.getCurrentGroupRound().store();
            data.setMenuState("House");
        }
        else if (button.equals("allow-selling"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.ALLOW_SELLING.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.ALLOW_SELLING, "");
            data.setMenuState("House");
        }
        else if (button.equals("finish-selling"))
        {
            popupSellHouses(data);
        }
        else if (button.equals("finish-selling-ok"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.SHOW_HOUSES_BUY.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.SHOW_HOUSES_BUY, "");
            data.setMenuState("House");
        }
        else if (button.equals("allow-buying"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.ALLOW_BUYING.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.ALLOW_BUYING, "");
            data.setMenuState("House");
        }
        else if (button.equals("finish-buying"))
        {
            popupBuyHouses(data);
        }
        else if (button.equals("finish-buying-ok"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.BUYING_FINISHED.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.BUYING_FINISHED, "");
            data.setMenuState("House");
        }
        else if (button.equals("show-taxes"))
        {
            ContentTaxes.calculateTaxes(data);
            data.getCurrentGroupRound().setGroupState(GroupState.SHOW_TAXES.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.SHOW_TAXES, "");
        }
        else if (button.equals("allow-improvements"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.ALLOW_IMPROVEMENTS.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.ALLOW_IMPROVEMENTS, "");
        }
        else if (button.equals("show-survey"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.SHOW_SURVEY.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.SHOW_SURVEY, "");
            data.setMenuState("Player");
        }
        else if (button.equals("complete-survey"))
        {
            popupSurvey(data);
        }
        else if (button.equals("complete-survey-ok"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.SURVEY_COMPLETED.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.SURVEY_COMPLETED, "");
        }
        else if (button.equals("roll-dice"))
        {
            FluvialPluvial fp = ContentFlood.handleDiceRoll(data, request);
            if (fp != null)
            {
                data.getCurrentGroupRound().setGroupState(GroupState.ROLLED_DICE.toString());
                data.getCurrentGroupRound().store();
                storeGroupState(data, GroupState.ROLLED_DICE, "Fluvial=" + fp.fluvial() + "\nPluvial=" + fp.pluvial());
                data.setMenuState("Flood");
            }
        }
        else if (button.equals("show-summary"))
        {
            data.getCurrentGroupRound().setGroupState(GroupState.SHOW_SUMMARY.toString());
            data.getCurrentGroupRound().store();
            storeGroupState(data, GroupState.SHOW_SUMMARY, "");
            data.setMenuState("Player");
        }
    }

    public void prepareAccordionButtons(final FacilitatorData data, final HttpServletRequest request)
    {
        for (String b : new String[] {"start-new-round", "announce-news", "show-houses", "allow-selling", "finish-selling",
                "allow-buying", "finish-buying", "show-taxes", "allow-improvements", "show-survey", "complete-survey",
                "roll-dice", "show-summary"})
        {
            data.putContentHtml("button/" + b, "btn btn-inactive");
            data.putContentHtml("disabled/" + b, "disabled");
        }
        for (String a : new String[] {"round", "news", "houses", "improvements", "survey", "dice", "summary"})
            data.putContentHtml("accordion/" + a, "");

        if (data.getGroupState().eq(GroupState.LOGIN))
        {
            data.putContentHtml("button/start-new-round", "btn btn-primary btn-active");
            data.putContentHtml("accordion/round", "in");
            data.putContentHtml("disabled/start-new-round", "");
        }
        else if (data.getGroupState().eq(GroupState.NEW_ROUND))
        {
            data.putContentHtml("button/announce-news", "btn btn-primary btn-active");
            data.putContentHtml("accordion/round", "in");
            data.putContentHtml("disabled/announce-news", "");
        }
        else if (data.getGroupState().eq(GroupState.ANNOUNCE_NEWS))
        {
            data.putContentHtml("button/show-houses", "btn btn-primary btn-active");
            data.putContentHtml("accordion/news", "in");
            data.putContentHtml("disabled/show-houses", "");
        }
        else if (data.getGroupState().eq(GroupState.SHOW_HOUSES_SELL))
        {
            data.putContentHtml("button/allow-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/allow-selling", "");
        }
        else if (data.getGroupState().eq(GroupState.ALLOW_SELLING))
        {
            data.putContentHtml("button/finish-selling", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/finish-selling", "");
        }
        else if (data.getGroupState().eq(GroupState.SHOW_HOUSES_BUY))
        {
            data.putContentHtml("button/allow-buying", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/allow-buying", "");
        }
        else if (data.getGroupState().eq(GroupState.ALLOW_BUYING))
        {
            data.putContentHtml("button/finish-buying", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/finish-buying", "");
        }
        else if (data.getGroupState().eq(GroupState.BUYING_FINISHED))
        {
            data.putContentHtml("button/show-taxes", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/show-taxes", "");
        }
        else if (data.getGroupState().eq(GroupState.SHOW_TAXES))
        {
            data.putContentHtml("button/allow-improvements", "btn btn-primary btn-active");
            data.putContentHtml("accordion/houses", "in");
            data.putContentHtml("disabled/allow-improvements", "");
        }
        else if (data.getGroupState().eq(GroupState.ALLOW_IMPROVEMENTS))
        {
            data.putContentHtml("button/show-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/improvements", "in");
            data.putContentHtml("disabled/show-survey", "");
        }
        else if (data.getGroupState().eq(GroupState.SHOW_SURVEY))
        {
            data.putContentHtml("button/complete-survey", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
            data.putContentHtml("disabled/complete-survey", "");
        }
        else if (data.getGroupState().eq(GroupState.SURVEY_COMPLETED))
        {
            data.putContentHtml("button/roll-dice", "btn btn-primary btn-active");
            data.putContentHtml("accordion/survey", "in");
            data.putContentHtml("disabled/roll-dice", "");
        }
        else if (data.getGroupState().eq(GroupState.ROLLED_DICE))
        {
            data.putContentHtml("button/show-summary", "btn btn-primary btn-active");
            data.putContentHtml("accordion/dice", "in");
            data.putContentHtml("disabled/show-summary", "");
        }
        else if (data.getGroupState().eq(GroupState.SHOW_SUMMARY))
        {
            if (data.getCurrentRoundNumber() < data.getScenario().getHighestRoundNumber())
            {
                data.putContentHtml("button/start-new-round", "btn btn-primary btn-active");
                data.putContentHtml("accordion/summary", "in");
                data.putContentHtml("disabled/start-new-round", "");
            }
        }
    }

    public void storeGroupState(final FacilitatorData data, final GroupState newState, final String content)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        GrouproundRecord groupRound = data.getCurrentGroupRound();
        GroupstateRecord groupState =
                dslContext.selectFrom(Tables.GROUPSTATE).where(Tables.GROUPSTATE.GROUPROUND_ID.eq(groupRound.getId()))
                        .and(Tables.GROUPSTATE.GROUP_STATE.eq(newState.toString())).fetchAny();
        if (groupState == null)
        {
            groupState = dslContext.newRecord(Tables.GROUPSTATE);
            groupState.setGroupState(newState.toString());
            groupState.setGrouproundId(groupRound.getId());
            groupState.setContent(content);
            groupState.setTimestamp(LocalDateTime.now());
            groupState.store();
        }
    }

    public static void popupSellHouses(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrPlayersWithHouse = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = FacilitatorUtils.getCurrentPlayerRound(data, player.getId());
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
    }

    public void popupBuyHouses(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrPlayersWithHouse = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = FacilitatorUtils.getCurrentPlayerRound(data, player.getId());
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
    }

    public void popupSurvey(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrReadyPlayers = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = FacilitatorUtils.getCurrentPlayerRound(data, player.getId());
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
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
