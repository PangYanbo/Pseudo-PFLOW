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
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

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
import java.util.Collections;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.DrmLink;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class PgSeiDrmLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = "seidrm2017.drm_32_table";
    public static final String SEIDRM_LINK_ID_COLUMN = "gid";
    public static final String SEIDRM_SOURCE_NODE_COLUMN = "source";
    public static final String SEIDRM_TARGET_NODE_COLUMN = "target";
    public static final String SEIDRM_COST_COLUMN = "length";
    public static final String SEIDRM_REVERSER_COST_COLUMN = "length";
    public static final String SEIDRM_GEOMETRY_COLUMN = "geom";
    public static final String SEIDRM_REGULATION_COLUMN = "regcd";
    public static final String SEIDRM_ROAD_WIDTH_COLUMN = "rdwdcd";
    public static final String SEIDRM_ROAD_TYPE_COLUMN = "rdclasscd";
    public static final String SEIDRM_LANE_NUM_COLUMN = "lanecd";

    public PgSeiDrmLoader() {
    }

    public PgSeiDrmLoader(Connection dbConn, String tablename) {
        super(dbConn, tablename);
    }

    @Override
    protected String createSqlQuery() throws NetworkLoadingException {
        String tablename = this.getTableName();
        if (tablename == null) {
            throw new NetworkLoadingException("no table name specified");
        }
        ArrayList<String> whereClauses = new ArrayList<String>();
        List<IQueryCondition> queries = this.listQueries();
        if (queries != null && !queries.isEmpty()) {
            for (IQueryCondition query : queries) {
                int[] roadTypes;
                ArrayList<String> tokens = new ArrayList<String>();
                Rectangle2D rect = query.getRect();
                double bufSize = query.getBuffer();
                if (rect != null) {
                    tokens.add(String.format("ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", SEIDRM_GEOMETRY_COLUMN, rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY(), bufSize));
                }
                int[] nArray = roadTypes = query instanceof DrmQueryCondition ? ((DrmQueryCondition)DrmQueryCondition.class.cast(query)).getRoadTypes() : null;
                if (roadTypes != null && roadTypes.length > 0) {
                    tokens.add(String.format("%s::int4 in (%s)", SEIDRM_ROAD_TYPE_COLUMN, StringUtils.join((Object[])ArrayUtils.toObject((int[])roadTypes), (String)",")));
                }
                if (tokens.isEmpty()) continue;
                whereClauses.add(String.format("(%s)", StringUtils.join(tokens, (String)" AND ")));
            }
        }
        String where = String.format("WHERE %s<>%s", SEIDRM_SOURCE_NODE_COLUMN, SEIDRM_TARGET_NODE_COLUMN);
        if (!whereClauses.isEmpty()) {
            where = String.valueOf(where) + String.format(" AND (%s)", StringUtils.join(whereClauses, (String)" OR "));
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", SEIDRM_GEOMETRY_COLUMN, tablename, where);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public Network load() throws NetworkLoadingException {
        Connection con = this.getConnection();
        String sql = this.createSqlQuery();
        boolean needGeom = this.needGeom();
        Network network = this.getNetwork();
        if (con == null) {
            throw new NetworkLoadingException("no DB connection");
        }
        if (network == null) {
            network = new Network(true, true);
        }
        try (Statement stmt = con.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {
            WKBReader wkbreader = new WKBReader();
            while (res.next()) {
                Node n1;
                Node n0;
                int gid = res.getInt(SEIDRM_LINK_ID_COLUMN);
                String src = String.valueOf(res.getInt(SEIDRM_SOURCE_NODE_COLUMN));
                String tgt = String.valueOf(res.getInt(SEIDRM_TARGET_NODE_COLUMN));
                double cst = res.getDouble("length");
                double rcst = res.getDouble("length");
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
                List<ILonLat> list = needGeom ? GeometryUtils.createPointList(line) : null;
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
                DrmLink link = new DrmLink(String.valueOf(gid), n0, n1, cst, cst, rcst, way, type, width, lanes, list);
                network.addLink(link);
            }
        }
        catch (ParseException | OutOfMemoryError | SQLException exp) {
            exp.printStackTrace();
            throw new NetworkLoadingException("failed to load network", exp);
        }
        return network;
    }
}

