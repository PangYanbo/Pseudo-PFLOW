/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.Coordinate
 *  com.vividsolutions.jts.geom.Geometry
 *  com.vividsolutions.jts.geom.GeometryFactory
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.MultiLineString
 *  com.vividsolutions.jts.geom.MultiPoint
 *  com.vividsolutions.jts.geom.MultiPolygon
 *  com.vividsolutions.jts.geom.Point
 *  com.vividsolutions.jts.geom.Polygon
 *  com.vividsolutions.jts.geom.PrecisionModel
 *  com.vividsolutions.jts.io.ParseException
 *  com.vividsolutions.jts.io.WKBReader
 *  com.vividsolutions.jts.io.WKBWriter
 *  com.vividsolutions.jts.io.WKTReader
 *  com.vividsolutions.jts.io.WKTWriter
 */
package jp.ac.ut.csis.pflow.geom2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;

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

    public static ILonLat createPoint(Point p) {
        return new LonLat(p.getX(), p.getY());
    }

    public static List<ILonLat> createPointList(LineString linestring) {
        int size = linestring.getNumPoints();
        ArrayList<ILonLat> points = new ArrayList<ILonLat>(size);
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

    public static <T extends ILonLat> Point createPoint(T point) {
        return GEOMFAC.createPoint(new Coordinate(point.getLon(), point.getLat()));
    }

    public static <T extends ILonLat> MultiPoint createMultiPoint(T[] points) {
        return GeometryUtils.createMultiPoint(Arrays.asList(points));
    }

    public static <T extends ILonLat> MultiPoint createMultiPoint(Collection<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int idx = 0;
        for (ILonLat p : points) {
            coords[idx++] = new Coordinate(p.getLon(), p.getLat());
        }
        return GEOMFAC.createMultiPoint(coords);
    }

    public static <T extends ILonLat> LineString createLineString(T[] points) {
        return GeometryUtils.createLineString(Arrays.asList(points));
    }

    public static <T extends ILonLat> LineString createLineString(List<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int i = points.size() - 1;
        while (i >= 0) {
            ILonLat ll = (ILonLat)points.get(i);
            coords[i] = new Coordinate(ll.getLon(), ll.getLat());
            --i;
        }
        return GEOMFAC.createLineString(coords);
    }

    public static <T extends ILonLat> MultiLineString createMultiLineString(T[][] lines) {
        int num = lines.length;
        LineString[] gLines = new LineString[num];
        int i = 0;
        while (i < num) {
            gLines[i] = GeometryUtils.createLineString(lines[i]);
            ++i;
        }
        return GEOMFAC.createMultiLineString(gLines);
    }

    public static <T extends ILonLat> MultiLineString createMultiLineString(List<List<T>> lines) {
        int num = lines.size();
        LineString[] gLines = new LineString[num];
        int i = 0;
        while (i < num) {
            gLines[i] = GeometryUtils.createLineString(lines.get(i));
            ++i;
        }
        return GEOMFAC.createMultiLineString(gLines);
    }

    public static <T extends ILonLat> Polygon createPolygon(T[] points) {
        return GeometryUtils.createPolygon(Arrays.asList(points));
    }

    public static <T extends ILonLat> Polygon createPolygon(List<T> points) {
        Coordinate[] coords = new Coordinate[points.size()];
        int i = points.size() - 1;
        while (i >= 0) {
            ILonLat ll = (ILonLat)points.get(i);
            coords[i] = new Coordinate(ll.getLon(), ll.getLat());
            --i;
        }
        return GEOMFAC.createPolygon(GEOMFAC.createLinearRing(coords), null);
    }

    public static <T extends ILonLat> MultiPolygon createMultiPolygon(T[][] polygons) {
        int num = polygons.length;
        Polygon[] gPolygons = new Polygon[num];
        int i = 0;
        while (i < num) {
            gPolygons[i] = GeometryUtils.createPolygon(polygons[i]);
            ++i;
        }
        return GEOMFAC.createMultiPolygon(gPolygons);
    }

    public static <T extends ILonLat> MultiPolygon createMultiPolygon(List<List<T>> polygons) {
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

