/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.io.ParseException
 *  org.locationtech.jts.io.WKBReader
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.APgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.loader.RailwayQueryCondition;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing4.res.RailwayNode;
import org.apache.commons.lang3.StringUtils;

public class PgRailwayLoader
extends APgNetworkLoader {
    public static final String NETWORK_TABLE = "rail.railway_network_v1";
    public static final String RAILWAY_LINK_ID_COLUMN = "linkId";
    public static final String RAILWAY_COMPANY_CODE_COLUMN = "comp_code";
    public static final String RAILWAY_COMPANY_NAME_COLUMN = "comp_name";
    public static final String RAILWAY_LINE_CODE_COLUMN = "line_code";
    public static final String RAILWAY_LINE_NAME_COLUMN = "line_name";
    public static final String RAILWAY_GEOMETRY_COLUMN = "geom";
    public static final String RAILWAY_LEGNTH_COLUMN = "length";
    public static final String RAILWAY_SOURCE_CODE_COLUMN = "source_station_code";
    public static final String RAILWAY_SOURCE_NAME_COLUMN = "source_station_name";
    public static final String RAILWAY_SOURCE_GROUP_COLUMN = "source_station_group";
    public static final String RAILWAY_SOURCE_PREF_COLUMN = "source_station_pref";
    public static final String RAILWAY_TARGET_CODE_COLUMN = "target_station_code";
    public static final String RAILWAY_TARGET_NAME_COLUMN = "target_station_name";
    public static final String RAILWAY_TARGET_GROUP_COLUMN = "target_station_group";
    public static final String RAILWAY_TARGET_PREF_COLUMN = "target_station_pref";

    public PgRailwayLoader() {
    }

    public PgRailwayLoader(Connection dbConn, String tablename) {
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
                ArrayList<String> tokens = new ArrayList<String>();
                Rectangle2D rect = query.getRect();
                double bufSize = query.getBuffer();
                if (rect != null) {
                    tokens.add(String.format("ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", RAILWAY_GEOMETRY_COLUMN, rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY(), bufSize));
                }
                if (query instanceof RailwayQueryCondition) {
                    String lineName;
                    int lineCode;
                    String compName;
                    RailwayQueryCondition rqc = (RailwayQueryCondition)RailwayQueryCondition.class.cast(query);
                    int compCode = rqc.getCompCode();
                    if (compCode > 0) {
                        tokens.add(String.format("%s=%d", RAILWAY_COMPANY_CODE_COLUMN, compCode));
                    }
                    if (StringUtils.isNotBlank((String)(compName = rqc.getCompName()))) {
                        tokens.add(String.format("%s='%s'", RAILWAY_COMPANY_NAME_COLUMN, compName));
                    }
                    if ((lineCode = rqc.getLineCode()) > 0) {
                        tokens.add(String.format("%s=%d", RAILWAY_LINE_CODE_COLUMN, lineCode));
                    }
                    if (StringUtils.isNotBlank((String)(lineName = rqc.getLineName()))) {
                        tokens.add(String.format("%s='%s'", RAILWAY_LINE_NAME_COLUMN, lineName));
                    }
                }
                if (tokens.isEmpty()) continue;
                whereClauses.add(String.format("(%s)", StringUtils.join(tokens, (String)" AND ")));
            }
        }
        String where = String.format("WHERE %s<>%s AND (line_code is null OR line_code<>11328) ", RAILWAY_SOURCE_CODE_COLUMN, RAILWAY_TARGET_CODE_COLUMN);
        if (!whereClauses.isEmpty()) {
            where = String.valueOf(where) + String.format(" AND (%s)", StringUtils.join(whereClauses, (String)" OR "));
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", RAILWAY_GEOMETRY_COLUMN, this.getTableName(), where);
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
                if (length == 0.0) {
                    length = 1.0;
                }
                LineString geom = (LineString)LineString.class.cast(wkbReader.read(res.getBytes("bgeom")));
                List<ILonLat> line = GeometryUtils.createPointList(geom);
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
        catch (ParseException | SQLException exp) {
            exp.printStackTrace();
        }
        return network;
    }

    private void updateStationGroup(Network network) {
        TreeMap<Integer, Set<RailwayNode>> groups = new TreeMap<Integer, Set<RailwayNode>>();
        for (Node node : network.listNodes()) {
            RailwayNode ekiNode = (RailwayNode)RailwayNode.class.cast(node);
            int groupCode = ekiNode.getStationGroupCode();
            if (!groups.containsKey(groupCode)) {
                groups.put(groupCode, new HashSet<RailwayNode>());
            }
            groups.get(groupCode).add(ekiNode);
        }
        for (Set<RailwayNode> val : groups.values()) {
            for (RailwayNode ekiNode : val) {
                ekiNode.setGroupNodes(val);
            }
        }
    }
}

