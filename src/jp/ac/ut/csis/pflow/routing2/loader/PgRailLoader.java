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
 *  org.apache.commons.lang3.StringUtils
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
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.RailQueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.RailLink;
import jp.ac.ut.csis.pflow.routing2.res.RailNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PgRailLoader
extends APgNetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(PgRailLoader.class);
    public static final String NETWORK_TABLE = System.getProperty("pflow.routing2.pgloader.rail_network_table", "railway.railnetwork");
    public static final String RAIL_LINK_ID_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_link_id_column", LINK_ID_COLUMN);
    public static final String RAIL_SOURCE_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_source_column", SOURCE_NODE_COLUMN);
    public static final String RAIL_TARGET_NODE_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_target_column", TARGET_NODE_COLUMN);
    public static final String RAIL_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_cost_column", COST_COLUMN);
    public static final String RAIL_REVERSER_COST_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_reverse_cost_column", REVERSER_COST_COLUMN);
    public static final String RAIL_GEOMETRY_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_geometry_column", GEOMETRY_COLUMN);
    public static final String RAIL_LEGNTH_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_length_column", "length");
    public static final String RAIL_COMPANY_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_company_column", "compname");
    public static final String RAIL_LINE_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_line_column", "linename");
    public static final String RAIL_SOURCE_STATION_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_source_station_column", "stnname0");
    public static final String RAIL_TARGET_STATION_COLUMN = System.getProperty("pflow.routing2.pgloader.rail_target_station_column", "stnname1");

    public PgRailLoader() {
        this(NETWORK_TABLE);
    }

    public PgRailLoader(String tablename) {
        super(tablename);
    }

    @Override
    protected String createQuery(QueryCondition qc) {
        String cond = String.format("WHERE %s<>%s", RAIL_SOURCE_NODE_COLUMN, RAIL_TARGET_NODE_COLUMN);
        if (qc != null) {
            String lineName;
            String compName;
            Rectangle2D bounds = qc.getRects();
            if (bounds != null) {
                cond = String.valueOf(cond) + String.format(" AND ST_Intersects(%s,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakeBox2D(ST_MakePoint(%f,%f),ST_MakePoint(%f,%f))),%f)),4326))", RAIL_GEOMETRY_COLUMN, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), qc.getBuffer());
            }
            String string = compName = qc instanceof RailQueryCondition ? ((RailQueryCondition)RailQueryCondition.class.cast(qc)).getCompName() : null;
            if (!StringUtils.isBlank(compName)) {
                cond = String.valueOf(cond) + String.format(" AND %s='%s' ", RAIL_COMPANY_COLUMN, compName);
            }
            String string2 = lineName = qc instanceof RailQueryCondition ? ((RailQueryCondition)RailQueryCondition.class.cast(qc)).getLineName() : null;
            if (!StringUtils.isBlank(lineName)) {
                cond = String.valueOf(cond) + String.format(" AND %s='%s' ", RAIL_LINE_COLUMN, lineName);
            }
        }
        return String.format("select *,ST_AsBinary(%s) as bgeom from %s %s", RAIL_GEOMETRY_COLUMN, this.getTableName(), cond);
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
                int gid = res.getInt(RAIL_LINK_ID_COLUMN);
                String src = String.valueOf(res.getInt(RAIL_SOURCE_NODE_COLUMN));
                String tgt = String.valueOf(res.getInt(RAIL_TARGET_NODE_COLUMN));
                double cst = res.getDouble(RAIL_LEGNTH_COLUMN);
                double rcst = res.getDouble(RAIL_LEGNTH_COLUMN);
                String comp = res.getString(RAIL_COMPANY_COLUMN);
                String ln = res.getString(RAIL_LINE_COLUMN);
                String stn0 = res.getString(RAIL_SOURCE_STATION_COLUMN);
                String stn1 = res.getString(RAIL_TARGET_STATION_COLUMN);
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
                RailNode n0 = network.hasNode(src) ? (RailNode)RailNode.class.cast(network.getNode(src)) : new RailNode(src, p0.getX(), p0.getY(), comp, ln, stn0);
                RailNode n1 = network.hasNode(tgt) ? (RailNode)RailNode.class.cast(network.getNode(tgt)) : new RailNode(tgt, p1.getX(), p1.getY(), comp, ln, stn1);
                RailLink link = new RailLink(String.valueOf(gid), n0, n1, cst, rcst, way, comp, ln, list);
                network.addLink(link);
            }
        }
        catch (ParseException | OutOfMemoryError | SQLException exp) {
            LOGGER.error("rail to load network", exp);
            return null;
        }
        return network;
    }
}

