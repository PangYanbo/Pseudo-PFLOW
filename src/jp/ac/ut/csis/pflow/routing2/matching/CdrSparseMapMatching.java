/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.matching;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing2.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.matching.MapMatching;
import jp.ac.ut.csis.pflow.routing2.matching.MatchingResult;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class CdrSparseMapMatching
extends MapMatching {
    private ARoutingLogic _routingLogic;
    private Network _matchingNetwork;

    public CdrSparseMapMatching() {
        this(new Dijkstra());
    }

    public CdrSparseMapMatching(ARoutingLogic routingLogic) {
        this._routingLogic = routingLogic;
        this._matchingNetwork = null;
    }

    private <T extends LonLat> MatchingResult runMatching(Network network, T point, double range, Network matchingNetwork) {
        List<Link> links = network.queryLink(point.getLon(), point.getLat(), range);
        Link nearestLink = null;
        LonLat nearestPoint = null;
        double distance = Double.MAX_VALUE;
        for (Link link : links) {
            LonLat foot = DistanceUtils.nearestPoint(link.getLineString(), point);
            double dist = DistanceUtils.distance(point, foot);
            if (dist < distance) {
                nearestLink = link;
                nearestPoint = foot;
                distance = dist;
            }
            this.buildNetwork(matchingNetwork, link);
        }
        return new MatchingResult(point, nearestPoint, nearestLink, distance);
    }

    private void buildNetwork(Network matchingNetwork, Link link) {
        Node prevTailNode = link.getTailNode();
        Node prevHeadNode = link.getHeadNode();
        Node tailNode = matchingNetwork.hasNode(prevTailNode.getNodeID()) ? matchingNetwork.getNode(prevTailNode.getNodeID()) : prevTailNode.clone();
        Node headNode = matchingNetwork.hasNode(prevHeadNode.getNodeID()) ? matchingNetwork.getNode(prevHeadNode.getNodeID()) : prevHeadNode.clone();
        matchingNetwork.addLink(link.clone(tailNode, headNode));
    }

    public Network getMatchingNetwork() {
        return this._matchingNetwork;
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points) {
        return this.runSparseMapMatching(network, points, 3000.0);
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points, double radius) {
        ArrayList<Double> radiusList = new ArrayList<Double>(points.size());
        int i = points.size() - 1;
        while (i >= 0) {
            radiusList.add(radius);
            --i;
        }
        return this.runSparseMapMatching(network, points, radiusList);
    }

    public <T extends LonLat> Route runSparseMapMatching(Network network, List<T> points, List<Double> radiusList) {
        Route route;
        Link linkNext;
        Link linkPrev;
        if (points.size() < 2) {
            return null;
        }
        this._matchingNetwork = new Network(false, false);
        int numPoints = points.size();
        MatchingResult orgInfo = super.runMatching(network, (LonLat)points.get(0), (double)radiusList.get(0));
        MatchingResult dstInfo = super.runMatching(network, (LonLat)points.get(numPoints - 1), (double)radiusList.get(numPoints - 1));
        if (!orgInfo.isValid() || !dstInfo.isValid()) {
            return null;
        }
        this.buildNetwork(this._matchingNetwork, orgInfo.getNearestLink());
        this.buildNetwork(this._matchingNetwork, dstInfo.getNearestLink());
        MatchingResult resultPrev = orgInfo;
        int i = 1;
        while (i < numPoints - 1) {
            double range;
            LonLat point = (LonLat)points.get(i);
            MatchingResult resultNext = this.runMatching(network, point, range = radiusList.get(i).doubleValue(), this._matchingNetwork);
            if (resultNext.isValid()) {
                Route route2;
                Link linkPrev2;
                Link linkNext2 = resultNext.getNearestLink();
                this.buildNetwork(this._matchingNetwork, linkNext2);
                if (resultPrev.isValid() && !(linkPrev2 = resultPrev.getNearestLink()).equals(linkNext2) && (route2 = this._routingLogic.getRoute(network, linkPrev2.getTailNode(), linkNext2.getHeadNode())) != null) {
                    for (Link link : route2.listLinks()) {
                        this.buildNetwork(this._matchingNetwork, link);
                    }
                }
                resultPrev = resultNext;
            }
            ++i;
        }
        if (resultPrev.isValid() && !(linkPrev = resultPrev.getNearestLink()).equals(linkNext = dstInfo.getNearestLink()) && (route = this._routingLogic.getRoute(network, linkPrev.getTailNode(), linkNext.getHeadNode())) != null) {
            for (Link link : route.listLinks()) {
                this.buildNetwork(this._matchingNetwork, link);
            }
        }
        Route tempRoute = this.queryRoute(this._matchingNetwork, orgInfo, dstInfo);
        return this.restoreRoute(network, tempRoute);
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

