/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.routing4.loader.PgDrmBasicRoadLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class DrmExample04 {
    public static void main(String[] args) throws IOException {
        LonLat org = new LonLat(139.76703183, 35.68147726);
        LonLat dst = new LonLat(138.38160671, 34.9754318);
        long ts00 = System.nanoTime();
        PgConnector conn = new PgConnector();
        try {
            try (Connection con = conn.setDBName("lmdb").setPassword("kashiwa64307").connect()) {
                Network network = new PgDrmBasicRoadLoader().setConnection(con).setTableName("drm3103.basic_links").setBounds(org.getLon(), org.getLat(), dst.getLon(), dst.getLat()).setGeometryFlag(true).load();
                long te00 = System.nanoTime();
                System.out.printf("%.09f(sec)\n", (double)(te00 - ts00) / 1.0E9);
                System.out.println(DrmExample04.getMemoryInfo());
                Dijkstra dijkstra = new Dijkstra(new LinkCost(DrmTransport.VEHICLE));
                long ts1 = System.nanoTime();
                Route route1 = dijkstra.getRoute(network, org.getLon(), org.getLat(), dst.getLon(), dst.getLat());
                long te1 = System.nanoTime();
                System.out.printf("%.09f(sec)\n", (double)(te1 - ts1) / 1.0E9);
                DrmExample04.printRoute(route1);
            }
            catch (SQLException exp) {
                exp.printStackTrace();
                conn.disconnect();
            }
        }
        finally {
            conn.disconnect();
        }
    }

    protected static void printRoute(Route route) {
        int N = 1;
        System.out.println("No,lon,lat");
        for (ILonLat pos : route.getTrajectory()) {
            System.out.printf("%d,%f,%f\n", N++, pos.getLon(), pos.getLat());
        }
    }

    public static String getMemoryInfo() {
        DecimalFormat df = new DecimalFormat("#,### KB");
        long free = Runtime.getRuntime().freeMemory() / 1024L;
        long total = Runtime.getRuntime().totalMemory() / 1024L;
        long max = Runtime.getRuntime().maxMemory() / 1024L;
        long used = total - free;
        double ratio = (double)(used * 100L) / (double)total;
        return String.format("Java memory: total=%s, usage=%s (%.1f %%), max=%s", df.format(total), df.format(used), ratio, df.format(max));
    }
}

