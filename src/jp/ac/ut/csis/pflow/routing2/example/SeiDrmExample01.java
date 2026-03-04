/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.logic.DrmLinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class SeiDrmExample01 {
    public static void main(String[] args) {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_password", "db_name");
             Connection con = pgloader.getConnection()) {
            PgSeiDrmLoader drmLoader = new PgSeiDrmLoader();
            DrmQueryCondition queryCondition = new DrmQueryCondition(new double[]{140.3850174, 35.7621148, 139.770298, 35.54842934}, 3000.0, new int[]{1, 2, 3, 4, 5}, true);
            Network network = drmLoader.load(con, (QueryCondition)queryCondition);
            Dijkstra logic = new Dijkstra(new DrmLinkCost(DrmLinkCost.Mode.VEHICLE));
            long ts = System.nanoTime();
            Route route = logic.getRoute(network, 140.3850174, 35.7621148, 139.770298, 35.54842934);
            long te = System.nanoTime();
            System.out.printf("%.09f(sec)\n", (double)(te - ts) / 1.0E9);
            List<LonLat> trajectory = logic.fillRouteGeometry(network, route);
            int N = 1;
            for (LonLat pos : trajectory) {
                System.out.printf("%d,%f,%f\n", N++, pos.getLon(), pos.getLat());
            }
        }
        catch (SQLException exp) {
            exp.printStackTrace();
        }
    }
}

