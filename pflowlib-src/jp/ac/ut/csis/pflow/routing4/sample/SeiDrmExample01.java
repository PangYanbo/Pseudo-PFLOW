/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.AStar;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class SeiDrmExample01 {
    public static void main(String[] args) throws IOException {
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("db_name");
        try {
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgLoader.connect();){
                    IQueryCondition queryCondition = new DrmQueryCondition().setRoadTypes(new int[]{1, 2, 3, 4, 5}).setBounds(new double[]{140.3850174, 35.7621148, 139.770298, 35.54842934}, 3000.0);
                    Network network = new PgSeiDrmLoader().setConnection(con).setTableName("seidrm2017.drm_32_table").setGeometryFlag(true).setQueryCondition(queryCondition).load();
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

