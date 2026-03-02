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
package jp.ac.ut.csis.pflow.routing2.loader;

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
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.OsmLink;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PgOsmLoader
extends APgNetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(PgOsmLoader.class);
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.osm_network_table", "osm.japan_road");
    public static final String OSM_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_link_id_column", "id");
    public static final String OSM_SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_source_column", SOURCE_NODE_COLUMN);
    public static final String OSM_TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_target_column", TARGET_NODE_COLUMN);
    public static final String OSM_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_cost_column", COST_COLUMN);
    public static final String OSM_REVERSER_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_reverse_cost_column", REVERSER_COST_COLUMN);
    public static final String OSM_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_geometry_column", "geom_way");
    public static final String OSM_LEGNTH_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_length_column", "km");
    public static final String OSM_VELOCITY_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_velocity_column", "kmh");
    public static final String OSM_ROAD_TYPE_COLUMN = System.getProperty("pflow.routing2.pgloader.osm_road_type_column", "clazz");

    public PgOsmLoader() {
        this(NETWORK_TABLE);
    }

    public PgOsmLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        int[] roadTypes;
        Rectangle2D bounds;
        String cond = String.format("WHERE %s<>%s", OSM_SOURCE_NODE_COLUMN, OSM_TARGET_NODE_COLUMN);
        if (qc != null && (bounds = qc.getRects()) != null) {
            cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", OSM_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
        }
        int[] nArray = roadTypes = qc instanceof OsmQueryCondition ? ((OsmQueryCondition)OsmQueryCondition.class.cast(qc)).getRoadTypes() : null;
        if (roadTypes != null && roadTypes.length > 0) {
            cond = String.valueOf(cond) + String.format(" AND %s in (%s)", OSM_ROAD_TYPE_COLUMN, StringUtils.join((Object[])ArrayUtils.toObject((int[])roadTypes), (String)","));
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", OSM_GEOMETRY_COLUMN, this.getTableName(), cond);
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
                            OsmLink link = new OsmLink(String.valueOf(gid), n0, n1, cst, rcst, way, clz, spd, list);
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

