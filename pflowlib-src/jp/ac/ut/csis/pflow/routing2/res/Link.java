/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.io.Serializable;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class Link
implements Serializable,
Cloneable {
    private static final long serialVersionUID = -9058532949182281951L;
    private String _linkid;
    private Node _tail;
    private Node _head;
    private double _cost;
    private double _revCost;
    private boolean _oneway;
    private List<LonLat> _geometry;

    public Link(String linkid, Node tailNode, Node headNode) {
        this(linkid, tailNode, headNode, DistanceUtils.distance(tailNode, headNode));
    }

    public Link(String linkid, Node tailNode, Node headNode, double cost) {
        this(linkid, tailNode, headNode, cost, false);
    }

    public Link(String linkid, Node tailNode, Node headNode, double cost, boolean oneway) {
        this(linkid, tailNode, headNode, cost, cost, oneway);
    }

    public Link(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway) {
        this._linkid = linkid;
        this._tail = tailNode;
        this._head = headNode;
        this._cost = cost;
        this._revCost = revCost;
        this._oneway = oneway;
        this._tail.addOutLink(this);
        this._head.addInLink(this);
        if (!this._oneway) {
            this._tail.addInLink(this);
            this._head.addOutLink(this);
        }
        this._geometry = null;
    }

    public Link(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, List<LonLat> geom) {
        this(linkid, tailNode, headNode, cost, revCost, oneway);
        this._geometry = geom;
    }

    public void setLineString(List<LonLat> geom) {
        this._geometry = geom;
    }

    public List<LonLat> getLineString() {
        return this._geometry;
    }

    public boolean hasGeometry() {
        return this._geometry != null && !this._geometry.isEmpty();
    }

    public String getLinkID() {
        return this._linkid;
    }

    public Node getTailNode() {
        return this._tail;
    }

    public Node getHeadNode() {
        return this._head;
    }

    public double getCost() {
        return this._cost;
    }

    public void setCost(double cost) {
        this._cost = cost;
    }

    public double getReverseCost() {
        return this._revCost;
    }

    public void setReverseCost(double revCost) {
        this._revCost = revCost;
    }

    public boolean isOneWay() {
        return this._oneway;
    }

    public void setOneWay(boolean oneway) {
        this._oneway = oneway;
    }

    public String toString() {
        return String.format("%s::(%s-(%f)-%s)::%b", this._linkid, this._tail, this._cost, this._head, this._oneway);
    }

    public Link clone(Node tail, Node head) {
        return new Link(this._linkid, tail, head, this._cost, this._revCost, this._oneway, this._geometry);
    }
}

