/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic.linkcost;

import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public class AStarLinkCost
extends LinkCost {
    private Node _depNode;
    private Node _arrNode;

    public AStarLinkCost() {
        this(Transport.VEHICLE);
    }

    public AStarLinkCost(ITransport transport) {
        this(transport, null, null);
    }

    public AStarLinkCost(ITransport transport, Node depNode, Node arrNode) {
        this(null, transport, depNode, arrNode);
    }

    public AStarLinkCost(Network network, ITransport transport, Node depNode, Node arrNode) {
        super(network, transport);
        this._depNode = depNode;
        this._arrNode = arrNode;
    }

    public Node getDepNode() {
        return this._depNode;
    }

    public void setDepNode(Node depNode) {
        this._depNode = depNode;
    }

    public Node getArrNode() {
        return this._arrNode;
    }

    public void setArrNode(Node arrNode) {
        this._arrNode = arrNode;
    }

    public void setNodes(Node depNode, Node arrNode) {
        this.setDepNode(depNode);
        this.setArrNode(arrNode);
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        ITransport transport = this.getTransport();
        double crntHeuristic = prevLink != null ? DistanceUtils.distance(crntNode, this.getArrNode()) / transport.getVelocity(prevLink.getLinkType()) : 0.0;
        boolean reverse = crntNode.equals(nextLink.getHeadNode());
        double velocity = transport.getVelocity(nextLink.getLinkType());
        double linkCost = nextLink.getLength() / velocity;
        if (Double.isNaN(velocity) || reverse && nextLink.isOneWay()) {
            return 2.147483647E9;
        }
        double nextHeuristic = reverse ? DistanceUtils.distance(nextLink.getTailNode(), this.getArrNode()) / velocity : DistanceUtils.distance(nextLink.getHeadNode(), this.getArrNode()) / velocity;
        return linkCost + nextHeuristic - crntHeuristic;
    }
}

