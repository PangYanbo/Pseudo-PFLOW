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
import jp.ac.ut.csis.pflow.reallocation.ZmapPOI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgis.PGgeometry;
import org.postgis.Point;

public abstract class PgZmapReallocator
implements IPTReallocator {
    private static final Logger LOGGER = LogManager.getLogger(PgZmapReallocator.class);
    public static final String REALLOCATION_TABLE = System.getProperty("pflow.reallocation.table_name", "zmap_realloctable");
    private PgLoader _dbLoader;
    private String _tablename;
    private Random _random;

    public PgZmapReallocator(PgLoader dbLoader) {
        this(dbLoader, REALLOCATION_TABLE);
    }

    public PgZmapReallocator(PgLoader dbLoader, String tablename) {
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
    public ZmapPOI reallocate(String zonecode, int purpose) {
        String column = this.getColumnName(purpose);
        String sql = String.format("SELECT gid AS gid,zonecode,%s,geom FROM %s WHERE zonecode='%s' ORDER by %s DESC", column, this._tablename, zonecode, column);
        LOGGER.debug(sql);
        ZmapPOI output = null;
        try {
            Throwable throwable = null;
            Object var7_9 = null;
            try {
                Connection con = this._dbLoader.getConnection();
                try {
                    block21: {
                        Statement stmt = con.createStatement();
                        try {
                            try (ResultSet res = stmt.executeQuery(sql);){
                                double ratio = 0.0;
                                double rand = this._random.nextDouble();
                                while (res.next()) {
                                    if (!(rand <= (ratio += res.getDouble(column)))) continue;
                                    Point p = (Point)Point.class.cast(((PGgeometry)PGgeometry.class.cast(res.getObject("geom"))).getGeometry());
                                    output = new ZmapPOI(res.getString("gid"), res.getString("zonecode"), p.getX(), p.getY());
                                    break;
                                }
                            }
                            if (stmt == null) break block21;
                        }
                        catch (Throwable throwable2) {
                            if (throwable == null) {
                                throwable = throwable2;
                            } else if (throwable != throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                            if (stmt == null) throw throwable;
                            stmt.close();
                            throw throwable;
                        }
                        stmt.close();
                    }
                    if (con == null) return output;
                }
                catch (Throwable throwable3) {
                    if (throwable == null) {
                        throwable = throwable3;
                    } else if (throwable != throwable3) {
                        throwable.addSuppressed(throwable3);
                    }
                    if (con == null) throw throwable;
                    con.close();
                    throw throwable;
                }
                con.close();
                return output;
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                    throw throwable;
                }
                if (throwable == throwable4) throw throwable;
                throwable.addSuppressed(throwable4);
                throw throwable;
            }
        }
        catch (SQLException exp) {
            LOGGER.error("fail to query DB", (Throwable)exp);
            return null;
        }
    }

    protected abstract String getColumnName(int var1);

    public static enum ColumnName {
        H_RATIO,
        B_RATIO,
        S_RATIO;

    }
}

