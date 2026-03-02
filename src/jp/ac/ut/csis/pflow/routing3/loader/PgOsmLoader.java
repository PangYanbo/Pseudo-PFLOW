/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.Geometry
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.MultiLineString
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.io.ParseException
 *  org.locationtech.jts.io.WKBReader
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang.StringUtils
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing3.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.OsmLink;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PgOsmLoader
extends APgNetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(PgOsmLoader.class);
    public static final String NETWORK_TABLE = "osm.japan_road";
    public static final String OSM_LINK_ID_COLUMN = "id";
    public static final String OSM_SOURCE_NODE_COLUMN = "source";
    public static final String OSM_TARGET_NODE_COLUMN = "target";
    public static final String OSM_COST_COLUMN = "cost";
    public static final String OSM_REVERSER_COST_COLUMN = "reverse_cost";
    public static final String OSM_GEOMETRY_COLUMN = "geom_way";
    public static final String OSM_LEGNTH_COLUMN = "km";
    public static final String OSM_VELOCITY_COLUMN = "kmh";
    public static final String OSM_ROAD_TYPE_COLUMN = "clazz";

    public PgOsmLoader() {
        this(NETWORK_TABLE);
    }

    public PgOsmLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition[] qcs) {
        ArrayList<String> whereClauses = new ArrayList<String>();
        if (qcs != null) {
            QueryCondition[] queryConditionArray = qcs;
            int n = qcs.length;
            int n2 = 0;
            while (n2 < n) {
                int[] roadTypes;
                QueryCondition qc = queryConditionArray[n2];
                ArrayList<String> tokens = new ArrayList<String>();
                Rectangle2D bounds = qc.getRects();
                if (bounds != null) {
                    tokens.add(String.format("ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", OSM_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer()));
                }
                int[] nArray = roadTypes = qc instanceof OsmQueryCondition ? ((OsmQueryCondition)OsmQueryCondition.class.cast(qc)).getRoadTypes() : null;
                if (roadTypes != null && roadTypes.length > 0) {
                    tokens.add(String.format(" AND %s in (%s)", OSM_ROAD_TYPE_COLUMN, StringUtils.join((Object[])ArrayUtils.toObject((int[])roadTypes), (String)",")));
                }
                if (!tokens.isEmpty()) {
                    whereClauses.add(String.format("(%s)", StringUtils.join(tokens, (String)" AND ")));
                }
                ++n2;
            }
        }
        String where = String.format("WHERE %s<>%s", OSM_SOURCE_NODE_COLUMN, OSM_TARGET_NODE_COLUMN);
        if (!whereClauses.isEmpty()) {
            where = String.valueOf(where) + String.format(" AND (%s)", StringUtils.join(whereClauses, (String)" OR "));
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", OSM_GEOMETRY_COLUMN, this.getTableName(), where);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public Network load(Network network, Connection con, String sql, boolean needGeom) {
        try {
            Throwable throwable = null;
            Object var6_8 = null;
            try {
                Statement stmt = con.createStatement();
                try {
                    try (ResultSet res = stmt.executeQuery(sql);){
                        WKBReader wkbreader = new WKBReader();
                        while (res.next()) {
                            int gid = res.getInt(OSM_LINK_ID_COLUMN);
                            String src = String.valueOf(res.getInt(OSM_SOURCE_NODE_COLUMN));
                            String tgt = String.valueOf(res.getInt(OSM_TARGET_NODE_COLUMN));
                            double len = res.getDouble(OSM_LEGNTH_COLUMN) * 1000.0;
                            double cst = res.getDouble(OSM_LEGNTH_COLUMN) * 1000.0;
                            double rcst = res.getDouble(OSM_LEGNTH_COLUMN) * 1000.0;
                            double spd = (double)res.getInt(OSM_VELOCITY_COLUMN) * 1000.0 / 3600.0;
                            int clz = res.getInt(OSM_ROAD_TYPE_COLUMN);
                            boolean way = false;
                            Geometry geom = wkbreader.read(res.getBytes("bgeom"));
                            LineString line = null;
                            if (geom instanceof LineString) {
                                line = (LineString)LineString.class.cast(geom);
                            } else if (geom instanceof MultiLineString) {
                                line = (LineString)((MultiLineString)MultiLineString.class.cast(geom)).getGeometryN(0);
                            }
                            Point p0 = line.getStartPoint();
                            Point p1 = line.getEndPoint();
                            List<LonLat> list = needGeom ? GeometryUtils.createPointList(line) : null;
                            Node n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
                            Node n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                            OsmLink link = new OsmLink(String.valueOf(gid), n0, n1, len, cst, rcst, way, clz, spd, list);
                            network.addLink(link);
                        }
                    }
                    if (stmt == null) return network;
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
                return network;
            }
            catch (Throwable throwable3) {
                if (throwable == null) {
                    throwable = throwable3;
                    throw throwable;
                } else {
                    if (throwable == throwable3) throw throwable;
                    throwable.addSuppressed(throwable3);
                }
                throw throwable;
            }
        }
        catch (ParseException | OutOfMemoryError | SQLException exp) {
            LOGGER.error("fail to load network", exp);
            return null;
        }
    }
}

