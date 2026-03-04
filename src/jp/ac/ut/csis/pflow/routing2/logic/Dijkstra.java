/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import jp.ac.ut.csis.pflow.routing2.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class Dijkstra
extends ARoutingLogic {
    public Dijkstra(double minDist, LinkCost linkcost) {
        super(1, minDist, linkcost);
    }

    public Dijkstra(LinkCost linkcost) {
        this(MIN_DIST, linkcost);
    }

    public Dijkstra() {
        this(null);
    }

    @Override
    public String getName() {
        return "Dijkstra";
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode, int N) {
        PriorityQueue<ARoutingLogic.Knot> queue = new PriorityQueue<ARoutingLogic.Knot>(network.listNodes().size(), new Comparator<ARoutingLogic.Knot>(){

            @Override
            public int compare(ARoutingLogic.Knot knot1, ARoutingLogic.Knot knot2) {
                return new Double(knot1.getCost()).compareTo(new Double(knot2.getCost()));
            }
        });
        ARoutingLogic.Knot knot = new Knot(depnode);
        HashMap<Node, ARoutingLogic.Knot> knots = new HashMap<Node, ARoutingLogic.Knot>();
        knots.put(depnode, knot);
        queue.add(knot);
        while (!queue.isEmpty()) {
            knot = queue.poll();
            knot.fix(true);
            if (knot.getNode().equals(arrnode)) break;
            for (Link link : knot.getNode().listOutLinks()) {
                double cost;
                boolean rev = knot.getNode().equals(link.getHeadNode());
                Node n = rev ? link.getTailNode() : link.getHeadNode();
                Link plink = this.getPreviousLink(knots, knot.getNode());
                double d = cost = rev ? this.getLinkCost().getReverseCost(plink, knot.getNode(), link) : this.getLinkCost().getCost(plink, knot.getNode(), link);
                if (knots.containsKey(n)) {
                    ((ARoutingLogic.Knot)knots.get(n)).update(knot, cost);
                    continue;
                }
                ARoutingLogic.Knot k = new Knot(n, knot, cost);
                knots.put(n, k);
                queue.add(k);
            }
        }
        return !knot.getNode().equals(arrnode) ? new ArrayList<Route>() : Arrays.asList(knot.getRoute());
    }

    public List<Route> getReachableRoutes(Network network, double lon, double lat, double cost) {
        Node org = this.getNearestNode(network, lon, lat);
        return org == null ? new ArrayList<Route>() : this.getReachableRoutes(network, org, cost);
    }

    public List<Route> getReachableRoutes(Network network, Node depnode, double cost) {
        ArrayList<Route> res = new ArrayList<Route>();
        Map<Node, ARoutingLogic.Knot> knots = this.getCost(network, depnode, cost);
        ArrayList<Node> nodes = new ArrayList<Node>(knots.keySet());
        while (!nodes.isEmpty()) {
            Node node = (Node)nodes.get(nodes.size() - 1);
            ARoutingLogic.Knot knot = knots.get(node);
            Route route = knot.getRoute();
            for (Node n : route.listNodes()) {
                nodes.remove(n);
            }
            res.add(route);
        }
        return res;
    }

    protected Map<Node, ARoutingLogic.Knot> getCost(Network network, Node depnode) {
        return this.getCost(network, depnode, -1.0);
    }

    protected Map<Node, ARoutingLogic.Knot> getCost(Network network, Node depnode, double cost) {
        PriorityQueue<ARoutingLogic.Knot> queue = new PriorityQueue<ARoutingLogic.Knot>(network.listNodes().size(), new Comparator<ARoutingLogic.Knot>(){

            @Override
            public int compare(ARoutingLogic.Knot knot1, ARoutingLogic.Knot knot2) {
                return new Double(knot1.getCost()).compareTo(new Double(knot2.getCost()));
            }
        });
        ARoutingLogic.Knot knot = new Knot(depnode);
        LinkedHashMap<Node, ARoutingLogic.Knot> knots = new LinkedHashMap<Node, ARoutingLogic.Knot>();
        knots.put(depnode, knot);
        queue.add(knot);
        while (!queue.isEmpty()) {
            knot = queue.poll();
            knot.fix(true);
            if (0.0 < cost && cost < knot.getCost()) break;
            for (Link link : knot.getNode().listOutLinks()) {
                double cst;
                boolean rev = knot.getNode().equals(link.getHeadNode());
                Node n = rev ? link.getTailNode() : link.getHeadNode();
                Link plink = this.getPreviousLink(knots, knot.getNode());
                double d = cst = rev ? this.getLinkCost().getReverseCost(plink, knot.getNode(), link) : this.getLinkCost().getCost(plink, knot.getNode(), link);
                if (knots.containsKey(n)) {
                    ((ARoutingLogic.Knot)knots.get(n)).update(knot, cst);
                    continue;
                }
                ARoutingLogic.Knot k = new Knot(n, knot, cst);
                knots.put(n, k);
                queue.add(k);
            }
        }
        ArrayList keys = new ArrayList(knots.keySet());
        int i = keys.size() - 1;
        while (i >= 0) {
            Node key = (Node)keys.get(i);
            ARoutingLogic.Knot val = (ARoutingLogic.Knot)knots.get(key);
            if (!val.isFixed() || 0.0 < cost && cost < val.getCost()) {
                knots.remove(key);
            }
            --i;
        }
        return knots;
    }
}

