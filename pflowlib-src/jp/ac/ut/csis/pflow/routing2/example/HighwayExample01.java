/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.example;

import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.routing2.loader.PgHighwayLoader;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.logic.HighwayLinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class HighwayExample01 {
    public static void main(String[] args) {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "pg_dbname");){
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgloader.getConnection();){
                    PgHighwayLoader loader = new PgHighwayLoader();
                    Network network = loader.load(con);
                    Node source = network.getNode("1981");
                    Node target = network.getNode("308");
                    Dijkstra logic = new Dijkstra(new HighwayLinkCost());
                    Route route = logic.getRoute(network, source, target);
                    if (route == null) {
                        System.err.println("fail to get route");
                        System.exit(1);
                    }
                    for (Node node : route.listNodes()) {
                        System.out.println(node);
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

