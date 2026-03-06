/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.text.StrTokenizer
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
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.STPoint;
import jp.ac.ut.csis.pflow.routing3.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing3.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.logic.AStar;
import jp.ac.ut.csis.pflow.routing3.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing3.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing3.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Route;
import org.apache.commons.lang3.text.StrTokenizer;

public class SparseMapMatchingSample01 {
    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        List<STPoint> points = SparseMapMatchingSample01.load(inputFile);
        double[] rect = PgSeiDrmLoader.createRect(points);
        PgLoader pgloader = new PgLoader("localhost", "pg_user", "pg_pass", "db_name");
        PgSeiDrmLoader drmLoader = new PgSeiDrmLoader("seidrm2017.drm_32_table");
        AStar logic = new AStar(new AStarLinkCost(DrmTransport.VEHICLE));
        SparseMapMatching matchingLogic = new SparseMapMatching(logic);
        try {
            try (Connection con = pgloader.getConnection()) {
                long t0 = System.currentTimeMillis();
                DrmQueryCondition queryCondition = new DrmQueryCondition(rect, 3000.0);
                Network network = drmLoader.load(con, (QueryCondition)queryCondition);
                long t1 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t1 - t0) / 1000.0);
                Route route = matchingLogic.runSparseMapMatching(network, points);
                long t2 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t2 - t1) / 1000.0);
                if (route == null) {
                    System.out.println("fail to get result");
                } else {
                    int idx = 0;
                    List<LonLat> traj = route.getTrajectory();
                    for (LonLat point : traj) {
                        System.out.printf("%04d,%.06f,%.06f\n", idx++, point.getLon(), point.getLat());
                    }
                }
            }
            catch (SQLException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
        finally {
            pgloader.close();
        }
    }

    private static List<STPoint> load(File inputFile) {
        ArrayList<STPoint> points = new ArrayList<STPoint>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
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
        catch (IOException | ParseException exp) {
            exp.printStackTrace();
        }
        return points;
    }
}

