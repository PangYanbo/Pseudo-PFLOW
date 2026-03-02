/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.res;

import java.io.Serializable;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public class Link
implements Serializable,
Cloneable {
    private static final long serialVersionUID = -3707310250337056795L;
    private String _linkid;
    private Node _tail;
    private Node _head;
    private double _length;
    private double _forwardCost;
    private double _backwardCost;
    private boolean _oneway;
    private List<ILonLat> _geometry;

    public Link(String linkid, Node tailNode, Node headNode) {
        this(linkid, tailNode, headNode, DistanceUtils.distance(tailNode, headNode));
    }

    public Link(String linkid, Node tailNode, Node headNode, double length) {
        this(linkid, tailNode, headNode, length, length, length, false);
    }

    public Link(String linkid, Node tailNode, Node headNode, double length, double cost, boolean oneway) {
        this(linkid, tailNode, headNode, length, cost, oneway ? Double.NaN : cost, oneway);
    }

    public Link(String linkid, Node tailNode, Node headNode, double length, double cost, double revCost, boolean oneway) {
        this(linkid, tailNode, headNode, length, cost, revCost, oneway, null);
    }

    public Link(String linkid, Node tailNode, Node headNode, double length, double cost, double revCost, boolean oneway, List<ILonLat> geom) {
        this._linkid = linkid;
        this._tail = tailNode;
        this._head = headNode;
        this._length = length;
        this._forwardCost = cost;
        this._backwardCost = Double.NaN;
        this._oneway = oneway;
        this._tail.addOutLink(this);
        this._head.addInLink(this);
        if (!this._oneway) {
            this._backwardCost = revCost;
            this._tail.addInLink(this);
            this._head.addOutLink(this);
        }
        this._geometry = geom;
    }

    public void setLineString(List<ILonLat> geom) {
        this._geometry = geom;
    }

    public List<ILonLat> getLineString() {
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

    public double getLength() {
        return this._length;
    }

    public double getForwardCost() {
        return this._forwardCost;
    }

    public void setForwardCost(double forwardCost) {
        this._forwardCost = forwardCost;
    }

    public double getBackwardCost() {
        return this._backwardCost;
    }

    public void setBackwardCost(double backwardCost) {
        this._backwardCost = backwardCost;
    }

    public boolean isOneWay() {
        return this._oneway;
    }

    public void setOneWay(boolean oneway) {
        this._oneway = oneway;
    }

    public int getLinkType() {
        return -1;
    }

    public String toString() {
        return String.format("%s::(%s-(%f/%f)-%s)", this._linkid, this._tail, this._forwardCost, this._backwardCost, this._head);
    }

    public Link clone(Node tail, Node head) {
        return new Link(this._linkid, tail, head, this._length, this._forwardCost, this._backwardCost, this._oneway, this._geometry);
    }
}

