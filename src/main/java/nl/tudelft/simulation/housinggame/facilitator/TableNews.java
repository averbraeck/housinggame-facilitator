package nl.tudelft.simulation.housinggame.facilitator;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.NewsitemRecord;

/**
 * TableNews fills the news table for the facilitator with the news up to the current round.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TableNews
{

    public static void makeNewsTable(final FacilitatorData data)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        StringBuilder s = new StringBuilder();
        s.append("        <h1>News for this round and previous rounds</h1>");
        s.append("        <div class=\"hg-fac-table\">\n");
        s.append("          <table class=\"pmd table table-striped\" style=\"text-align:left; width:90%;\">\n");
        s.append("                <thead>\n");
        s.append("                  <tr>\n");
        s.append("                    <th>Round</th>\n");
        s.append("                    <th>News Item</th>\n");
        s.append("                  </tr>\n");
        s.append("                </thead>\n");
        s.append("                <tbody>\n");
        List<NewsitemRecord> newsItemList =
                dslContext.selectFrom(Tables.NEWSITEM).where(Tables.NEWSITEM.SCENARIO_ID.eq(data.getScenario().getId()))
                        .and(Tables.NEWSITEM.ROUND_NUMBER.le(data.getCurrentRoundNumber())).fetch()
                        .sortDesc(Tables.NEWSITEM.ROUND_NUMBER);
        for (NewsitemRecord news : newsItemList)
        {
            s.append("                  <tr>\n");
            s.append("                    <td>" + news.getRoundNumber() + "</td>\n");
            s.append("                    <td style=\"text-align:left;\">" + news.getContent() + "</td>\n");
            s.append("                  </tr>\n");
        }
        s.append("                </tbody>\n");
        s.append("           </table>\n");
        s.append("        </div>\n");
        data.getContentHtml().put("facilitator/tables", s.toString());
    }

}
