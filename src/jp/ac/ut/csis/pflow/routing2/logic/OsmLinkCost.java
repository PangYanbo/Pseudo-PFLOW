/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.OsmLink;

public class OsmLinkCost
extends LinkCost {
    @Override
    public double getCost(Link link) {
        return this.getCost(null, null, link);
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        if (nextLink instanceof OsmLink) {
            OsmLink osmlink = (OsmLink)OsmLink.class.cast(nextLink);
            double vel_m_s = osmlink.getSpeed();
            return nextLink.getCost() / vel_m_s;
        }
        return nextLink.getCost();
    }

    @Override
    public double getReverseCost(Link link) {
        return this.getReverseCost(null, null, link);
    }

    @Override
    public double getReverseCost(Link prevLink, Node crntNode, Link nextLink) {
        if (nextLink instanceof OsmLink) {
            OsmLink osmlink = (OsmLink)OsmLink.class.cast(nextLink);
            double vel_m_s = osmlink.getSpeed();
            return nextLink.getReverseCost() / vel_m_s;
        }
        return nextLink.getReverseCost();
    }
}

