/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing3.res.RailwayNode;
import jp.ac.ut.csis.pflow.routing3.res.Route;

public class RailwayRouting
extends Dijkstra {
    public RailwayRouting() {
        this(new RailwayLinkCost());
    }

    public RailwayRouting(RailwayLinkCost linkCost) {
        super(linkCost);
    }

    @Override
    public String getName() {
        return "RailwayRouting";
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode, int N) {
        RailwayNode depEkiNode = (RailwayNode)RailwayNode.class.cast(depnode);
        RailwayNode arrEkiNode = (RailwayNode)RailwayNode.class.cast(arrnode);
        Set<RailwayNode> depNodeSet = depEkiNode.getGroupNodes();
        Set<RailwayNode> arrNodeSet = arrEkiNode.getGroupNodes();
        HashMap routes = new HashMap();
        for (RailwayNode depNode : depNodeSet) {
            for (RailwayNode arrNode : arrNodeSet) {
                List<Route> routeList = super.getRoutes(network, depNode, arrNode, 1);
                if (routeList == null || routeList.isEmpty()) continue;
                HashSet<Integer> set = new HashSet<Integer>();
                Route route = routeList.get(0);
                for (Link link : route.listLinks()) {
                    set.add(((RailwayLink)RailwayLink.class.cast(link)).getLineCode());
                }
                routes.put(route, set);
            }
        }
        ArrayList entryList = new ArrayList(routes.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<Route, Set<Integer>>>(){

            @Override
            public int compare(Map.Entry<Route, Set<Integer>> entryA, Map.Entry<Route, Set<Integer>> entryB) {
                Route routeA = entryA.getKey();
                Route routeB = entryB.getKey();
                int numLinesA = entryA.getValue().size();
                int numLinesB = entryB.getValue().size();
                Double valA = (double)numLinesA * 100000.0 + routeA.getCost();
                Double valB = (double)numLinesB * 100000.0 + routeB.getCost();
                return valA.compareTo(valB);
            }
        });
        ArrayList<Route> result = new ArrayList<Route>(N);
        int i = 0;
        while (i < N) {
            result.add((Route)((Map.Entry)entryList.get(i)).getKey());
            ++i;
        }
        return result;
    }
}

