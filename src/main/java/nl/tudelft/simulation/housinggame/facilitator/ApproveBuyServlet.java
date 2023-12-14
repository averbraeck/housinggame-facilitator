package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.tudelft.simulation.housinggame.common.HouseRoundStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseroundRecord;
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

        if (request.getParameter("playerCode") != null)
        {
            System.out.println("BUY - " + request.getParameter("approve") + " player " + request.getParameter("playerCode")
                    + ", comment: " + request.getParameter("comment") + ", hrrId = " + request.getParameter("hrrId"));
            try
            {
                // String playerCode = SessionUtils.stripQuotes(request.getParameter("playerCode"));
                String hrrIdStr = SessionUtils.stripQuotes(request.getParameter("hrrId"));
                int hrrId = Integer.valueOf(hrrIdStr);
                String approve = SessionUtils.stripQuotes(request.getParameter("approve"));
                String comment = SessionUtils.stripQuotes(request.getParameter("comment"));
                HouseroundRecord hrr = SqlUtils.readRecordFromId(data, Tables.HOUSEROUND, hrrId);
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, hrr.getHouseId());
                if (approve.equals("APPROVE"))
                {
                    hrr.setBidExplanation(comment);
                    hrr.setHousePriceBought(hrr.getBidPrice());
                    hrr.setStatus(HouseRoundStatus.APPROVED_BUY);
                    hrr.store();

                    PlayerroundRecord prr = SqlUtils.readRecordFromId(data, Tables.PLAYERROUND, hrr.getPlayerroundId());
                    int price = hrr.getBidPrice();
                    if (price > prr.getMaximumMortgage())
                    {
                        prr.setMortgageLeftEnd(prr.getMaximumMortgage());
                        prr.setSpentSavingsForBuyingHouse(price - prr.getMaximumMortgage());
                        prr.setSpendableIncome(prr.getSpendableIncome() - prr.getSpentSavingsForBuyingHouse());
                    }
                    else
                    {
                        prr.setMortgageLeftEnd(price);
                        prr.setSpentSavingsForBuyingHouse(0);
                    }
                    prr.setMortgagePayment(prr.getMortgageLeftEnd() * data.getMortgagePercentage() / 100);
                    prr.setSpendableIncome(prr.getSpendableIncome() - prr.getMortgagePayment());
                    int phr = prr.getPreferredHouseRating();
                    int hr = house.getRating();
                    prr.setSatisfactionHouseRatingDelta(hr - phr);
                    prr.setPersonalSatisfaction(prr.getPersonalSatisfaction() + hr - phr);
                    prr.store();
                }
                else
                {
                    hrr.setBidExplanation(comment);
                    hrr.setStatus(HouseRoundStatus.REJECTED_BUY);
                    hrr.store();
                }
                return;
            }
            catch (Exception e)
            {
                System.err.println("Error in approve-sell: " + e.getMessage());
            }
        }

        System.err.println("approve-buy called, but no playerCode");

        response.sendRedirect("jsp/facilitator/facilitator.jsp");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        doPost(request, response);
    }

}
