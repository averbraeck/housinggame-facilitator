package nl.tudelft.simulation.housinggame.facilitator;

import java.util.Map;

import nl.tudelft.simulation.housinggame.common.Taxes;
import nl.tudelft.simulation.housinggame.data.tables.records.CommunityRecord;

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
        Map<CommunityRecord, Integer> taxMap = Taxes.calcTaxMap(data, data.getCurrentGroupRound());
        for (var playerRound : data.getPlayerRoundList())
        {
            Taxes.applyTax(data, taxMap, playerRound);
        }
    }

}
