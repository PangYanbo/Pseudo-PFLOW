/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.routing3.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.Route;

public class RailwayExample {
    public static void main(String[] args) throws IOException {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "db_name");){
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgloader.getConnection();){
                    PgRailwayLoader railLoader = new PgRailwayLoader("rail.railway_network_v1");
                    Network network = railLoader.load(con);
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
                            System.out.println(node);
                        }
                    } else {
                        System.out.println("failed");
                    }
                }
                catch (Throwable throwable2) {
                    if (throwable == null) {
                        throwable = throwable2;
                    } else if (throwable != throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            }
            catch (SQLException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
    }
}

