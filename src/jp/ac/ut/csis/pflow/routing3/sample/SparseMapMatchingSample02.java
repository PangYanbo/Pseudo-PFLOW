/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing3.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom.STPoint;
import jp.ac.ut.csis.pflow.routing3.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing3.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing3.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.Route;
import org.apache.commons.lang.text.StrTokenizer;

public class SparseMapMatchingSample02 {
    public static void main(String[] args) {
        Network network;
        SparseMapMatching matchingLogic;
        List<STPoint> points;
        block17: {
            File inputFile = new File(args[0]);
            points = SparseMapMatchingSample02.load(inputFile);
            PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "db_name");
            PgRailwayLoader railLoader = new PgRailwayLoader();
            Dijkstra logic = new Dijkstra(new RailwayLinkCost());
            matchingLogic = new SparseMapMatching(logic);
            network = null;
            try {
                try {
                    Throwable throwable = null;
                    Object var9_12 = null;
                    try (Connection con = pgloader.getConnection();){
                        long t0 = System.currentTimeMillis();
                        network = railLoader.load(con);
                        long t1 = System.currentTimeMillis();
                        System.out.printf("time duration: %.03f(sec)\n", (double)(t1 - t0) / 1000.0);
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
                    break block17;
                }
            }
            catch (Throwable throwable) {
                pgloader.close();
                throw throwable;
            }
            pgloader.close();
        }
        long t1 = System.currentTimeMillis();
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

    private static List<STPoint> load(File inputFile) {
        ArrayList<STPoint> points = new ArrayList<STPoint>();
        try {
            Throwable throwable = null;
            Object var3_5 = null;
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile));){
                String line = br.readLine();
                StrTokenizer st = StrTokenizer.getTSVInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while ((line = br.readLine()) != null) {
                    String[] tokens = st.reset(line).getTokenArray();
                    Date date = sdf.parse(tokens[1]);
                    double lat = Double.parseDouble(tokens[2]);
                    double lon = Double.parseDouble(tokens[3]);
                    points.add(new STPoint(date, lon, lat));
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
        catch (IOException | ParseException exp) {
            exp.printStackTrace();
        }
        return points;
    }
}

