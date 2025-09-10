package nl.tudelft.simulation.housinggame.facilitator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.TransactionStatus;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousemeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousetransactionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasuretypeRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MovingreasonRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

/**
 * TableHouse creates and fills the house table.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TableHouse
{

    public static void makeHouseTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("        <h1>Available houses in this round</h1>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Address</th>\n");
        s.append("                    <th>Available<br/>round</th>\n");
        s.append("                    <th>Rating</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Buy<br/>price</th>\n");
        s.append("                    <th>House<br/>satisf.</th>\n");
        s.append("                    <th>Measures</th>\n");
        s.append("                    <th>Owner</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        var houseGroupList = data.getHouseGroupList();
        for (var houseGroup : houseGroupList)
        {
            if (HouseGroupStatus.isAvailableOrOccupied(houseGroup.getStatus()))
            {
                HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
                List<HousemeasureRecord> measureList = dslContext.selectFrom(Tables.HOUSEMEASURE)
                        .where(Tables.HOUSEMEASURE.HOUSEGROUP_ID.eq(houseGroup.getId())).fetch();
                s.append("                  <tr style=\"text-align:center;\">\n");
                s.append("                    <td>" + house.getCode() + "</td>\n");
                s.append("                    <td>" + house.getAvailableRound() + "</td>\n");
                s.append("                    <td>" + house.getRating() + "</td>\n");
                int marketPrice = houseGroup.getMarketValue();
                s.append("                    <td>" + data.k(marketPrice) + "</td>\n");
                Integer buyPrice = houseGroup.getLastSoldPrice();
                if (buyPrice == null)
                    s.append("                    <td>" + "--" + "</td>\n");
                else
                    s.append("                    <td>" + data.k(buyPrice) + "</td>\n");
                int houseSatisfaction = houseGroup.getHouseSatisfaction();
                s.append("                    <td>" + houseSatisfaction + "</td>\n");
                if (measureList.size() == 0)
                    s.append("                    <td>" + "--" + "</td>\n");
                else
                {
                    s.append("                    <td style=\"text-align:left;\">");
                    boolean first = true;
                    for (HousemeasureRecord measure : measureList)
                    {
                        MeasuretypeRecord measureType =
                                FacilitatorUtils.readRecordFromId(data, Tables.MEASURETYPE, measure.getMeasuretypeId());
                        if (!first)
                            s.append("<br />");
                        first = false;
                        s.append(measureType.getName());
                    }
                    s.append("</td>\n");
                }
                if (houseGroup.getOwnerId() != null)
                {
                    PlayerRecord player = FacilitatorUtils.readRecordFromId(data, Tables.PLAYER, houseGroup.getOwnerId());
                    s.append("                    <td>" + player.getCode() + "</td>\n");
                }
                else
                    s.append("                    <td>" + "--" + "</td>\n");
                s.append("                  </tr>\n");
            }
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");

        s.append(makeHouseBuyDecisionTable(data));
        s.append(makeHouseSellDecisionTable(data));
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    public static String makeHouseBuyDecisionTable(final FacilitatorData data)
    {
        // get the players with an unapproved buy for a house
        List<HousetransactionRecord> unapprovedBuyTransactions = getUnapprovedBuyTransactions(data);
        if (unapprovedBuyTransactions.size() == 0)
            return "";

        StringBuilder s = new StringBuilder();
        s.append("        <h1>Buying requests</h1>");
        s.append("        <p>Ensure all players have sufficient time to send their requests before handling them.<br />");
        s.append("        This way, requests that need the facilitator's attention can be flagged as a note in red.</p>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Maximum<br/>mortgage</th>\n");
        s.append("                    <th>Savings<br/>/ Debt</th>\n");
        s.append("                    <th>Maximum<br/>price</th>\n");
        s.append("                    <th>Selected<br/>house</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Player's buy<br/>/bid price</th>\n");
        s.append("                    <th>Note</th>\n");
        s.append("                    <th>Approval</th>\n");
        s.append("                    <th>Rejection</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        Set<String> houseCodes = new HashSet<>();
        Set<String> doubleHouseCodes = new HashSet<>();
        for (HousetransactionRecord transaction : unapprovedBuyTransactions)
        {
            HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
            if (houseCodes.contains(hgr.getCode()))
                doubleHouseCodes.add(hgr.getCode());
            else
                houseCodes.add(hgr.getCode());
        }

        for (HousetransactionRecord transaction : unapprovedBuyTransactions)
        {
            HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
            PlayerroundRecord playerRound = FacilitatorUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
            PlayerRecord player = FacilitatorUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());

            // check if there is anything wrong
            String note = "";
            boolean noApprove = false;
            if (!hgr.getStatus().equals(HouseGroupStatus.AVAILABLE.toString()))
            {
                note = "HOUSE NOT AVAILABLE!";
                noApprove = true;
            }
            else if (doubleHouseCodes.contains(hgr.getCode()))
            {
                note = "HOUSE BOUGHT TWICE!";
                noApprove = true;
            }
            else if (transaction.getPrice() > playerRound.getMaximumMortgage() + playerRound.getSpendableIncome())
                note = "PLAYER CANNOT AFFORD!";
            else if (transaction.getPrice().intValue() != hgr.getMarketValue().intValue())
                note = "PRICE/BID CHANGE!";

            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getMaximumMortgage()) + "</td>\n");
            s.append("                    <td>" + data.k(playerRound.getSpendableIncome()) + "</td>\n");
            int maxHousePrice = playerRound.getMaximumMortgage() + playerRound.getSpendableIncome();
            s.append("                    <td>" + data.k(maxHousePrice) + "</td>\n");
            s.append("                    <td>" + hgr.getCode() + "</td>\n");
            s.append("                    <td>" + data.k(hgr.getMarketValue()) + "</td>\n");
            s.append("                    <td>" + data.k(transaction.getPrice()) + "</td>\n");
            if (note.length() == 0)
                s.append("                    <td style=\"color:green; text-align:left;\">OK</td>\n");
            else
                s.append("                    <td style=\"color:red; text-align:left;\">" + note + "</td>\n");
            if (noApprove)
                s.append("                    <td><button name='approve-" + player.getCode() + "' id='approve-"
                        + player.getCode() + "' style=\"color:grey;\" disabled>APPROVE</button></td>\n");
            else
                s.append("                    <td><button name=\"approve-" + player.getCode() + "\" id=\"approve-"
                        + player.getCode() + "\" onclick=\"popupApproveBuy('" + player.getCode() + "', " + transaction.getId()
                        + ");\">APPROVE</button></td>\n");
            s.append("                    <td><button name=\"reject-" + player.getCode() + "\" id=\"reject-" + player.getCode()
                    + "\" onclick=\"popupRejectBuy('" + player.getCode() + "', " + transaction.getId()
                    + ");\">REJECT</button></td>\n");
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        return s.toString();
    }

    public static String makeHouseSellDecisionTable(final FacilitatorData data)
    {
        // get the players with an unapproved buy for a house
        List<HousetransactionRecord> unapprovedSellStayTransactions = getUnapprovedSellStayTransactions(data);
        if (unapprovedSellStayTransactions.size() == 0)
            return "";

        StringBuilder s = new StringBuilder();
        s.append("        <h1>Selling / staying requests</h1>");
        s.append("        <p>Ensure all players have sufficient time to send their requests before handling them.<br />");
        s.append("        This way, requests that need the facilitator's attention can be flagged as a note in red.</p>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>House</th>\n");
        s.append("                    <th>Decision</th>\n");
        s.append("                    <th>Market<br/>price</th>\n");
        s.append("                    <th>Bought<br/>price</th>\n");
        s.append("                    <th>Sell<br/>price</th>\n");
        s.append("                    <th>Moving<br/>reason</th>\n");
        s.append("                    <th>Approval</th>\n");
        s.append("                    <th>Rejection</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        for (HousetransactionRecord transaction : unapprovedSellStayTransactions)
        {
            HousegroupRecord hgr = FacilitatorUtils.readRecordFromId(data, Tables.HOUSEGROUP, transaction.getHousegroupId());
            PlayerroundRecord playerRound = FacilitatorUtils.readRecordFromId(data, Tables.PLAYERROUND, transaction.getPlayerroundId());
            PlayerRecord player = FacilitatorUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            s.append("                    <td>" + hgr.getCode() + "</td>\n");
            String decision = (transaction.getTransactionStatus().equals(TransactionStatus.UNAPPROVED_STAY)) ? "STAY" : "SELL";
            s.append("                    <td>" + decision + "</td>\n");
            s.append("                    <td>" + data.k(hgr.getMarketValue()) + "</td>\n");
            s.append("                    <td>" + data.k(hgr.getLastSoldPrice()) + "</td>\n");
            s.append("                    <td>" + data.k(transaction.getPrice()) + "</td>\n");
            MovingreasonRecord movingReason = playerRound.getMovingreasonId() == null ? null
                    : FacilitatorUtils.readRecordFromId(data, Tables.MOVINGREASON, playerRound.getMovingreasonId());
            String mrString = movingReason == null ? "--" : movingReason.getKey();
            s.append("                    <td>" + mrString + "</td>\n");
            if (decision.equals("STAY"))
            {
                s.append("                    <td><button name=\"approve-" + player.getCode() + "\" id=\"approve-"
                        + player.getCode() + "\" onclick=\"popupApproveStay('" + player.getCode() + "', " + transaction.getId()
                        + ");\">APPROVE STAY</button></td>\n");
                s.append("                    <td><button name=\"reject-" + player.getCode() + "\" id=\"reject-"
                        + player.getCode() + "\" onclick=\"popupRejectStay('" + player.getCode() + "', " + transaction.getId()
                        + ");\">REJECT STAY</button></td>\n");
            }
            else
            {
                s.append("                    <td><button name=\"approve-" + player.getCode() + "\" id=\"approve-"
                        + player.getCode() + "\" onclick=\"popupApproveSell('" + player.getCode() + "', " + transaction.getId()
                        + ");\">APPROVE SELL</button></td>\n");
                s.append("                    <td><button name=\"reject-" + player.getCode() + "\" id=\"reject-"
                        + player.getCode() + "\" onclick=\"popupRejectSell('" + player.getCode() + "', " + transaction.getId()
                        + ");\">REJECT SELL</button></td>\n");
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        return s.toString();
    }

    public static List<HousetransactionRecord> getUnapprovedBuyTransactions(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.le(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_BUY)))
                .fetch();
    }

    public static List<HousetransactionRecord> getUnapprovedSellStayTransactions(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        var sellList = dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.le(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_SELL)))
                .fetch();
        var stayList = dslContext.selectFrom(Tables.HOUSETRANSACTION)
                .where(Tables.HOUSETRANSACTION.GROUPROUND_ID.le(data.getCurrentGroupRound().getId())
                        .and(Tables.HOUSETRANSACTION.TRANSACTION_STATUS.eq(TransactionStatus.UNAPPROVED_STAY)))
                .fetch();
        List<HousetransactionRecord> sellStayList = new ArrayList<>();
        sellStayList.addAll(sellList);
        sellStayList.addAll(stayList);
        return sellStayList;
    }

}
