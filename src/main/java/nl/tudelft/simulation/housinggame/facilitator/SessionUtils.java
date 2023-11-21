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
        if (data == null)
        {
            data = new FacilitatorData();
            session.setAttribute("facilitatorData", data);
            data.getDataSource();
        }
        return data;
    }

}
