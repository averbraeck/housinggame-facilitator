package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GamesessionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GameversionRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GroupRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.RoundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.ScenarioRecord;
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

        FacilitatorData data = (FacilitatorData) session.getAttribute("facilitatorData");
        session.setAttribute("facilitatorData", data);
        try
        {
            data.setDataSource((DataSource) new InitialContext().lookup("/housinggame-facilitator_datasource"));
        }
        catch (NamingException e)
        {
            throw new ServletException(e);
        }

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
            GamesessionRecord gs = SqlUtils.readRecordFromId(data, Tables.GAMESESSION, gameSessionId);
            UserRecord user = SqlUtils.readUserFromUsername(data, username);
            String userPassword = user == null ? "" : user.getPassword() == null ? "" : user.getPassword();
            // TODO: hashedPassword
            if (user == null || !userPassword.equals(password))
                ok = false;
            else
            {
                data.setUser(user);
                GroupRecord groupRecord = dslContext.selectFrom(Tables.GROUP)
                        .where(Tables.GROUP.GAMESESSION_ID.eq(gs.getId()).and(Tables.GROUP.FACILITATOR_ID.eq(user.getId())))
                        .fetchAny();
                if (groupRecord == null)
                    ok = false;
                else
                {
                    data.setGroup(groupRecord);
                    data.setGameSession(gs);
                    ScenarioRecord scenario = SqlUtils.readRecordFromId(data, Tables.SCENARIO, groupRecord.getScenarioId());
                    data.setScenario(scenario);
                    GameversionRecord gameVersion =
                            SqlUtils.readRecordFromId(data, Tables.GAMEVERSION, scenario.getGameversionId());
                    data.setGameVersion(gameVersion);
                    GrouproundRecord groupRound = SqlUtils.getOrMakeLatestGroupRound(data, data.getGroup());
                    RoundRecord round =
                            dslContext.selectFrom(Tables.ROUND).where(Tables.ROUND.ID.eq(groupRound.getRoundId())).fetchOne();
                    data.setRound(round);
                    data.setGroupRound(groupRound);
                }
            }
        }

        if (ok)
        {
            response.sendRedirect("jsp/facilitator/facilitator-player.jsp");
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
