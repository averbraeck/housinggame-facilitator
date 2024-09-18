package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.tudelft.simulation.housinggame.common.CumulativeNewsEffects;
import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

@WebServlet("/approve-reject-sell")
public class ApproveRejectSellServlet extends HttpServlet
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
                    HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
                    HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, hgr.getHouseId());
                    PlayerroundRecord prr = FacilitatorUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
                    Map<Integer, CumulativeNewsEffects> newsEffects = CumulativeNewsEffects
                            .readCumulativeNewsEffects(data.getDataSource(), data.getScenario(), data.getCurrentRoundNumber());

                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.APPROVED_SELL);
                    transaction.store();

                    hgr.setLastSoldPrice(transaction.getPrice());
                    hgr.setOwnerId(null);
                    hgr.setStatus(HouseGroupStatus.AVAILABLE);
                    hgr.store();

                    int price = transaction.getPrice();
                    int mortgageBank = prr.getMortgageLeftStart();
                    int profitOrLoss = price - mortgageBank;
                    prr.setProfitSoldHouse(profitOrLoss);
                    prr.setSpendableIncome(prr.getSpendableIncome() + profitOrLoss);
                    prr.setMortgageHouseEnd(0);
                    prr.setMortgageLeftEnd(0);
                    prr.setFinalHousegroupId(null);
                    prr.setHousePriceSold(price);
                    prr.setSatisfactionHouseMeasures(0);
                    int movePenalty = data.getScenarioParameters().getSatisfactionMovePenalty()
                            + newsEffects.get(house.getCommunityId()).getSatisfactionMoveChange();
                    prr.setSatisfactionMovePenalty(movePenalty);
                    prr.setPersonalSatisfaction(prr.getPersonalSatisfaction() - movePenalty);
                    prr.store();
                }
                else
                {
                    transaction.setComment(comment);
                    transaction.setTransactionStatus(TransactionStatus.REJECTED_SELL);
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
