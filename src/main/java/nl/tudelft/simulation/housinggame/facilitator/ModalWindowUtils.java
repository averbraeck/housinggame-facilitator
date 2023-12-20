package nl.tudelft.simulation.housinggame.facilitator;

import java.util.HashMap;
import java.util.Map;

public class ModalWindowUtils
{

    public static void makeModalWindowMethod(final FacilitatorData data, final String title, final String content,
            final String button, final String buttonMethod)
    {
        StringBuilder s = new StringBuilder();
        s.append("    <div class=\"hg-modal\">\n");
        s.append("      <div class=\"hg-modal-window\" id=\"hg-modal-window\">\n");
        s.append("        <div class=\"hg-modal-window-header\" id=\"hg-modal-window-header\">");
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

        s.append("            <div class=\"hg-button\">\n");
        s.append("              <div class=\"btn btn-primary\" onclick=\"" + buttonMethod + "\">" + button + "</div>\n");
        s.append("            </div>\n");

        s.append("            <div>\n");
        s.append("              <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("                <input type=\"hidden\" name=\"button\" value=\"\" />\n");
        s.append("                <div class=\"hg-button\">\n");
        s.append("                  <input type=\"submit\" value=\"CANCEL\" class=\"btn btn-primary\" />\n");
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

    public static void make2ButtonModalWindow(final FacilitatorData data, final String title, final String content,
            final String buttonText1, final String buttonReturn1, final String buttonText2, final String buttonReturn2,
            final String closeReturn, final Map<String, String> parameterMap)
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

        s.append("            <div>\n");
        s.append("              <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("                <input type=\"hidden\" name=\"button\" value=\"");
        s.append(buttonReturn1);
        s.append("\" />\n");
        for (String key : parameterMap.keySet())
            s.append(
                    "                <input type=\"hidden\" name=\"" + key + "\" value=\"" + parameterMap.get(key) + "\" />\n");
        s.append("                <div class=\"hg-button\">\n");
        s.append("                  <input type=\"submit\" value=\"");
        s.append(buttonText1);
        s.append("\" class=\"btn btn-primary\" />\n");
        s.append("                </div>\n");
        s.append("              </form>\n");
        s.append("            </div>\n");

        s.append("            <div>\n");
        s.append("              <form action=\"/housinggame-facilitator/facilitator\" method=\"post\">\n");
        s.append("                <input type=\"hidden\" name=\"button\" value=\"");
        s.append(buttonReturn2);
        s.append("\" />\n");
        s.append("                <div class=\"hg-button\">\n");
        s.append("                  <input type=\"submit\" value=\"");
        s.append(buttonText2);
        s.append("\" class=\"btn btn-primary\" />\n");
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

    public static void make2ButtonModalWindow(final FacilitatorData data, final String title, final String content,
            final String buttonText1, final String buttonReturn1, final String buttonText2, final String buttonReturn2,
            final String closeReturn)
    {
        make2ButtonModalWindow(data, title, content, buttonText1, buttonReturn1, buttonText2, buttonReturn2, closeReturn,
                new HashMap<>());
    }

}
