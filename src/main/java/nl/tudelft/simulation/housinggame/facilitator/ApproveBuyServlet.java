package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

@WebServlet("/approve-buy")
public class ApproveBuyServlet extends HttpServlet
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
            System.out.println(
                    "BUY - " + request.getParameter("approve") + " player " + request.getParameter("playerCode") + ", comment: "
                            + request.getParameter("comment") + ", transactionId = " + request.getParameter("transactionId"));

            try
            {
                String transactionIdStr = SessionUtils.stripQuotes(request.getParameter("transactionId"));
                int transactionId = Integer.valueOf(transactionIdStr);
                String approve = SessionUtils.stripQuotes(request.getParameter("approve"));
                String comment = SessionUtils.stripQuotes(request.getParameter("comment"));
                HousetransactionRecord transaction = SqlUtils.readRecordFromId(data, Tables.HOUSETRANSACTION, transactionId);
                HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, hgr.getHouseId());
                if (approve.equals("APPROVE"))
                {
                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.APPROVED_BUY);
                    transaction.store();

                    PlayerroundRecord prr = SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());

                    hgr.setLastSoldPrice(transaction.getPrice());
                    hgr.setOwnerId(prr.getPlayerId());
                    hgr.setStatus(HouseGroupStatus.OCCUPIED);
                    hgr.store();

                    int price = transaction.getPrice();
                    prr.setHousePriceBought(price);
                    if (price > prr.getMaximumMortgage())
                    {
                        prr.setMortgageHouseEnd(prr.getMaximumMortgage());
                        prr.setMortgageLeftEnd(prr.getMaximumMortgage());
                        prr.setSpentSavingsForBuyingHouse(price - prr.getMaximumMortgage());
                        prr.setSpendableIncome(prr.getSpendableIncome() - prr.getSpentSavingsForBuyingHouse());
                    }
                    else
                    {
                        prr.setMortgageHouseEnd(price);
                        prr.setMortgageLeftEnd(price);
                        prr.setSpentSavingsForBuyingHouse(0);
                    }
                    prr.setMortgagePayment((int) (prr.getMortgageLeftEnd() * data.getMortgagePercentage() / 100.0));
                    prr.setMortgageLeftEnd(prr.getMortgageLeftEnd() - prr.getMortgagePayment());
                    prr.setSpendableIncome(prr.getSpendableIncome() - prr.getMortgagePayment());
                    int phr = prr.getPreferredHouseRating();
                    int hr = house.getRating();
                    prr.setSatisfactionHouseRatingDelta(hr - phr);
                    prr.setPersonalSatisfaction(prr.getPersonalSatisfaction() + hr - phr);
                    prr.setFinalHousegroupId(hgr.getId());
                    prr.store();
                }
                else
                {
                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.REJECTED_BUY);
                    transaction.store();
                }
                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in approve-buy: " + e.getMessage());
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
