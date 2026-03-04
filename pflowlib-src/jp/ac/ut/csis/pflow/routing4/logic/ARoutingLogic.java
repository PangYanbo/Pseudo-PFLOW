/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.routing4.logic.IRoutingLogic;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.ILinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public abstract class ARoutingLogic
implements IRoutingLogic {
    public static final int ROUTE_NUM = 5;
    public static final double MIN_DIST = 3000.0;
    private static final double APPROX_1KM = 1.2E-5;
    private int _routeNum;
    private double _minDist;
    private ILinkCost _linkcost;

    protected ARoutingLogic(int routeNum, double minDist, ILinkCost linkcost) {
        this._routeNum = routeNum;
        this._minDist = minDist;
        this._linkcost = linkcost == null ? new LinkCost() : linkcost;
    }

    protected ARoutingLogic(int routeNum, double minDist) {
        this(routeNum, minDist, null);
    }

    protected ARoutingLogic(int routeNum) {
        this(routeNum, 3000.0);
    }

    protected ARoutingLogic() {
        this(5);
    }

    @Override
    public List<Route> getRoutes(Network network, String depnodeid, String arrnodeid, int n) {
        Node dep = network.getNode(depnodeid);
        Node arr = network.getNode(arrnodeid);
        return this.getRoutes(network, dep, arr, n);
    }

    @Override
    public List<Route> getRoutes(Network network, String depnodeid, String arrnodeid) {
        return this.getRoutes(network, depnodeid, arrnodeid, this.getRouteNum());
    }

    @Override
    public Route getRoute(Network network, String depnodeid, String arrnodeid) {
        List<Route> routes = this.getRoutes(network, depnodeid, arrnodeid, 1);
        return routes == null || routes.isEmpty() ? null : routes.get(0);
    }

    @Override
    public List<Route> getRoutes(Network network, double depx, double depy, double arrx, double arry, int n) {
        Node n0 = this.getNearestNode(network, depx, depy);
        Node n1 = this.getNearestNode(network, arrx, arry);
        return n0 == null || n1 == null ? new ArrayList<Route>() : this.getRoutes(network, n0, n1, n);
    }

    @Override
    public List<Route> getRoutes(Network network, double depx, double depy, double arrx, double arry) {
        return this.getRoutes(network, depx, depy, arrx, arry, this.getRouteNum());
    }

    @Override
    public Route getRoute(Network network, double depx, double depy, double arrx, double arry) {
        List<Route> routes = this.getRoutes(network, depx, depy, arrx, arry, 1);
        return routes == null || routes.isEmpty() ? null : routes.get(0);
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode) {
        return this.getRoutes(network, depnode, arrnode, this.getRouteNum());
    }

    @Override
    public Route getRoute(Network network, Node depnode, Node arrnode) {
        List<Route> routes = this.getRoutes(network, depnode, arrnode, 1);
        return routes == null || routes.isEmpty() ? null : routes.get(0);
    }

    @Override
    public Node getNearestNode(Network network, double x, double y, double mindist) {
        Node node = null;
        double dist = mindist;
        double w = mindist * 1.2E-5;
        double h = mindist * 1.2E-5;
        for (Node n : network.queryNode(x - w, y - h, x + w, y + h)) {
            double d = DistanceUtils.distance(x, y, n.getLon(), n.getLat());
            if (!(d < dist)) continue;
            node = n;
            dist = d;
        }
        return node;
    }

    @Override
    public Node getNearestNode(Network network, double x, double y) {
        return this.getNearestNode(network, x, y, this.getSearchDistance());
    }

    @Override
    public Link getNearestLink(Network network, double x, double y, double mindist) {
        Link link = null;
        LonLat point = new LonLat(x, y);
        double dist = mindist;
        for (Link L : network.listLinks()) {
            double d;
            List<ILonLat> geom = L.getLineString();
            double d2 = d = geom == null ? DistanceUtils.distance(L.getTailNode(), L.getHeadNode(), point) : DistanceUtils.distance(geom, point);
            if (!(d < dist)) continue;
            link = L;
            dist = d;
        }
        return link;
    }

    @Override
    public Link getNearestLink(Network network, double x, double y) {
        return this.getNearestLink(network, x, y, Double.MAX_VALUE);
    }

    public int getRouteNum() {
        return this._routeNum;
    }

    public void setRouteNum(int routeNum) {
        this._routeNum = routeNum;
    }

    public double getSearchDistance() {
        return this._minDist;
    }

    public void setSearchDistance(double minDist) {
        this._minDist = minDist;
    }

    public ILinkCost getLinkCost() {
        return this._linkcost;
    }

    public void setLinkCost(LinkCost linkcost) {
        this._linkcost = linkcost;
    }

    protected Link getPreviousLink(Network network, Knot crntKnot) {
        if (!crntKnot.hasFrom()) {
            return null;
        }
        Node crntNode = crntKnot.getNode();
        Node prevNode = crntKnot.getFrom().getNode();
        return network.getLink(prevNode, crntNode);
    }

    public class Knot
    implements Comparable<Knot> {
        private Node __node;
        private Knot __from;
        private double __cost;
        private boolean __fixed;

        protected Knot(Node node) {
            this(node, null, 0.0);
        }

        protected Knot(Node node, Knot from, double cost) {
            this.__node = node;
            this.__from = from;
            this.__cost = cost;
            this.__fixed = false;
        }

        protected boolean isFixed() {
            return this.__fixed;
        }

        protected boolean hasFrom() {
            return this.__from != null;
        }

        protected void setValues(Knot from, double cost) {
            this.__from = from;
            this.__cost = cost;
        }

        protected void fix(boolean flag) {
            this.__fixed = flag;
        }

        public Route getRoute() {
            ArrayList<Node> list = new ArrayList<Node>();
            list.add(this.getNode());
            Knot knot = this.__from;
            while (knot != null) {
                list.add(0, knot.getNode());
                knot = knot.getFrom();
            }
            return new Route(list, this.getCost());
        }

        protected Knot getFrom() {
            return this.__from;
        }

        public double getCost() {
            return this.__cost;
        }

        protected Node getNode() {
            return this.__node;
        }

        @Override
        public int compareTo(Knot knot) {
            double cost1;
            double cost0 = this.getCost();
            if (cost0 < (cost1 = knot.getCost())) {
                return -1;
            }
            if (cost0 == cost1) {
                return 0;
            }
            return 1;
        }
    }
}

