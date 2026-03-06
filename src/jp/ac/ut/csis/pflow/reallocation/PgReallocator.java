/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.Geometry
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.postgis.PGgeometry
 *  org.postgis.Point
 */
package jp.ac.ut.csis.pflow.reallocation;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.reallocation.AReallocator;
import jp.ac.ut.csis.pflow.reallocation.POI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgis.PGgeometry;
import org.postgis.Point;

public class PgReallocator
extends AReallocator {
    private static final Logger LOGGER = LogManager.getLogger(PgReallocator.class);
    public static final String REALLOCATION_TABLE = System.getProperty("pflow.reallocation.table_name", "realloctable");
    private PgLoader _dbLoader;
    private String _tablename;
    private Random _random;

    public PgReallocator(PgLoader dbLoader) {
        this(dbLoader, REALLOCATION_TABLE, AReallocator.Method.RANDOM);
    }

    public PgReallocator(PgLoader dbLoader, String tablename) {
        this(dbLoader, tablename, AReallocator.Method.RANDOM);
    }

    public PgReallocator(PgLoader dbLoader, String tablename, AReallocator.Method method) {
        super(method);
        this._dbLoader = dbLoader;
        this._tablename = tablename;
        this._random = new Random();
    }

    @Override
    public POI reallocate(LonLat point, double radius) {
        switch (this.getMethod()) {
            default: {
                return this.reallocateRandom(point, radius);
            }
            case AREA_PROB: 
        }
        return this.reallocateAreaProb(point, radius);
    }

    @Override
    public POI reallocate(Polygon areaPolygon) {
        switch (this.getMethod()) {
            default: {
                return this.reallocateRandom(areaPolygon);
            }
            case AREA_PROB: 
        }
        return this.reallocateAreaProb(areaPolygon);
    }

    private POI reallocateRandom(LonLat point, double radius) {
        String sql = String.format("select a.gid,a.name,a.area,a.geom from %s where ST_Within(geom,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakePoint(%f,%f)),%f)),4326)))", this._tablename, point.getLon(), point.getLat(), radius);
        LOGGER.debug(sql);
        return this.reallocateRandom(sql, point);
    }

    private POI reallocateRandom(Polygon areaPolygon) {
        String wkt = GeometryUtils.createWKTString((Geometry)areaPolygon);
        String sql = String.format("select a.gid,a.name,a.area,a.geom from %s where ST_Within(geom,ST_SetSRID(ST_GeomFromText('%s'),4326)))", this._tablename, wkt);
        LOGGER.debug(sql);
        org.locationtech.jts.geom.Point centroid = areaPolygon.getCentroid();
        return this.reallocateRandom(sql, new LonLat(centroid.getX(), centroid.getY()));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private POI reallocateRandom(String sql, LonLat point) {
        POI output = null;
        try (Connection con = this._dbLoader.getConnection();
             Statement stmt = con.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {
            ArrayList<POI> pois = new ArrayList<POI>();
            while (res.next()) {
                Point p = (Point)Point.class.cast(((PGgeometry)PGgeometry.class.cast(res.getObject("geom"))).getGeometry());
                pois.add(new POI(res.getString("gid"), res.getString("name"), res.getDouble("area"), p.getX(), p.getY()));
            }
            if (pois.isEmpty()) {
                output = new POI(null, point.getLon(), point.getLat());
            } else {
                output = (POI)pois.get(this._random.nextInt(pois.size()));
            }
        }
        catch (SQLException exp) {
            LOGGER.error("fail to query DB", (Throwable)exp);
            return null;
        }
        return output;
    }

    private POI reallocateAreaProb(LonLat point, double radius) {
        String sql = String.format("select a.gid,a.name,a.area,a.geom,area/b.R as ratio from (select gid,name,area,geom from %s where ST_Within(geom,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakePoint(%f,%f)),%f)),4326))) a,(select sum(area) as R     from %s where ST_Within(geom,ST_SetSRID(GEOMETRY(ST_Buffer(GEOGRAPHY(ST_MakePoint(%f,%f)),%f)),4326))) b order by ratio desc", this._tablename, point.getLon(), point.getLat(), radius, this._tablename, point.getLon(), point.getLat(), radius);
        LOGGER.debug(sql);
        return this.reallocateAreaProb(sql, point);
    }

    private POI reallocateAreaProb(Polygon areaPolygon) {
        String wkt = GeometryUtils.createWKTString((Geometry)areaPolygon);
        String sql = String.format("select a.gid,a.name,a.area,a.geom,area/b.R as ratio from (select gid,name,area,geom from %s where ST_Within(geom,ST_SetSRID(ST_GeomFromText('%s'),4326))) a,(select sum(area) as R     from %s where ST_Within(geom,ST_SetSRID(ST_GeomFromText('%s'),4326))) b order by ratio desc", this._tablename, wkt, this._tablename, wkt);
        LOGGER.debug(sql);
        org.locationtech.jts.geom.Point centroid = areaPolygon.getCentroid();
        return this.reallocateAreaProb(sql, new LonLat(centroid.getX(), centroid.getY()));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private POI reallocateAreaProb(String sql, LonLat point) {
        POI output = null;
        try (Connection con = this._dbLoader.getConnection();
             Statement stmt = con.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {
            double rand = this._random.nextDouble();
            while (res.next()) {
                double r = res.getDouble("ratio");
                if (r >= rand) {
                    Point p = (Point)Point.class.cast(((PGgeometry)PGgeometry.class.cast(res.getObject("geom"))).getGeometry());
                    output = new POI(res.getString("gid"), res.getString("name"), res.getDouble("area"), p.getX(), p.getY());
                    break;
                }
                rand -= r;
            }
            if (output == null) {
                output = new POI(null, point.getLon(), point.getLat());
            }
        }
        catch (SQLException exp) {
            LOGGER.error("fail to query DB", (Throwable)exp);
            return null;
        }
        return output;
    }
}

