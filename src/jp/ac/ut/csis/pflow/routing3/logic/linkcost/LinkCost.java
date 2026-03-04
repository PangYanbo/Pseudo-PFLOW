/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.logic.linkcost;

import jp.ac.ut.csis.pflow.routing3.logic.linkcost.ILinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing3.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;

public class LinkCost
implements ILinkCost {
    private Network _network;
    private ITransport _transport;

    public LinkCost() {
        this(Transport.VEHICLE);
    }

    public LinkCost(ITransport transport) {
        this(null, transport);
    }

    public LinkCost(Network network, ITransport transport) {
        this._network = network;
        this._transport = transport;
    }

    public Network getNetwork() {
        return this._network;
    }

    public void setNetwork(Network network) {
        this._network = network;
    }

    @Override
    public ITransport getTransport() {
        return this._transport;
    }

    @Override
    public void setTransport(ITransport transport) {
        this._transport = transport;
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        ITransport transport = this.getTransport();
        boolean reverse = crntNode.equals(nextLink.getHeadNode());
        double velocity = transport.getVelocity(nextLink.getLinkType());
        if (Double.isNaN(velocity) || reverse && nextLink.isOneWay()) {
            return 2.147483647E9;
        }
        return nextLink.getLength() / velocity;
    }
}

