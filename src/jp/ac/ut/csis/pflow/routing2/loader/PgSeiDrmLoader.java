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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PgSeiDrmLoader
extends APgNetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(PgSeiDrmLoader.class);
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.drm_network_table", "seidrm2015.drm_32_table");
    public static final String SEIDRM_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_link_id_column", LINK_ID_COLUMN);
    public static final String SEIDRM_SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_source_column", SOURCE_NODE_COLUMN);
    public static final String SEIDRM_TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_target_column", TARGET_NODE_COLUMN);
    public static final String SEIDRM_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_cost_column", "length");
    public static final String SEIDRM_REVERSER_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_reverse_cost_column", "length");
    public static final String SEIDRM_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_geometry_column", GEOMETRY_COLUMN);
    public static final String SEIDRM_REGULATION_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_regulation_column", "regcd");
    public static final String SEIDRM_ROAD_WIDTH_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_road_width_column", "rdwdcd");
    public static final String SEIDRM_ROAD_TYPE_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_road_type_column", "rdclasscd");
    public static final String SEIDRM_LANE_NUM_COLUMN = System.getProperty("pflow.routing2.pgloader.seidrm_lane_num_column", "lanecd");

    public PgSeiDrmLoader() {
        this(NETWORK_TABLE);
    }

    public PgSeiDrmLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        String cond = String.format("WHERE %s<>%s", SEIDRM_SOURCE_NODE_COLUMN, SEIDRM_TARGET_NODE_COLUMN);
        if (qc != null) {
            int[] roadTypes;
            Rectangle2D bounds = qc.getRects();
            if (bounds != null) {
                cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", SEIDRM_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
            }
            int[] nArray = roadTypes = qc instanceof DrmQueryCondition ? ((DrmQueryCondition)DrmQueryCondition.class.cast(qc)).getRoadTypes() : null;
            if (roadTypes != null && roadTypes.length > 0) {
                cond = String.valueOf(cond) + String.format(" AND %s::int4 in (%s)", SEIDRM_ROAD_TYPE_COLUMN, StringUtils.join((Object[])ArrayUtils.toObject((int[])roadTypes), (String)","));
            }
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", SEIDRM_GEOMETRY_COLUMN, this.getTableName(), cond);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public Network load(Network network, Connection con, String sql, boolean needGeom) {
        try (Statement stmt = con.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {
            WKBReader wkbreader = new WKBReader();
            while (res.next()) {
                Node n1;
                Node n0;
                int gid = res.getInt(SEIDRM_LINK_ID_COLUMN);
                String src = String.valueOf(res.getInt(SEIDRM_SOURCE_NODE_COLUMN));
                String tgt = String.valueOf(res.getInt(SEIDRM_TARGET_NODE_COLUMN));
                double cst = res.getDouble(SEIDRM_COST_COLUMN);
                double rcst = res.getDouble(SEIDRM_REVERSER_COST_COLUMN);
                int reg = res.getInt(SEIDRM_REGULATION_COLUMN);
                int type = res.getInt(SEIDRM_ROAD_TYPE_COLUMN);
                int width = res.getInt(SEIDRM_ROAD_WIDTH_COLUMN);
                int lanes = res.getInt(SEIDRM_LANE_NUM_COLUMN);
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
                List<LonLat> list = needGeom ? GeometryUtils.createPointList(line) : null;
                if (DrmLink.isOnewayAndReverse(reg)) {
                    n0 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
                    n1 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
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
        catch (ParseException | OutOfMemoryError | SQLException exp) {
            LOGGER.error("fail to load network data", exp);
            return null;
        }
        return network;
    }
}

