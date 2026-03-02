/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing3.loader.PgOsmLoader;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.logic.AStar;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Route;

public class OsmExample01 {
    public static void main(String[] args) throws IOException {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "db_name");){
            try (Connection con = pgloader.getConnection()) {
                PgOsmLoader osmLoader = new PgOsmLoader("bangladesh.osm_road_available");
                OsmQueryCondition queryCondition = new OsmQueryCondition(new double[]{90.39327621, 23.73286919, 90.41121483, 23.84753397}, 3000.0);
                Network network = osmLoader.load(con, (QueryCondition)queryCondition);
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
            catch (SQLException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
    }

    protected static void printRoute(Route route) {
        int N = 1;
        for (LonLat pos : route.getTrajectory()) {
            System.out.printf("%d,%f,%f\n", N++, pos.getLon(), pos.getLat());
        }
    }
}

