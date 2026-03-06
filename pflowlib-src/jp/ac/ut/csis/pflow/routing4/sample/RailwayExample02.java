/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.File;
import java.io.IOException;
import jp.ac.ut.csis.pflow.routing4.loader.CsvRailwayLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.RailwayNode;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class RailwayExample02 {
    public static void main(String[] args) throws IOException {
        File railwayCsv = new File(args[0]);
        Network network = new CsvRailwayLoader().setNetworkFile(railwayCsv).setHeaderFlag(true).setGeometryFlag(true).load();
        Dijkstra logic = new Dijkstra(new RailwayLinkCost());
        Node org = network.getNode("1131104");
        Node dst = network.getNode("1131218");
        System.out.println("dep:" + org);
        System.out.println("arr:" + dst);
        long ts = System.nanoTime();
        Route route = logic.getRoute(network, org, dst);
        long te = System.nanoTime();
        System.out.printf("%.09f(sec)\n", (double)(te - ts) / 1.0E9);
        if (route != null) {
            for (Node node : route.listNodes()) {
                System.out.println(((RailwayNode)RailwayNode.class.cast(node)).getStationName());
            }
        } else {
            System.out.println("failed");
        }
    }
}

