/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing3.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.logic.AStar;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Route;

public class SeiDrmExample01 {
    public static void main(String[] args) throws IOException {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "db_name");){
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgloader.getConnection();){
                    PgSeiDrmLoader drmLoader = new PgSeiDrmLoader("seidrm2017.drm_32_table");
                    DrmQueryCondition queryCondition = new DrmQueryCondition(new double[]{140.3850174, 35.7621148, 139.770298, 35.54842934}, 3000.0, new int[]{1, 2, 3, 4, 5});
                    Network network = drmLoader.load(con, (QueryCondition)queryCondition);
                    Dijkstra dijkstra = new Dijkstra(new LinkCost(DrmTransport.VEHICLE));
                    long ts1 = System.nanoTime();
                    Route route1 = dijkstra.getRoute(network, 140.3850174, 35.7621148, 139.770298, 35.54842934);
                    long te1 = System.nanoTime();
                    System.out.printf("%.09f(sec)\n", (double)(te1 - ts1) / 1.0E9);
                    SeiDrmExample01.printRoute(route1);
                    AStar astar = new AStar(new AStarLinkCost(DrmTransport.VEHICLE));
                    long ts2 = System.nanoTime();
                    Route route2 = astar.getRoute(network, 140.3850174, 35.7621148, 139.770298, 35.54842934);
                    long te2 = System.nanoTime();
                    System.out.printf("%.09f(sec)\n", (double)(te2 - ts2) / 1.0E9);
                    SeiDrmExample01.printRoute(route2);
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

    protected static void printRoute(Route route) {
        int N = 1;
        for (LonLat pos : route.getTrajectory()) {
            System.out.printf("%d,%f,%f\n", N++, pos.getLon(), pos.getLat());
        }
    }
}

