/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class OsmLink
extends Link {
    private static final long serialVersionUID = -337838600805670552L;
    public static final int[] ROAD_TYPES = new int[128];
    private int _clazz;
    private double _speed;

    static {
        int i = 0;
        while (i < 128) {
            OsmLink.ROAD_TYPES[i] = i;
            ++i;
        }
    }

    public OsmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int clazz, double speed) {
        super(linkid, tailNode, headNode, cost, revCost, oneway);
        this._clazz = clazz;
        this._speed = speed;
    }

    public OsmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int clazz, double speed, List<LonLat> geom) {
        super(linkid, tailNode, headNode, cost, revCost, oneway, geom);
        this._clazz = clazz;
        this._speed = speed;
    }

    public int getRoadClass() {
        return this._clazz;
    }

    public double getSpeed() {
        return this._speed;
    }

    @Override
    public OsmLink clone(Node tailNode, Node headNode) {
        return new OsmLink(this.getLinkID(), tailNode, headNode, this.getCost(), this.getReverseCost(), this.isOneWay(), this.getRoadClass(), this.getSpeed(), this.getLineString());
    }
}

