/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import jp.ac.ut.csis.pflow.routing2.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class Penalty
extends ARoutingLogic {
    public static final double INCREASE_RATIO = 0.1;
    private double _ratio;

    public Penalty() {
        this(0.1);
    }

    public Penalty(double ratio) {
        this(null, ratio);
    }

    public Penalty(LinkCost linkcost) {
        this(ROUTE_NUM, linkcost, 0.1);
    }

    public Penalty(LinkCost linkcost, double ratio) {
        this(ROUTE_NUM, linkcost, ratio);
    }

    public Penalty(int routeNum, LinkCost linkcost, double ratio) {
        this(routeNum, MIN_DIST, linkcost, ratio);
    }

    public Penalty(int routeNum, double minDist, LinkCost linkcost, double ratio) {
        super(routeNum, minDist, linkcost);
        this._ratio = ratio;
    }

    @Override
    public String getName() {
        return "Penalty";
    }

    public double getRatio() {
        return this._ratio;
    }

    public void setRatio(double ratio) {
        this._ratio = ratio;
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode, int n) {
        ArrayList<Route> routes = new ArrayList<Route>();
        HashMap<String, double[]> costs = new HashMap<String, double[]>();
        LinkCost operator = this.getLinkCost();
        int i = 0;
        while (i < n) {
            Route result = this.getRoute(network, depnode, arrnode, costs);
            if (result != null) {
                routes.add(result);
                Node prevNode = depnode;
                Link prevLink = null;
                for (Link link : result.listLinks()) {
                    String lid = link.getLinkID();
                    boolean rev = prevNode.equals(link.getHeadNode());
                    Node node = rev ? link.getHeadNode() : link.getTailNode();
                    double[] cst = (double[])costs.get(lid);
                    if (cst == null) {
                        cst = new double[]{operator.getCost(prevLink, node, link), operator.getReverseCost(prevLink, node, link)};
                        costs.put(lid, cst);
                    }
                    cst[rev ? 1 : 0] = cst[rev ? 1 : 0] * (1.0 + this.getRatio());
                    prevNode = node;
                    prevLink = link;
                }
            }
            ++i;
        }
        return routes;
    }

    private Route getRoute(Network network, Node depnode, Node arrnode, Map<String, double[]> costs) {
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
                String lid = link.getLinkID();
                boolean rev = knot.getNode().equals(link.getHeadNode());
                Node n = rev ? link.getTailNode() : link.getHeadNode();
                Link plink = this.getPreviousLink(knots, knot.getNode());
                double cost = rev ? this.getLinkCost().getReverseCost(plink, knot.getNode(), link) : this.getLinkCost().getCost(plink, knot.getNode(), link);
                double cst = costs.containsKey(lid) ? costs.get(lid)[rev ? 1 : 0] : cost;
                if (knots.containsKey(n)) {
                    ((ARoutingLogic.Knot)knots.get(n)).update(knot, cst);
                    continue;
                }
                ARoutingLogic.Knot k = new Knot(n, knot, cst);
                knots.put(n, k);
                queue.add(k);
            }
        }
        return !knot.getNode().equals(arrnode) ? null : knot.getRoute();
    }
}

