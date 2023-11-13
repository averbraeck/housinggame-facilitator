package nl.tudelft.simulation.housinggame.facilitator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/facilitator")
@MultipartConfig
public class FacilitatorServlet extends HttpServlet {

	/** */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();

		FacilitatorData data = SessionUtils.getData(session);
		if (data == null) {
			response.sendRedirect("/housinggame-facilitator/login");
			return;
		}

		String click = "";
		if (request.getParameter("click") != null)
			click = request.getParameter("click").toString();
		else if (request.getParameter("editClick") != null)
			click = request.getParameter("editClick").toString();

		int recordNr = 0;
		if (request.getParameter("recordNr") != null)
			recordNr = Integer.parseInt(request.getParameter("recordNr"));
		else if (request.getParameter("editRecordNr") != null)
			recordNr = Integer.parseInt(request.getParameter("editRecordNr"));

		data.setShowModalWindow(0);
		data.setModalWindowHtml("");

		switch (click) {
		case "language":
			break;

		default:
			break;
		}

		response.sendRedirect("jsp/facilitator/facilitator.jsp");
	}

	public static String getTopMenu(final FacilitatorData data) {
		StringBuilder s = new StringBuilder();
		topmenu(data, s, "language", "Language"); // Language - LanguageGroup (non-heriarchic) - Label
		topmenu(data, s, "parameters", "Parameters"); // ScenarioParameters
		topmenu(data, s, "scenario", "Scenario"); // GameVersion - Scenario
		topmenu(data, s, "welfaretype", "Welfare"); // (GameVersion) - (Scenario) - WelfareType
		topmenu(data, s, "measuretype", "Measure"); // (GameVersion) - MeasureType
		topmenu(data, s, "community", "Community"); // (GameVersion) - Community - Tax
		topmenu(data, s, "house", "House"); // (GameVersion) - (Community) - House - InitialHouseMeasure
		topmenu(data, s, "round", "Round"); // (GameVersion) - (Scenario) - Round - NewsItem
		topmenu(data, s, "question", "Question"); // (GameVersion) - (Scenario) - Question
		topmenu(data, s, "user", "User"); // User - Facilitator
		topmenu(data, s, "gamesession", "Session"); // (GameVersion) - GameSession - Group - Player
		topmenu(data, s, "play", "Play"); // (GameSession) - (Group) - GroupRound - (Player) - PlayerRound -
											// Measure/Question
		topmenu(data, s, "result", "Result"); // (GameSession) - (Group) - Result
		return s.toString();
	}

	private static final String bn = "          <div class=\"hg-facilitator-menu-button\"";

	private static final String br = "          <div class=\"hg-facilitator-menu-button-red\"";

	private static void topmenu(final FacilitatorData data, final StringBuilder s, final String key,
			final String text) {
		s.append(key.equals(data.getMenuChoice()) ? br : bn);
		s.append(" onclick=\"clickMenu('");
		s.append(key);
		s.append("')\">");
		s.append(text);
		s.append("</div>\n");
	}
}
