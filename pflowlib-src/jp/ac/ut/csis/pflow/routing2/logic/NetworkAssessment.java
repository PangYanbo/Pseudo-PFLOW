/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jp.ac.ut.csis.pflow.routing2.logic.ARoutingLogic;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkAssessment {
    private static final Logger LOGGER = LogManager.getLogger(NetworkAssessment.class);

    public List<Node> listEndNodes(Network network) {
        ArrayList<Node> endNodes = new ArrayList<Node>();
        for (Node node : network.listNodes()) {
            List<Link> links = node.listAllLinks();
            if (links.size() != 1) continue;
            endNodes.add(node);
        }
        LOGGER.info(String.format("[End Nodes] there are %d end nodes", endNodes.size()));
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
        LOGGER.info(String.format("[Count Nodes] there are %d nodes including %d road ends", nodeList.size(), numRoadEnd));
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
            LOGGER.info(String.format("\t[Validate Isolation] group(%d) includes %d nodes", networkGroup.size(), nodeGroup.size()));
        }
        LOGGER.info(String.format("[Validate Isolation] there are %d independent groups", networkGroup.size()));
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

