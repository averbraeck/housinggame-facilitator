package nl.tudelft.simulation.housinggame.facilitator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.common.CumulativeNewsEffects;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasuretypeRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

/**
 * TableFlood makes and fills the flood information tables.
 * <p>
 * Copyright (c) 2020-2020 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TableFlood
{

    record FPRecord(int fluvial, int pluvial)
    {
    }

    public static void makeFloodTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();

        // table with dice rolls for all rounds
        s.append("  <br/>\n");
        s.append("  <div style=\"display:flex; flex-direction:column; flex-wrap: wrap;\">\n");
        s.append("    <div style=\"display:flex; flex-direction:row; gap:25px;\">\n");
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>Round</th>\n");
        s.append("              <th colspan=\"2\">Dice roll</th>\n");
        s.append("            </tr>\n");
        s.append("            <tr>\n");
        s.append("              <th>&nbsp;<br/>&nbsp;</th>\n");
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

        // table with protection and discounts per community per round
        List<CommunityRecord> communityList = dslContext.selectFrom(Tables.COMMUNITY)
                .where(Tables.COMMUNITY.GAMEVERSION_ID.eq(data.getScenario().getGameversionId())).fetch();
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>Round</th>\n");
        for (var community : communityList)
        {
            s.append("              <th colspan=\"2\">" + community.getName() + "</th>\n");
        }
        s.append("              <th>View</th>\n");
        s.append("            </tr>\n");
        s.append("            <tr>\n");
        s.append("              <th>nr.</th>\n");
        for (int i = 0; i < communityList.size(); i++)
        {
            s.append("              <th>House<br/>discount</th>\n");
            s.append("              <th>Public<br/>prot.</th>\n");
        }
        s.append("              <th>details</th>\n");
        s.append("            </tr>\n");
        s.append("          </thead>\n");
        s.append("          <tbody>\n");
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
                    var cumulativeNewsEffects =
                            CumulativeNewsEffects.readCumulativeNewsEffects(data.getDataSource(), data.getScenario(), round);
                    s.append("              <td>" + calcDiscountStr(data, community, cumulativeNewsEffects, round) + "</td>\n");
                    int pl = community.getPluvialProtection()
                            + cumulativeNewsEffects.get(community.getId()).getPluvialProtectionDelta();
                    int fl = community.getFluvialProtection()
                            + cumulativeNewsEffects.get(community.getId()).getFluvialProtectionDelta();
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

        // table with house flood information for selected round
        List<HousegroupRecord> houseGroupList = dslContext.selectFrom(Tables.HOUSEGROUP)
                .where(Tables.HOUSEGROUP.GROUP_ID.eq(data.getCurrentGroupRound().getGroupId())).fetch();
        Map<Integer, HousegroupRecord> playerHouseGroupMap = new HashMap<>();
        for (var houseGroup : houseGroupList)
        {
            if (houseGroup.getOwnerId() != null)
                playerHouseGroupMap.put(houseGroup.getOwnerId(), houseGroup);
        }
        List<PlayerroundRecord> playerRoundList = dslContext.selectFrom(Tables.PLAYERROUND)
                .where(Tables.PLAYERROUND.GROUPROUND_ID.eq(data.getCurrentGroupRound().getId())).fetch();
        Map<Integer, PlayerroundRecord> playerRoundMap = new HashMap<>();
        for (var playerRound : playerRoundList)
            playerRoundMap.put(playerRound.getPlayerId(), playerRound);
        s.append("    <div>\n");
        s.append("      <h3>Selected data for round " + data.getFloodInfoRoundNumber() + "</h3>\n");
        s.append("      <div class=\"hg-fac-table\">\n");
        s.append("        <table class=\"pmd table table-striped\" style=\"text-align:center;\">\n");
        s.append("          <thead>\n");
        s.append("            <tr>\n");
        s.append("              <th>House</th>\n");
        s.append("              <th>From</th>\n");
        s.append("              <th>Owner</th>\n");
        s.append("              <th>Measures</th>\n");
        s.append("              <th colspan=\"2\">Public protection</th>\n");
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
            HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
            s.append("            <tr>\n");
            s.append("              <td>" + house.getCode() + "</td>\n");
            s.append("              <td>" + house.getAvailableRound() + "</td>\n");
            PlayerroundRecord playerRound = null;
            String playerCode = "--";
            if (houseGroup.getOwnerId() != null)
            {
                playerRound = playerRoundMap.get(houseGroup.getOwnerId());
                PlayerRecord player = SqlUtils.readRecordFromId(data, Tables.PLAYER, playerRound.getPlayerId());
                playerCode = player.getCode();
            }
            if (playerRound != null)
                s.append("              <td>" + playerCode + "</td>\n");
            else
                s.append("              <td>--</td>\n");
            List<MeasureRecord> measureList =
                    dslContext.selectFrom(Tables.MEASURE).where(Tables.MEASURE.HOUSEGROUP_ID.eq(houseGroup.getId())).fetch();
            s.append("              <td>");
            for (int i = 0; i < measureList.size(); i++)
            {
                if (i > 0)
                    s.append("<br/>");
                MeasuretypeRecord mt =
                        SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, measureList.get(i).getMeasuretypeId());
                s.append(mt.getShortAlias());
            }
            int fCommBaseProt = houseGroup.getFluvialBaseProtection();
            int pCommBaseProt = houseGroup.getPluvialBaseProtection();
            int fCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getFluvialProtectionDelta();
            int pCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getPluvialProtectionDelta();
            var fpRecord = fpMeasureProtectionTillRound(data, data.getFloodInfoRoundNumber(), houseGroup);
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

        data.getContentHtml().put("facilitator/tables", s.toString());
    }

    private static String calcDiscountStr(final FacilitatorData data, final CommunityRecord community,
            final Map<Integer, CumulativeNewsEffects> cumulativeNewsEffects, final int round)
    {
        int lastRound = -100;
        for (int r = 1; r <= round; r++)
        {
            GrouproundRecord gr = data.getGroupRoundList().get(r);
            if (gr != null)
            {
                int fluvialIntensity = gr.getFluvialFloodIntensity() == null ? 0 : gr.getFluvialFloodIntensity();
                int protection = community.getFluvialProtection()
                        + cumulativeNewsEffects.get(community.getId()).getFluvialProtectionDelta();
                if (protection - fluvialIntensity < 0)
                    lastRound = r;
            }
        }
        if (round - lastRound < 3)
        {
            int diff = round - lastRound + 1;
            int discount = diff == 1 ? cumulativeNewsEffects.get(community.getId()).getDiscountRound1()
                    : diff == 2 ? cumulativeNewsEffects.get(community.getId()).getDiscountRound2()
                            : diff == 3 ? cumulativeNewsEffects.get(community.getId()).getDiscountRound3() : 0;
            if (cumulativeNewsEffects.get(community.getId()).isDiscountEuros())
            {
                return data.k(discount);
            }
            else
            {
                return Integer.toString(discount) + "%";
            }
        }
        return "--";
    }

    private static FPRecord fpMeasureProtectionTillRound(final FacilitatorData data, final int round,
            final HousegroupRecord houseGroup)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        List<MeasureRecord> measureList = dslContext.selectFrom(Tables.MEASURE)
                .where(Tables.MEASURE.HOUSEGROUP_ID.eq(houseGroup.getId())).fetch().sortAsc(Tables.MEASURE.ROUND_NUMBER);
        int fluvial = 0;
        int pluvial = 0;
        for (var measure : measureList)
        {
            if (measure.getRoundNumber() <= round
                    && (measure.getConsumedInRound() == null || measure.getConsumedInRound().intValue() == 0))
            {
                var mt = SqlUtils.readRecordFromId(data, Tables.MEASURETYPE, measure.getMeasuretypeId());
                fluvial += mt.getFluvialProtectionDelta();
                pluvial += mt.getPluvialProtectionDelta();
            }
        }
        return new FPRecord(fluvial, pluvial);
    }

}
