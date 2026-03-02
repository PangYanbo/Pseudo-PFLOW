/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.postgis.PGgeometry
 *  org.postgis.Point
 */
package jp.ac.ut.csis.pflow.reallocation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.reallocation.IPTReallocator;
import jp.ac.ut.csis.pflow.reallocation.TelepointPOI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgis.PGgeometry;
import org.postgis.Point;

public abstract class PgTelepointReallocator
implements IPTReallocator {
    private static final Logger LOGGER = LogManager.getLogger(PgTelepointReallocator.class);
    public static final String REALLOCATION_TABLE = System.getProperty("pflow.reallocation.table_name", "telepoint_realloctable");
    private PgLoader _dbLoader;
    private String _tablename;
    private Random _random;

    public PgTelepointReallocator(PgLoader dbLoader) {
        this(dbLoader, REALLOCATION_TABLE);
    }

    public PgTelepointReallocator(PgLoader dbLoader, String tablename) {
        this._dbLoader = dbLoader;
        this._tablename = tablename;
        this._random = new Random();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public TelepointPOI reallocate(String zonecode, int purpose) {
        String[] columns = this.getColumnNames(purpose);
        TelepointPOI output = null;
        String[] stringArray = columns;
        int n = columns.length;
        int n2 = 0;
        while (n2 < n) {
            String column = stringArray[n2];
            String sql = String.format("SELECT gid AS gid,zonecode,%s,geom FROM %s WHERE zonecode='%s' ORDER by %s DESC", column, this._tablename, zonecode, column);
            LOGGER.debug(sql);
            try (Connection con = this._dbLoader.getConnection();
                 Statement stmt = con.createStatement();
                 ResultSet res = stmt.executeQuery(sql)) {
                double ratio = 0.0;
                double rand = this._random.nextDouble();
                while (res.next()) {
                    if (!(rand <= (ratio += res.getDouble(column)))) continue;
                    Point p = (Point)Point.class.cast(((PGgeometry)PGgeometry.class.cast(res.getObject("geom"))).getGeometry());
                    output = new TelepointPOI(res.getString("gid"), res.getString("zonecode"), p.getX(), p.getY());
                    break;
                }
                if (output != null) return output;
            }
            catch (SQLException exp) {
                LOGGER.error("fail to query DB", (Throwable)exp);
                output = null;
            }
            ++n2;
        }
        return output;
    }

    protected abstract String[] getColumnNames(int var1);
}

