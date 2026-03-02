/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class Dial
extends ARoutingLogic {
    public static final Double MARGIN_RADIUS = 1000.0;
    public static final Integer REPEAT_MAX_COUNT = 30;
    public static final Double THETA = 0.001;
    private double _theta;
    private DialWeight _weight;

    public Dial() {
        this(ROUTE_NUM);
    }

    public Dial(int n) {
        this(n, THETA);
    }

    public Dial(int n, double theta) {
        this(n, theta, MIN_DIST, null);
    }

    public Dial(int n, LinkCost linkcost) {
        this(n, THETA, linkcost);
    }

    public Dial(int n, double theta, LinkCost linkcost) {
        this(n, theta, MIN_DIST, linkcost);
    }

    public Dial(int n, double theta, double minDist, LinkCost linkcost) {
        this(n, theta, minDist, linkcost, null);
    }

    public Dial(int n, double theta, double minDist, LinkCost linkcost, DialWeight weight) {
        super(n, minDist, linkcost);
        this._theta = theta;
        this._weight = weight != null ? weight : new DialWeight();
    }

    @Override
    public String getName() {
        return "Dial";
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode, int n) {
        Map<Node, ARoutingLogic.Knot> cost = this.calcRevCost(network, depnode, arrnode);
        Network net = this.calcLikelihood(cost);
        net = this.calcProbability(net, cost, depnode, arrnode);
        int N = n * 5;
        if (N > REPEAT_MAX_COUNT) {
            N = REPEAT_MAX_COUNT;
        }
        ArrayList<Route> routes = new ArrayList<Route>();
        int i = 0;
        while (i < N) {
            Route route = this.retrieveRoute(network, net, depnode, arrnode);
            if (route != null) {
                routes.add(route);
                if (routes.size() >= n) break;
            }
            ++i;
        }
        return routes;
    }

    public double getTheta() {
        return this._theta;
    }

    public void setTheta(double theta) {
        this._theta = theta;
    }

    public DialWeight getWeightOperator() {
        return this._weight;
    }

    public void setWeightOperator(DialWeight weight) {
        this._weight = weight;
    }

    private Route retrieveRoute(Network srcNetwork, Network probNetwork, Node depnode, Node arrnode) {
        Node n0 = probNetwork.getNode(depnode.getNodeID());
        Node n1 = probNetwork.getNode(arrnode.getNodeID());
        if (n0 == null || n1 == null) {
            return null;
        }
        Route route = new Route();
        route.add(depnode, 0.0);
        Node prev = depnode;
        Node node = n0;
        Random rand = new Random();
        while (!node.equals(n1)) {
            double r = rand.nextDouble();
            double cst = -1.0;
            boolean rev = false;
            Link link = null;
            for (Link ln : node.listOutLinks()) {
                rev = node.equals(ln.getHeadNode());
                double d = cst = rev ? ln.getReverseCost() : ln.getCost();
                if (!(cst > 0.0)) continue;
                link = ln;
                if (cst >= r) break;
                r -= link.getCost();
            }
            if (link == null) {
                return null;
            }
            node = rev ? link.getTailNode() : link.getHeadNode();
            Node next = srcNetwork.getNode(node.getNodeID());
            Link ln = srcNetwork.getLink(prev, next);
            double lncst = prev.equals(ln.getTailNode()) ? this.getLinkCost().getCost(ln) : this.getLinkCost().getReverseCost(ln);
            route.add(next, lncst);
            prev = next;
        }
        return route;
    }

    /*
     * WARNING - void declaration
     */
    private Network calcProbability(Network network, Map<Node, ARoutingLogic.Knot> costs, Node depnode, Node arrnode) {
        void var7_10;
        ArrayList<Map.Entry<Node, ARoutingLogic.Knot>> entries = new ArrayList<Map.Entry<Node, ARoutingLogic.Knot>>(costs.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<Node, ARoutingLogic.Knot>>(){

            @Override
            public int compare(Map.Entry<Node, ARoutingLogic.Knot> obj1, Map.Entry<Node, ARoutingLogic.Knot> obj2) {
                Double c1 = obj1.getValue().getCost();
                Double c2 = obj2.getValue().getCost();
                return c1.compareTo(c2);
            }
        });
        Map<Node, Double> weights = this._weight.createInitialWeights(network, network.getNode(depnode.getNodeID()), network.getNode(arrnode.getNodeID()));
        for (Map.Entry entry : entries) {
            Node node = network.getNode(((Node)entry.getKey()).getNodeID());
            if (node == null) continue;
            double weight = weights.containsKey(node) ? weights.get(node) : 0.0;
            for (Link link : node.listInLinks()) {
                boolean rev = node.equals(link.getTailNode());
                Node target = rev ? link.getHeadNode() : link.getTailNode();
                double likelihood = rev ? link.getReverseCost() : link.getCost();
                double w = likelihood * weight;
                if (likelihood > 0.0) {
                    if (rev) {
                        link.setReverseCost(w);
                    } else {
                        link.setCost(w);
                    }
                }
                weights.put(target, weights.containsKey(target) ? weights.get(target) : 0.0 + w);
            }
        }
        int n = entries.size() - 1;
        while (var7_10 >= 0) {
            Node node = network.getNode(((Node)((Map.Entry)entries.get((int)var7_10)).getKey()).getNodeID());
            if (node != null) {
                for (Link link : node.listOutLinks()) {
                    double w;
                    double d = w = weights.containsKey(node) ? weights.get(node) : 0.0;
                    if (node.equals(link.getTailNode())) {
                        link.setCost(w > 0.0 ? link.getCost() / w : 0.0);
                        continue;
                    }
                    link.setReverseCost(w > 0.0 ? link.getReverseCost() / w : 0.0);
                }
            }
            --var7_10;
        }
        return network;
    }

    private Network calcLikelihood(Map<Node, ARoutingLogic.Knot> costs) {
        Network net = new Network();
        LinkCost operator = this.getLinkCost();
        for (Node node : costs.keySet()) {
            for (Link ln : node.listAllLinks()) {
                Node head = ln.getHeadNode();
                Node tail = ln.getTailNode();
                double fcost = 0.0;
                double rcost = 0.0;
                if (costs.containsKey(head) && costs.containsKey(tail)) {
                    double cstL;
                    boolean rev = node.equals(tail);
                    double cstH = rev ? costs.get(head).getCost() : costs.get(tail).getCost();
                    double d = cstL = rev ? costs.get(tail).getCost() : costs.get(head).getCost();
                    if (rev) {
                        rcost = cstH > cstL ? Math.exp(this._theta * (cstH - cstL - operator.getReverseCost(ln))) : 0.0;
                    } else {
                        double d2 = fcost = cstH > cstL ? Math.exp(this._theta * (cstH - cstL - operator.getCost(ln))) : 0.0;
                    }
                }
                if (net.getLink(ln.getLinkID()) != null) continue;
                String headID = head.getNodeID();
                String tailID = tail.getNodeID();
                Node newHead = net.hasNode(headID) ? net.getNode(headID) : new Node(headID, head.getLon(), head.getLat());
                Node newTail = net.hasNode(tailID) ? net.getNode(tailID) : new Node(tailID, tail.getLon(), tail.getLat());
                net.addLink(new Link(ln.getLinkID(), newTail, newHead, fcost, rcost, ln.isOneWay(), ln.getLineString()));
            }
        }
        return net;
    }

    private Map<Node, ARoutingLogic.Knot> calcRevCost(Network network, Node depnode, Node arrnode) {
        LonLat cn = new LonLat((depnode.getLon() + arrnode.getLon()) / 2.0, (depnode.getLat() + arrnode.getLat()) / 2.0);
        double bounds = DistanceUtils.distance(depnode, arrnode) * 0.5 + MARGIN_RADIUS;
        PriorityQueue<ARoutingLogic.Knot> queue = new PriorityQueue<ARoutingLogic.Knot>(network.listNodes().size(), new Comparator<ARoutingLogic.Knot>(){

            @Override
            public int compare(ARoutingLogic.Knot knot1, ARoutingLogic.Knot knot2) {
                return new Double(knot1.getCost()).compareTo(new Double(knot2.getCost()));
            }
        });
        ARoutingLogic.Knot knot = new ARoutingLogic.Knot(this, arrnode);
        LinkedHashMap<Node, ARoutingLogic.Knot> knots = new LinkedHashMap<Node, ARoutingLogic.Knot>();
        knots.put(arrnode, knot);
        queue.add(knot);
        while (!queue.isEmpty()) {
            knot = queue.poll();
            knot.fix(true);
            for (Link link : knot.getNode().listInLinks()) {
                double cst;
                boolean rev = knot.getNode().equals(link.getTailNode());
                Node n = rev ? link.getHeadNode() : link.getTailNode();
                double d = cst = rev ? this.getLinkCost().getReverseCost(link) : this.getLinkCost().getCost(link);
                if (DistanceUtils.distance(cn, n) > bounds) continue;
                if (knots.containsKey(n)) {
                    ((ARoutingLogic.Knot)knots.get(n)).update(knot, cst);
                    continue;
                }
                ARoutingLogic.Knot k = new ARoutingLogic.Knot(this, n, knot, cst);
                knots.put(n, k);
                queue.add(k);
            }
        }
        ArrayList keys = new ArrayList(knots.keySet());
        int i = keys.size() - 1;
        while (i >= 0) {
            Node key = (Node)keys.get(i);
            if (!((ARoutingLogic.Knot)knots.get(key)).isFixed()) {
                knots.remove(key);
            }
            --i;
        }
        return knots;
    }

    public static class DialWeight {
        protected Map<Node, Double> createInitialWeights(Network network, Node depnode, Node arrnode) {
            HashMap<Node, Double> weights = new HashMap<Node, Double>();
            weights.put(arrnode, 1.0);
            return weights;
        }
    }
}

