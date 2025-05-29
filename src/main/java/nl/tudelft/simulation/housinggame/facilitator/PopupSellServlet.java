package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

@WebServlet("/popup-sell")
public class PopupSellServlet extends HttpServlet
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

        if (request.getParameter("playerCode") != null && request.getParameter("transactionId") != null)
        {
            try
            {
                String playerCode = SessionUtils.stripQuotes(request.getParameter("playerCode"));
                String transactionIdStr = SessionUtils.stripQuotes(request.getParameter("transactionId"));
                int transactionId = Integer.valueOf(transactionIdStr);
                String approve = SessionUtils.stripQuotes(request.getParameter("approve"));
                HousetransactionRecord transaction = FacilitatorUtils.readRecordFromId(data, Tables.HOUSETRANSACTION, transactionId);
                HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, hgr.getHouseId());
                PlayerroundRecord playerRound =
                        FacilitatorUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
                PlayerRecord player = FacilitatorUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());

                // check if there is anything wrong
                String note = "";
                if (!hgr.getStatus().equals(HouseGroupStatus.OCCUPIED.toString()))
                    note = "HOUSE NOT OCCUPIED!<br />";
                if (transaction.getPrice().intValue() != hgr.getMarketValue().intValue())
                    note += "PRICE/BID CHANGE!<br />";

                StringBuilder s = new StringBuilder();

                s.append("Player code: " + player.getCode() + "<br/>\n");
                s.append("House address: " + hgr.getCode() + "<br/>\n");

                if (note.length() > 0)
                {
                    s.append("<p style=\"color:red; text-align:left;\">\n");
                    s.append(note);
                    s.append("</p>\n");
                }

                s.append("<br/>\n");
                s.append("<div class=\"form-group pmd-textfield\">\n");
                s.append("  <label for=\"comment-sell\">Comment:</label>\n");
                s.append("  <input type=\"text\" id=\"comment-sell\" name=\"comment-sell\" class=\"form-control\" "
                        + "style=\"z-index: 1000 !important;\" />\n");
                s.append("</div>\n");

                if (approve.equals("APPROVE"))
                {
                    String title = "Approve sell of house " + house.getCode() + " by " + playerCode;
                    String content = s.toString();
                    String method = "approveSell('" + playerCode + "', " + transactionId + ");";
                    ModalWindowUtils.makeModalWindowMethod(data, title, content, "APPROVE", method);
                }
                else
                {
                    String title = "Reject sell of house " + house.getCode() + " by " + playerCode;
                    String content = s.toString();
                    String method = "rejectSell('" + playerCode + "', " + transactionId + ");";
                    ModalWindowUtils.makeModalWindowMethod(data, title, content, "REJECT", method);
                }
                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in popup-sell: " + e.getMessage());
            }
        }
        else
        {
            System.err.println("approve-sell called, but no playerCode or transactionId");
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
