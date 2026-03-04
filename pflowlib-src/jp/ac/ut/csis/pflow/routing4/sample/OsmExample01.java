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
import jp.ac.ut.csis.pflow.routing4.loader.PgOsmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.AStar;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class OsmExample01 {
    public static void main(String[] args) throws IOException {
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("db_name");
        try {
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgLoader.connect();){
                    OsmQueryCondition queryCondition = new OsmQueryCondition(new double[]{90.39327621, 23.73286919, 90.41121483, 23.84753397}, 3000.0);
                    Network network = new PgOsmLoader().setConnection(con).setTableName("bangladesh.osm_road_available").setQueryCondition(queryCondition).setGeometryFlag(true).load();
                    Dijkstra dijkstra = new Dijkstra(new LinkCost(Transport.VEHICLE));
                    long ts1 = System.nanoTime();
                    Route route1 = dijkstra.getRoute(network, 90.39327621, 23.73286919, 90.41121483, 23.84753397);
                    long te1 = System.nanoTime();
                    System.out.printf("%.09f(sec)\n", (double)(te1 - ts1) / 1.0E9);
                    OsmExample01.printRoute(route1);
                    AStar astar = new AStar(new AStarLinkCost(Transport.VEHICLE));
                    long ts2 = System.nanoTime();
                    Route route2 = astar.getRoute(network, 90.39327621, 23.73286919, 90.41121483, 23.84753397);
                    long te2 = System.nanoTime();
                    System.out.printf("%.09f(sec)\n", (double)(te2 - ts2) / 1.0E9);
                    OsmExample01.printRoute(route2);
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

