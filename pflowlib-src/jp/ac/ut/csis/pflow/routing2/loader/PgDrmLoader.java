/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.Geometry
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.MultiLineString
 *  com.vividsolutions.jts.geom.Point
 *  com.vividsolutions.jts.io.ParseException
 *  com.vividsolutions.jts.io.WKBReader
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
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
import jp.ac.ut.csis.pflow.routing2.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.DrmLink;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class PgDrmLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.drm_network_table", "data2503.drmallroad");
    public static final String DRM_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_link_id_column", LINK_ID_COLUMN);
    public static final String DRM_SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_source_column", SOURCE_NODE_COLUMN);
    public static final String DRM_TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_target_column", TARGET_NODE_COLUMN);
    public static final String DRM_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_cost_column", "length");
    public static final String DRM_REVERSER_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_reverse_cost_column", REVERSER_COST_COLUMN);
    public static final String DRM_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_geometry_column", "the_geom");
    public static final String DRM_REGULATION_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_regulation_column", "regulation");
    public static final String DRM_ROAD_WIDTH_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_road_width_column", "road_width");
    public static final String DRM_ROAD_TYPE_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_road_type_column", "road_type");
    public static final String DRM_LANE_NUM_COLUMN = System.getProperty("pflow.routing2.pgloader.drm_lane_num_column", "lane_num");

    public PgDrmLoader() {
        this(NETWORK_TABLE);
    }

    public PgDrmLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        String cond = String.format("WHERE %s<>%s", DRM_SOURCE_NODE_COLUMN, DRM_TARGET_NODE_COLUMN);
        if (qc != null) {
            int[] roadTypes;
            Rectangle2D bounds = qc.getRects();
            if (bounds != null) {
                cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", DRM_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
            }
            int[] nArray = roadTypes = qc instanceof DrmQueryCondition ? ((DrmQueryCondition)DrmQueryCondition.class.cast(qc)).getRoadTypes() : null;
            if (roadTypes != null && roadTypes.length > 0) {
                cond = String.valueOf(cond) + String.format(" AND %s in (%s)", DRM_ROAD_TYPE_COLUMN, StringUtils.join((Object[])ArrayUtils.toObject((int[])roadTypes), (String)","));
            }
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", DRM_GEOMETRY_COLUMN, this.getTableName(), cond);
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
                            int gid = res.getInt(DRM_LINK_ID_COLUMN);
                            String src = String.valueOf(res.getInt(DRM_SOURCE_NODE_COLUMN));
                            String tgt = String.valueOf(res.getInt(DRM_TARGET_NODE_COLUMN));
                            double cst = res.getDouble(DRM_COST_COLUMN);
                            double rcst = res.getDouble(DRM_REVERSER_COST_COLUMN);
                            int reg = res.getInt(DRM_REGULATION_COLUMN);
                            int type = res.getInt(DRM_ROAD_TYPE_COLUMN);
                            int width = res.getInt(DRM_ROAD_WIDTH_COLUMN);
                            int lanes = res.getInt(DRM_LANE_NUM_COLUMN);
                            boolean way = DrmLink.isOneway(reg);
                            Geometry geom = wkbreader.read(res.getBytes("bgeom"));
                            LineString line = null;
                            if (geom instanceof LineString) {
                                line = (LineString)LineString.class.cast(geom);
                            } else if (geom instanceof MultiLineString) {
                                line = (LineString)((MultiLineString)MultiLineString.class.cast(geom)).getGeometryN(0);
                            }
                            Point p0 = line.getStartPoint();
                            Point p1 = line.getEndPoint();
                            List<LonLat> list2 = list = needGeom ? GeometryUtils.createPointList(line) : null;
                            if (DrmLink.isOnewayAndReverse(reg)) {
                                n0 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                                Node node = n1 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
                                if (list != null && !list.isEmpty()) {
                                    Collections.reverse(list);
                                }
                            } else {
                                n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
                                n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                            }
                            DrmLink link = new DrmLink(String.valueOf(gid), n0, n1, cst, rcst, way, type, width, lanes, list);
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

