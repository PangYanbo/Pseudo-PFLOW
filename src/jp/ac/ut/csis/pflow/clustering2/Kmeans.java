/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.math.stat.StatUtils
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import org.apache.commons.math.stat.StatUtils;

public class Kmeans {
    private int _k;
    private IDistrib _distrib;
    public static final IDistrib RANDOM_ON_POINTS_DISTRIB = new IDistrib(){

        @Override
        public <T extends ILonLat> List<ILonLat> generateInitialCentroids(int k, List<T> points) {
            int N = points.size();
            Random rand = new Random();
            HashSet<Integer> indexSet = new HashSet<Integer>();
            while (indexSet.size() < k) {
                indexSet.add(rand.nextInt(N));
            }
            ArrayList<ILonLat> centroids = new ArrayList<ILonLat>();
            Iterator iterator = indexSet.iterator();
            while (iterator.hasNext()) {
                int idx = (Integer)iterator.next();
                ILonLat point = (ILonLat)points.get(idx);
                centroids.add(new LonLat(point.getLon(), point.getLat()));
            }
            return centroids;
        }
    };
    public static final IDistrib UNIFORM_ON_POINTS_DISTRIB = new IDistrib(){

        @Override
        public <T extends ILonLat> List<ILonLat> generateInitialCentroids(int k, List<T> points) {
            int N = points.size();
            int[] indexes = new int[k];
            int i = k - 2;
            while (i >= 0) {
                indexes[i] = N / k * i;
                --i;
            }
            indexes[k - 1] = N - 1;
            ArrayList<ILonLat> centroids = new ArrayList<ILonLat>();
            int[] nArray = indexes;
            int n = indexes.length;
            int n2 = 0;
            while (n2 < n) {
                int idx = nArray[n2];
                centroids.add((ILonLat)points.get(idx));
                ++n2;
            }
            return centroids;
        }
    };
    public static final IDistrib SPATIALLY_RANDOM_DISTRIB = new IDistrib(){

        @Override
        public <T extends ILonLat> List<ILonLat> generateInitialCentroids(int k, List<T> points) {
            Rectangle2D.Double mbr = TrajectoryUtils.makeMBR(points);
            ArrayList<ILonLat> centroids = new ArrayList<ILonLat>();
            Random rand = new Random();
            int i = 0;
            while (i < k) {
                double x = mbr.getMinX() + ((RectangularShape)mbr).getWidth() * rand.nextDouble();
                double y = mbr.getMinY() + ((RectangularShape)mbr).getHeight() * rand.nextDouble();
                centroids.add(new LonLat(x, y));
                ++i;
            }
            return centroids;
        }
    };
    public static final IDistrib SPATIALLY_GRID_DISTRIB = new IDistrib(){

        @Override
        public <T extends ILonLat> List<ILonLat> generateInitialCentroids(int k, List<T> points) {
            HashMap<Rectangle2D.Double, List<T>> rects = new HashMap<Rectangle2D.Double, List<T>>();
            Rectangle2D.Double mbr = TrajectoryUtils.makeMBR(points);
            rects.put(mbr, points);
            int prevN = 0;
            double prevLen = 0.0;
            while (k > rects.size()) {
                double len = 0.0;
                ArrayList<Rectangle2D.Double> itr = new ArrayList<Rectangle2D.Double>(rects.keySet());
                for (Rectangle2D.Double double_ : itr) {
                    List<T> cands = rects.get(double_);
                    double dist = this.getDistribution(cands);
                    if (dist < 0.01) {
                        len += double_.getWidth();
                        continue;
                    }
                    List<Rectangle2D.Double> subRects = this.listSubRectangles(double_);
                    for (Rectangle2D.Double subRect : subRects) {
                        List<T> subPoints = this.intersectPoints(subRect, cands);
                        if (subPoints == null || subPoints.isEmpty()) continue;
                        len += subRect.getWidth();
                        rects.put(subRect, subPoints);
                    }
                    rects.remove(double_);
                }
                if (prevLen == len && prevN == rects.size()) break;
                prevN = rects.size();
                prevLen = len;
            }
            ArrayList<Map.Entry<Rectangle2D.Double, List<T>>> list = new ArrayList<Map.Entry<Rectangle2D.Double, List<T>>>(rects.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<Rectangle2D.Double, List<T>>>(){

                @Override
                public int compare(Map.Entry<Rectangle2D.Double, List<T>> a, Map.Entry<Rectangle2D.Double, List<T>> b) {
                    Integer ia = a.getValue().size();
                    Integer ib = b.getValue().size();
                    return ib.compareTo(ia);
                }
            });
            ArrayList<ILonLat> centroids = new ArrayList<ILonLat>();
            int idx = 1;
            for (Map.Entry<Rectangle2D.Double, List<T>> entry : list) {
                Rectangle2D.Double rect = entry.getKey();
                centroids.add(new LonLat(rect.getCenterX(), rect.getCenterY()));
                if (++idx > k) break;
            }
            return centroids;
        }

        private <T extends ILonLat> double getDistribution(List<T> points) {
            int N = points.size();
            double[] dists = new double[N * (N - 1) / 2];
            int idx = 0;
            int i = 0;
            while (i < N) {
                ILonLat p0 = (ILonLat)points.get(i);
                int j = i + 1;
                while (j < N) {
                    ILonLat p1 = (ILonLat)points.get(j);
                    dists[idx++] = DistanceUtils.distance(p0, p1);
                    ++j;
                }
                ++i;
            }
            return StatUtils.mean((double[])dists);
        }

        private <T extends ILonLat> List<T> intersectPoints(Rectangle2D.Double rect, List<T> points) {
            ArrayList<T> list = new ArrayList<T>();
            for (T p : points) {
                if (!rect.contains(p.getLon(), p.getLat())) continue;
                list.add(p);
            }
            return list;
        }

        private List<Rectangle2D.Double> listSubRectangles(Rectangle2D.Double rect) {
            return Arrays.asList(new Rectangle2D.Double(rect.getMinX(), rect.getMinY(), rect.getWidth() / 2.0, rect.getHeight() / 2.0), new Rectangle2D.Double(rect.getMinX(), rect.getCenterY(), rect.getWidth() / 2.0, rect.getHeight() / 2.0), new Rectangle2D.Double(rect.getCenterX(), rect.getCenterY(), rect.getWidth() / 2.0, rect.getHeight() / 2.0), new Rectangle2D.Double(rect.getCenterX(), rect.getMinY(), rect.getWidth() / 2.0, rect.getHeight() / 2.0));
        }
    };

    public Kmeans(int k, IDistrib distrib) {
        this._k = k;
        this._distrib = distrib;
    }

    public Kmeans(int k) {
        this(k, RANDOM_ON_POINTS_DISTRIB);
    }

    public int getK() {
        return this._k;
    }

    public void setK(int k) {
        this._k = k;
    }

    public IDistrib getDistributionMethod() {
        return this._distrib;
    }

    public void setDistributionMethod(IDistrib distrib) {
        this._distrib = distrib;
    }

    public <T extends ILonLat> Map<ILonLat, List<T>> clustering(List<T> points) {
        List<ILonLat> centroids = this._distrib.generateInitialCentroids(this._k, points);
        Map<ILonLat, List<T>> clusters = null;
        do {
            if (clusters == null) continue;
            centroids = new ArrayList(clusters.keySet());
        } while (this.updateCentroids(clusters = this.exploreNearestPoint(points, centroids)));
        return clusters;
    }

    private <T extends ILonLat> boolean updateCentroids(Map<ILonLat, List<T>> nearestPoints) {
        boolean updateFlag = false;
        for (Map.Entry<ILonLat, List<T>> entry : nearestPoints.entrySet()) {
            ILonLat centroid = entry.getKey();
            List<T> points = entry.getValue();
            double[] xArray = new double[points.size()];
            double[] yArray = new double[points.size()];
            int i = points.size() - 1;
            while (i >= 0) {
                xArray[i] = ((ILonLat)points.get(i)).getLon();
                yArray[i] = ((ILonLat)points.get(i)).getLat();
                --i;
            }
            double newX = StatUtils.mean((double[])xArray);
            double newY = StatUtils.mean((double[])yArray);
            double diff = DistanceUtils.distance(centroid.getLon(), centroid.getLat(), newX, newY);
            updateFlag |= diff >= 1.0E-5;
            centroid.setLocation(newX, newY);
        }
        return updateFlag;
    }

    private <T extends ILonLat> Map<ILonLat, List<T>> exploreNearestPoint(Collection<T> points, List<ILonLat> centroids) {
        HashMap<ILonLat, List<T>> nearestPoints = new HashMap<ILonLat, List<T>>(centroids.size());
        for (ILonLat point : points) {
            ILonLat nearestPoint = null;
            double dist = Double.MAX_VALUE;
            for (ILonLat centroid : centroids) {
                double d = DistanceUtils.distance(point, centroid);
                if (!(d < dist)) continue;
                nearestPoint = centroid;
                dist = d;
            }
            if (!nearestPoints.containsKey(nearestPoint)) {
                nearestPoints.put(nearestPoint, new ArrayList());
            }
            ((List)nearestPoints.get(nearestPoint)).add(point);
        }
        return nearestPoints;
    }

    public static interface IDistrib {
        public <T extends ILonLat> List<ILonLat> generateInitialCentroids(int var1, List<T> var2);
    }
}

