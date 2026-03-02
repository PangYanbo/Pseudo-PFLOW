/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic.linkcost;

import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public interface ILinkCost {
    public ITransport getTransport();

    public void setTransport(ITransport var1);

    public double getCost(Link var1, Node var2, Link var3);
}

