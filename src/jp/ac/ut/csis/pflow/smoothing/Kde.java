/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.smoothing;

import java.util.LinkedHashMap;
import java.util.Map;
import jp.ac.ut.csis.pflow.geom.LonLat;

public class Kde {
    public static final IKernel GAUSSIAN = new IKernel(){

        @Override
        public double getDensity(double x, double xi, double h) {
            double v = (x - xi) / h;
            return Math.exp(-1.0 * v * v / 2.0) / Math.sqrt(Math.PI * 2);
        }
    };
    public static final IKernel EPANECHNIKOV = new IKernel(){

        @Override
        public double getDensity(double x, double xi, double h) {
            double v = (x - xi) / h;
            return Math.abs(v) <= 1.0 ? (1.0 - v * v) * 3.0 / 4.0 : 0.0;
        }
    };
    public static final IKernel RECTANGULAR = new IKernel(){

        @Override
        public double getDensity(double x, double xi, double h) {
            double v = (x - xi) / h;
            return Math.abs(v) < 1.0 ? 0.5 : 0.0;
        }
    };
    public static final IKernel TRIANGULAR = new IKernel(){

        @Override
        public double getDensity(double x, double xi, double h) {
            double v = (x - xi) / h;
            return Math.abs(v) < 1.0 ? 1.0 - Math.abs(v) : 0.0;
        }
    };
    public static final IKernel COSINE = new IKernel(){

        @Override
        public double getDensity(double x, double xi, double h) {
            double v = (x - xi) / h;
            return Math.abs(v) < 1.0 ? 0.7853981633974483 * Math.cos(Math.PI * v / 2.0) : 0.0;
        }
    };

    public Map<Double, Double> estimate(double[] data, double h, IKernel kernel) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double[] dArray = data;
        int n = data.length;
        int n2 = 0;
        while (n2 < n) {
            double d = dArray[n2];
            min = Math.min(min, d);
            max = Math.max(max, d);
            ++n2;
        }
        double w = (max - min) / 100.0;
        return this.estimate(data, min, max, w, h, kernel);
    }

    public Map<Double, Double> estimate(double[] data, double min, double max, double range, double h, IKernel kernel) {
        LinkedHashMap<Double, Double> result = new LinkedHashMap<Double, Double>();
        double x = min;
        while (x <= max) {
            double sum = 0.0;
            double[] dArray = data;
            int n = data.length;
            int n2 = 0;
            while (n2 < n) {
                double d = dArray[n2];
                sum += kernel.getDensity(x, d, h);
                ++n2;
            }
            result.put(x, data.length == 0 ? 0.0 : sum / ((double)data.length * h));
            x += range;
        }
        return result;
    }

    public <T extends LonLat> Map<LonLat, Double> estimate2d(T[] data, double h1, double h2, IKernel kernel) {
        double minx = Double.MAX_VALUE;
        double maxx = Double.MIN_VALUE;
        double miny = Double.MAX_VALUE;
        double maxy = Double.MIN_VALUE;
        T[] TArray = data;
        int n = data.length;
        int n2 = 0;
        while (n2 < n) {
            T d = TArray[n2];
            minx = Math.min(minx, ((LonLat)d).getLon());
            maxx = Math.max(maxx, ((LonLat)d).getLon());
            miny = Math.min(miny, ((LonLat)d).getLat());
            maxy = Math.max(maxy, ((LonLat)d).getLat());
            ++n2;
        }
        double rangex = (maxx - minx) / 100.0;
        double rangey = (maxy - miny) / 100.0;
        return this.estimate2d((LonLat[])data, minx, maxx, miny, maxy, rangex, rangey, h1, h2, kernel);
    }

    public <T extends LonLat> Map<LonLat, Double> estimate2d(T[] data, double minx, double maxx, double miny, double maxy, double rangex, double rangey, double h1, double h2, IKernel kernel) {
        LinkedHashMap<LonLat, Double> result = new LinkedHashMap<LonLat, Double>();
        double D = 0.0;
        double x = minx;
        while (x <= maxx) {
            double y = miny;
            while (y <= maxy) {
                double sum = 0.0;
                T[] TArray = data;
                int n = data.length;
                int n2 = 0;
                while (n2 < n) {
                    T d = TArray[n2];
                    sum += kernel.getDensity(x, ((LonLat)d).getLon(), h1) * kernel.getDensity(y, ((LonLat)d).getLat(), h2);
                    ++n2;
                }
                double density = data.length == 0 ? 0.0 : sum / ((double)data.length * h1 * h2);
                D += density;
                result.put(new LonLat(x, y), density);
                y += rangey;
            }
            x += rangex;
        }
        for (LonLat p : result.keySet()) {
            result.put(p, (Double)result.get(p) / D);
        }
        return result;
    }

    public static interface IKernel {
        public double getDensity(double var1, double var3, double var5);
    }
}

