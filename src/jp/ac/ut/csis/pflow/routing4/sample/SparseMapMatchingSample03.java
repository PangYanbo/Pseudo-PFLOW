/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class SparseMapMatchingSample03 {
    private static final int[] ROAD_TYPES = new int[]{1, 2, 3, 4, 5, 6, 7};

    public static void main(String[] args) {
        LonLat tokyo = new LonLat(139.767052, 35.681167);
        LonLat osaka = new LonLat(135.495951, 34.702485);
        LonLat kanazawa = new LonLat(136.648035, 36.578268);
        List<ILonLat> points = Arrays.asList(tokyo, kanazawa);
        IQueryCondition[] conditions = new DrmQueryCondition[]{new DrmQueryCondition(SparseMapMatchingSample03.createRect(tokyo), 3000.0), new DrmQueryCondition(SparseMapMatchingSample03.createRect(points), 3000.0, ROAD_TYPES), new DrmQueryCondition(SparseMapMatchingSample03.createRect(osaka), 3000.0)};
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("dbname");
        Dijkstra logic = new Dijkstra(new LinkCost(DrmTransport.VEHICLE));
        SparseMapMatching matchingLogic = new SparseMapMatching(logic);
        try {
            try (Connection con = pgLoader.connect()) {
                long t0 = System.currentTimeMillis();
                Network network = new PgSeiDrmLoader().setConnection(con).setTableName("seidrm2017.drm_32_table").setQueryConditions(conditions).setGeometryFlag(true).load();
                long t1 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t1 - t0) / 1000.0);
                Route route = matchingLogic.runSparseMapMatching(network, points);
                long t2 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t2 - t1) / 1000.0);
                if (route == null) {
                    System.out.println("fail to get result");
                } else {
                    int idx = 0;
                    for (Node node : route.listNodes()) {
                        System.out.printf("%04d,%s,%.06f,%.06f\n", idx++, node.getNodeID(), node.getLon(), node.getLat());
                    }
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

    public static <T extends ILonLat> double[] createRect(T point) {
        return SparseMapMatchingSample03.createRect(Arrays.asList(point));
    }

    private static <T extends ILonLat> double[] createRect(List<T> points) {
        Rectangle2D.Double rect = TrajectoryUtils.makeMBR(points);
        return new double[]{rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY()};
    }
}

