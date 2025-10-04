package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.CalcPlayerState;
import nl.tudelft.simulation.housinggame.common.GroupState;
import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.SqlUtils;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.TaxRecord;

@WebServlet("/approve-reject-buy")
public class ApproveRejectBuyServlet extends HttpServlet
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
                    }
                    else
                    {
                        prr.setMortgageHouseEnd(price);
                        prr.setMortgageLeftEnd(price);
                        prr.setSpentSavingsForBuyingHouse(0);
                    }
                    prr.setMortgagePayment((int) (prr.getMortgageLeftEnd() * data.getMortgagePercentage() / 100.0));
                    prr.setMortgageLeftEnd(prr.getMortgageLeftEnd() - prr.getMortgagePayment());
                    int phr = prr.getPreferredHouseRating();
                    int hr = house.getRating();
                    prr.setSatisfactionHouseRatingDelta(hr - phr);
                    // recalculate player satisfaction and income
                    CalcPlayerState.calculatePlayerRoundTotals(data, prr);
                    prr.setFinalHousegroupId(hgr.getId());
                    prr.store();

                    // calculate taxes if round has already moved beyond tax calculation
                    if (GroupState.valueOf(data.getCurrentGroupRound().getGroupState()).nr >= GroupState.BUYING_FINISHED.nr)
                        calculateTaxes(data, prr);
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

    public void calculateTaxes(final FacilitatorData data, final PlayerroundRecord prr)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        Map<CommunityRecord, Integer> communityMap = new HashMap<>();
        Map<CommunityRecord, Integer> taxMap = new HashMap<>();
        for (var playerRound : data.getPlayerRoundList())
        {
            if (playerRound.getFinalHousegroupId() != null)
            {
                HousegroupRecord houseGroup =
                        SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, playerRound.getFinalHousegroupId());
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
                CommunityRecord community = SqlUtils.readRecordFromId(data, Tables.COMMUNITY, house.getCommunityId());
                if (communityMap.containsKey(community))
                    communityMap.put(community, communityMap.get(community) + 1);
                else
                    communityMap.put(community, 1);
            }
        }

        for (var community : communityMap.keySet())
        {
            List<TaxRecord> taxList =
                    dslContext.selectFrom(Tables.TAX).where(Tables.TAX.COMMUNITY_ID.eq(community.getId())).fetch();
            taxMap.put(community, 1000);
            for (TaxRecord tax : taxList)
            {
                int nr = communityMap.get(community);
                if (nr >= tax.getMinimumInhabitants() && nr <= tax.getMaximumInhabitants())
                {
                    taxMap.put(community, tax.getTaxCost().intValue());
                    break;
                }
            }
        }

        if (prr.getFinalHousegroupId() != null)
        {
            HousegroupRecord houseGroup = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getFinalHousegroupId());
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
            CommunityRecord community = SqlUtils.readRecordFromId(data, Tables.COMMUNITY, house.getCommunityId());
            int taxCost = taxMap.get(community);
            prr.setCostTaxes(taxCost);
            // recalculate player satisfaction and income
            CalcPlayerState.calculatePlayerRoundTotals(data, prr);
            prr.store();
        }
    }

}
