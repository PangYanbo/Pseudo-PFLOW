/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.logic.linkcost;

import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing4.res.RailwayNode;
import org.apache.commons.lang3.StringUtils;

public class RailwayLinkCost
extends LinkCost {
    public static double TIME_TRANSFER = 300.0;
    private double _walkingVelocity;
    private double _waitingTime;

    public RailwayLinkCost() {
        this(null, Transport.RAILWAY, Transport.WALK, TIME_TRANSFER);
    }

    public RailwayLinkCost(Network network, ITransport railway, ITransport walk, double waitingTime) {
        super(network, railway);
        this._walkingVelocity = walk.getOrdinaryVelocity();
        this._waitingTime = waitingTime;
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        ITransport transport = this.getTransport();
        if (prevLink instanceof RailwayLink && nextLink instanceof RailwayLink) {
            String nextLineName;
            Node prevNode = crntNode.equals(prevLink.getHeadNode()) ? prevLink.getTailNode() : prevLink.getHeadNode();
            Node nextNode = crntNode.equals(nextLink.getTailNode()) ? nextLink.getHeadNode() : nextLink.getTailNode();
            String prevLineName = ((RailwayNode)RailwayNode.class.cast(prevNode)).getLineName();
            if (StringUtils.equals((String)prevLineName, (String)(nextLineName = ((RailwayNode)RailwayNode.class.cast(nextNode)).getLineName()))) {
                return nextLink.getLength() / transport.getOrdinaryVelocity();
            }
            return nextLink.getLength() / this._walkingVelocity + this._waitingTime;
        }
        return nextLink.getLength() / transport.getOrdinaryVelocity();
    }
}

