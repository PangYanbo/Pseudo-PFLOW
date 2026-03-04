/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import jp.ac.ut.csis.pflow.routing4.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.ILinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class Dijkstra
extends ARoutingLogic {
    public Dijkstra(double minDist, ILinkCost linkcost) {
        super(1, minDist, linkcost);
    }

    public Dijkstra(ILinkCost linkcost) {
        this(3000.0, linkcost);
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
        if (network == null || depnode == null || arrnode == null) {
            return null;
        }
        HashMap<Node, ARoutingLogic.Knot> knots = new HashMap<Node, ARoutingLogic.Knot>();
        PriorityQueue<ARoutingLogic.Knot> queue = new PriorityQueue<ARoutingLogic.Knot>();
        ARoutingLogic.Knot crntKnot = new ARoutingLogic.Knot(depnode);
        knots.put(depnode, crntKnot);
        queue.add(crntKnot);
        while (!queue.isEmpty()) {
            crntKnot = (ARoutingLogic.Knot)queue.poll();
            crntKnot.fix(true);
            Node crntNode = crntKnot.getNode();
            if (crntNode.equals(arrnode)) break;
            for (Link nextLink : crntNode.listOutLinks()) {
                Node nextNode;
                Node node = nextNode = crntNode.equals(nextLink.getHeadNode()) ? nextLink.getTailNode() : nextLink.getHeadNode();
                if (knots.containsKey(nextNode) && ((ARoutingLogic.Knot)knots.get(nextNode)).isFixed()) continue;
                Link prevLink = this.getPreviousLink(network, crntKnot);
                double totalCostToNextNode = crntKnot.getCost() + this.getLinkCost().getCost(prevLink, crntNode, nextLink);
                ARoutingLogic.Knot nextKnot = null;
                if (knots.containsKey(nextNode)) {
                    nextKnot = (ARoutingLogic.Knot)knots.get(nextNode);
                    queue.remove(nextKnot);
                } else {
                    nextKnot = new ARoutingLogic.Knot(nextNode, crntKnot, 2.147483647E9);
                    knots.put(nextNode, nextKnot);
                }
                if (totalCostToNextNode < nextKnot.getCost()) {
                    nextKnot.setValues(crntKnot, totalCostToNextNode);
                }
                queue.add(nextKnot);
            }
        }
        return !crntKnot.getNode().equals(arrnode) ? new ArrayList<Route>() : Arrays.asList(crntKnot.getRoute());
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

    public Map<Node, ARoutingLogic.Knot> getCost(Network network, Node depnode) {
        return this.getCost(network, depnode, -1.0);
    }

    public Map<Node, ARoutingLogic.Knot> getCost(Network network, Node depnode, double cost) {
        if (network == null || depnode == null) {
            return null;
        }
        HashMap<Node, ARoutingLogic.Knot> knots = new HashMap<Node, ARoutingLogic.Knot>();
        PriorityQueue<ARoutingLogic.Knot> queue = new PriorityQueue<ARoutingLogic.Knot>();
        ARoutingLogic.Knot crntKnot = new ARoutingLogic.Knot(depnode);
        knots.put(depnode, crntKnot);
        queue.add(crntKnot);
        while (!queue.isEmpty()) {
            crntKnot = (ARoutingLogic.Knot)queue.poll();
            crntKnot.fix(true);
            if (0.0 < cost && cost < crntKnot.getCost()) break;
            Node crntNode = crntKnot.getNode();
            for (Link nextLink : crntNode.listOutLinks()) {
                Node nextNode;
                Node node = nextNode = crntNode.equals(nextLink.getHeadNode()) ? nextLink.getTailNode() : nextLink.getHeadNode();
                if (knots.containsKey(nextNode) && ((ARoutingLogic.Knot)knots.get(nextNode)).isFixed()) continue;
                Link prevLink = this.getPreviousLink(network, crntKnot);
                double totalCostToNextNode = crntKnot.getCost() + this.getLinkCost().getCost(prevLink, crntNode, nextLink);
                ARoutingLogic.Knot nextKnot = null;
                if (knots.containsKey(nextNode)) {
                    nextKnot = (ARoutingLogic.Knot)knots.get(nextNode);
                    queue.remove(nextKnot);
                } else {
                    nextKnot = new ARoutingLogic.Knot(nextNode, crntKnot, 2.147483647E9);
                    knots.put(nextNode, nextKnot);
                }
                if (totalCostToNextNode < nextKnot.getCost()) {
                    nextKnot.setValues(crntKnot, totalCostToNextNode);
                }
                queue.add(nextKnot);
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

