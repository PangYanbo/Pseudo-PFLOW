/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class OsmWay
extends Link {
    private static final long serialVersionUID = -591699380914514336L;
    private int _classId;
    private double _length;
    private double _forwardMaxSpeed;
    private double _backwardMaxSpeed;

    public OsmWay(String linkid, Node tailNode, Node headNode, int classId, double length, double cost, double revCost, double forwardMaxSpeed, double backwardMaxSpeed, boolean oneway) {
        this(linkid, tailNode, headNode, classId, length, cost, revCost, forwardMaxSpeed, backwardMaxSpeed, oneway, null);
    }

    public OsmWay(String linkid, Node tailNode, Node headNode, int classId, double length, double cost, double revCost, double forwardMaxSpeed, double backwardMaxSpeed, boolean oneway, List<LonLat> geom) {
        super(linkid, tailNode, headNode, cost, revCost, oneway, geom);
        this._classId = classId;
        this._length = length;
        this._forwardMaxSpeed = forwardMaxSpeed;
        this._backwardMaxSpeed = backwardMaxSpeed;
    }

    public int getClassId() {
        return this._classId;
    }

    public double getLength() {
        return this._length;
    }

    public double getForwardMaxSpeed() {
        return this._forwardMaxSpeed;
    }

    public double getBackwardMaxSpeed() {
        return this._backwardMaxSpeed;
    }

    @Override
    public OsmWay clone(Node tailNode, Node headNode) {
        return new OsmWay(this.getLinkID(), tailNode, headNode, this.getClassId(), this.getLength(), this.getCost(), this.getReverseCost(), this.getForwardMaxSpeed(), this.getBackwardMaxSpeed(), this.isOneWay(), this.getLineString());
    }
}

