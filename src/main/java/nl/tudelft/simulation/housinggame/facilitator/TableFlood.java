package nl.tudelft.simulation.housinggame.facilitator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.common.CumulativeNewsEffects;
import nl.tudelft.simulation.housinggame.common.FPRecord;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousemeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasuretypeRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;

/**
 * TableFlood makes and fills the flood information tables.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TableFlood
{

    public static void makeFloodTable(final FacilitatorData data)
    {
        StringBuilder s = new StringBuilder();
        makeDiceTable(data, s);
        makeCommunityTable(data, s);
        makeHouseTable(data, s);
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    /**
     * Table (1) with dice rolls for all rounds.
     * @param data FacilitatorData
     * @param s StringBuilder
     */
    private static void makeDiceTable(final FacilitatorData data, final StringBuilder s)
    {
        s.append("  <br/>\n");
        s.append("  <div style=\"display:flex; flex-direction:column; flex-wrap: wrap;\">\n");
        s.append("    <div style=\"display:flex; flex-direction:row; gap:25px;\">\n");
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"hg-flood-table1 pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>&nbsp;</th>\n");
        s.append("              <th colspan=\"2\">Dice roll</th>\n");
        s.append("            </tr>\n");
        s.append("            <tr>\n");
        s.append("              <th>Round<br/>nr.</th>\n");
        s.append("              <th>River<br/>&nbsp;</th>\n");
        s.append("              <th>Rain<br/>&nbsp;</th>\n");
        s.append("            </tr>\n");
        s.append("          </thead>\n");
        s.append("          <tbody>\n");
        for (int round = 1; round <= data.getScenario().getHighestRoundNumber(); round++)
        {
            s.append("            <tr>\n");
            s.append("              <td>" + round + "</td>\n");
            String fl = round > data.getCurrentRoundNumber()
                    || data.getGroupRoundList().get(round).getFluvialFloodIntensity() == null ? "--"
                            : Integer.toString(data.getGroupRoundList().get(round).getFluvialFloodIntensity());
            s.append("              <td>" + fl + "</td>\n");
            String pl = round > data.getCurrentRoundNumber()
                    || data.getGroupRoundList().get(round).getPluvialFloodIntensity() == null ? "--"
                            : Integer.toString(data.getGroupRoundList().get(round).getPluvialFloodIntensity());
            s.append("          <td>" + pl + "</td>\n");
            s.append("            </tr>\n");
        }
        s.append("          </tbody>\n");
        s.append("        </table>\n");
        s.append("      </div>\n");
    }

    /**
     * Table (2) with protection and discounts per community per round.
     * @param data FacilitatorData
     * @param s StringBuilder
     */
    private static void makeCommunityTable(final FacilitatorData data, final StringBuilder s)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        List<CommunityRecord> communityList = dslContext.selectFrom(Tables.COMMUNITY)
                .where(Tables.COMMUNITY.GAMEVERSION_ID.eq(data.getScenario().getGameversionId())).fetch();
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"hg-flood-table2 pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>&nbsp;</th>\n");
        for (var community : communityList)
        {
            s.append("              <th colspan=\"2\">" + community.getName() + "</th>\n");
        }
        s.append("              <th>&nbsp;</th>\n");
        s.append("            </tr>\n");
        s.append("            <tr>\n");
        s.append("              <th>Round<br/>nr.</th>\n");
        for (int i = 0; i < communityList.size(); i++)
        {
            s.append("              <th>House<br/>discount</th>\n");
            s.append("              <th>Public<br/>protection</th>\n");
        }
        s.append("              <th>View<br/>details</th>\n");
        s.append("            </tr>\n");
        s.append("          </thead>\n");
        s.append("          <tbody>\n");

        // make a list (per round) of maps from communityId to CumulativeNewsEffects
        List<Map<Integer, CumulativeNewsEffects>> cumulativeNewsEffects = new ArrayList<Map<Integer, CumulativeNewsEffects>>();
        for (int r = 0; r <= data.getCurrentRoundNumber(); r++)
            cumulativeNewsEffects
                    .add(CumulativeNewsEffects.readCumulativeNewsEffects(data.getDataSource(), data.getScenario(), r));

        for (int round = 1; round <= data.getScenario().getHighestRoundNumber(); round++)
        {
            s.append("            <tr>\n");
            s.append("              <td>" + round + "</td>\n");
            for (var community : communityList)
            {
                if (round > data.getCurrentRoundNumber())
                {
                    s.append("              <td>--</td>\n");
                    s.append("              <td>--</td>\n");
                }
                else
                {
                    s.append("              <td>" + calcDiscountStr(data, community, cumulativeNewsEffects, round) + "</td>\n");
                    int fl = community.getFluvialProtection()
                            + cumulativeNewsEffects.get(round).get(community.getId()).getFluvialProtectionDelta();
                    int pl = community.getPluvialProtection()
                            + cumulativeNewsEffects.get(round).get(community.getId()).getPluvialProtectionDelta();
                    s.append(String.format("              <td> River: %2d, Rain: %2d</td>\n", fl, pl));
                }
            }
            s.append("              <td>\n");
            s.append("                <form action=\"/housinggame-facilitator/facilitator\" method=\"post\" "
                    + "id=\"hg-flood-round-" + round + "\">\n");
            s.append("                  <a href=\"#\" onclick=\"floodRound(" + round + ")\">Details</a>\n");
            s.append("                  <input type=\"hidden\" name=\"floodRound\" value=\"" + round + "\" />\n");
            s.append("                </form>\n");
            s.append("              </td>\n");
            s.append("            </tr>\n");
        }
        s.append("          </tbody>\n");
        s.append("        </table>\n");
        s.append("      </div>\n");
        s.append("    </div>\n");
    }

    /**
     * Table (3) with house flood information for selected round.
     * @param data FacilitatorData
     * @param s StringBuilder
     */
    private static void makeHouseTable(final FacilitatorData data, final StringBuilder s)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        List<HousegroupRecord> houseGroupList = dslContext.selectFrom(Tables.HOUSEGROUP)
                .where(Tables.HOUSEGROUP.GROUP_ID.eq(data.getCurrentGroupRound().getGroupId())).fetch();
        Map<Integer, HousegroupRecord> playerHouseGroupMap = new HashMap<>();
        for (var houseGroup : houseGroupList)
        {
            if (houseGroup.getOwnerId() != null)
                playerHouseGroupMap.put(houseGroup.getOwnerId(), houseGroup);
        }
        s.append("    <div>\n");
        s.append("      <h3>Selected data for round " + data.getFloodInfoRoundNumber() + "</h3>\n");
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"hg-flood-table3 pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>House</th>\n");
        s.append("              <th>From</th>\n");
        s.append("              <th>Owner</th>\n");
        s.append("              <th>Measures</th>\n");
        s.append("              <th colspan=\"2\">Base protection</th>\n");
        s.append("              <th colspan=\"2\">Public delta (news)</th>\n");
        s.append("              <th colspan=\"2\">Private delta (measures)</th>\n");
        s.append("              <th colspan=\"2\">Damage points</th>\n");
        s.append("            </tr>\n");
        s.append("            <tr>\n");
        s.append("              <th>code</th>\n");
        s.append("              <th>round</th>\n");
        s.append("              <th>&nbsp;</th>\n");
        s.append("              <th>&nbsp;</th>\n");
        s.append("              <th>River</th>\n");
        s.append("              <th>Rain</th>\n");
        s.append("              <th>River</th>\n");
        s.append("              <th>Rain</th>\n");
        s.append("              <th>River</th>\n");
        s.append("              <th>Rain</th>\n");
        s.append("              <th>River</th>\n");
        s.append("              <th>Rain</th>\n");
        s.append("            </tr>\n");
        s.append("          </thead>\n");
        s.append("          <tbody>\n");

        var cumulativeNewsEffects = CumulativeNewsEffects.readCumulativeNewsEffects(data.getDataSource(), data.getScenario(),
                data.getFloodInfoRoundNumber());
        for (var houseGroup : houseGroupList)
        {
            HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
            s.append("            <tr>\n");
            s.append("              <td>" + house.getCode() + "</td>\n");
            s.append("              <td>" + house.getAvailableRound() + "</td>\n");
            String playerCode = "--";
            if (houseGroup.getOwnerId() != null)
            {
                PlayerRecord player = FacilitatorUtils.readRecordFromId(data, Tables.PLAYER, houseGroup.getOwnerId());
                playerCode = player.getCode();
            }
            s.append("              <td>" + playerCode + "</td>\n");
            List<HousemeasureRecord> measureList = dslContext.selectFrom(Tables.HOUSEMEASURE)
                    .where(Tables.HOUSEMEASURE.HOUSEGROUP_ID.eq(houseGroup.getId())).fetch();
            s.append("              <td>");
            for (int i = 0; i < measureList.size(); i++)
            {
                if (i > 0)
                    s.append("<br/>");
                MeasuretypeRecord mt =
                        FacilitatorUtils.readRecordFromId(data, Tables.MEASURETYPE, measureList.get(i).getMeasuretypeId());
                s.append(mt.getShortAlias());
            }
            int fCommBaseProt = houseGroup.getFluvialBaseProtection();
            int pCommBaseProt = houseGroup.getPluvialBaseProtection();
            int fCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getFluvialProtectionDelta();
            int pCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getPluvialProtectionDelta();
            var fpRecord = FPRecord.measureProtectionTillRound(data, data.getFloodInfoRoundNumber(), houseGroup);
            int fHouseDelta = fpRecord.fluvial();
            int pHouseDelta = fpRecord.pluvial();
            s.append("</td>\n");
            s.append("              <td>" + fCommBaseProt + "</td>\n");
            s.append("              <td>" + pCommBaseProt + "</td>\n");
            s.append("              <td>" + fCommDelta + "</td>\n");
            s.append("              <td>" + pCommDelta + "</td>\n");
            s.append("              <td>" + fHouseDelta + "</td>\n");
            s.append("              <td>" + pHouseDelta + "</td>\n");
            if (data.getFloodInfoRoundNumber() > data.getCurrentRoundNumber()
                    || data.getGroupRoundList().get(data.getFloodInfoRoundNumber()).getFluvialFloodIntensity() == null)
            {
                s.append("              <td>--</td>\n");
                s.append("              <td>--</td>\n");
            }
            else
            {
                int fDice = data.getGroupRoundList().get(data.getFloodInfoRoundNumber()).getFluvialFloodIntensity();
                int pDice = data.getGroupRoundList().get(data.getFloodInfoRoundNumber()).getPluvialFloodIntensity();
                int fDamage = Math.max(0, fDice - fCommBaseProt - fCommDelta - fHouseDelta);
                int pDamage = Math.max(0, pDice - pCommBaseProt - pCommDelta - pHouseDelta);
                s.append("              <td>" + fDamage + "</td>\n");
                s.append("              <td>" + pDamage + "</td>\n");
            }
            s.append("            </tr>\n");
        }
        s.append("          </tbody>\n");
        s.append("        </table>\n");
        s.append("      </div>\n");

        s.append("    </div>\n");
        s.append("  </div>\n");
    }

    /**
     * Calculate the discount in the given round number for the given community, based on the rules in the NewsEffects. If the
     * river caused flooding in 'round - 1', the discount for round1 is applied. If it flooded in 'round - 2', the discount for
     * round2 is applied. Same for round3.
     * @param data FacilitatorData
     * @param community the community record
     * @param cumulativeNewsEffects List per round of cumulative news effects as a map of cummunityId to news effects
     * @param round round number
     * @return String with the discount, if any, otherwise "--"
     */
    private static String calcDiscountStr(final FacilitatorData data, final CommunityRecord community,
            final List<Map<Integer, CumulativeNewsEffects>> cumulativeNewsEffects, final int round)
    {
        int cId = community.getId();
        boolean[] isFloodedInRound = new boolean[round + 1];
        isFloodedInRound[0] = false;
        for (int r = 1; r <= round; r++)
        {
            isFloodedInRound[r] = false;
            GrouproundRecord gr = data.getGroupRoundList().get(r);
            if (gr != null)
            {
                int fluvialIntensity = gr.getFluvialFloodIntensity() == null ? 0 : gr.getFluvialFloodIntensity();
                int protection =
                        community.getFluvialProtection() + cumulativeNewsEffects.get(r).get(cId).getFluvialProtectionDelta();
                isFloodedInRound[r] = protection - fluvialIntensity < 0;
            }
        }
        int discount = 0;
        if (round - 1 > 0 && isFloodedInRound[round - 1])
            discount = cumulativeNewsEffects.get(round).get(cId).getDiscountRound1();
        else if (round - 2 > 0 && isFloodedInRound[round - 2])
            discount = cumulativeNewsEffects.get(round).get(cId).getDiscountRound2();
        else if (round - 3 > 0 && isFloodedInRound[round - 3])
            discount = cumulativeNewsEffects.get(round).get(cId).getDiscountRound3();
        if (cumulativeNewsEffects.get(round).get(cId).isDiscountEuros())
        {
            return discount == 0 ? "--" : data.k(discount);
        }
        else
        {
            return discount == 0 ? "--" : Integer.toString(discount) + "%";
        }
    }

}
