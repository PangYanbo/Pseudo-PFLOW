/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jp.ac.ut.csis.pflow.routing4.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public class NetworkAssessment {
    public List<Node> listEndNodes(Network network) {
        ArrayList<Node> endNodes = new ArrayList<Node>();
        for (Node node : network.listNodes()) {
            List<Link> links = node.listAllLinks();
            if (links.size() != 1) continue;
            endNodes.add(node);
        }
        System.out.printf("[End Nodes] there are %d end nodes\n", endNodes.size());
        return endNodes;
    }

    public Map<Node, Integer> countConnectedLinks(Network network) {
        HashMap<Node, Integer> nodeMap = new HashMap<Node, Integer>();
        int numRoadEnd = 0;
        List<Node> nodeList = network.listNodes();
        for (Node node : nodeList) {
            List<Link> links = node.listAllLinks();
            nodeMap.put(node, links.size());
            if (links.size() != 1) continue;
            ++numRoadEnd;
        }
        System.out.printf("[Count Nodes] there are %d nodes including %d road ends\n", nodeList.size(), numRoadEnd);
        return nodeMap;
    }

    public List<Set<Node>> validateIsolation(Network network) {
        ArrayList<Set<Node>> networkGroup = new ArrayList<Set<Node>>();
        List<Link> links = network.listLinks();
        Dijkstra dijkstra = new Dijkstra();
        List<Node> nodes = null;
        while (!(nodes = network.listNodes()).isEmpty()) {
            Node node = nodes.get(0);
            Map<Node, ARoutingLogic.Knot> costs = dijkstra.getCost(network, node);
            Set<Node> nodeGroup = costs.keySet();
            for (Node n : costs.keySet()) {
                network.remove(n);
            }
            networkGroup.add(nodeGroup);
            System.out.printf("\t[Validate Isolation] group(%d) includes %d nodes\n", networkGroup.size(), nodeGroup.size());
        }
        System.out.printf("[Validate Isolation] there are %d independent group\n", networkGroup.size());
        Collections.sort(networkGroup, new Comparator<Set<Node>>(){

            @Override
            public int compare(Set<Node> a, Set<Node> b) {
                Integer ai = a.size();
                Integer bi = b.size();
                return bi.compareTo(ai);
            }
        });
        network.clear();
        for (Link link : links) {
            network.addLink(link);
        }
        return networkGroup;
    }
}

