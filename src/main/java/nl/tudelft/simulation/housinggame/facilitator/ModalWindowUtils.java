package nl.tudelft.simulation.housinggame.facilitator;

public class ModalWindowUtils
{

    public static void popup(final FacilitatorData data, final String title, final String message, final String okReturn)
    {
        // make popup
        StringBuilder s = new StringBuilder();
        s.append("<p>");
        s.append(message);
        s.append("</p>\n");
        data.setModalWindowHtml(makeOkModalWindow(title, s.toString(), okReturn));
        data.setShowModalWindow(1);
    }

    public static String makeModalWindow(final String title, final String content, final String onClickClose)
    {
        StringBuilder s = new StringBuilder();
        s.append("    <div class=\"hg-modal\">\n");
        s.append("      <div class=\"hg-modal-window\" id=\"hg-modal-window\">\n");
        s.append("        <div class=\"hg-modal-window-header\">");
        s.append("          <span class=\"hg-modal-close\" onclick=\"");
        s.append(onClickClose);
        s.append("\">");
        s.append("&times;</span>\n");
        s.append("          <p>");
        s.append(title);
        s.append("</p>\n");
        s.append("        </div>\n");
        s.append(content);
        s.append("      </div>\n");
        s.append("    </div>\n");
        s.append("    <script>");
        s.append("      dragElement(document.getElementById(\"hg-modal-window\"));");
        s.append("    </script>");
        return s.toString();
    }

    public static String makeOkModalWindow(final String title, final String htmlText, final String okReturn)
    {
        StringBuilder s = new StringBuilder();
        s.append("        <div class=\"hg-modal-body\">");
        s.append("          <div class=\"hg-modal-text\">\n");
        s.append("            <p>\n");
        s.append(htmlText);
        s.append("            </p>\n");
        s.append("          <div class=\"hg-modal-button-row\">\n");
        s.append("            <div class=\"hg-button-small\" onclick=\"" + okReturn + "\">OK</div>\n");
        s.append("          </div>\n");
        s.append("        </div>\n");
        return makeModalWindow(title, s.toString(), okReturn);
    }

    public static String makeOkModalWindow(final String title, final String htmlText)
    {
        return makeOkModalWindow(title, htmlText, "clickModalWindowOk()");
    }

    public static void make2ButtonModalWindow(final FacilitatorData data, final String title, final String content,
            final String buttonText1, final String buttonReturn1, final String buttonText2, final String buttonReturn2,
            final String closeReturn)
    {
        StringBuilder s = new StringBuilder();
        s.append("    <div class=\"hg-modal\">\n");
        s.append("      <div class=\"hg-modal-window\" id=\"hg-modal-window\">\n");
        s.append("        <div class=\"hg-modal-window-header\">");
        // TODO close button at the right
        s.append("          <p>");
        s.append(title);
        s.append("</p>\n");
        s.append("        </div>\n"); // hg-modal-window-header
        s.append("        <div class=\"hg-modal-body\">");
        s.append("          <div class=\"hg-modal-text\">\n");
        s.append("            <p>\n");
        s.append(content);
        s.append("            </p>\n");
        s.append("          </div>\n"); // hg-modal-text
        s.append("          <div class=\"hg-modal-button-row\">\n");

        s.append("            <div class=\"hg-button-small\"\n");
        s.append("              <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("                <input type=\"hidden\" name=\"button\" value=\"");
        s.append(buttonReturn1);
        s.append("\" />\n");
        s.append("                <div class=\"hg-button\">\n");
        s.append("                  <input type=\"submit\" value=\"");
        s.append(buttonText1);
        s.append("\" />\n");
        s.append("                </div>\n");
        s.append("              </form>\n");
        s.append("            </div>\n");

        s.append("            <div class=\"hg-button-small\"\n");
        s.append("              <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("                <input type=\"hidden\" name=\"button\" value=\"");
        s.append(buttonReturn2);
        s.append("\" />\n");
        s.append("                <div class=\"hg-button\">\n");
        s.append("                  <input type=\"submit\" value=\"");
        s.append(buttonText2);
        s.append("\" />\n");
        s.append("                </div>\n");
        s.append("              </form>\n");
        s.append("            </div>\n");

        s.append("          </div>\n"); // hg-modal-button-row
        s.append("        </div>\n"); // hg-hg-modal-body
        s.append("      </div>\n"); // hg-modal-window
        s.append("    </div>\n"); // hg-modal
        s.append("  </form>\n");
        s.append("  <script>");
        s.append("    dragElement(document.getElementById(\"hg-modal-window\"));");
        s.append("  </script>");

        data.setModalWindowHtml(s.toString());
        data.setShowModalWindow(1);
    }

}
