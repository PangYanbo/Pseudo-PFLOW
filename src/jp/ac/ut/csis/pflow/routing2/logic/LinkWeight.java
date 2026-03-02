/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.logic.Dial;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class LinkWeight
extends Dial.DialWeight {
    private Map<LonLat, Double> _weights;

    protected LinkWeight() {
        this(new LinkedHashMap<LonLat, Double>());
    }

    protected LinkWeight(Map<LonLat, Double> weights) {
        this._weights = weights;
    }

    public void setWeights(Map<LonLat, Double> weights) {
        this._weights = weights;
    }

    @Override
    protected Map<Node, Double> createInitialWeights(Network network, Node depnode, Node arrnode) {
        Hashtable<Node, Double> weights = new Hashtable<Node, Double>();
        for (Map.Entry<LonLat, Double> entry : this._weights.entrySet()) {
            LonLat bspoint = entry.getKey();
            double sigma2 = entry.getValue();
            double sigma = sigma2 / 2.0;
            for (Node node : network.listNodes()) {
                double d2 = DistanceUtils.distance(bspoint, node);
                if (d2 > sigma2) continue;
                double w = 1.0 / Math.sqrt(Math.PI * 2 * sigma * sigma) * Math.exp(-1.0 * (d2 * d2) / (2.0 * sigma * sigma));
                weights.put(node, weights.containsKey(node) ? (Double)weights.get(node) + w : w);
            }
        }
        weights.put(arrnode, weights.containsKey(arrnode) ? (Double)weights.get(arrnode) + 1.0 : 1.0);
        return weights;
    }
}

