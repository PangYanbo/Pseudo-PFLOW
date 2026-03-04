/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.io.ParseException
 *  org.locationtech.jts.io.WKBReader
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.awt.geom.Rectangle2D;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.HighwayLink;
import jp.ac.ut.csis.pflow.routing4.res.HighwayNode;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class PgHighwayLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = "highway.highway_network";
    public static final String HIGHWAY_LINK_ID_COLUMN = "gid";
    public static final String HIGHWAY_LINE_SEQ_COLUMN = "line_seq";
    public static final String HIGHWAY_LINE_NAME_COLUMN = "line_name";
    public static final String HIGHWAY_LEGNTH_COLUMN = "length";
    public static final String HIGHWAY_GEOMETRY_COLUMN = "geom";
    public static final String HIGHWAY_SOURCE_NODE_COLUMN = "source";
    public static final String HIGHWAY_SOURCE_SEQ_COLUMN = "source_icseq";
    public static final String HIGHWAY_SOURCE_ROAD_CODE_COLUMN = "source_road_code";
    public static final String HIGHWAY_SOURCE_IC_CODE_COLUMN = "source_ic_code";
    public static final String HIGHWAY_SOURCE_IC_NAME_COLUMN = "source_ic_name";
    public static final String HIGHWAY_SOURCE_IC_NAME_KANA_COLUMN = "source_ic_name_kana";
    public static final String HIGHWAY_SOURCE_TYPE_FLAG_COLUMN = "source_type_flag";
    public static final String HIGHWAY_SOURCE_ETC_CODE1_COLUMN = "source_etc_code1";
    public static final String HIGHWAY_SOURCE_ETC_CODE2_COLUMN = "source_etc_code2";
    public static final String HIGHWAY_SOURCE_CENSUS_CODE_COLUMN = "source_census_code";
    public static final String HIGHWAY_SOURCE_PREF_CODE_COLUMN = "source_pref_code";
    public static final String HIGHWAY_TARGET_NODE_COLUMN = "target";
    public static final String HIGHWAY_TARGET_SEQ_COLUMN = "target_icseq";
    public static final String HIGHWAY_TARGET_ROAD_CODE_COLUMN = "target_road_code";
    public static final String HIGHWAY_TARGET_IC_CODE_COLUMN = "target_ic_code";
    public static final String HIGHWAY_TARGET_IC_NAME_COLUMN = "target_ic_name";
    public static final String HIGHWAY_TARGET_IC_NAME_KANA_COLUMN = "target_ic_name_kana";
    public static final String HIGHWAY_TARGET_TYPE_FLAG_COLUMN = "target_type_flag";
    public static final String HIGHWAY_TARGET_ETC_CODE1_COLUMN = "target_etc_code1";
    public static final String HIGHWAY_TARGET_ETC_CODE2_COLUMN = "target_etc_code2";
    public static final String HIGHWAY_TARGET_CENSUS_CODE_COLUMN = "target_census_code";
    public static final String HIGHWAY_TARGET_PREF_CODE_COLUMN = "target_pref_code";

    public PgHighwayLoader() {
    }

    public PgHighwayLoader(Connection dbConn, String tablename) {
        super(dbConn, tablename);
    }

    @Override
    protected String createSqlQuery() throws NetworkLoadingException {
        String tablename = this.getTableName();
        if (tablename == null) {
            throw new NetworkLoadingException("no table name specified");
        }
        ArrayList whereClauses = new ArrayList();
        List<IQueryCondition> queries = this.listQueries();
        if (queries != null && !queries.isEmpty()) {
            for (IQueryCondition query : queries) {
                ArrayList<String> tokens = new ArrayList<String>();
                Rectangle2D rect = query.getRect();
                double bufSize = query.getBuffer();
                if (rect == null) continue;
                tokens.add(String.format("ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", HIGHWAY_GEOMETRY_COLUMN, rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY(), bufSize));
            }
        }
        String where = String.format("WHERE %s IS NOT NULL ", HIGHWAY_GEOMETRY_COLUMN);
        if (!whereClauses.isEmpty()) {
            where = String.valueOf(where) + String.format(" AND (%s)", StringUtils.join(whereClauses, (String)" OR "));
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", HIGHWAY_GEOMETRY_COLUMN, tablename, where);
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
            WKBReader wkbReader = new WKBReader();
            while (res.next()) {
                HighwayNode tgtNode;
                Node tgt;
                int linkId = res.getInt(HIGHWAY_LINK_ID_COLUMN);
                int lineSeq = res.getInt(HIGHWAY_LINE_SEQ_COLUMN);
                String lineName = res.getString(HIGHWAY_LINE_NAME_COLUMN);
                double length = res.getDouble(HIGHWAY_LEGNTH_COLUMN);
                LineString line = (LineString)LineString.class.cast(wkbReader.read(res.getBytes("bgeom")));
                List<ILonLat> geom = needGeom ? GeometryUtils.createPointList(line) : null;
                Point p0 = line.getStartPoint();
                Point p1 = line.getEndPoint();
                HighwayNode srcNode = this.createSourceNode(res, p0);
                Node src = network.getNode(srcNode.getNodeID());
                if (src != null) {
                    srcNode = (HighwayNode)HighwayNode.class.cast(src);
                }
                if ((tgt = network.getNode((tgtNode = this.createTargetNode(res, p1)).getNodeID())) != null) {
                    tgtNode = (HighwayNode)HighwayNode.class.cast(tgt);
                }
                HighwayLink link = new HighwayLink(String.valueOf(linkId), lineSeq, lineName, srcNode, tgtNode, length, length, length, geom);
                network.addLink(link);
            }
        }
        catch (ParseException | SQLException exp) {
            exp.printStackTrace();
        }
        return network;
    }

    private HighwayNode createSourceNode(ResultSet res, Point point) throws SQLException {
        int nodeNo = res.getInt(HIGHWAY_SOURCE_NODE_COLUMN);
        int icseq = res.getInt(HIGHWAY_SOURCE_SEQ_COLUMN);
        String roadCode = res.getString(HIGHWAY_SOURCE_ROAD_CODE_COLUMN);
        int icCode = res.getInt(HIGHWAY_SOURCE_IC_CODE_COLUMN);
        String icName = res.getString(HIGHWAY_SOURCE_IC_NAME_COLUMN);
        int typeFlag = res.getInt(HIGHWAY_SOURCE_TYPE_FLAG_COLUMN);
        Array etcArray1 = res.getArray(HIGHWAY_SOURCE_ETC_CODE1_COLUMN);
        Integer[] etcCode1 = etcArray1 != null ? (Integer[])etcArray1.getArray() : null;
        Array etcArray2 = res.getArray(HIGHWAY_SOURCE_ETC_CODE2_COLUMN);
        Integer[] etcCode2 = etcArray2 != null ? (Integer[])etcArray2.getArray() : null;
        String censusCode = res.getString(HIGHWAY_SOURCE_CENSUS_CODE_COLUMN);
        String prefCode = res.getString(HIGHWAY_SOURCE_PREF_CODE_COLUMN);
        return new HighwayNode(icseq, String.valueOf(nodeNo), roadCode, icCode, icName, typeFlag, ArrayUtils.toPrimitive((Integer[])etcCode1), ArrayUtils.toPrimitive((Integer[])etcCode2), censusCode, prefCode, point.getX(), point.getY());
    }

    private HighwayNode createTargetNode(ResultSet res, Point point) throws SQLException {
        int nodeNo = res.getInt(HIGHWAY_TARGET_NODE_COLUMN);
        int icseq = res.getInt(HIGHWAY_TARGET_SEQ_COLUMN);
        String roadCode = res.getString(HIGHWAY_TARGET_ROAD_CODE_COLUMN);
        int icCode = res.getInt(HIGHWAY_TARGET_IC_CODE_COLUMN);
        String icName = res.getString(HIGHWAY_TARGET_IC_NAME_COLUMN);
        int typeFlag = res.getInt(HIGHWAY_TARGET_TYPE_FLAG_COLUMN);
        Array etcArray1 = res.getArray(HIGHWAY_SOURCE_ETC_CODE1_COLUMN);
        Integer[] etcCode1 = etcArray1 != null ? (Integer[])etcArray1.getArray() : null;
        Array etcArray2 = res.getArray(HIGHWAY_SOURCE_ETC_CODE2_COLUMN);
        Integer[] etcCode2 = etcArray2 != null ? (Integer[])etcArray2.getArray() : null;
        String censusCode = res.getString(HIGHWAY_TARGET_CENSUS_CODE_COLUMN);
        String prefCode = res.getString(HIGHWAY_TARGET_PREF_CODE_COLUMN);
        return new HighwayNode(icseq, String.valueOf(nodeNo), roadCode, icCode, icName, typeFlag, ArrayUtils.toPrimitive((Integer[])etcCode1), ArrayUtils.toPrimitive((Integer[])etcCode2), censusCode, prefCode, point.getX(), point.getY());
    }
}

