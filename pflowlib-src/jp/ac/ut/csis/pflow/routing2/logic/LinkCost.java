/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class LinkCost {
    public double getCost(Link link) {
        return link.getCost();
    }

    public double getCost(Link prevlink, Node crntNode, Link nextlink) {
        return nextlink.getCost();
    }

    public double getReverseCost(Link link) {
        return link.getReverseCost();
    }

    public double getReverseCost(Link prevlink, Node crntNode, Link nextlink) {
        return nextlink.getReverseCost();
    }
}

