package nl.tudelft.simulation.housinggame.facilitator;

import javax.servlet.http.HttpSession;

public final class SessionUtils
{

    private SessionUtils()
    {
        // utility class
    }

    public static FacilitatorData getData(final HttpSession session)
    {
        FacilitatorData data = (FacilitatorData) session.getAttribute("facilitatorData");
        return data;
    }

}
