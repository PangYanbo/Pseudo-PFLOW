/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.io.ParseException
 *  org.locationtech.jts.io.WKBReader
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.RailwayQueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing2.res.RailwayNode;
import org.apache.commons.lang.StringUtils;

public class PgRailwayLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.railway_network_table", "rail.railway_network_v1");
    public static final String RAILWAY_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_link_id_column", "linkId");
    public static final String RAILWAY_COMPANY_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_company_code_column", "comp_code");
    public static final String RAILWAY_COMPANY_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_company_name_column", "comp_name");
    public static final String RAILWAY_LINE_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_line_code_column", "line_code");
    public static final String RAILWAY_LINE_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_line_name_column", "line_name");
    public static final String RAILWAY_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_geometry_column", "geom");
    public static final String RAILWAY_LEGNTH_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_length_column", "length");
    public static final String RAILWAY_SOURCE_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_source_code_column", "source_station_code");
    public static final String RAILWAY_SOURCE_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_source_name_column", "source_station_name");
    public static final String RAILWAY_SOURCE_GROUP_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_source_group_column", "source_station_group");
    public static final String RAILWAY_SOURCE_PREF_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_source_pref_column", "source_station_pref");
    public static final String RAILWAY_TARGET_CODE_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_target_code_column", "target_station_code");
    public static final String RAILWAY_TARGET_NAME_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_target_name_column", "target_station_name");
    public static final String RAILWAY_TARGET_GROUP_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_target_group_column", "target_station_group");
    public static final String RAILWAY_TARGET_PREF_COLUMN = System.getProperty("pflow.routing2.pgloader.railway_target_pref_column", "target_station_pref");

    public PgRailwayLoader() {
        this(NETWORK_TABLE);
    }

    public PgRailwayLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        String cond = String.format("WHERE %s IS NOT NULL AND %s<>%s", RAILWAY_GEOMETRY_COLUMN, RAILWAY_SOURCE_CODE_COLUMN, RAILWAY_TARGET_CODE_COLUMN);
        if (qc != null) {
            Rectangle2D bounds = qc.getRects();
            if (bounds != null) {
                cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", RAILWAY_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
            }
            if (qc instanceof RailwayQueryCondition) {
                String lineName;
                int lineCode;
                String compName;
                RailwayQueryCondition rqc = (RailwayQueryCondition)RailwayQueryCondition.class.cast(qc);
                int compCode = rqc.getCompCode();
                if (compCode > 0) {
                    cond = String.valueOf(cond) + String.format(" AND %s=%d ", RAILWAY_COMPANY_CODE_COLUMN, compCode);
                }
                if (StringUtils.isNotBlank((String)(compName = rqc.getCompName()))) {
                    cond = String.valueOf(cond) + String.format(" AND %s='%s' ", RAILWAY_COMPANY_NAME_COLUMN, compName);
                }
                if ((lineCode = rqc.getLineCode()) > 0) {
                    cond = String.valueOf(cond) + String.format(" AND %s=%d ", RAILWAY_LINE_CODE_COLUMN, lineCode);
                }
                if (StringUtils.isNotBlank((String)(lineName = rqc.getLineName()))) {
                    cond = String.valueOf(cond) + String.format(" AND %s='%s' ", RAILWAY_LINE_NAME_COLUMN, lineName);
                }
            }
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", RAILWAY_GEOMETRY_COLUMN, this.getTableName(), cond);
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
                            Node tgtNode;
                            int linkId = res.getInt(RAILWAY_LINK_ID_COLUMN);
                            int source = res.getInt(RAILWAY_SOURCE_CODE_COLUMN);
                            int target = res.getInt(RAILWAY_TARGET_CODE_COLUMN);
                            int companyCode = res.getInt(RAILWAY_COMPANY_CODE_COLUMN);
                            String companyName = res.getString(RAILWAY_COMPANY_NAME_COLUMN);
                            int lineCode = res.getInt(RAILWAY_LINE_CODE_COLUMN);
                            String lineName = res.getString(RAILWAY_LINE_NAME_COLUMN);
                            String srcStationName = res.getString(RAILWAY_SOURCE_NAME_COLUMN);
                            String tgtStationName = res.getString(RAILWAY_TARGET_NAME_COLUMN);
                            int srcStationGroup = res.getInt(RAILWAY_SOURCE_GROUP_COLUMN);
                            int tgtStationGroup = res.getInt(RAILWAY_TARGET_GROUP_COLUMN);
                            int srcStationPref = res.getInt(RAILWAY_SOURCE_PREF_COLUMN);
                            int tgtStationPref = res.getInt(RAILWAY_TARGET_PREF_COLUMN);
                            double length = res.getDouble(RAILWAY_LEGNTH_COLUMN);
                            LineString geom = (LineString)LineString.class.cast(wkbReader.read(res.getBytes("bgeom")));
                            List<LonLat> line = GeometryUtils.createPointList(geom);
                            Point p0 = geom.getStartPoint();
                            Point p1 = geom.getEndPoint();
                            Node srcNode = network.getNode(String.valueOf(source));
                            if (srcNode == null) {
                                srcNode = new RailwayNode(companyCode, lineCode, source, srcStationGroup, companyName, lineName, srcStationName, srcStationPref, p0.getX(), p0.getY());
                            }
                            if ((tgtNode = network.getNode(String.valueOf(target))) == null) {
                                tgtNode = new RailwayNode(companyCode, lineCode, target, tgtStationGroup, companyName, lineName, tgtStationName, tgtStationPref, p1.getX(), p1.getY());
                            }
                            RailwayLink link = new RailwayLink(String.valueOf(linkId), lineCode, (RailwayNode)srcNode, (RailwayNode)tgtNode, length);
                            if (needGeom) {
                                link.setLineString(line);
                            }
                            network.addLink(link);
                        }
                        this.updateStationGroup(network);
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

    private void updateStationGroup(Network network) {
        TreeMap groups = new TreeMap();
        for (Node node : network.listNodes()) {
            RailwayNode ekiNode = (RailwayNode)RailwayNode.class.cast(node);
            int groupCode = ekiNode.getStationGroupCode();
            if (!groups.containsKey(groupCode)) {
                groups.put(groupCode, new HashSet());
            }
            ((Set)groups.get(groupCode)).add(ekiNode);
        }
        for (Set val : groups.values()) {
            for (RailwayNode ekiNode : val) {
                ekiNode.setGroupNodes(val);
            }
        }
    }
}

