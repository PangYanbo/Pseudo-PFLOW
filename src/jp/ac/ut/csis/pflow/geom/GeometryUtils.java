/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.Coordinate
 *  org.locationtech.jts.geom.Geometry
 *  org.locationtech.jts.geom.GeometryFactory
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.MultiLineString
 *  org.locationtech.jts.geom.MultiPoint
 *  org.locationtech.jts.geom.MultiPolygon
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 *  org.locationtech.jts.geom.PrecisionModel
 *  org.locationtech.jts.io.ParseException
 *  org.locationtech.jts.io.WKBReader
 *  org.locationtech.jts.io.WKBWriter
 *  org.locationtech.jts.io.WKTReader
 *  org.locationtech.jts.io.WKTWriter
 */
package jp.ac.ut.csis.pflow.geom;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;

public class GeometryUtils {
    public static final int SRID = Integer.getInteger("pflow.geometryutils.srid", 4326);
    public static final GeometryFactory GEOMFAC = new GeometryFactory(new PrecisionModel(), SRID);

    public static String createWKTString(Geometry geom) {
        return new WKTWriter().write(geom);
    }

    public static Geometry parseWKT(String wktstring) {
        Geometry geom = null;
        try {
            geom = new WKTReader().read(wktstring);
        }
        catch (ParseException exp) {
            exp.printStackTrace();
            geom = null;
        }
        return geom;
    }

    public static String createWKBString(Geometry geom) {
        return WKBWriter.toHex((byte[])new WKBWriter(2, true).write(geom));
    }

    public static Geometry parseWKB(String wkbstring) {
        Geometry geom = null;
        try {
            geom = new WKBReader().read(WKBReader.hexToBytes((String)wkbstring));
        }
        catch (ParseException exp) {
            exp.printStackTrace();
            geom = null;
        }
        return geom;
    }

    public static LonLat createPoint(Point p) {
        return new LonLat(p.getX(), p.getY());
    }

    public static List<LonLat> createPointList(LineString linestring) {
        int size = linestring.getNumPoints();
        ArrayList<LonLat> points = new ArrayList<LonLat>(size);
        int i = 0;
        while (i < size) {
            Point point = linestring.getPointN(i);
            points.add(new LonLat(point.getX(), point.getY()));
            ++i;
        }
        return points;
    }

    public static Point createPoint(double lon, double lat) {
        return GeometryUtils.createPoint(new LonLat(lon, lat));
    }

    public static <T extends LonLat> Point createPoint(T point) {
        return GEOMFAC.createPoint(new Coordinate(point.getLon(), point.getLat()));
    }

    public static <T extends LonLat> MultiPoint createMultiPoint(T[] points) {
        return GeometryUtils.createMultiPoint(Arrays.asList(points));
    }

    public static <T extends LonLat> MultiPoint createMultiPoint(Collection<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int idx = 0;
        for (LonLat p : points) {
            coords[idx++] = new Coordinate(p.getLon(), p.getLat());
        }
        return GEOMFAC.createMultiPoint(coords);
    }

    public static <T extends LonLat> LineString createLineString(T[] points) {
        return GeometryUtils.createLineString(Arrays.asList(points));
    }

    public static <T extends LonLat> LineString createLineString(List<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int i = points.size() - 1;
        while (i >= 0) {
            LonLat ll = (LonLat)points.get(i);
            coords[i] = new Coordinate(ll.getLon(), ll.getLat());
            --i;
        }
        return GEOMFAC.createLineString(coords);
    }

    public static <T extends LonLat> MultiLineString createMultiLineString(T[][] lines) {
        int num = lines.length;
        LineString[] gLines = new LineString[num];
        int i = 0;
        while (i < num) {
            gLines[i] = GeometryUtils.createLineString(lines[i]);
            ++i;
        }
        return GEOMFAC.createMultiLineString(gLines);
    }

    public static <T extends LonLat> MultiLineString createMultiLineString(List<List<T>> lines) {
        int num = lines.size();
        LineString[] gLines = new LineString[num];
        int i = 0;
        while (i < num) {
            gLines[i] = GeometryUtils.createLineString(lines.get(i));
            ++i;
        }
        return GEOMFAC.createMultiLineString(gLines);
    }

    public static <T extends LonLat> Polygon createPolygon(T[] points) {
        return GeometryUtils.createPolygon(Arrays.asList(points));
    }

    public static <T extends LonLat> Polygon createPolygon(List<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int i = points.size() - 1;
        while (i >= 0) {
            LonLat ll = (LonLat)points.get(i);
            coords[i] = new Coordinate(ll.getLon(), ll.getLat());
            --i;
        }
        return GEOMFAC.createPolygon(GEOMFAC.createLinearRing(coords), null);
    }

    public static <T extends LonLat> MultiPolygon createMultiPolygon(T[][] polygons) {
        int num = polygons.length;
        Polygon[] gPolygons = new Polygon[num];
        int i = 0;
        while (i < num) {
            gPolygons[i] = GeometryUtils.createPolygon(polygons[i]);
            ++i;
        }
        return GEOMFAC.createMultiPolygon(gPolygons);
    }

    public static <T extends LonLat> MultiPolygon createMultiPolygon(List<List<T>> polygons) {
        int num = polygons.size();
        Polygon[] gPolygons = new Polygon[num];
        int i = 0;
        while (i < num) {
            gPolygons[i] = GeometryUtils.createPolygon(polygons.get(i));
            ++i;
        }
        return GEOMFAC.createMultiPolygon(gPolygons);
    }
}

