/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;

public final class DistanceUtils {
    private static final double WGS84_EQUATOR_RADIUS = 6378137.0;
    private static final double WGS84_POLAR_RADIUS = 6356752.314245;
    private static final double WGS84_ECCENTRICITY_2 = 0.006694379990197585;

    public static <T extends LonLat, S extends LonLat> double distance(T p0, S p1) {
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

    public static <T extends LonLat> double distance(T p0, T p1, T p) {
        LonLat foot = DistanceUtils.nearestPoint(p0, p1, p);
        return DistanceUtils.distance(foot, p);
    }

    public static <T extends LonLat> LonLat nearestPoint(T p0, T p1, T p) {
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

    public static <T extends LonLat, S extends LonLat> double distance(List<T> line, S p) {
        LonLat foot = DistanceUtils.nearestPoint(line, p);
        return DistanceUtils.distance(foot, p);
    }

    public static <T extends LonLat, S extends LonLat> LonLat nearestPoint(List<T> line, S p) {
        return DistanceUtils.nearestPointEntry(line, p).getKey();
    }

    public static <T extends LonLat, S extends LonLat> AbstractMap.SimpleEntry<LonLat, Double> nearestPointEntry(List<T> line, S p) {
        int len = line.size();
        LonLat p0 = (LonLat)line.get(0);
        LonLat res = null;
        double dis = Double.MAX_VALUE;
        int i = 1;
        while (i < len) {
            LonLat p1 = (LonLat)line.get(i);
            LonLat q = DistanceUtils.nearestPoint(p0, p1, p);
            double d = p.distance(q);
            if (d < dis) {
                res = q;
                dis = d;
            }
            p0 = p1;
            ++i;
        }
        return new AbstractMap.SimpleEntry<LonLat, Double>(res, dis);
    }

    public static <T extends LonLat, S extends LonLat> double hausdorff(Collection<T> setA, Collection<S> setB) {
        double D0 = Double.MIN_VALUE;
        for (LonLat a : setA) {
            double d = Double.MAX_VALUE;
            for (LonLat b : setB) {
                d = Math.min(d, DistanceUtils.distance(a, b));
            }
            D0 = Math.max(D0, d);
        }
        double D1 = Double.MIN_VALUE;
        for (LonLat b : setB) {
            double d = Double.MAX_VALUE;
            for (LonLat a : setA) {
                d = Math.min(d, DistanceUtils.distance(b, a));
            }
            D1 = Math.max(D1, d);
        }
        return Math.max(D0, D1);
    }
}

