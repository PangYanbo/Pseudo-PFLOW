/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.Point
 *  com.vividsolutions.jts.io.ParseException
 *  com.vividsolutions.jts.io.WKBReader
 *  org.apache.commons.lang.ArrayUtils
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import java.awt.geom.Rectangle2D;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.HighwayLink;
import jp.ac.ut.csis.pflow.routing2.res.HighwayNode;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.commons.lang.ArrayUtils;

public class PgHighwayLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.highway_network_table", "highway.highway_network");
    public static final String HIGHWAY_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_link_id_column", LINK_ID_COLUMN);
    public static final String HIGHWAY_LINE_SEQ_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_lineseq_column", "line_seq");
    public static final String HIGHWAY_LINE_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_line_name_column", "line_name");
    public static final String HIGHWAY_LEGNTH_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_length_column", "length");
    public static final String HIGHWAY_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_geometry_column", "geom");
    public static final String HIGHWAY_SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_node_column", "source");
    public static final String HIGHWAY_SOURCE_SEQ_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_seq_column", "source_icseq");
    public static final String HIGHWAY_SOURCE_ROAD_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_road_code_column", "source_road_code");
    public static final String HIGHWAY_SOURCE_IC_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_ic_code_column", "source_ic_code");
    public static final String HIGHWAY_SOURCE_IC_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_ic_name_column", "source_ic_name");
    public static final String HIGHWAY_SOURCE_IC_NAME_KANA_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_ic_name_kana_column", "source_ic_name_kana");
    public static final String HIGHWAY_SOURCE_TYPE_FLAG_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_type_flag_column", "source_type_flag");
    public static final String HIGHWAY_SOURCE_ETC_CODE1_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_etc_code1_column", "source_etc_code1");
    public static final String HIGHWAY_SOURCE_ETC_CODE2_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_etc_code2_column", "source_etc_code2");
    public static final String HIGHWAY_SOURCE_CENSUS_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_census_code_column", "source_census_code");
    public static final String HIGHWAY_SOURCE_PREF_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_source_pref_code_column", "source_pref_code");
    public static final String HIGHWAY_TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_node_column", "target");
    public static final String HIGHWAY_TARGET_SEQ_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_seq_column", "target_icseq");
    public static final String HIGHWAY_TARGET_ROAD_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_road_code_column", "target_road_code");
    public static final String HIGHWAY_TARGET_IC_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_ic_code_column", "target_ic_code");
    public static final String HIGHWAY_TARGET_IC_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_ic_name_column", "target_ic_name");
    public static final String HIGHWAY_TARGET_IC_NAME_KANA_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_ic_name_kana_column", "target_ic_name_kana");
    public static final String HIGHWAY_TARGET_TYPE_FLAG_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_type_flag_column", "target_type_flag");
    public static final String HIGHWAY_TARGET_ETC_CODE1_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_etc_code1_column", "target_etc_code1");
    public static final String HIGHWAY_TARGET_ETC_CODE2_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_etc_code2_column", "target_etc_code2");
    public static final String HIGHWAY_TARGET_CENSUS_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_census_code_column", "target_census_code");
    public static final String HIGHWAY_TARGET_PREF_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.highway_target_pref_code_column", "target_pref_code");

    public PgHighwayLoader() {
        this(NETWORK_TABLE);
    }

    public PgHighwayLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        Rectangle2D bounds;
        String cond = String.format("WHERE %s IS NOT NULL ", HIGHWAY_GEOMETRY_COLUMN);
        if (qc != null && (bounds = qc.getRects()) != null) {
            cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", HIGHWAY_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", HIGHWAY_GEOMETRY_COLUMN, this.getTableName(), cond);
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
                        WKBReader wkbReader = new WKBReader();
                        while (res.next()) {
                            Node tgt;
                            int linkId = res.getInt(HIGHWAY_LINK_ID_COLUMN);
                            int lineSeq = res.getInt(HIGHWAY_LINE_SEQ_COLUMN);
                            String lineName = res.getString(HIGHWAY_LINE_NAME_COLUMN);
                            double length = res.getDouble(HIGHWAY_LEGNTH_COLUMN);
                            LineString geom = (LineString)LineString.class.cast(wkbReader.read(res.getBytes("bgeom")));
                            List<LonLat> line = GeometryUtils.createPointList(geom);
                            Point p0 = geom.getStartPoint();
                            Point p1 = geom.getEndPoint();
                            HighwayNode srcNode = this.createSourceNode(res, p0);
                            HighwayNode tgtNode = this.createTargetNode(res, p1);
                            Node src = network.getNode(srcNode.getNodeID());
                            if (src != null) {
                                srcNode = (HighwayNode)HighwayNode.class.cast(src);
                            }
                            if ((tgt = network.getNode(tgtNode.getNodeID())) != null) {
                                tgtNode = (HighwayNode)HighwayNode.class.cast(tgt);
                            }
                            HighwayLink link = new HighwayLink(String.valueOf(linkId), lineSeq, lineName, srcNode, tgtNode, length);
                            if (needGeom) {
                                link.setLineString(line);
                            }
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

