package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

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

        if (request.getParameter("playerCode") != null && request.getParameter("transactionId") != null)
        {
            try
            {
                String transactionIdStr = SessionUtils.stripQuotes(request.getParameter("transactionId"));
                int transactionId = Integer.valueOf(transactionIdStr);
                String approve = SessionUtils.stripQuotes(request.getParameter("approve"));
                String comment = SessionUtils.stripQuotes(request.getParameter("comment"));
                HousetransactionRecord transaction = FacilitatorUtils.readRecordFromId(data, Tables.HOUSETRANSACTION, transactionId);
                if (approve.equals("APPROVE"))
                {
                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.APPROVED_STAY);
                    transaction.store();

                    PlayerroundRecord prr = FacilitatorUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
                    prr.setMortgagePayment((int) (prr.getMortgageHouseEnd() * data.getMortgagePercentage() / 100.0));
                    prr.setMortgageLeftEnd(prr.getMortgageLeftEnd() - prr.getMortgagePayment());
                    prr.setSpendableIncome(prr.getSpendableIncome() - prr.getMortgagePayment());
                    if (data.getScenarioParameters().getSatisfactionHouseRatingPerRound() != 0)
                    {
                        HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                        HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, hgr.getHouseId());
                        int phr = prr.getPreferredHouseRating();
                        int hr = house.getRating();
                        prr.setSatisfactionHouseRatingDelta(hr - phr);
                        prr.setSatisfactionTotal(prr.getSatisfactionTotal() + hr - phr);
                        prr.setFinalHousegroupId(hgr.getId());
                    }
                    prr.store();
                }
                else
                {
                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.REJECTED_STAY);
                    transaction.store();
                }
                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in approve-buy: " + e.getMessage());
                response.sendRedirect("jsp/facilitator/facilitator.jsp");
                return;
            }
        }

        System.err.println("approve-buy called, but no playerCode or transactionId");
        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
