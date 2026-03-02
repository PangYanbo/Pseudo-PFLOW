/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ac.ut.csis.pflow.clustering2.Cluster;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;

public class DBSCAN {
    private double _eps;
    private int _minPts;

    public DBSCAN(double eps, int minPts) {
        this._eps = eps;
        this._minPts = minPts;
    }

    public double getEps() {
        return this._eps;
    }

    public int getMinPts() {
        return this._minPts;
    }

    public <T extends ILonLat> List<Cluster<T>> clustering(Collection<T> points) {
        ArrayList<Cluster<T>> clusters = new ArrayList<Cluster<T>>();
        HashMap<ILonLat, PointLabel> checkMap = new HashMap<ILonLat, PointLabel>();
        for (ILonLat point : points) {
            if (checkMap.containsKey(point)) continue;
            List<ILonLat> neighbors = this.exploreNeighbors(point, points);
            if (neighbors.size() >= this._minPts) {
                checkMap.put(point, PointLabel.VALID);
                Cluster<ILonLat> cluster = new Cluster<ILonLat>();
                cluster.addPoint(point);
                clusters.add(this.expandCluster(cluster, neighbors, points, checkMap));
                continue;
            }
            checkMap.put(point, PointLabel.NOISE);
        }
        return clusters;
    }

    private <T extends ILonLat> Cluster<T> expandCluster(Cluster<T> cluster, List<T> neighbors, Collection<T> points, Map<T, PointLabel> checkMap) {
        ArrayList<T> cands = new ArrayList<T>(neighbors);
        int idx = 0;
        while (idx < cands.size()) {
            List<ILonLat> seedNeighbors;
            ILonLat cand = (ILonLat)cands.get(idx);
            PointLabel label = checkMap.get(cand);
            if (label == null && (seedNeighbors = this.exploreNeighbors(cand, points)).size() >= this._minPts) {
                for (ILonLat seedNeighbor : seedNeighbors) {
                    if (cands.contains(seedNeighbor)) continue;
                    cands.add(seedNeighbor);
                }
            }
            if (label != PointLabel.VALID) {
                checkMap.put(cand, PointLabel.VALID);
                cluster.addPoint(cand);
            }
            ++idx;
        }
        return cluster;
    }

    private <T extends ILonLat> List<T> exploreNeighbors(T point, Collection<T> points) {
        ArrayList<ILonLat> neighbors = new ArrayList<ILonLat>();
        for (ILonLat p : points) {
            if (point.equals(p) || !(DistanceUtils.distance(point, p) < this._eps)) continue;
            neighbors.add(p);
        }
        return neighbors;
    }

    private static enum PointLabel {
        VALID,
        NOISE;

    }
}

