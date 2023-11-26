package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

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

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;

@WebServlet("/facilitator-house")
public class FacilitatorHouse extends HttpServlet
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

        makeHouseTable(data);

        response.sendRedirect("jsp/facilitator/house.jsp");
    }

    public static void makeHouseTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("                <thead>");
        s.append("                  <tr>");
        s.append("                    <th>Address</th>");
        s.append("                    <th>Available round</th>");
        s.append("                    <th>Initial price</th>");
        s.append("                    <th>Rating</th>");
        s.append("                    <th>Adjusted price</th>");
        s.append("                    <th>Owner</th>");
        s.append("                    <th>Measures</th>");
        s.append("                  </tr>");
        s.append("                </thead>");
        s.append("                <tbody>");
        Result<org.jooq.Record> resultList =
                dslContext.fetch("SELECT house.id FROM house INNER JOIN community ON house.community_id=community.id "
                        + "WHERE community.gameversion_id=" + data.getGameVersion().getId() + " ORDER BY available_round, address ASC;");
        for (org.jooq.Record record : resultList)
        {
            int id = Integer.valueOf(record.get(0).toString());
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, id);
            s.append("                  <tr>");
            s.append("                    <td>" + house.getCode() + "</td>");
            s.append("                    <td>" + house.getAvailableRound() + "</td>");
            s.append("                    <td>" + data.k(house.getPrice()) + "</td>");
            s.append("                    <td>" + house.getRating() + "</td>");
            s.append("                    <td>" + "--" + "</td>");
            s.append("                    <td>" + "--" + "</td>");
            s.append("                    <td>" + "--" + "</td>");
            s.append("                  </tr>");
        }
        s.append("                </tbody>");
        data.getContentHtml().put("house/houseTable", s.toString());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
