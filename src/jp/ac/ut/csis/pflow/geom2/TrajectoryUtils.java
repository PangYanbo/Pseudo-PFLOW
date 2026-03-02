/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.time.DateUtils
 */
package jp.ac.ut.csis.pflow.geom2;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import org.apache.commons.lang3.time.DateUtils;

public final class TrajectoryUtils {
    public static <T extends ILonLat> Rectangle2D.Double makeMBR(Collection<T> set) {
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        double ymin = Double.MAX_VALUE;
        double ymax = Double.MIN_VALUE;
        for (ILonLat c : set) {
            xmin = Math.min(xmin, c.getLon());
            xmax = Math.max(xmax, c.getLon());
            ymin = Math.min(ymin, c.getLat());
            ymax = Math.max(ymax, c.getLat());
        }
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    public static boolean validateLonLat(ILonLat lonlat) {
        return TrajectoryUtils.validateLonLat(lonlat.getLon(), lonlat.getLat());
    }

    public static boolean validateLonLat(double lon, double lat) {
        return !Double.isNaN(lon) && !Double.isNaN(lat) && -180.0 <= lon && lon <= 180.0 && -90.0 <= lat && lat <= 90.0;
    }

    public static <T extends ILonLat> double length(List<T> points) {
        if (points == null || points.size() <= 1) {
            return 0.0;
        }
        int size = points.size();
        double d = 0.0;
        ILonLat p = (ILonLat)points.get(0);
        int i = 1;
        while (i < size) {
            ILonLat q = (ILonLat)points.get(i);
            d += DistanceUtils.distance(p, q);
            p = q;
            ++i;
        }
        return d;
    }

    public static double getLocatePointRatio(List<? extends ILonLat> points, ILonLat p) {
        int len = points.size();
        ILonLat p0 = points.get(0);
        double dis = Double.MAX_VALUE;
        double inc = 0.0;
        double sum = 0.0;
        int i = 1;
        while (i < len) {
            ILonLat p1 = points.get(i);
            ILonLat q = DistanceUtils.nearestPoint(p0, p1, p);
            double d = DistanceUtils.distance(p, q);
            if (d < dis) {
                dis = d;
                inc = sum + DistanceUtils.distance(p0, q);
            }
            sum += DistanceUtils.distance(p0, p1);
            p0 = p1;
            ++i;
        }
        return inc / sum;
    }

    public static <T extends ILonLat> ILonLat getLineInterpolatePoint(List<T> points, double ratio) {
        double length = TrajectoryUtils.length(points);
        double divLen = length * ratio;
        int size = points.size();
        double dist = 0.0;
        ILonLat p0 = (ILonLat)points.get(0);
        int i = 1;
        while (i < size) {
            ILonLat p1 = (ILonLat)points.get(i);
            double d = DistanceUtils.distance(p0, p1);
            if (d != 0.0 && divLen <= dist + d) {
                double rn = (dist + d - divLen) / d;
                double rm = 1.0 - rn;
                return new LonLat(p0.getLon() * rn + p1.getLon() * rm, p0.getLat() * rn + p1.getLat() * rm);
            }
            dist += d;
            p0 = p1;
            ++i;
        }
        return new LonLat(p0.getLon(), p0.getLat());
    }

    public static <T extends ILonLat> List<ILonLat> getLineSubstring(List<T> points, double r0, double r1) {
        double length = TrajectoryUtils.length(points);
        double divLen0 = length * r0;
        double divLen1 = length * r1;
        int size = points.size();
        double dist = 0.0;
        ILonLat p0 = (ILonLat)points.get(0);
        ArrayList<ILonLat> subline = new ArrayList<ILonLat>();
        int i = 1;
        while (i < size) {
            double tempD;
            ILonLat p1 = (ILonLat)points.get(i);
            double d = DistanceUtils.distance(p0, p1);
            if (d != 0.0 && !((tempD = dist + d) < divLen0)) {
                double rm;
                double rn;
                if (subline.isEmpty()) {
                    rn = (dist + d - divLen0) / d;
                    rm = 1.0 - rn;
                    subline.add(new LonLat(p0.getLon() * rn + p1.getLon() * rm, p0.getLat() * rn + p1.getLat() * rm));
                }
                if (divLen0 <= tempD && tempD < divLen1) {
                    if (divLen0 != dist + d) {
                        subline.add(p1);
                    }
                } else {
                    rn = (dist + d - divLen1) / d;
                    rm = 1.0 - rn;
                    subline.add(new LonLat(p0.getLon() * rn + p1.getLon() * rm, p0.getLat() * rn + p1.getLat() * rm));
                    break;
                }
            }
            dist += d;
            p0 = p1;
            ++i;
        }
        return subline;
    }

    public static <T extends ILonLat> List<List<ILonLat>> splitTrajectory(List<T> line, double[] ratios) {
        ArrayList<Double> ratioList = new ArrayList<Double>();
        double[] dArray = ratios;
        int n = ratios.length;
        int n2 = 0;
        while (n2 < n) {
            double ratio = dArray[n2];
            ratioList.add(ratio);
            ++n2;
        }
        if ((Double)ratioList.get(0) != 0.0) {
            ratioList.add(0, 0.0);
        }
        if ((Double)ratioList.get(ratioList.size() - 1) != 1.0) {
            ratioList.add(1.0);
        }
        ArrayList<List<ILonLat>> result = new ArrayList<List<ILonLat>>();
        int len = ratioList.size();
        double r0 = (Double)ratioList.get(0);
        int i = 1;
        while (i < len) {
            double r1 = (Double)ratioList.get(i);
            List<ILonLat> subline = TrajectoryUtils.getLineSubstring(line, r0, r1);
            result.add(subline);
            r0 = r1;
            ++i;
        }
        return result;
    }

    public static <T extends ILonLat> String asWKT(List<T> points) {
        if (points == null || points.size() <= 1) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (ILonLat p : points) {
            buf.append(String.format(",%.06f %.06f", p.getLon(), p.getLat()));
        }
        return "LINESTRING(" + buf.substring(1) + ")";
    }

    public static <T extends ILonLat> Map<T, Date> putTimeStamp(List<T> points, Date ts, Date te) {
        double dist = TrajectoryUtils.length(points);
        long duration = te.getTime() - ts.getTime();
        LinkedHashMap<T, Date> result = new LinkedHashMap<T, Date>();
        Date dt = (Date)Date.class.cast(ts.clone());
        T p0 = points.get(0);
        result.put(p0, dt);
        int N = points.size() - 1;
        int i = 1;
        while (i < N) {
            T p1 = points.get(i);
            double len = DistanceUtils.distance(p0, p1);
            double ratio = len / dist;
            dt = DateUtils.addMilliseconds((Date)dt, (int)((int)(ratio * (double)duration)));
            result.put(p1, dt);
            p0 = p1;
            ++i;
        }
        Date de = (Date)Date.class.cast(te.clone());
        T pe = points.get(N);
        result.put(pe, de);
        return result;
    }

    public static <T extends ILonLat> List<ILonLatTime> interpolateUnitTime(List<T> points, Date ts, Date te) {
        return TrajectoryUtils.interpolateUnitTime(points, ts, te, 60);
    }

    public static <T extends ILonLat> List<ILonLatTime> interpolateUnitTime(List<T> points, Date ts, Date te, int unitTimeInSecond) {
        double dist = TrajectoryUtils.length(points);
        int N = (int)((te.getTime() - ts.getTime()) / ((long)unitTimeInSecond * 1000L));
        double unit_d = dist / (double)N;
        double unit_ts = (double)(te.getTime() - ts.getTime()) / (double)N;
        ILonLat p = (ILonLat)points.get(0);
        ArrayList<ILonLatTime> result = new ArrayList<ILonLatTime>();
        result.add(new LonLatTime(p.getLon(), p.getLat(), (Date)Date.class.cast(ts.clone())));
        double len = 0.0;
        int idx = 1;
        int i = 1;
        while (i < N) {
            double ln_len;
            double d;
            ILonLat p1;
            ILonLat p0;
            while (true) {
                p0 = (ILonLat)points.get(idx - 1);
                p1 = (ILonLat)points.get(idx);
                d = unit_d * (double)i;
                ln_len = DistanceUtils.distance(p0.getLon(), p0.getLat(), p1.getLon(), p1.getLat());
                double tmp_len = len + ln_len;
                if (d <= tmp_len) break;
                len = tmp_len;
                ++idx;
            }
            result.add(new LonLatTime(ln_len == 0.0 ? p.getLon() : p0.getLon() + (p1.getLon() - p0.getLon()) * (d - len) / ln_len, ln_len == 0.0 ? p.getLat() : p0.getLat() + (p1.getLat() - p0.getLat()) * (d - len) / ln_len, DateUtils.addMilliseconds((Date)ts, (int)((int)(unit_ts * (double)i)))));
            ++i;
        }
        ILonLat pn = (ILonLat)points.get(points.size() - 1);
        result.add(new LonLatTime(pn.getLon(), pn.getLat(), (Date)Date.class.cast(te.clone())));
        return result;
    }

    public static <T extends ILonLatTime> ILonLatTime getSTPointAt(List<T> stpoints, Date dt) {
        int L = stpoints.size();
        ILonLatTime ps = (ILonLatTime)stpoints.get(0);
        Date ts = ps.getTimeStamp();
        if (dt.before(ts)) {
            return null;
        }
        ILonLatTime output = null;
        if (dt.equals(ts)) {
            output = ps.clone();
        } else {
            int i = 1;
            while (i < L) {
                ILonLatTime p = (ILonLatTime)stpoints.get(i);
                Date t = p.getTimeStamp();
                if (dt.equals(t)) {
                    output = p.clone();
                    break;
                }
                if (dt.before(t)) {
                    long m = dt.getTime() - ps.getTimeStamp().getTime();
                    long n = t.getTime() - dt.getTime();
                    double x = (ps.getLon() * (double)n + p.getLon() * (double)m) / (double)(m + n);
                    double y = (ps.getLat() * (double)n + p.getLat() * (double)m) / (double)(m + n);
                    output = new LonLatTime(x, y, (Date)Date.class.cast(dt.clone()));
                    break;
                }
                ps = p;
                ++i;
            }
        }
        return output;
    }

    public static <T extends ILonLatTime, S extends ILonLatTime> long getProximateDuration(List<T> trajA, List<S> trajB, double R) {
        Map<Date, Double> dists = TrajectoryUtils.getSTDistances(trajA, trajB);
        return TrajectoryUtils.getProximateDuration(dists, R);
    }

    public static <T extends ILonLatTime, S extends ILonLatTime> long getProximateDuration(Map<Date, Double> dists, double R) {
        if (dists.size() <= 2) {
            return 0L;
        }
        Iterator<Date> keyItr = dists.keySet().iterator();
        Date t0 = keyItr.next();
        double d0 = dists.get(t0);
        long sum = 0L;
        while (keyItr.hasNext()) {
            Date t1 = keyItr.next();
            double d1 = dists.get(t1);
            if (d0 <= R) {
                sum += t1.getTime() - t0.getTime();
            }
            d0 = d1;
            t0 = t1;
        }
        return sum;
    }

    public static <T extends ILonLatTime, S extends ILonLatTime> Map<Date, Double> getSTDistances(List<T> trajA, List<S> trajB) {
        TreeSet<Date> tAxis = new TreeSet<Date>();
        for (ILonLatTime p : trajA) {
            tAxis.add(p.getTimeStamp());
        }
        for (ILonLatTime p : trajB) {
            tAxis.add(p.getTimeStamp());
        }
        TreeMap<Date, Double> distMap = new TreeMap<Date, Double>();
        for (Date t : tAxis) {
            ILonLatTime pa = TrajectoryUtils.getSTPointAt(trajA, t);
            ILonLatTime pe = TrajectoryUtils.getSTPointAt(trajB, t);
            if (pa == null || pe == null) continue;
            distMap.put(t, DistanceUtils.distance(pa, pe));
        }
        return distMap;
    }
}

