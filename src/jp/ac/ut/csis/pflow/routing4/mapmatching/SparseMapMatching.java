/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.mapmatching;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing4.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.IRoutingLogic;
import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.MapMatching;
import jp.ac.ut.csis.pflow.routing4.mapmatching.MatchingResult;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class SparseMapMatching
extends MapMatching {
    private MatchingType _matchingType;
    private IRoutingLogic _routingLogic;
    private double _searchRange;

    public SparseMapMatching() {
        this(new Dijkstra());
    }

    public SparseMapMatching(IRoutingLogic routingLogic) {
        this(MatchingType.LINK, routingLogic);
    }

    public SparseMapMatching(MatchingType matchingType, IRoutingLogic routingLogic) {
        this(matchingType, routingLogic, 1000.0);
    }

    public SparseMapMatching(MatchingType matchingType, IRoutingLogic routingLogic, double searchRange) {
        this._matchingType = matchingType;
        this._routingLogic = routingLogic;
        this._searchRange = searchRange;
    }

    public MatchingType getMatchingType() {
        return this._matchingType;
    }

    public SparseMapMatching setMatchingType(MatchingType matchingType) {
        this._matchingType = matchingType;
        return this;
    }

    public double getSearchRange() {
        return this._searchRange;
    }

    public SparseMapMatching setSearchRange(double searchRange) {
        this._searchRange = searchRange;
        return this;
    }

    public IRoutingLogic getRoutingLogic() {
        return this._routingLogic;
    }

    public <T extends ILonLat> Route runSparseMapMatching(Network network, List<T> points) {
        return this.runSparseMapMatching(network, points, this.getSearchRange());
    }

    public <T extends ILonLat> Route runSparseMapMatching(Network network, List<T> points, double range) {
        switch (this.getMatchingType()) {
            default: {
                return this.runSparseMapMatchingToLink(network, points, range);
            }
            case NODE: 
        }
        return this.runSparseMapMatchingToNode(network, points, range);
    }

    public <T extends ILonLat> Route runSparseMapMatchingToLink(Network network, List<T> points, double range) {
        if (points.size() < 2) {
            System.err.println("[error] insufficient input points. requires 2 or more");
            return null;
        }
        this.setSearchRange(range);
        Network midtermNetwork = new Network(false, false);
        MatchingResult resultPrev = null;
        MatchingResult orgInfo = null;
        MatchingResult dstInfo = null;
        for (ILonLat point : points) {
            MatchingResult resultNext = this.runMatchingToLink(network, point, this.getSearchRange());
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

    public <T extends ILonLat> Route runSparseMapMatchingToNode(Network network, List<T> points, double range) {
        if (points.size() < 2) {
            System.err.println("[error] insufficient input points. requires 2 or more");
            return null;
        }
        this.setSearchRange(range);
        Network midtermNetwork = new Network(false, false);
        MatchingResult resultPrev = null;
        MatchingResult orgInfo = null;
        MatchingResult dstInfo = null;
        for (ILonLat point : points) {
            Route route;
            Node nodeNext;
            Node nodePrev;
            MatchingResult resultNext = this.runMatchingToNode(network, point, this.getSearchRange());
            if (!resultNext.isValid()) continue;
            if (resultPrev != null && resultPrev.isValid() && !(nodePrev = (Node)Node.class.cast(resultPrev.getNearestPoint())).equals(nodeNext = (Node)Node.class.cast(resultNext.getNearestPoint())) && (route = this.getRoutingLogic().getRoute(network, nodePrev, nodeNext)) != null && !route.isEmpty()) {
                for (Link link : route.listLinks()) {
                    this.buildNetwork(midtermNetwork, link);
                }
            }
            resultPrev = resultNext;
            if (orgInfo == null) {
                orgInfo = resultNext;
            }
            dstInfo = resultNext;
        }
        if (orgInfo != null && dstInfo != null) {
            Route tempRoute = this.getRoutingLogic().getRoute(midtermNetwork, (Node)Node.class.cast(orgInfo.getNearestPoint()), (Node)Node.class.cast(dstInfo.getNearestPoint()));
            return tempRoute == null || tempRoute.isEmpty() ? null : this.restoreRoute(network, tempRoute);
        }
        return null;
    }

    @Override
    public <T extends ILonLat> MatchingResult runMatchingToNode(Network network, T point, double range) {
        return super.runMatchingToNode(network, point, range);
    }

    @Override
    public <T extends ILonLat> MatchingResult runMatchingToLink(Network network, T point, double range) {
        List<Link> links = network.queryLink(point.getLon(), point.getLat(), range);
        ITransport transport = ((ARoutingLogic)ARoutingLogic.class.cast(this.getRoutingLogic())).getLinkCost().getTransport();
        double distance = Double.MAX_VALUE;
        Link nearestLink = null;
        ILonLat nearestPoint = null;
        for (Link candidate : links) {
            AbstractMap.SimpleEntry<ILonLat, Double> nearestEntry = DistanceUtils.nearestPointEntry(candidate.getLineString(), point);
            if (nearestEntry == null || Double.isNaN(transport.getVelocity(candidate.getLinkType()))) continue;
            ILonLat p = nearestEntry.getKey();
            double d = nearestEntry.getValue();
            if (!(d < distance)) continue;
            nearestLink = candidate;
            nearestPoint = p;
            distance = d;
        }
        double ratio = Double.NaN;
        if (nearestLink != null) {
            ratio = TrajectoryUtils.getLocatePointRatio(nearestLink.getLineString(), nearestPoint);
        }
        return new MatchingResult(point, nearestPoint, nearestLink, distance, ratio);
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
        return this.getRoutingLogic().getRoute(network, orgNode, dstNode);
    }

    public static enum MatchingType {
        NODE,
        LINK;

    }
}

