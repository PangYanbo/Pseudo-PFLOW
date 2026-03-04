/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;

public final class DistanceUtils {
    private static final double WGS84_EQUATOR_RADIUS = 6378137.0;
    private static final double WGS84_POLAR_RADIUS = 6356752.314245;
    private static final double WGS84_ECCENTRICITY_2 = 0.006694379990197585;

    public static <T extends ILonLat, S extends ILonLat> double distance(T p0, S p1) {
        return DistanceUtils.distance(p0.getLon(), p0.getLat(), p1.getLon(), p1.getLat());
    }

    public static double distance(double lon0, double lat0, double lon1, double lat1) {
        double a = 6378137.0;
        double e2 = 0.006694379990197585;
        double dy = Math.toRadians(lat0 - lat1);
        double dx = Math.toRadians(lon0 - lon1);
        double cy = Math.toRadians((lat0 + lat1) / 2.0);
        double m = a * (1.0 - e2);
        double sc = Math.sin(cy);
        double W = Math.sqrt(1.0 - e2 * sc * sc);
        double M = m / (W * W * W);
        double N = a / W;
        double ym = dy * M;
        double xn = dx * N * Math.cos(cy);
        return Math.sqrt(ym * ym + xn * xn);
    }

    public static <T extends ILonLat> double distance(T p0, T p1, T p) {
        ILonLat foot = DistanceUtils.nearestPoint(p0, p1, p);
        return DistanceUtils.distance(foot, p);
    }

    public static <T extends ILonLat> ILonLat nearestPoint(T p0, T p1, T p) {
        double dx = p1.getLon() - p0.getLon();
        double dy = p1.getLat() - p0.getLat();
        double a = dx * dx + dy * dy;
        double b = dx * (p0.getLon() - p.getLon()) + dy * (p0.getLat() - p.getLat());
        if (a == 0.0) {
            return new LonLat(p0.getLon(), p0.getLat());
        }
        double t = -b / a;
        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }
        double x = t * dx + p0.getLon();
        double y = t * dy + p0.getLat();
        return new LonLat(x, y);
    }

    public static <T extends ILonLat, S extends ILonLat> double distance(List<T> line, S p) {
        ILonLat foot = DistanceUtils.nearestPoint(line, p);
        return DistanceUtils.distance(foot, p);
    }

    public static <T extends ILonLat, S extends ILonLat> ILonLat nearestPoint(List<T> line, S p) {
        return DistanceUtils.nearestPointEntry(line, p).getKey();
    }

    public static <T extends ILonLat, S extends ILonLat> AbstractMap.SimpleEntry<ILonLat, Double> nearestPointEntry(List<T> line, S p) {
        int len = line.size();
        ILonLat p0 = (ILonLat)line.get(0);
        ILonLat res = null;
        double dis = Double.MAX_VALUE;
        int i = 1;
        while (i < len) {
            ILonLat p1 = (ILonLat)line.get(i);
            ILonLat q = DistanceUtils.nearestPoint(p0, p1, p);
            double d = DistanceUtils.distance(p, q);
            if (d < dis) {
                res = q;
                dis = d;
            }
            p0 = p1;
            ++i;
        }
        return new AbstractMap.SimpleEntry<ILonLat, Double>(res, dis);
    }

    public static <T extends ILonLat, S extends ILonLat> double hausdorff(Collection<T> setA, Collection<S> setB) {
        double D0 = Double.MIN_VALUE;
        for (ILonLat a : setA) {
            double d = Double.MAX_VALUE;
            for (ILonLat b : setB) {
                d = Math.min(d, DistanceUtils.distance(a, b));
            }
            D0 = Math.max(D0, d);
        }
        double D1 = Double.MIN_VALUE;
        for (ILonLat b : setB) {
            double d = Double.MAX_VALUE;
            for (ILonLat a : setA) {
                d = Math.min(d, DistanceUtils.distance(b, a));
            }
            D1 = Math.max(D1, d);
        }
        return Math.max(D0, D1);
    }
}

