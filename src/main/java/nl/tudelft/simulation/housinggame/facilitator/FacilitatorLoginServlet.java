package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nl.tudelft.simulation.housinggame.common.GroupState;
import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GamesessionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.UserRecord;

@WebServlet("/login")
public class FacilitatorLoginServlet extends HttpServlet
{

    /** */
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException
    {
        super.init();
        System.getProperties().setProperty("org.jooq.no-logo", "true");
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {

        String gamesession = request.getParameter("gamesession");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        /*-
        MessageDigest md;
        String hashedPassword;
        try
        {
            // https://www.baeldung.com/java-md5
            md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            hashedPassword = DatatypeConverter.printHexBinary(digest).toLowerCase();
        }
        catch (NoSuchAlgorithmException e1)
        {
            throw new ServletException(e1);
        }
        */

        HttpSession session = request.getSession();
        FacilitatorData data = SessionUtils.getData(session);

        boolean ok = true;
        int gameSessionId = 0;
        if (gamesession == null)
            ok = false;
        else
        {
            try
            {
                gameSessionId = Integer.parseInt(gamesession);
            }
            catch (Exception e)
            {
                ok = false;
            }
        }
        if (ok)
        {
            DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
            GamesessionRecord gs = FacilitatorUtils.readRecordFromId(data, Tables.GAMESESSION, gameSessionId);
            UserRecord user = FacilitatorUtils.readUserFromUsername(data, username);
            String userPassword = user == null ? "" : user.getPassword() == null ? "" : user.getPassword();
            // TODO: hashedPassword
            if (user == null || !userPassword.equals(password))
                ok = false;
            else
            {
                GroupRecord groupRecord = dslContext.selectFrom(Tables.GROUP)
                        .where(Tables.GROUP.GAMESESSION_ID.eq(gs.getId()).and(Tables.GROUP.FACILITATOR_ID.eq(user.getId())))
                        .fetchAny();
                if (groupRecord == null)
                    ok = false;
                else
                {
                    data.readFacilitatorData(user, groupRecord);
                    if (data.isState(GroupState.LOGIN))
                    {
                        if (data.getCurrentRoundNumber() == 0)
                            data.getCurrentGroupRound().setGroupState(GroupState.LOGIN.toString());
                        else
                            data.getCurrentGroupRound().setGroupState(GroupState.NEW_ROUND.toString());
                        data.getCurrentGroupRound().store();
                    }
                }
            }
        }

        if (ok)
        {
            response.sendRedirect("/housinggame-facilitator/facilitator");
        }
        else
        {
            session.removeAttribute("facilitatorData");
            response.sendRedirect("jsp/facilitator/login.jsp");
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        response.sendRedirect("jsp/facilitator/login.jsp");
    }

}
