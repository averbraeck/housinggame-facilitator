package nl.tudelft.simulation.housinggame.facilitator;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.common.GroupState;
import nl.tudelft.simulation.housinggame.common.HouseGroupStatus;
import nl.tudelft.simulation.housinggame.common.PlayerState;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HouseRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.HousemeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.InitialhousemeasureRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.MeasuretypeRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;

/**
 * NewRound to handle popups and processing of a new round.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ContentNewRound
{
    public static void popupNewRound(final FacilitatorData data)
    {
        int nrLoggedInPlayers = 0;
        int nrReadyPlayers = 0;
        int nrActivePlayers = 0;
        for (PlayerRecord player : data.getPlayerList())
        {
            List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, player.getId());
            PlayerroundRecord playerRound = FacilitatorUtils.getCurrentPlayerRound(data, player.getId());
            if (!playerRoundList.isEmpty())
            {
                if (playerRoundList.get(0) != null)
                    nrLoggedInPlayers++;
                if (playerRound != null && playerRound.getGrouproundId().equals(data.getCurrentGroupRound().getId()))
                {
                    PlayerState playerState = PlayerState.valueOf(playerRound.getPlayerState());
                    nrActivePlayers++;
                    if (playerState.equals(PlayerState.VIEW_SUMMARY))
                        nrReadyPlayers++;
                }
            }
        }

        String content = "There are " + nrLoggedInPlayers + " players who have logged in";
        content += "<br>There are " + nrActivePlayers + " players who are active (in the same round)";
        content += "<br>There are " + nrReadyPlayers + " players who are at the summary screen<br>";
        if (data.getCurrentRoundNumber() == 0)
        {
            if (nrLoggedInPlayers < data.getScenario().getMinimumPlayers())
                content += "<br>This is LESS than the minimum number: " + data.getScenario().getMinimumPlayers();
            else
                content += "<br>This number would be sufficient to play the game.";
        }
        else
        {
            if (nrReadyPlayers < nrActivePlayers)
                content += "<br>NOT ALL PLAYERS are at the summary screen! (" + nrReadyPlayers + " < " + nrActivePlayers + ")";
            else if (nrActivePlayers == 0)
                content += "<br>NO PLAYERS HAVE CARRIED OUT ANY ACTIONS YET!";
            else
                content += "<br>All players are at the summary screen";
        }
        content += "<br>Do you really want to move to the next round?<br>";

        ModalWindowUtils.make2ButtonModalWindow(data, "Move to next round?", content, "YES", "start-new-round-ok", "NO", "",
                "");
    }

    public static void newRound(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);

        // make a new groupround
        GrouproundRecord groupRound = dslContext.newRecord(Tables.GROUPROUND);
        groupRound.setGroupState(GroupState.NEW_ROUND.toString());
        groupRound.setRoundNumber(data.getCurrentRoundNumber() + 1);
        groupRound.setGroupId(data.getGroup().getId());
        groupRound.setPluvialFloodIntensity(null);
        groupRound.setFluvialFloodIntensity(null);
        groupRound.store();

        // make houses that are not used unavailable for this round
        List<HousegroupRecord> currentHouseGroupList =
                dslContext.selectFrom(Tables.HOUSEGROUP).where(Tables.HOUSEGROUP.GROUP_ID.eq(data.getGroup().getId())).fetch();
        for (var houseGroup : currentHouseGroupList)
        {
            if (HouseGroupStatus.AVAILABLE.equals(houseGroup.getStatus()))
            {
                houseGroup.setStatus(HouseGroupStatus.NOT_AVAILABLE);
                houseGroup.store();
            }
        }

        // make the new houses available for this round
        List<Integer> houseIdList = dslContext
                .selectFrom(Tables.HOUSE.innerJoin(Tables.COMMUNITY).on(Tables.HOUSE.COMMUNITY_ID.eq(Tables.COMMUNITY.ID)))
                .where(Tables.COMMUNITY.GAMEVERSION_ID.eq(data.getGameVersion().getId()))
                .and(Tables.HOUSE.AVAILABLE_ROUND.eq(groupRound.getRoundNumber())).fetch(Tables.HOUSE.ID);
        for (int houseId : houseIdList)
        {
            HouseRecord house = FacilitatorUtils.readRecordFromId(data, Tables.HOUSE, houseId);
            HousegroupRecord houseGroup = dslContext.newRecord(Tables.HOUSEGROUP);
            CommunityRecord community = FacilitatorUtils.readRecordFromId(data, Tables.COMMUNITY, house.getCommunityId());
            houseGroup.setCode(house.getCode());
            houseGroup.setAddress(house.getAddress());
            houseGroup.setRating(house.getRating());
            houseGroup.setOriginalPrice(house.getPrice());
            houseGroup.setDamageReduction(0);
            houseGroup.setMarketValue(house.getPrice());
            houseGroup.setLastSoldPrice(null);
            houseGroup.setHouseSatisfaction(0);
            houseGroup.setStatus(HouseGroupStatus.AVAILABLE);
            houseGroup.setFluvialBaseProtection(community.getFluvialProtection());
            houseGroup.setPluvialBaseProtection(community.getPluvialProtection());
            houseGroup.setFluvialHouseProtection(0);
            houseGroup.setPluvialHouseProtection(0);
            houseGroup.setLastRoundCommFluvial(null);
            houseGroup.setLastRoundCommPluvial(null);
            houseGroup.setLastRoundHouseFluvial(null);
            houseGroup.setLastRoundHousePluvial(null);
            houseGroup.setHouseId(house.getId());
            houseGroup.setGroupId(data.getGroup().getId());
            houseGroup.setOwnerId(null);
            houseGroup.store();
            List<InitialhousemeasureRecord> initialMeasureList = dslContext.selectFrom(Tables.INITIALHOUSEMEASURE)
                    .where(Tables.INITIALHOUSEMEASURE.HOUSE_ID.eq(house.getId())).fetch();
            for (InitialhousemeasureRecord initialMeasure : initialMeasureList)
            {
                if (initialMeasure.getRoundNumber() <= groupRound.getRoundNumber())
                {
                    var measureType = FacilitatorUtils.readRecordFromId(data, Tables.MEASURETYPE, initialMeasure.getMeasuretypeId());
                    createMeasureForHouse(data, houseGroup, measureType, groupRound.getRoundNumber());
                }
            }
        }

        data.readDynamicData();
    }

    private static void createMeasureForHouse(final FacilitatorData data, final HousegroupRecord houseGroup,
            final MeasuretypeRecord measureType, final int roundNumber)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        HousemeasureRecord measure = dslContext.newRecord(Tables.HOUSEMEASURE);
        measure.setBoughtInRound(roundNumber);
        measure.setMeasuretypeId(measureType.getId());
        measure.setHousegroupId(houseGroup.getId());
        measure.store();

        houseGroup.setFluvialHouseProtection(houseGroup.getFluvialHouseProtection() + measureType.getFluvialProtectionDelta());
        houseGroup.setPluvialHouseProtection(houseGroup.getPluvialHouseProtection() + measureType.getPluvialProtectionDelta());
        houseGroup.setHouseSatisfaction(houseGroup.getHouseSatisfaction() + measureType.getSatisfactionDeltaPermanent());
        houseGroup.store();
    }

    public static void createMeasureForHouse(final FacilitatorData data, final HousegroupRecord houseGroup,
            final InitialhousemeasureRecord ihmr)
    {
        MeasuretypeRecord measureType = FacilitatorUtils.readRecordFromId(data, Tables.MEASURETYPE, ihmr.getMeasuretypeId());
        createMeasureForHouse(data, houseGroup, measureType, ihmr.getRoundNumber());
    }

}
