/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.mapmatching;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.IRoutingLogic;
import jp.ac.ut.csis.pflow.routing3.mapmatching.MapMatching;
import jp.ac.ut.csis.pflow.routing3.mapmatching.MatchingResult;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.Route;

public class SparseMapMatching
extends MapMatching {
    private IRoutingLogic _routingLogic;

    public SparseMapMatching() {
        this(new Dijkstra());
    }

    public SparseMapMatching(IRoutingLogic routingLogic) {
        this._routingLogic = routingLogic;
    }

    public IRoutingLogic getRoutingLogic() {
        return this._routingLogic;
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points) {
        return this.runSparseMapMatching(network, points, 1000.0);
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points, double range) {
        if (points.size() < 2) {
            System.err.println("[error] insufficient input points. requires 2 or more");
            return null;
        }
        Network midtermNetwork = new Network(false, false);
        MatchingResult resultPrev = null;
        MatchingResult orgInfo = null;
        MatchingResult dstInfo = null;
        for (LonLat point : points) {
            MatchingResult resultNext = this.runMatchingToLink(network, point, range);
            if (!resultNext.isValid()) continue;
            if (resultPrev != null && resultPrev.isValid()) {
                Route route;
                Link linkNext;
                Link linkPrev = resultPrev.getNearestLink();
                if (!linkPrev.equals(linkNext = resultNext.getNearestLink()) && (route = this.queryRoute(network, resultPrev, resultNext)) != null && !route.isEmpty()) {
                    for (Link link : route.listLinks()) {
                        this.buildNetwork(midtermNetwork, link);
                    }
                }
                this.buildNetwork(midtermNetwork, linkPrev);
                this.buildNetwork(midtermNetwork, linkNext);
            }
            resultPrev = resultNext;
            if (orgInfo == null) {
                orgInfo = resultNext;
            }
            dstInfo = resultNext;
        }
        if (orgInfo != null && dstInfo != null) {
            Route tempRoute = this.queryRoute(midtermNetwork, orgInfo, dstInfo);
            return tempRoute == null || tempRoute.isEmpty() ? null : this.restoreRoute(network, tempRoute);
        }
        return null;
    }

    private Route restoreRoute(Network originalNetwork, Route route) {
        List<Node> nodes = route.listNodes();
        ArrayList<Node> orgNodes = new ArrayList<Node>(nodes.size());
        for (Node node : nodes) {
            Node orgNode = originalNetwork.getNode(node.getNodeID());
            orgNodes.add(orgNode);
        }
        return new Route(orgNodes, route.getCost());
    }

    private void buildNetwork(Network matchingNetwork, Link link) {
        if (matchingNetwork.getLink(link.getLinkID()) == null) {
            Node tailNode = link.getTailNode();
            tailNode = matchingNetwork.hasNode(tailNode.getNodeID()) ? matchingNetwork.getNode(tailNode.getNodeID()) : tailNode.clone();
            Node headNode = link.getHeadNode();
            headNode = matchingNetwork.hasNode(headNode.getNodeID()) ? matchingNetwork.getNode(headNode.getNodeID()) : headNode.clone();
            matchingNetwork.addLink(link.clone(tailNode, headNode));
        }
    }

    private Route queryRoute(Network network, MatchingResult orgInfo, MatchingResult dstInfo) {
        Link orgLink = orgInfo.getNearestLink();
        Node orgNode = orgInfo.getRatio() < 0.5 ? network.getNode(orgLink.getTailNode().getNodeID()) : network.getNode(orgLink.getHeadNode().getNodeID());
        Link dstLink = dstInfo.getNearestLink();
        Node dstNode = dstInfo.getRatio() < 0.5 ? network.getNode(dstLink.getTailNode().getNodeID()) : network.getNode(dstLink.getHeadNode().getNodeID());
        return this._routingLogic.getRoute(network, orgNode, dstNode);
    }
}

