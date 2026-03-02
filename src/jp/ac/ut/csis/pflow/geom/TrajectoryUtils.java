/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.time.DateUtils
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.STPoint;
import org.apache.commons.lang3.time.DateUtils;

public final class TrajectoryUtils {
    public static <T extends LonLat> double length(List<T> points) {
        if (points == null || points.size() <= 1) {
            return 0.0;
        }
        int size = points.size();
        double d = 0.0;
        LonLat p = (LonLat)points.get(0);
        int i = 1;
        while (i < size) {
            LonLat q = (LonLat)points.get(i);
            d += DistanceUtils.distance(p, q);
            p = q;
            ++i;
        }
        return d;
    }

    public static double getLocatePointRatio(List<? extends LonLat> points, LonLat p) {
        int len = points.size();
        LonLat p0 = points.get(0);
        double dis = Double.MAX_VALUE;
        double inc = 0.0;
        double sum = 0.0;
        int i = 1;
        while (i < len) {
            LonLat p1 = points.get(i);
            LonLat q = DistanceUtils.nearestPoint(p0, p1, p);
            double d = p.distance(q);
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

    public static <T extends LonLat> LonLat getLineInterpolatePoint(List<T> points, double ratio) {
        double length = TrajectoryUtils.length(points);
        double divLen = length * ratio;
        int size = points.size();
        double dist = 0.0;
        LonLat p0 = (LonLat)points.get(0);
        int i = 1;
        while (i < size) {
            LonLat p1 = (LonLat)points.get(i);
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

    public static <T extends LonLat> List<LonLat> getLineSubstring(List<T> points, double r0, double r1) {
        double length = TrajectoryUtils.length(points);
        double divLen0 = length * r0;
        double divLen1 = length * r1;
        int size = points.size();
        double dist = 0.0;
        LonLat p0 = (LonLat)points.get(0);
        ArrayList<LonLat> subline = new ArrayList<LonLat>();
        int i = 1;
        while (i < size) {
            double tempD;
            LonLat p1 = (LonLat)points.get(i);
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

    public static <T extends LonLat> List<List<LonLat>> splitTrajectory(List<T> line, double[] ratios) {
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
        ArrayList<List<LonLat>> result = new ArrayList<List<LonLat>>();
        int len = ratioList.size();
        double r0 = (Double)ratioList.get(0);
        int i = 1;
        while (i < len) {
            double r1 = (Double)ratioList.get(i);
            List<LonLat> subline = TrajectoryUtils.getLineSubstring(line, r0, r1);
            result.add(subline);
            r0 = r1;
            ++i;
        }
        return result;
    }

    public static <T extends LonLat> String asWKT(List<T> points) {
        if (points == null || points.size() <= 1) {
            return null;
        }
        boolean m = false;
        StringBuffer buf = new StringBuffer();
        for (LonLat p : points) {
            if (m |= p instanceof STPoint) {
                STPoint q = (STPoint)STPoint.class.cast(p);
                buf.append(String.format(",%.06f %.06f %d", q.getLon(), q.getLat(), q.getTimeStamp().getTime()));
                continue;
            }
            buf.append(String.format(",%.06f %.06f", p.getLon(), p.getLat()));
        }
        return m ? "LINESTRING M(" + buf.substring(1) + ")" : "LINESTRING(" + buf.substring(1) + ")";
    }

    public static <T extends LonLat> List<STPoint> assignTimeStamp(List<T> points, Date ts, Date te) {
        double dist = TrajectoryUtils.length(points);
        long duration = te.getTime() - ts.getTime();
        ArrayList<STPoint> result = new ArrayList<STPoint>();
        Date dt = (Date)Date.class.cast(ts.clone());
        LonLat p0 = (LonLat)points.get(0);
        result.add(new STPoint(dt, p0.getLon(), p0.getLat()));
        int i = 1;
        while (i < points.size()) {
            LonLat p1 = (LonLat)points.get(i);
            double len = DistanceUtils.distance(p0, p1);
            double ratio = len / dist;
            dt = DateUtils.addMilliseconds((Date)dt, (int)((int)(ratio * (double)duration)));
            result.add(new STPoint(dt, p1.getLon(), p1.getLat()));
            p0 = p1;
            ++i;
        }
        ((STPoint)result.get(result.size() - 1)).setTimeStamp(te);
        return result;
    }

    public static <T extends LonLat> Map<T, Date> putTimeStamp(List<T> points, Date ts, Date te) {
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

    public static <T extends LonLat> List<STPoint> interpolateUnitTime(List<T> points, Date ts, Date te) {
        return TrajectoryUtils.interpolateUnitTime(points, ts, te, 60);
    }

    public static <T extends LonLat> List<STPoint> interpolateUnitTime(List<T> points, Date ts, Date te, int unitTimeInSecond) {
        double dist = TrajectoryUtils.length(points);
        int N = (int)((te.getTime() - ts.getTime()) / ((long)unitTimeInSecond * 1000L));
        double unit_d = dist / (double)N;
        double unit_ts = (double)(te.getTime() - ts.getTime()) / (double)N;
        LonLat p = (LonLat)points.get(0);
        ArrayList<STPoint> result = new ArrayList<STPoint>();
        result.add(new STPoint((Date)Date.class.cast(ts.clone()), p.getLon(), p.getLat()));
        double len = 0.0;
        int idx = 1;
        int i = 1;
        while (i < N) {
            double ln_len;
            double d;
            LonLat p1;
            LonLat p0;
            while (true) {
                p0 = (LonLat)points.get(idx - 1);
                p1 = (LonLat)points.get(idx);
                d = unit_d * (double)i;
                ln_len = DistanceUtils.distance(p0.getLon(), p0.getLat(), p1.getLon(), p1.getLat());
                double tmp_len = len + ln_len;
                if (d <= tmp_len) break;
                len = tmp_len;
                ++idx;
            }
            result.add(new STPoint(DateUtils.addMilliseconds((Date)ts, (int)((int)(unit_ts * (double)i))), ln_len == 0.0 ? p.getLon() : p0.getLon() + (p1.getLon() - p0.getLon()) * (d - len) / ln_len, ln_len == 0.0 ? p.getLat() : p0.getLat() + (p1.getLat() - p0.getLat()) * (d - len) / ln_len));
            ++i;
        }
        LonLat pn = (LonLat)points.get(points.size() - 1);
        result.add(new STPoint((Date)Date.class.cast(te.clone()), pn.getLon(), pn.getLat()));
        return result;
    }

    public static <T extends STPoint> STPoint getSTPointAt(List<T> stpoints, Date dt) {
        int L = stpoints.size();
        STPoint ps = (STPoint)stpoints.get(0);
        Date ts = ps.getTimeStamp();
        if (dt.before(ts)) {
            return null;
        }
        STPoint output = null;
        if (dt.equals(ts)) {
            output = ps.clone();
        } else {
            int i = 1;
            while (i < L) {
                STPoint p = (STPoint)stpoints.get(i);
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
                    output = new STPoint((Date)Date.class.cast(dt.clone()), x, y);
                    break;
                }
                ps = p;
                ++i;
            }
        }
        return output;
    }

    public static <T extends STPoint, S extends STPoint> long getProximateDuration(List<T> trajA, List<S> trajB, double R) {
        Map<Date, Double> dists = TrajectoryUtils.getSTDistances(trajA, trajB);
        return TrajectoryUtils.getProximateDuration(dists, R);
    }

    public static <T extends STPoint, S extends STPoint> long getProximateDuration(Map<Date, Double> dists, double R) {
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

    public static <T extends STPoint, S extends STPoint> Map<Date, Double> getSTDistances(List<T> trajA, List<S> trajB) {
        TreeSet<Date> tAxis = new TreeSet<Date>();
        for (STPoint p : trajA) {
            tAxis.add(p.getTimeStamp());
        }
        for (STPoint p : trajB) {
            tAxis.add(p.getTimeStamp());
        }
        TreeMap<Date, Double> distMap = new TreeMap<Date, Double>();
        for (Date t : tAxis) {
            STPoint pa = TrajectoryUtils.getSTPointAt(trajA, t);
            STPoint pe = TrajectoryUtils.getSTPointAt(trajB, t);
            if (pa == null || pe == null) continue;
            distMap.put(t, DistanceUtils.distance(pa, pe));
        }
        return distMap;
    }
}

