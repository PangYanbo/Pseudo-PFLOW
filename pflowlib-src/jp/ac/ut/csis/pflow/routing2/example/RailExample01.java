/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.STPoint;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing2.loader.PgRailLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.RailQueryCondition;
import jp.ac.ut.csis.pflow.routing2.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class RailExample01 {
    public static void main(String[] args) {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "pg_dbname");){
            try {
                Throwable throwable = null;
                Object var3_5 = null;
                try (Connection con = pgloader.getConnection();){
                    PgRailLoader loader = new PgRailLoader();
                    Network network = loader.load(con, (QueryCondition)new RailQueryCondition(true));
                    Dijkstra logic = new Dijkstra();
                    Route route = logic.getRoute(network, 139.95268822, 35.8933612, 139.80488777, 35.74985592);
                    if (route == null) {
                        System.err.println("fail to get route");
                        System.exit(1);
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date t0 = sdf.parse("2016-09-06 10:00:00");
                    Date t1 = sdf.parse("2016-09-06 11:00:00");
                    List<LonLat> geomTrajectory = logic.fillRouteGeometry(network, route);
                    List<STPoint> stTrajectory = TrajectoryUtils.interpolateUnitTime(geomTrajectory, t0, t1);
                    int idx = 1;
                    System.out.println("index,timestamp,lon,lat");
                    for (STPoint point : stTrajectory) {
                        System.out.printf("%d,%s,%f,%f\n", idx++, sdf.format(point.getTimeStamp()), point.getLon(), point.getLat());
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
            catch (SQLException | ParseException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
    }
}

