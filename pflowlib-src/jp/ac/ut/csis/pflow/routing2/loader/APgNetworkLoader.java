/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.postgis.Geometry
 *  org.postgis.LineString
 *  org.postgis.MultiLineString
 *  org.postgis.PGgeometry
 *  org.postgis.Point
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.MultiLineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

public abstract class APgNetworkLoader
extends ANetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(APgNetworkLoader.class);
    public static final String GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.geometry_column", "geom");
    public static final String LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.link_id_column", "gid");
    public static final String SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.source_column", "source");
    public static final String TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.target_column", "target");
    public static final String COST_COLUMN = System.getProperty("pflow.routing2.pgloader.cost_column", "cost");
    public static final String REVERSER_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.reverse_cost_column", "reverse_cost");
    private String _tablename;

    public static List<LonLat> parseLineString(LineString geomLineString) {
        ArrayList<LonLat> linestring = new ArrayList<LonLat>();
        Point[] pointArray = geomLineString.getPoints();
        int n = pointArray.length;
        int n2 = 0;
        while (n2 < n) {
            Point p = pointArray[n2];
            linestring.add(new LonLat(p.getX(), p.getY()));
            ++n2;
        }
        return linestring;
    }

    protected APgNetworkLoader(String tablename) {
        this._tablename = tablename;
    }

    public String getTableName() {
        return this._tablename;
    }

    @Override
    public Network load(QueryCondition[] conds) {
        return this.load(new Network(), conds);
    }

    @Override
    public Network load(Network network, QueryCondition[] conds) {
        try (PgLoader loader = new PgLoader();){
            try {
                Throwable throwable = null;
                Object var5_7 = null;
                try (Connection con = loader.getConnection();){
                    network = con == null ? null : this.load(con, conds);
                }
                catch (Throwable throwable2) {
                    if (throwable == null) {
                        throwable = throwable2;
                    } else if (throwable != throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            }
            catch (SQLException exp) {
                LOGGER.error("fail to load network", (Throwable)exp);
                loader.close();
            }
        }
        return network;
    }

    public Network load(Connection con, double x0, double y0, double x1, double y1, boolean needGeom) {
        return this.load(con, new double[]{x0, y0, x1, y1}, needGeom);
    }

    public Network load(Connection con) {
        return this.load(con, new double[0], true);
    }

    public Network load(Connection con, boolean needGeom) {
        return this.load(con, new double[0], needGeom);
    }

    public Network load(Connection con, double[] rect, boolean needGeom) {
        return this.load(con, new QueryCondition(rect, 3000.0, needGeom));
    }

    public Network load(Connection con, double[] rect, double bufSize, boolean needGeom) {
        return this.load(con, new QueryCondition(rect, bufSize, needGeom));
    }

    public Network load(Connection con, QueryCondition cond) {
        return this.load(con, new QueryCondition[]{cond});
    }

    public Network load(Network network, Connection con, QueryCondition cond) {
        return this.load(network, con, new QueryCondition[]{cond});
    }

    public Network load(Connection con, QueryCondition[] conds) {
        return this.load(new Network(), con, conds);
    }

    public Network load(Network network, Connection con) {
        return this.load(network, con, new QueryCondition[0]);
    }

    public Network load(Network network, Connection con, QueryCondition[] conds) {
        if (conds != null && conds.length > 0) {
            QueryCondition[] queryConditionArray = conds;
            int n = conds.length;
            int n2 = 0;
            while (n2 < n) {
                QueryCondition cond = queryConditionArray[n2];
                String sql = this.createQuery(cond);
                network = this.load(network, con, sql, cond.needGeom());
                ++n2;
            }
        } else {
            String sql = this.createQuery(null);
            network = this.load(network, con, sql, true);
        }
        return network;
    }

    protected abstract String createQuery(QueryCondition var1);

    public abstract Network load(Network var1, Connection var2, String var3, boolean var4);

    public Network load(Connection con, String sql, boolean needGeom) {
        return this.load(new Network(), con, sql, needGeom);
    }

    public List<LonLat> fillGeometry(Connection con, List<Node> nodes) {
        ArrayList<LonLat> points = new ArrayList<LonLat>();
        try {
            try {
                Throwable throwable = null;
                Object var5_7 = null;
                try (Statement stmt = con.createStatement();){
                    int N = nodes.size();
                    Node n0 = nodes.get(0);
                    int i = 1;
                    while (i < N) {
                        Node n1 = nodes.get(i);
                        String sql = String.format("select %s,%s,%s from %s where (source='%s' and target='%s') OR (source='%s' and target='%s') ", SOURCE_NODE_COLUMN, TARGET_NODE_COLUMN, GEOMETRY_COLUMN, this.getTableName(), n0.getNodeID(), n1.getNodeID(), n1.getNodeID(), n0.getNodeID());
                        try {
                            Throwable throwable2 = null;
                            Object var13_18 = null;
                            try (ResultSet res = stmt.executeQuery(sql);){
                                if (res.next()) {
                                    String src = res.getString(SOURCE_NODE_COLUMN);
                                    Geometry geom = ((PGgeometry)PGgeometry.class.cast(res.getObject(GEOMETRY_COLUMN))).getGeometry();
                                    LineString line = null;
                                    if (geom instanceof LineString) {
                                        line = (LineString)LineString.class.cast(geom);
                                    } else if (geom instanceof MultiLineString) {
                                        line = ((MultiLineString)MultiLineString.class.cast(geom)).getLine(0);
                                    }
                                    if (src.equals(n0.getNodeID())) {
                                        points.addAll(APgNetworkLoader.parseLineString(line));
                                    } else {
                                        points.addAll(APgNetworkLoader.parseLineString(line.reverse()));
                                    }
                                }
                            }
                            catch (Throwable throwable3) {
                                if (throwable2 == null) {
                                    throwable2 = throwable3;
                                } else if (throwable2 != throwable3) {
                                    throwable2.addSuppressed(throwable3);
                                }
                                throw throwable2;
                            }
                        }
                        catch (SQLException exp) {
                            LOGGER.error("fail to load geometry", (Throwable)exp);
                        }
                        n0 = n1;
                        ++i;
                    }
                }
                catch (Throwable throwable4) {
                    if (throwable == null) {
                        throwable = throwable4;
                    } else if (throwable != throwable4) {
                        throwable.addSuppressed(throwable4);
                    }
                    throw throwable;
                }
            }
            catch (OutOfMemoryError | SQLException exp) {
                LOGGER.error("fail to load geometry", exp);
                points = null;
                System.gc();
            }
        }
        finally {
            System.gc();
        }
        return points;
    }
}

