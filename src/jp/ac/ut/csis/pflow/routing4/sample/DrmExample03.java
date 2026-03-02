/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.CsvDrmBasicRoadLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import org.apache.commons.lang.StringUtils;

public class DrmExample03 {
    public static void main(String[] args) throws IOException {
        File inputFile = new File(args[1]);
        long ts00 = System.nanoTime();
        Network network = new CsvDrmBasicRoadLoader().setNetworkFile(new File(args[0])).setDelimiter(ICsvNetworkLoader.Delimiter.TSV).setHeaderFlag(true).setGeometryFlag(true).load();
        long te00 = System.nanoTime();
        System.err.printf("%.09f(sec)\n", (double)(te00 - ts00) / 1.0E9);
        System.err.println(DrmExample03.getMemoryInfo());
        try (BufferedReader br = Files.newBufferedReader(inputFile.toPath())) {
            Dijkstra dijkstra = new Dijkstra(new LinkCost(DrmTransport.WALK));
            int no = 1;
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] tokens = StringUtils.splitPreserveAllTokens((String)line, (String)",");
                double x0 = Double.parseDouble(tokens[0].trim());
                double y0 = Double.parseDouble(tokens[1].trim());
                double x1 = Double.parseDouble(tokens[2].trim());
                double y1 = Double.parseDouble(tokens[3].trim());
                long ts1 = System.nanoTime();
                Route route1 = dijkstra.getRoute(network, x0, y0, x1, y1);
                long te1 = System.nanoTime();
                System.err.printf("%.09f(sec)\n", (double)(te1 - ts1) / 1.0E9);
                DrmExample03.printRoute(no++, route1);
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    protected static void printRoute(int no, Route route) {
        int N = 1;
        for (ILonLat pos : route.getTrajectory()) {
            System.out.printf("%d,%d,%f,%f\n", no, N++, pos.getLon(), pos.getLat());
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

