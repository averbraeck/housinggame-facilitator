package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.SqlUtils;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

@WebServlet("/popup-buy")
public class PopupBuyServlet extends HttpServlet
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
                List<HousetransactionRecord> unapprovedBuyTransactions = TableHouse.getUnapprovedBuyTransactions(data);
                Set<String> houseCodes = new HashSet<>();
                Set<String> doubleHouseCodes = new HashSet<>();
                for (HousetransactionRecord transaction : unapprovedBuyTransactions)
                {
                    HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                    if (houseCodes.contains(hgr.getCode()))
                        doubleHouseCodes.add(hgr.getCode());
                    else
                        houseCodes.add(hgr.getCode());
                }

                String playerCode = SessionUtils.stripQuotes(request.getParameter("playerCode"));
                String transactionIdStr = SessionUtils.stripQuotes(request.getParameter("transactionId"));
                int transactionId = Integer.valueOf(transactionIdStr);
                String approve = SessionUtils.stripQuotes(request.getParameter("approve"));
                HousetransactionRecord transaction = SqlUtils.readRecordFromId(data, Tables.HOUSETRANSACTION, transactionId);
                HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, hgr.getHouseId());
                PlayerroundRecord playerRound =
                        SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
                PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());

                // check if there is anything wrong
                String note = "";
                if (!hgr.getStatus().equals(HouseGroupStatus.AVAILABLE.toString()))
                    note = "HOUSE NOT AVAILABLE!<br />";
                if (doubleHouseCodes.contains(hgr.getCode()))
                    note += "HOUSE BOUGHT TWICE!<br />";
                if (transaction.getPrice() > playerRound.getMaximumMortgage() + playerRound.getSpendableIncome())
                    note += "PLAYER CANNOT AFFORD!<br />";
                if (transaction.getPrice().intValue() != hgr.getMarketValue().intValue())
                    note += "PRICE/BID CHANGE!<br />";

                StringBuilder s = new StringBuilder();

                s.append("<table style=\"border:none; width:100%;\"><tr>\n");
                s.append("  <td style=\"align:left; width:50%;\">\n");
                s.append("    Player code: " + player.getCode() + "<br/>\n");
                s.append("    Maximum mortgage: " + data.k(playerRound.getMaximumMortgage()) + "<br/>\n");
                s.append("    Spendable income: " + data.k(playerRound.getSpendableIncome()) + "<br/>\n");
                int maxHousePrice = playerRound.getMaximumMortgage() + playerRound.getSpendableIncome();
                s.append("    Maximum house price: " + data.k(maxHousePrice) + "<br/>\n");
                s.append("  </td>\n");
                s.append("  <td style=\"align:left; width:50%;\">\n");
                s.append("    House address: " + hgr.getCode() + "<br/>\n");
                s.append("    House market value: " + data.k(hgr.getMarketValue()) + "<br/>\n");
                s.append("    Entered price: " + data.k(transaction.getPrice()) + "<br/>\n");
                s.append("  </td>\n");
                s.append("</tr></table>\n");

                if (note.length() > 0)
                {
                    s.append("<p style=\"color:red; text-align:left;\">\n");
                    s.append(note);
                    s.append("</p>\n");
                }

                s.append("<br/>\n");
                s.append("<div class=\"form-group pmd-textfield\">\n");
                s.append("  <label for=\"comment-buy\">Comment:</label>\n");
                s.append("  <input type=\"text\" id=\"comment-buy\" name=\"comment-buy\" class=\"form-control\" "
                        + "style=\"z-index: 1000 !important;\" />\n");
                s.append("</div>\n");

                if (approve.equals("APPROVE"))
                {
                    String title = "Approve buy of house " + house.getCode() + " by " + playerCode;
                    String content = s.toString();
                    String method = "approveBuy('" + playerCode + "', " + transactionId + ");";
                    ModalWindowUtils.makeModalWindowMethod(data, title, content, "APPROVE", method);
                }
                else
                {
                    String title = "Reject buy of house " + house.getCode() + " by " + playerCode;
                    String content = s.toString();
                    String method = "rejectBuy('" + playerCode + "', " + transactionId + ");";
                    ModalWindowUtils.makeModalWindowMethod(data, title, content, "REJECT", method);
                }
                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in popup-buy: " + e.getMessage());
            }
        }
        else
        {
            System.err.println("approve-buy called, but no playerCode or transactionId");
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
