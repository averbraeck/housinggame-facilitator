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
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.WelfaretypeRecord;

@WebServlet("/facilitator-player")
public class FacilitatorPlayerServlet extends HttpServlet
{

    /** */
    private static final long serialVersionUID = 1L;

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

        makePlayerStateTable(data);
        makePlayerBudgetTable(data);

        response.sendRedirect("jsp/facilitator/player.jsp");
    }

    public static void makePlayerStateTable(final FacilitatorData data)
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
        s.append("                <thead>");
        s.append("                  <tr>");
        s.append("                    <th>Player</th>");
        s.append("                    <th>Round</th>");
        s.append("                    <th>State</th>");
        s.append("                    <th>Satisfaction</th>");
        s.append("                    <th>Income</th>");
        s.append("                    <th>House</th>");
        s.append("                  </tr>");
        s.append("                </thead>");
        s.append("                <tbody>");
        List<PlayerRecord> playerList = dslContext.selectFrom(Tables.PLAYER)
                .where(Tables.PLAYER.GROUP_ID.eq(data.getGroup().getId())).fetch().sortAsc(Tables.PLAYER.CODE);
        for (PlayerRecord player : playerList)
        {
            s.append("                  <tr>");
            s.append("                    <td>" + player.getCode() + "</td>");
            SortedMap<Integer, PlayerroundRecord> playerRoundMap = SqlUtils.getPlayerRoundMap(data, player.getId());
            if (playerRoundMap.isEmpty())
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>0</td>");
                s.append("                    <td>LOGIN</td>");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>");
                s.append("                    <td>" + data.k(welfareType.getIncomePerRound().intValue()) + "</td>");
                s.append("                    <td>--</td>");
            }
            else
            {
                int highestRound = playerRoundMap.lastKey();
                PlayerroundRecord prr = playerRoundMap.get(highestRound);
                s.append("                    <td>" + highestRound + "</td>");
                s.append("                    <td>" + prr.getPlayerState() + "</td>");
                s.append("                    <td>" + prr.getSatisfaction() + "</td>");
                s.append("                    <td>" + data.k(prr.getIncomePerRound().intValue()) + "</td>");
                if (prr.getHouseId() == null)
                    s.append("                    <td>--</td>");
                else
                {
                    HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, prr.getHouseId());
                    s.append("                    <td>" + house.getAddress() + "</td>");
                }
            }
            s.append("                  </tr>");
        }
        s.append("                </tbody>");
        data.getContentHtml().put("player/playerStateTable", s.toString());
    }

    public static void makePlayerBudgetTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("                <thead>");
        s.append("                  <tr>");
        s.append("                    <th>Player</th>");
        s.append("                    <th>Income</th>");
        s.append("                    <th>Living costs</th>");
        s.append("                    <th>Maximum mortgage</th>");
        s.append("                    <th>Savings</th>");
        s.append("                    <th>Debt</th>");
        s.append("                    <th>Preferred rating</th>");
        s.append("                  </tr>");
        s.append("                </thead>");
        s.append("                <tbody>");
        List<PlayerRecord> playerList = dslContext.selectFrom(Tables.PLAYER)
                .where(Tables.PLAYER.GROUP_ID.eq(data.getGroup().getId())).fetch().sortAsc(Tables.PLAYER.CODE);
        for (PlayerRecord player : playerList)
        {
            s.append("                  <tr>");
            s.append("                    <td>" + player.getCode() + "</td>");
            SortedMap<Integer, PlayerroundRecord> playerRoundMap = SqlUtils.getPlayerRoundMap(data, player.getId());
            if (playerRoundMap.isEmpty())
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>" + data.k(welfareType.getIncomePerRound().intValue()) + "</td>");
                s.append("                    <td>" + data.k(welfareType.getLivingCosts().intValue()) + "</td>");
                s.append("                    <td>" + data.k(welfareType.getMaximumMortgage().intValue()) + "</td>");
                s.append("                    <td>" + data.k(welfareType.getInitialMoney().intValue()) + "</td>");
                s.append("                    <td>" + data.k(0) + "</td>");
                s.append("                    <td>" + welfareType.getPreferredHouseRating().intValue() + "</td>");
            }
            else
            {
                int highestRound = playerRoundMap.lastKey();
                PlayerroundRecord prr = playerRoundMap.get(highestRound);
                s.append("                    <td>" + data.k(prr.getIncomePerRound().intValue()) + "</td>");
                s.append("                    <td>" + data.k(prr.getLivingCosts().intValue()) + "</td>");
                s.append("                    <td>" + data.k(prr.getMaximumMortgage().intValue()) + "</td>");
                s.append("                    <td>" + data.k(prr.getSavings().intValue()) + "</td>");
                s.append("                    <td>" + data.k(prr.getDebt().intValue()) + "</td>");
                s.append("                    <td>" + prr.getPreferredHouseRating().intValue() + "</td>");
            }
            s.append("                  </tr>");
        }
        s.append("                </tbody>");
        data.getContentHtml().put("player/playerBudgetTable", s.toString());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
