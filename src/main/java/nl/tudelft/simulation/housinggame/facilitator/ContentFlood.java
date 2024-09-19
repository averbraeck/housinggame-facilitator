package nl.tudelft.simulation.housinggame.facilitator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.common.CumulativeNewsEffects;
import nl.tudelft.simulation.housinggame.common.FluvialPluvial;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

/**
 * ContentFlood handles the processing of the dice roll information.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ContentFlood
{

    public static FluvialPluvial handleDiceRoll(final FacilitatorData data, final HttpServletRequest request)
    {
        // read dice values; check if dice values are valid. Popup if incorrect -- ask to resubmit
        String fluvialStr = request.getParameter("fluvial");
        String pluvialStr = request.getParameter("pluvial");
        if (pluvialStr == null || fluvialStr == null || pluvialStr.length() == 0 || fluvialStr.length() == 0)
        {
            ModalWindowUtils.makeErrorModalWindow(data, "Incorrect dice values", "One or both of the dice values are blank");
            return null;
        }
        int fluvialIntensity = 0;
        int pluvialIntensity = 0;
        try
        {
            fluvialIntensity = Integer.parseInt(fluvialStr);
            pluvialIntensity = Integer.parseInt(pluvialStr);
        }
        catch (Exception e)
        {
            ModalWindowUtils.makeErrorModalWindow(data, "Incorrect dice values",
                    "One or both of the dice values are incorrect: " + e.getMessage());
            return null;
        }
        if (fluvialIntensity < 1 || fluvialIntensity > data.getScenarioParameters().getHighestFluvialScore())
        {
            ModalWindowUtils.makeErrorModalWindow(data, "Incorrect dice values",
                    "The river dice value is not within the range 1-" + data.getScenarioParameters().getHighestFluvialScore());
            return null;
        }
        if (pluvialIntensity < 1 || pluvialIntensity > data.getScenarioParameters().getHighestPluvialScore())
        {
            ModalWindowUtils.makeErrorModalWindow(data, "Incorrect dice values",
                    "The rain dice value is not within the range 1-" + data.getScenarioParameters().getHighestPluvialScore());
            return null;
        }

        // store the dice rolls in the groupround
        data.getCurrentGroupRound().setPluvialFloodIntensity(pluvialIntensity);
        data.getCurrentGroupRound().setFluvialFloodIntensity(fluvialIntensity);
        data.getCurrentGroupRound().store();

        // retrieve the relevant house and player information
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
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

        // get the NewsEffects per community
        var cumulativeNewsEffects = CumulativeNewsEffects.readCumulativeNewsEffects(data.getDataSource(), data.getScenario(),
                data.getCurrentRoundNumber());
        var params = data.getScenarioParameters();

        // check the protection of the communities and houses
        for (var houseGroup : houseGroupList)
        {
            HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, houseGroup.getHouseId());
            int pCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getPluvialProtectionDelta();
            int pluvialCommunityProtection = houseGroup.getPluvialBaseProtection() + pCommDelta;
            int fCommDelta = cumulativeNewsEffects.get(house.getCommunityId()).getFluvialProtectionDelta();
            int fluvialCommunityProtection = houseGroup.getFluvialBaseProtection() + fCommDelta;
            var fpRecord = FluvialPluvial.measureProtectionTillRound(data, data.getCurrentRoundNumber(), houseGroup);
            int pHouseDelta = fpRecord.pluvial();
            int fHouseDelta = fpRecord.fluvial();
            int pluvialHouseProtection = pluvialCommunityProtection + pHouseDelta;
            int fluvialHouseProtection = fluvialCommunityProtection + fHouseDelta;

            int pluvialCommunityDamage = Math.max(0, pluvialIntensity - pluvialCommunityProtection);
            int fluvialCommunityDamage = Math.max(0, fluvialIntensity - fluvialCommunityProtection);
            int pluvialHouseDamage = Math.max(0, pluvialIntensity - pluvialHouseProtection);
            int fluvialHouseDamage = Math.max(0, fluvialIntensity - fluvialHouseProtection);

            // set the last round where damage happened
            if (pluvialCommunityDamage > 0)
                houseGroup.setLastRoundCommPluvial(data.getCurrentRoundNumber());
            if (fluvialCommunityDamage > 0)
                houseGroup.setLastRoundCommFluvial(data.getCurrentRoundNumber());
            if (pluvialHouseDamage > 0)
                houseGroup.setLastRoundHousePluvial(data.getCurrentRoundNumber());
            if (fluvialHouseDamage > 0)
                houseGroup.setLastRoundHouseFluvial(data.getCurrentRoundNumber());

            // check if a player owns this house in this round, and set the protection data for the playerround
            if (houseGroup.getOwnerId() != null && playerRoundMap.get(houseGroup.getOwnerId()) != null)
            {
                var playerRound = playerRoundMap.get(houseGroup.getOwnerId());
                playerRound.setPluvialBaseProtection(houseGroup.getPluvialBaseProtection());
                playerRound.setFluvialBaseProtection(houseGroup.getFluvialBaseProtection());
                playerRound.setPluvialCommunityDelta(pCommDelta);
                playerRound.setFluvialCommunityDelta(fCommDelta);
                playerRound.setPluvialHouseDelta(pHouseDelta);
                playerRound.setFluvialHouseDelta(fHouseDelta);

                // calculate the damage satisfaction penalties
                if (pluvialCommunityDamage > 0 && params.getPluvialSatisfactionPenaltyIfAreaFlooded() != null)
                {
                    playerRound.setSatisfactionPluvialPenalty(params.getPluvialSatisfactionPenaltyIfAreaFlooded());
                    playerRound.setPersonalSatisfaction(
                            playerRound.getPersonalSatisfaction() - params.getPluvialSatisfactionPenaltyIfAreaFlooded());
                }

                if (fluvialCommunityDamage > 0 && params.getFluvialSatisfactionPenaltyIfAreaFlooded() != null)
                {
                    playerRound.setSatisfactionFluvialPenalty(params.getFluvialSatisfactionPenaltyIfAreaFlooded());
                    playerRound.setPersonalSatisfaction(
                            playerRound.getPersonalSatisfaction() - params.getFluvialSatisfactionPenaltyIfAreaFlooded());
                }

                if (pluvialHouseDamage > 0 && params.getPluvialSatisfactionPenaltyHouseFloodedFixed() != null)
                {
                    playerRound.setSatisfactionPluvialPenalty(playerRound.getSatisfactionPluvialPenalty()
                            + params.getPluvialSatisfactionPenaltyHouseFloodedFixed());
                    playerRound.setPersonalSatisfaction(
                            playerRound.getPersonalSatisfaction() - params.getPluvialSatisfactionPenaltyHouseFloodedFixed());
                }

                if (fluvialHouseDamage > 0 && params.getFluvialSatisfactionPenaltyHouseFloodedFixed() != null)
                {
                    playerRound.setSatisfactionFluvialPenalty(playerRound.getSatisfactionFluvialPenalty()
                            + params.getFluvialSatisfactionPenaltyHouseFloodedFixed());
                    playerRound.setPersonalSatisfaction(
                            playerRound.getPersonalSatisfaction() - params.getFluvialSatisfactionPenaltyHouseFloodedFixed());
                }

                if (pluvialHouseDamage > 0 && params.getPluvialSatisfactionPenaltyPerDamagePoint() != null)
                {
                    playerRound.setSatisfactionPluvialPenalty(playerRound.getSatisfactionPluvialPenalty()
                            + pluvialHouseDamage * params.getPluvialSatisfactionPenaltyPerDamagePoint());
                    playerRound.setPersonalSatisfaction(playerRound.getPersonalSatisfaction()
                            - pluvialHouseDamage * params.getPluvialSatisfactionPenaltyPerDamagePoint());
                }

                if (fluvialHouseDamage > 0 && params.getFluvialSatisfactionPenaltyPerDamagePoint() != null)
                {
                    playerRound.setSatisfactionFluvialPenalty(playerRound.getSatisfactionFluvialPenalty()
                            + fluvialHouseDamage * params.getFluvialSatisfactionPenaltyPerDamagePoint());
                    playerRound.setPersonalSatisfaction(playerRound.getPersonalSatisfaction()
                            - fluvialHouseDamage * params.getFluvialSatisfactionPenaltyPerDamagePoint());
                }

                // normalize the satisfaction scores if so dictated by the parameters
                if (params.getAllowPersonalSatisfactionNeg() == 0)
                    playerRound.setPersonalSatisfaction(Math.max(0, playerRound.getPersonalSatisfaction()));
                if (params.getAllowTotalSatisfactionNeg() == 0)
                    playerRound.setPersonalSatisfaction(
                            Math.max(-houseGroup.getHouseSatisfaction(), playerRound.getPersonalSatisfaction()));

                // calculate the damage cost
                if (pluvialHouseDamage > 0 && params.getPluvialRepairCostsFixed() != null)
                {
                    playerRound.setCostPluvialDamage(params.getPluvialRepairCostsFixed());
                    playerRound.setSpendableIncome(playerRound.getSpendableIncome() - params.getPluvialRepairCostsFixed());
                }

                if (fluvialHouseDamage > 0 && params.getFluvialRepairCostsFixed() != null)
                {
                    playerRound.setCostFluvialDamage(params.getFluvialRepairCostsFixed());
                    playerRound.setSpendableIncome(playerRound.getSpendableIncome() - params.getFluvialRepairCostsFixed());
                }

                if (pluvialHouseDamage > 0 && params.getPluvialRepairCostsPerDamagePoint() != null)
                {
                    playerRound.setCostPluvialDamage(playerRound.getCostPluvialDamage()
                            + pluvialHouseDamage * params.getPluvialRepairCostsPerDamagePoint());
                    playerRound.setSpendableIncome(playerRound.getSpendableIncome()
                            - pluvialHouseDamage * params.getPluvialRepairCostsPerDamagePoint());
                }

                if (fluvialHouseDamage > 0 && params.getFluvialRepairCostsPerDamagePoint() != null)
                {
                    playerRound.setCostFluvialDamage(playerRound.getCostFluvialDamage()
                            + fluvialHouseDamage * params.getFluvialRepairCostsPerDamagePoint());
                    playerRound.setSpendableIncome(playerRound.getSpendableIncome()
                            - fluvialHouseDamage * params.getFluvialRepairCostsPerDamagePoint());
                }

                playerRound.store();

            } // if (houseGroup.getOwnerId() != null)

            // see if there are one-time measures that have been consumed, and adapt the protection and house satisfaction
            // TODO one-time measures

            // update the market value of (all) houses, also based if an area was flooded (fluvial) in previous rounds
            if (houseGroup.getLastRoundCommFluvial() != null && houseGroup.getLastRoundCommFluvial().intValue() != 0
                    && data.getCurrentRoundNumber() - houseGroup.getLastRoundCommFluvial().intValue() < 3)
            {
                int diff = data.getCurrentRoundNumber() - houseGroup.getLastRoundCommFluvial().intValue() + 1;
                int discount = diff == 1 ? cumulativeNewsEffects.get(house.getCommunityId()).getDiscountRound1()
                        : diff == 2 ? cumulativeNewsEffects.get(house.getCommunityId()).getDiscountRound2()
                                : diff == 3 ? cumulativeNewsEffects.get(house.getCommunityId()).getDiscountRound3() : 0;
                if (cumulativeNewsEffects.get(house.getCommunityId()).isDiscountEuros())
                {
                    houseGroup.setMarketValue(houseGroup.getOriginalPrice() - discount);
                }
                else
                {
                    houseGroup.setMarketValue((int) ((1.0 - discount / 100.0) * houseGroup.getOriginalPrice()));
                }
            }

            houseGroup.store();

        } // for (var houseGroup : houseGroupList)

        return new FluvialPluvial(fluvialIntensity, pluvialIntensity);
    }

}
