/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.math3.stat.StatUtils
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
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

    public <T extends ILonLat> Map<ILonLat, List<T>> clustering(List<T> points) {
        List<Canopy> canopies = this.extractCanopies(points, this._innerRadius, this._outerRadius);
        ArrayList<ILonLat> centroids = new ArrayList<ILonLat>(canopies.size());
        for (Canopy canopy : canopies) {
            ILonLat centroid = canopy.getCentroid();
            centroids.add(new LonLat(centroid.getLon(), centroid.getLat()));
        }
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

    protected <T extends ILonLat> List<Canopy> extractCanopies(List<T> points, double innerRadius, double outerRadius) {
        ArrayList<Canopy> canopies = new ArrayList<Canopy>();
        ArrayList<T> tempPoints = new ArrayList<T>(points);
        Random random = new Random();
        while (!tempPoints.isEmpty()) {
            int size = tempPoints.size();
            int randIndex = random.nextInt(size);
            ILonLat centroid = (ILonLat)tempPoints.get(randIndex);
            Canopy canopy = new Canopy(innerRadius, outerRadius, centroid);
            int i = size - 1;
            while (i >= 0) {
                Type type = canopy.add((ILonLat)tempPoints.get(i));
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
        private ILonLat _centroid;
        private Map<ILonLat, Type> _points;

        protected Canopy(double innerRadius, double outerRadius, ILonLat centroid) {
            this._centroid = centroid;
            this._innerRadius = innerRadius;
            this._outerRadius = outerRadius;
            this._points = new HashMap<ILonLat, Type>();
            this._points.put(centroid, Type.INNER);
        }

        protected ILonLat getCentroid() {
            return this._centroid;
        }

        protected Type add(ILonLat point) {
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

