package nl.tudelft.simulation.housinggame.facilitator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import nl.tudelft.simulation.housinggame.data.Tables;
import nl.tudelft.simulation.housinggame.data.tables.records.GrouproundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.PlayerroundRecord;
import nl.tudelft.simulation.housinggame.data.tables.records.UserRecord;

public final class FacilitatorUtils
{

    private FacilitatorUtils()
    {
        // utility class
    }

    public static Connection dbConnection() throws SQLException, ClassNotFoundException
    {
        String jdbcURL = "jdbc:mysql://localhost:3306/housinggame";
        String dbUser = "housinggame";
        String dbPassword = "tudHouse#4";

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(jdbcURL, dbUser, dbPassword);
    }

    public static UserRecord readUserFromUserId(final FacilitatorData data, final int userId)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.USER).where(Tables.USER.ID.eq(userId)).fetchAny();
    }

    public static UserRecord readUserFromUsername(final FacilitatorData data, final String username)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(Tables.USER).where(Tables.USER.USERNAME.eq(username)).fetchAny();
    }

    @SuppressWarnings("unchecked")
    public static <R extends org.jooq.UpdatableRecord<R>> R readRecordFromId(final FacilitatorData data, final Table<R> table,
            final int recordId)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        return dslContext.selectFrom(table).where(((TableField<R, Integer>) table.field("id")).eq(recordId)).fetchOne();
    }

    /**
     * This method always returns a list of length up to and including the current round number. Not played rounds are null.
     * @param data FacilitatorData
     * @param playerId player to retrieve
     * @return list of PlayerRoundRecords
     */
    public static List<PlayerroundRecord> getPlayerRoundList(final FacilitatorData data, final int playerId)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        List<PlayerroundRecord> playerRoundList = new ArrayList<>();
        for (int i = 0; i < data.getGroupRoundList().size(); i++)
        {
            GrouproundRecord groupRound = data.getGroupRoundList().get(i);
            PlayerroundRecord playerRound =
                    dslContext.selectFrom(Tables.PLAYERROUND).where(Tables.PLAYERROUND.PLAYER_ID.eq(playerId))
                            .and(Tables.PLAYERROUND.GROUPROUND_ID.eq(groupRound.getId())).fetchAny();
            playerRoundList.add(playerRound);
        }
        return playerRoundList;
    }

    /**
     * Return the current PlayerRound or null when not started.
     * @param data FacilitatorData
     * @param playerId player to retrieve
     * @return list of PlayerRoundRecords
     */
    public static PlayerroundRecord getCurrentPlayerRound(final FacilitatorData data, final int playerId)
    {
        DSLContext dslContext = DSL.using(data.getDataSource(), SQLDialect.MYSQL);
        GrouproundRecord groupRound = data.getGroupRoundList().get(data.getGroupRoundList().size() - 1);
        return dslContext.selectFrom(Tables.PLAYERROUND).where(Tables.PLAYERROUND.PLAYER_ID.eq(playerId))
                .and(Tables.PLAYERROUND.GROUPROUND_ID.eq(groupRound.getId())).fetchAny();
    }

    public static PlayerroundRecord getLastPlayerRound(final FacilitatorData data, final int playerId)
    {
        List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, playerId);
        if (!playerRoundList.isEmpty())
        {
            PlayerroundRecord prr = playerRoundList.get(0);
            for (int i = 0; i < playerRoundList.size(); i++)
            {
                if (playerRoundList.get(i) != null)
                    prr = playerRoundList.get(i);
            }
            return prr;
        }
        return null;
    }

    public static PlayerroundRecord getPrevPlayerRound(final FacilitatorData data, final int playerId)
    {
        List<PlayerroundRecord> playerRoundList = FacilitatorUtils.getPlayerRoundList(data, playerId);
        if (!playerRoundList.isEmpty())
        {
            PlayerroundRecord prr = playerRoundList.get(0);
            PlayerroundRecord prrPrev = playerRoundList.get(0);
            for (int i = 0; i < playerRoundList.size(); i++)
            {
                if (playerRoundList.get(i) != null)
                {
                    prrPrev = prr;
                    prr = playerRoundList.get(i);
                }
            }
            return prrPrev;
        }
        return null;
    }

}
