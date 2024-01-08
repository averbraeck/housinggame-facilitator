package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/reload-tables")
public class ReloadTablesServlet extends HttpServlet
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

        if (request.getParameter("reloadTables") != null)
        {
            response.setContentType("text/plain");
            if (data.getMenuState().equals("Player"))
                TablePlayer.makePlayerTables(data);
            else if (data.getMenuState().equals("House"))
                TableHouse.makeHouseTable(data);
            else if (data.getMenuState().equals("News"))
                TableNews.makeNewsTable(data);
            else if (data.getMenuState().equals("Flood"))
                TableFlood.makeFloodTable(data);
            response.getWriter().write(data.getContentHtml("facilitator/tables"));
            return;
        }

        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
