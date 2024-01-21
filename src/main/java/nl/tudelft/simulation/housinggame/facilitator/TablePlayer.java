package nl.tudelft.simulation.housinggame.facilitator;

import java.util.List;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioparametersRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.WelfaretypeRecord;

/**
 * TablePlayer makes the tables with the player information.
 * <p>
 * Copyright (c) 2020-2020 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TablePlayer
{

    public static String makePlayerStateTable(final FacilitatorData data)
    {
        /*-
        <thead>
          <tr>
            <th>Player</th>
            <th>Round</th>
            <th>State</th>
            <th>Satisfaction</th>
            <th>Income</th>
            <th>House</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>t1p1</td>
            <td>0</td>
            <td>Login</td>
            <td>5</td>
            <td>30k</td>
            <td>--</td>
          </tr>
          ...
        </tbody>
         */
        StringBuilder s = new StringBuilder();
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Round</th>\n");
        s.append("                    <th>State</th>\n");
        s.append("                    <th>House</th>\n");
        s.append("                    <th>Taxes</th>\n");
        s.append("                    <th>Spendable<br/>income</th>\n");
        s.append("                    <th>Player<br/>satisf</th>\n");
        s.append("                    <th>House<br/>satisf</th>\n");
        s.append("                    <th>Debt<br/>penalty</th>\n");
        s.append("                    <th>Flood<br/>penalty</th>\n");
        s.append("                    <th>Total<br/>satisf</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");

        ScenarioparametersRecord spr = data.getScenarioParameters();

        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>" + data.k(welfareType.getInitialMoney()) + "</td>\n");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>--</td>\n");
                s.append("                    <td>" + welfareType.getInitialSatisfaction() + "</td>\n");
            }
            else
            {
                int highestRound = 0;
                PlayerroundRecord prr = playerRoundList.get(0);
                PlayerroundRecord prrPrev = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                    {
                        prrPrev = prr;
                        prr = playerRoundList.get(i);
                        highestRound = i;
                    }
                }
                int currentHouseSatisfaction = 0;
                if (prr.getFinalHousegroupId() != null)
                {
                    HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getFinalHousegroupId());
                    currentHouseSatisfaction = hgr.getHouseSatisfaction();
                }
                else if (prr.getStartHousegroupId() != null)
                {
                    HousegroupRecord hgr = SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, prr.getStartHousegroupId());
                    currentHouseSatisfaction = hgr.getHouseSatisfaction();
                }

                s.append("                    <td>" + highestRound + "</td>\n");
                s.append("                    <td>" + prr.getPlayerState() + "</td>\n");

                HouseRecord house = data.getApprovedHouseForPlayerRound(prr);
                if (house == null)
                {
                    s.append("                    <td>--</td>\n");
                    s.append("                    <td>--</td>\n");
                }
                else
                {
                    s.append("                    <td>" + house.getCode() + "</td>\n");
                    s.append("                    <td>" + data.k(prr.getCostTaxes()) + "</td>\n");
                }
                s.append("                    <td>" + data.k(prr.getSpendableIncome()) + "</td>\n");
                int netSatisfaction = prr.getPersonalSatisfaction() - prr.getSatisfactionFluvialPenalty()
                        - prr.getSatisfactionPluvialPenalty() - prr.getSatisfactionDebtPenalty();
                s.append("                    <td>" + netSatisfaction + "</td>\n");
                if (house == null)
                    s.append("                    <td>--</td>\n");
                else
                    s.append("                    <td>" + currentHouseSatisfaction + "</td>\n");
                if (prrPrev.getSpendableIncome() >= 0)
                    s.append("                    <td>-</td>\n");
                else
                    s.append("                    <td>-" + spr.getSatisfactionDebtPenalty() + "</td>\n");
                if (prr.getSatisfactionFluvialPenalty() + prr.getSatisfactionPluvialPenalty() == 0)
                    s.append("                    <td>--</td>\n");
                else
                    s.append("                    <td>"
                            + (prr.getSatisfactionFluvialPenalty() + prr.getSatisfactionPluvialPenalty()) + "</td>\n");
                s.append("                    <td>" + (prr.getPersonalSatisfaction() + currentHouseSatisfaction) + "</td>\n");
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        return s.toString();
    }

    public static String makePlayerBudgetTable(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Player</th>\n");
        s.append("                    <th>Income</th>\n");
        s.append("                    <th>Living<br/>costs</th>\n");
        s.append("                    <th>Maximun<br/>mortgage</th>\n");
        s.append("                    <th>Start<br/>savings</th>\n");
        s.append("                    <th>Start<br/>debt</th>\n");
        s.append("                    <th>Increase<br/>satisf</th>\n");
        s.append("                    <th>Preferred<br/>rating</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        for (PlayerRecord player : data.getPlayerList())
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + player.getCode() + "</td>\n");
            List<PlayerroundRecord> playerRoundList = SqlUtils.getPlayerRoundList(data, player.getId());
            WelfaretypeRecord welfareType = SqlUtils.readRecordFromId(data, Tables.WELFARETYPE, player.getWelfaretypeId());
            if (playerRoundList.isEmpty() || playerRoundList.get(0) == null)
            {
                s.append("                    <td>" + data.k(welfareType.getRoundIncome()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getLivingCosts()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getMaximumMortgage()) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getInitialMoney()) + "</td>\n");
                s.append("                    <td>" + data.k(0) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getSatisfactionCostPerPoint()) + "</td>\n");
                s.append("                    <td>" + welfareType.getPreferredHouseRating() + "</td>\n");
            }
            else
            {
                PlayerroundRecord prr = playerRoundList.get(0);
                PlayerroundRecord prrPrev = playerRoundList.get(0);
                for (int i = 0; i < playerRoundList.size(); i++)
                {
                    if (playerRoundList.get(i) != null)
                    {
                        prrPrev = prr;
                        prr = playerRoundList.get(i);
                    }
                }
                s.append("                    <td>" + data.k(prr.getRoundIncome()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getLivingCosts()) + "</td>\n");
                s.append("                    <td>" + data.k(prr.getMaximumMortgage()) + "</td>\n");
                s.append("                    <td>"
                        + data.k(prrPrev.getSpendableIncome() > 0 ? prrPrev.getSpendableIncome() : 0) + "</td>\n");
                s.append("                    <td>"
                        + data.k(prrPrev.getSpendableIncome() < 0 ? -prrPrev.getSpendableIncome() : 0) + "</td>\n");
                s.append("                    <td>" + data.k(welfareType.getSatisfactionCostPerPoint()) + "</td>\n");
                s.append("                    <td>" + prr.getPreferredHouseRating() + "</td>\n");
            }
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        return s.toString();
    }

    public static void makePlayerTables(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        s.append("        <h1>Players in the group</h1>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append(makePlayerStateTable(data));
        s.append("          </table>\n");
        s.append("        </div>\n");
        s.append("\n");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("           <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append(makePlayerBudgetTable(data));
        s.append("           </table>\n");
        s.append("        </div>\n");
        s.append("\n");
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

}
