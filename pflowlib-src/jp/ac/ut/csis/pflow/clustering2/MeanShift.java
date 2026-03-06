/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;

public class MeanShift {
    public static final IKernel GAUSSIAN = new IKernel(){

        @Override
        public double getDensity(ILonLat p, ILonLat pi, double h) {
            double x = DistanceUtils.distance(p.getLon(), p.getLat(), pi.getLon(), p.getLat());
            double y = DistanceUtils.distance(p.getLon(), p.getLat(), p.getLon(), pi.getLat());
            double x2 = x * x;
            double y2 = y * y;
            double h2 = h * h;
            return 1.0 / (Math.PI * 2 * h2) * Math.exp((x2 + y2) / (-2.0 * h2));
        }
    };
    public static final IKernel RECTANGULAR = new IKernel(){

        @Override
        public double getDensity(ILonLat p, ILonLat pi, double h) {
            double r = DistanceUtils.distance(p, pi);
            return r < h ? 1 : 0;
        }
    };

    public static <T extends ILonLat> Map<ILonLat, List<T>> clustering2d(List<T> data, double h, double e) {
        return MeanShift.clustering2d(RECTANGULAR, data, h, e);
    }

    public static <T extends ILonLat> Map<ILonLat, List<T>> clustering2d(IKernel kernel, List<T> data, double h, double e) {
        LinkedHashMap<ILonLat, List<T>> result = new LinkedHashMap<ILonLat, List<T>>();
        int N = data.size();
        int i = 0;
        while (i < N) {
            LonLat m;
            LonLat mean = new LonLat(((ILonLat)data.get(i)).getLon(), ((ILonLat)data.get(i)).getLat());
            while (true) {
                double numx = 0.0;
                double numy = 0.0;
                double din = 0.0;
                int j = 0;
                while (j < N) {
                    ILonLat p = (ILonLat)data.get(j);
                    double k = kernel.getDensity(mean, p, h);
                    numx += k * p.getLon();
                    numy += k * p.getLat();
                    din += k;
                    ++j;
                }
                m = new LonLat(numx / din, numy / din);
                if (DistanceUtils.distance(mean, m) < e) break;
                mean = m;
            }
            mean = m;
            List<ILonLat> cluster = null;
            for (ILonLat p : result.keySet()) {
                if (!(DistanceUtils.distance(mean, p) < e)) continue;
                cluster = (List)result.get(p);
                break;
            }
            if (cluster == null) {
                cluster = new ArrayList<ILonLat>();
                result.put(mean, cluster);
            }
            cluster.add((ILonLat)data.get(i));
            ++i;
        }
        return result;
    }

    public static interface IKernel {
        public double getDensity(ILonLat var1, ILonLat var2, double var3);
    }
}

