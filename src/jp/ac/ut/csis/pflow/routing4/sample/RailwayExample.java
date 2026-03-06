/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.routing4.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class RailwayExample {
    public static void main(String[] args) throws IOException {
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("db_name");
        try (Connection con = pgLoader.connect()) {
            Network network = new PgRailwayLoader().setConnection(con).setTableName("rail.railway_network_v1").load();
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
        catch (SQLException exp) {
            exp.printStackTrace();
        }
        finally {
            pgLoader.disconnect();
        }
    }
}

