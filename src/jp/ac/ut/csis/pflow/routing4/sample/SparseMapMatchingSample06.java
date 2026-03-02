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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.routing4.loader.CsvDrmAllRoadLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import org.apache.commons.lang.StringUtils;

public class SparseMapMatchingSample06 {
    public static void main(String[] args) {
        File drmFile = new File(args[0]);
        File inputFile = new File(args[1]);
        Network network = new CsvDrmAllRoadLoader().setNetworkFile(drmFile).setDelimiter(ICsvNetworkLoader.Delimiter.TSV).setHeaderFlag(true).setGeometryFlag(true).load();
        Dijkstra logic = new Dijkstra(new LinkCost(DrmTransport.WALK));
        SparseMapMatching matchingLogic = new SparseMapMatching(logic);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (BufferedReader br = Files.newBufferedReader(inputFile.toPath())) {
            int no = 1;
            String line = null;
            ArrayList<LonLatTime> points = new ArrayList<LonLatTime>();
            while ((line = br.readLine()) != null) {
                try {
                    String[] tokens = StringUtils.splitPreserveAllTokens((String)line, (String)",");
                    Date t = sdf.parse(tokens[0].trim());
                    double x = Double.parseDouble(tokens[2].trim());
                    double y = Double.parseDouble(tokens[1].trim());
                    points.add(new LonLatTime(x, y, t));
                }
                catch (ParseException exp) {
                    exp.printStackTrace();
                }
            }
            long tn0 = System.nanoTime();
            Route route = matchingLogic.setMatchingType(SparseMapMatching.MatchingType.LINK).setSearchRange(3000.0).runSparseMapMatching(network, points);
            long tn1 = System.nanoTime();
            System.err.printf("%.09f(sec)\n", (double)(tn1 - tn0) / 1.0E9);
            SparseMapMatchingSample06.printRoute(no++, route);
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
}

