package nl.tudelft.simulation.housinggame.facilitator;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import jakarta.servlet.http.HttpServletRequest;
import nl.tudelft.simulation.housinggame.common.CalcHouseGroup;
import nl.tudelft.simulation.housinggame.common.FluvialPluvial;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.HousegroupRecord;

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

        // check the protection of the communities and houses
        for (var houseGroup : houseGroupList)
        {
            CalcHouseGroup.calcFloodHousePlayer(data, houseGroup, data.getCurrentRoundNumber(), data.getCumulativeNewsEffects(),
                    pluvialIntensity, fluvialIntensity);
        }

        return new FluvialPluvial(fluvialIntensity, pluvialIntensity);
    }

}
