/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.List;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public interface IRoutingLogic {
    public String getName();

    public List<Route> getRoutes(Network var1, Node var2, Node var3, int var4);

    public List<Route> getRoutes(Network var1, Node var2, Node var3);

    public Route getRoute(Network var1, Node var2, Node var3);

    public List<Route> getRoutes(Network var1, String var2, String var3, int var4);

    public List<Route> getRoutes(Network var1, String var2, String var3);

    public Route getRoute(Network var1, String var2, String var3);

    public List<Route> getRoutes(Network var1, double var2, double var4, double var6, double var8, int var10);

    public List<Route> getRoutes(Network var1, double var2, double var4, double var6, double var8);

    public Route getRoute(Network var1, double var2, double var4, double var6, double var8);

    public Node getNearestNode(Network var1, double var2, double var4);

    public Node getNearestNode(Network var1, double var2, double var4, double var6);

    public Link getNearestLink(Network var1, double var2, double var4);

    public Link getNearestLink(Network var1, double var2, double var4, double var6);
}

