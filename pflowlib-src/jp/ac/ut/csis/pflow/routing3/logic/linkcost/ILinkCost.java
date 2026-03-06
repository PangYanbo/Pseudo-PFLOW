/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.logic.linkcost;

import jp.ac.ut.csis.pflow.routing3.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Node;

public interface ILinkCost {
    public ITransport getTransport();

    public void setTransport(ITransport var1);

    public double getCost(Link var1, Node var2, Link var3);
}

