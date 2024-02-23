package nl.tudelft.simulation.housinggame.facilitator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.TaxRecord;

/**
 * ContentTaxes collects the taxes.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ContentTaxes
{
    public static void calculateTaxes(final FacilitatorData data)
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

        // TODO: tax increases based on measures

        for (var playerRound : data.getPlayerRoundList())
        {
            if (playerRound.getFinalHousegroupId() != null)
            {
                HousegroupRecord houseGroup =
                        SqlUtils.readRecordFromId(data, Tables.HOUSEGROUP, playerRound.getFinalHousegroupId());
                HouseRecord house = SqlUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
                CommunityRecord community = SqlUtils.readRecordFromId(data, Tables.COMMUNITY, house.getCommunityId());
                int taxCost = taxMap.get(community);
                playerRound.setCostTaxes(taxCost);
                playerRound.setSpendableIncome(playerRound.getSpendableIncome() - taxCost);
                playerRound.store();
            }
        }
    }

}
