package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;

@WebServlet("/approve-reject-stay")
public class ApproveRejectStayServlet extends HttpServlet
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

        if (request.getParameter("playerCode") != null)
        {
            try
            {
                System.out.println("STAY - " + request.getParameter("approve") + " player "
                        + request.getParameter("playerCode") + ", comment: " + request.getParameter("comment") + ", hgrId = "
                        + request.getParameter("hgrId"));
                int hgrId = Integer.valueOf(request.getParameter("hgrId"));
                HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, hgrId);

                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in approve-sell: " + e.getMessage());
            }
        }

        System.err.println("approve-sell called, but no playerCode");

        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
