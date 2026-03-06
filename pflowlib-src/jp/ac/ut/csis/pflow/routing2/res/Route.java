/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class Route
implements Cloneable {
    private List<Node> _route;
    private double _cost;

    public Route() {
        this._route = new ArrayList<Node>();
        this._cost = 0.0;
    }

    public Route(List<Node> points, double cost) {
        this._route = points;
        this._cost = cost;
    }

    public void add(Node node, double cost) {
        this._route.add(node);
        this._cost += cost;
    }

    public double getCost() {
        return this._cost;
    }

    public int numNodes() {
        return this._route.size();
    }

    public List<Link> listLinks() {
        ArrayList<Link> list = new ArrayList<Link>();
        Node n0 = this._route.get(0);
        int len = this._route.size();
        int i = 1;
        while (i < len) {
            Node n1 = this._route.get(i);
            for (Link L : n0.listAllLinks()) {
                if (!L.getTailNode().equals(n1) && !L.getHeadNode().equals(n1)) continue;
                list.add(L);
                break;
            }
            n0 = n1;
            ++i;
        }
        return list;
    }

    public List<Node> listNodes() {
        return this._route;
    }

    public Node getNode(int idx) {
        return this._route.get(idx);
    }

    public boolean contains(Node node) {
        return this._route.contains(node);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            return this._route.equals(((Route)Route.class.cast(obj)).listNodes());
        }
        return super.equals(obj);
    }
}

