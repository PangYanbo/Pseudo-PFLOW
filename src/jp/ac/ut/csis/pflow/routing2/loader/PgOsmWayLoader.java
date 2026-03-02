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
import java.util.Collections;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.OsmWay;

public class PgOsmWayLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = System.getProperty("pgloader.osm_ways", "public.ways");
    public static final String OSM_LINK_ID_COLUMN = System.getProperty("pgloader.osm_ways.link_id_column", LINK_ID_COLUMN);
    public static final String OSM_SOURCE_NODE_COLUMN = System.getProperty("pgloader.osm_ways.source_column", SOURCE_NODE_COLUMN);
    public static final String OSM_TARGET_NODE_COLUMN = System.getProperty("pgloader.osm_ways.target_column", TARGET_NODE_COLUMN);
    public static final String OSM_LENGTH_COLUMN = System.getProperty("pgloader.osm_ways.length_column", "length_m");
    public static final String OSM_COST_COLUMN = System.getProperty("pgloader.osm_ways.cost_column", "cost_s");
    public static final String OSM_REVERSE_COST_COLUMN = System.getProperty("pgloader.osm_ways.reverse_cost_column", "reverse_cost_s");
    public static final String OSM_FORWARD_SPEED_COLUMN = System.getProperty("pgloader.osm_ways.forward_speed_column", "maxspeed_forward");
    public static final String OSM_BACKWARD_SPEED_COLUMN = System.getProperty("pgloader.osm_ways.backward_speed_column", "maxspeed_backward");
    public static final String OSM_ONE_WAY_COLUMN = System.getProperty("pgloader.osm_ways.one_way_column", "one_way");
    public static final String OSM_CLASS_ID_COLUMN = System.getProperty("pgloader.osm_ways.class_id_column", "class_id");
    public static final String OSM_GEOMETRY_COLUMN = System.getProperty("pgloader.osm_ways.geometry_column", "the_geom");

    public PgOsmWayLoader() {
        this(NETWORK_TABLE);
    }

    public PgOsmWayLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        Rectangle2D bounds;
        String cond = String.format("WHERE %s<>%s", OSM_SOURCE_NODE_COLUMN, OSM_TARGET_NODE_COLUMN);
        if (qc != null && (bounds = qc.getRects()) != null) {
            cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", OSM_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
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
                            Node n1;
                            Node n0;
                            List<LonLat> list;
                            String gid = String.valueOf(res.getLong(OSM_LINK_ID_COLUMN));
                            String src = String.valueOf(res.getInt(OSM_SOURCE_NODE_COLUMN));
                            String tgt = String.valueOf(res.getInt(OSM_TARGET_NODE_COLUMN));
                            int clazz = res.getInt(OSM_CLASS_ID_COLUMN);
                            double length = res.getDouble(OSM_LENGTH_COLUMN);
                            double cst = res.getDouble(OSM_COST_COLUMN);
                            double rcst = res.getDouble(OSM_REVERSE_COST_COLUMN);
                            double fspeed = (double)res.getInt(OSM_FORWARD_SPEED_COLUMN) * 1000.0 / 3600.0;
                            double bspeed = (double)res.getInt(OSM_BACKWARD_SPEED_COLUMN) * 1000.0 / 3600.0;
                            int wayFlag = res.getInt(OSM_ONE_WAY_COLUMN);
                            Geometry geom = wkbreader.read(res.getBytes("bgeom"));
                            LineString line = null;
                            if (geom instanceof LineString) {
                                line = (LineString)LineString.class.cast(geom);
                            } else if (geom instanceof MultiLineString) {
                                line = (LineString)((MultiLineString)MultiLineString.class.cast(geom)).getGeometryN(0);
                            }
                            Point p0 = line.getStartPoint();
                            Point p1 = line.getEndPoint();
                            boolean oneway = wayFlag == -1 || wayFlag == 1;
                            List<LonLat> list2 = list = needGeom ? GeometryUtils.createPointList(line) : null;
                            if (wayFlag == -1) {
                                n0 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                                Node node = n1 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
                                if (list != null && !list.isEmpty()) {
                                    Collections.reverse(list);
                                }
                            } else {
                                n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
                                n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                            }
                            OsmWay link = new OsmWay(gid, n0, n1, clazz, length, cst, rcst, fspeed, bspeed, oneway, list);
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
            exp.printStackTrace();
            return null;
        }
    }
}

