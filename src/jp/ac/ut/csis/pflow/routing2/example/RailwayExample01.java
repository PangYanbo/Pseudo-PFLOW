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
import jp.ac.ut.csis.pflow.routing2.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.RailwayQueryCondition;
import jp.ac.ut.csis.pflow.routing2.logic.RailwayRouting;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.Route;

public class RailwayExample01 {
    public static void main(String[] args) {
        try (PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "pg_dbname");){
            try (Connection con = pgloader.getConnection()) {
                PgRailwayLoader loader = new PgRailwayLoader();
                Network network = loader.load(con, (QueryCondition)new RailwayQueryCondition(true));
                Node source = network.getNode("1132005");
                Node target = network.getNode("1130101");
                RailwayRouting logic = new RailwayRouting();
                Route route = logic.getRoute(network, source, target);
                if (route == null) {
                    System.err.println("fail to get route");
                    System.exit(1);
                }
                for (Node node : route.listNodes()) {
                    System.out.println(node);
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
            catch (SQLException | ParseException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
    }
}

