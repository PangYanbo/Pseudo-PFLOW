/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.math3.stat.StatUtils
 */
package jp.ac.ut.csis.pflow.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import org.apache.commons.math3.stat.StatUtils;

public class CanopyKmeans {
    private double _innerRadius;
    private double _outerRadius;

    public CanopyKmeans(double innerRadius, double outerRadius) {
        this._innerRadius = innerRadius;
        this._outerRadius = outerRadius;
    }

    public double getInnerRadius() {
        return this._innerRadius;
    }

    public double getOuterRadius() {
        return this._outerRadius;
    }

    public <T extends LonLat> Map<LonLat, List<T>> clustering(List<T> points) {
        List<Canopy> canopies = this.extractCanopies(points, this._innerRadius, this._outerRadius);
        ArrayList<LonLat> centroids = new ArrayList<LonLat>(canopies.size());
        for (Canopy canopy : canopies) {
            centroids.add(canopy.getCentroid().clone());
        }
        Map<LonLat, List<T>> clusters = null;
        do {
            if (clusters == null) continue;
            centroids = new ArrayList(clusters.keySet());
        } while (this.updateCentroids(clusters = this.exploreNearestPoint(points, centroids)));
        return clusters;
    }

    private <T extends LonLat> boolean updateCentroids(Map<LonLat, List<T>> nearestPoints) {
        boolean updateFlag = false;
        for (Map.Entry<LonLat, List<T>> entry : nearestPoints.entrySet()) {
            LonLat centroid = entry.getKey();
            List<T> points = entry.getValue();
            double[] xArray = new double[points.size()];
            double[] yArray = new double[points.size()];
            int i = points.size() - 1;
            while (i >= 0) {
                xArray[i] = ((LonLat)points.get(i)).getLon();
                yArray[i] = ((LonLat)points.get(i)).getLat();
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

    private <T extends LonLat> Map<LonLat, List<T>> exploreNearestPoint(Collection<T> points, List<LonLat> centroids) {
        HashMap<LonLat, List<T>> nearestPoints = new HashMap<LonLat, List<T>>(centroids.size());
        for (LonLat point : points) {
            LonLat nearestPoint = null;
            double dist = Double.MAX_VALUE;
            for (LonLat centroid : centroids) {
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

    protected <T extends LonLat> List<Canopy> extractCanopies(List<T> points, double innerRadius, double outerRadius) {
        ArrayList<Canopy> canopies = new ArrayList<Canopy>();
        ArrayList<T> tempPoints = new ArrayList<T>(points);
        Random random = new Random();
        while (!tempPoints.isEmpty()) {
            int size = tempPoints.size();
            int randIndex = random.nextInt(size);
            LonLat centroid = (LonLat)tempPoints.get(randIndex);
            Canopy canopy = new Canopy(innerRadius, outerRadius, centroid);
            int i = size - 1;
            while (i >= 0) {
                Type type = canopy.add((LonLat)tempPoints.get(i));
                if (type != null && type.equals((Object)Type.INNER)) {
                    tempPoints.remove(i);
                }
                --i;
            }
            canopies.add(canopy);
        }
        return canopies;
    }

    protected class Canopy {
        private double _innerRadius;
        private double _outerRadius;
        private LonLat _centroid;
        private Map<LonLat, Type> _points;

        protected Canopy(double innerRadius, double outerRadius, LonLat centroid) {
            this._centroid = centroid;
            this._innerRadius = innerRadius;
            this._outerRadius = outerRadius;
            this._points = new HashMap<LonLat, Type>();
            this._points.put(centroid, Type.INNER);
        }

        protected LonLat getCentroid() {
            return this._centroid;
        }

        protected Type add(LonLat point) {
            double dist = DistanceUtils.distance(this._centroid, point);
            Type type = null;
            if (dist <= this._innerRadius) {
                type = Type.INNER;
                this._points.put(point, type);
            } else if (dist <= this._outerRadius) {
                type = Type.OUTER;
                this._points.put(point, type);
            }
            return type;
        }
    }

    protected static enum Type {
        INNER,
        OUTER;

    }
}

