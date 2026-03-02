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
            try {
                Throwable throwable = null;
                Object var11_13 = null;
                try {
                    ResultSet res;
                    Statement stmt;
                    Connection con;
                    block25: {
                        con = this._dbLoader.getConnection();
                        try {
                            block27: {
                                stmt = con.createStatement();
                                try {
                                    block26: {
                                        res = stmt.executeQuery(sql);
                                        try {
                                            double ratio = 0.0;
                                            double rand = this._random.nextDouble();
                                            while (res.next()) {
                                                if (!(rand <= (ratio += res.getDouble(column)))) continue;
                                                Point p = (Point)Point.class.cast(((PGgeometry)PGgeometry.class.cast(res.getObject("geom"))).getGeometry());
                                                output = new TelepointPOI(res.getString("gid"), res.getString("zonecode"), p.getX(), p.getY());
                                                break;
                                            }
                                            if (output == null) break block25;
                                            if (res == null) break block26;
                                        }
                                        catch (Throwable throwable2) {
                                            if (res == null) throw throwable2;
                                            res.close();
                                            throw throwable2;
                                        }
                                        res.close();
                                    }
                                    if (stmt == null) break block27;
                                }
                                catch (Throwable throwable3) {
                                    if (throwable == null) {
                                        throwable = throwable3;
                                    } else if (throwable != throwable3) {
                                        throwable.addSuppressed(throwable3);
                                    }
                                    if (stmt == null) throw throwable;
                                    stmt.close();
                                    throw throwable;
                                }
                                stmt.close();
                            }
                            if (con == null) return output;
                        }
                        catch (Throwable throwable4) {
                            if (throwable == null) {
                                throwable = throwable4;
                            } else if (throwable != throwable4) {
                                throwable.addSuppressed(throwable4);
                            }
                            if (con == null) throw throwable;
                            con.close();
                            throw throwable;
                        }
                        con.close();
                        return output;
                    }
                    if (res != null) {
                        res.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                }
                catch (Throwable throwable5) {
                    if (throwable == null) {
                        throwable = throwable5;
                        throw throwable;
                    }
                    if (throwable == throwable5) throw throwable;
                    throwable.addSuppressed(throwable5);
                    throw throwable;
                }
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

