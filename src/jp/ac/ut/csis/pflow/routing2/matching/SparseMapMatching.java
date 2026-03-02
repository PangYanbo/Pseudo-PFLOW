/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.matching;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.logic.IRoutingLogic;
import jp.ac.ut.csis.pflow.routing2.matching.MapMatching;
import jp.ac.ut.csis.pflow.routing2.matching.MatchingResult;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class SparseMapMatching
extends MapMatching {
    private IRoutingLogic _routingLogic;
    private Network _midtermNetwork;

    public SparseMapMatching() {
        this(new Dijkstra());
    }

    public SparseMapMatching(IRoutingLogic routingLogic) {
        this._routingLogic = routingLogic;
        this._midtermNetwork = null;
    }

    public Network getMidtermNetwork() {
        return this._midtermNetwork;
    }

    public IRoutingLogic getRoutingLogic() {
        return this._routingLogic;
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points, double range) {
        MatchingResult resultPrev;
        if (points.size() < 2) {
            return null;
        }
        this._midtermNetwork = new Network(false, false);
        int numPoints = points.size();
        MatchingResult orgInfo = resultPrev = this.runMatching(network, (LonLat)points.get(0), range);
        MatchingResult dstInfo = null;
        int i = 1;
        while (i < numPoints) {
            LonLat point = (LonLat)points.get(i);
            MatchingResult resultNext = this.runMatching(network, point, range);
            if (resultNext.isValid()) {
                if (resultPrev.isValid()) {
                    Route route;
                    Link linkNext;
                    Link linkPrev = resultPrev.getNearestLink();
                    if (!linkPrev.equals(linkNext = resultNext.getNearestLink()) && (route = this.queryRoute(network, resultPrev, resultNext)) != null) {
                        for (Link link : route.listLinks()) {
                            this.buildNetwork(this._midtermNetwork, link);
                        }
                    }
                    this.buildNetwork(this._midtermNetwork, linkNext);
                }
                dstInfo = resultNext;
                resultPrev = resultNext;
            }
            if (!orgInfo.isValid()) {
                orgInfo = resultNext;
            }
            ++i;
        }
        if (orgInfo.isValid()) {
            this.buildNetwork(this._midtermNetwork, orgInfo.getNearestLink());
        }
        if (orgInfo != null && dstInfo != null && orgInfo.isValid() && dstInfo.isValid()) {
            Route tempRoute = this.queryRoute(this._midtermNetwork, orgInfo, dstInfo);
            return this.restoreRoute(network, tempRoute);
        }
        return null;
    }

    private Route restoreRoute(Network originalNetwork, Route route) {
        if (route == null) {
            return null;
        }
        List<Node> nodes = route.listNodes();
        ArrayList<Node> orgNodes = new ArrayList<Node>(nodes.size());
        for (Node node : nodes) {
            Node orgNode = originalNetwork.getNode(node.getNodeID());
            orgNodes.add(orgNode);
        }
        return new Route(orgNodes, route.getCost());
    }

    private void buildNetwork(Network matchingNetwork, Link link) {
        Node prevTailNode = link.getTailNode();
        Node tailNode = matchingNetwork.hasNode(prevTailNode.getNodeID()) ? matchingNetwork.getNode(prevTailNode.getNodeID()) : prevTailNode.clone();
        Node prevHeadNode = link.getHeadNode();
        Node headNode = matchingNetwork.hasNode(prevHeadNode.getNodeID()) ? matchingNetwork.getNode(prevHeadNode.getNodeID()) : prevHeadNode.clone();
        matchingNetwork.addLink(link.clone(tailNode, headNode));
    }

    private Route queryRoute(Network network, MatchingResult orgInfo, MatchingResult dstInfo) {
        Link orgLink = orgInfo.getNearestLink();
        LonLat orgPoint = orgInfo.getNearestPoint();
        Node orgNode = TrajectoryUtils.getLocatePointRatio(orgLink.getLineString(), orgPoint) < 0.5 ? network.getNode(orgLink.getTailNode().getNodeID()) : network.getNode(orgLink.getHeadNode().getNodeID());
        Link dstLink = dstInfo.getNearestLink();
        LonLat dstPoint = dstInfo.getNearestPoint();
        Node dstNode = TrajectoryUtils.getLocatePointRatio(dstLink.getLineString(), dstPoint) < 0.5 ? network.getNode(dstLink.getTailNode().getNodeID()) : network.getNode(dstLink.getHeadNode().getNodeID());
        return this._routingLogic.getRoute(network, orgNode, dstNode);
    }
}

