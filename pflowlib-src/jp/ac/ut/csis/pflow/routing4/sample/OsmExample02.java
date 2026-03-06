/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.PgOsmWayLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class OsmExample02 {
    public static void main(String[] args) throws IOException {
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("db_name");
        try {
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgLoader.connect();){
                    OsmQueryCondition queryCondition = new OsmQueryCondition(new double[]{32.56347656, -25.81967194, 32.58544922, -25.96915687}, 3000.0);
                    Network network = new PgOsmWayLoader().setConnection(con).setTableName("mozambique.osm_road_available").setQueryCondition(queryCondition).setGeometryFlag(true).load();
                    Dijkstra dijkstra = new Dijkstra(new LinkCost());
                    long ts1 = System.nanoTime();
                    Route route1 = dijkstra.getRoute(network, 32.56347656, -25.81967194, 32.58544922, -25.96915687);
                    long te1 = System.nanoTime();
                    System.out.printf("%.09f(sec)\n", (double)(te1 - ts1) / 1.0E9);
                    OsmExample02.printRoute(route1);
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
                pgLoader.disconnect();
            }
        }
        finally {
            pgLoader.disconnect();
        }
    }

    protected static void printRoute(Route route) {
        int N = 1;
        for (ILonLat pos : route.getTrajectory()) {
            System.out.printf("%d,%f,%f\n", N++, pos.getLon(), pos.getLat());
        }
    }
}

