/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing4.sample;

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
import jp.ac.ut.csis.pflow.dbi.PgConnector;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.routing4.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import org.apache.commons.lang.text.StrTokenizer;

public class SparseMapMatchingSample02 {
    public static void main(String[] args) {
        Network network;
        SparseMapMatching matchingLogic;
        List<ILonLatTime> points;
        block17: {
            File inputFile = new File(args[0]);
            points = SparseMapMatchingSample02.load(inputFile);
            PgConnector pgLoader = new PgConnector();
            pgLoader.setPassword("password").setDBName("db_name");
            Dijkstra logic = new Dijkstra(new RailwayLinkCost());
            matchingLogic = new SparseMapMatching(logic);
            network = null;
            try {
                try {
                    Throwable throwable = null;
                    Object var8_11 = null;
                    try (Connection con = pgLoader.connect();){
                        long t0 = System.currentTimeMillis();
                        network = new PgRailwayLoader().setConnection(con).setTableName("rail.railway_network_v1").load();
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
                    pgLoader.disconnect();
                    break block17;
                }
            }
            catch (Throwable throwable) {
                pgLoader.disconnect();
                throw throwable;
            }
            pgLoader.disconnect();
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

    private static List<ILonLatTime> load(File inputFile) {
        ArrayList<ILonLatTime> points = new ArrayList<ILonLatTime>();
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
                    points.add(new LonLatTime(lon, lat, date));
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

