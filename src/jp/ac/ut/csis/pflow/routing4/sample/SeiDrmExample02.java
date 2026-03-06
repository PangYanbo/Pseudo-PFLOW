/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.routing4.loader.CsvSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.AStar;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class SeiDrmExample02 {
    public static void main(String[] args) throws IOException {
        LonLat org = new LonLat(140.7623291, 40.82212358);
        LonLat dst = new LonLat(130.53405762, 31.62064369);
        long ts00 = System.nanoTime();
        Network network = new CsvSeiDrmLoader().setNetworkFile(new File(args[0])).setHeaderFlag(true).setGeometryFlag(false).load();
        long te00 = System.nanoTime();
        System.out.printf("%.09f(sec)\n", (double)(te00 - ts00) / 1.0E9);
        System.out.println(SeiDrmExample02.getMemoryInfo());
        AStar astar = new AStar(new AStarLinkCost(DrmTransport.VEHICLE));
        long ts2 = System.nanoTime();
        Route route2 = astar.getRoute(network, org.getLon(), org.getLat(), dst.getLon(), dst.getLat());
        long te2 = System.nanoTime();
        System.out.printf("%.09f(sec)\n", (double)(te2 - ts2) / 1.0E9);
        SeiDrmExample02.printRoute(route2);
    }

    protected static void printRoute(Route route) {
        int N = 1;
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
        return String.format("Java memory: total=%s, usage=%s (%.1f %), max=%s", df.format(total), df.format(used), ratio, df.format(max));
    }
}

