/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.awt.geom.Rectangle2D;
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
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.AStar;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import org.apache.commons.lang.text.StrTokenizer;

public class SparseMapMatchingSample01 {
    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        List<ILonLatTime> points = SparseMapMatchingSample01.load(inputFile);
        Rectangle2D.Double rect = TrajectoryUtils.makeMBR(points);
        PgConnector pgLoader = new PgConnector();
        pgLoader.setPassword("password").setDBName("db_name");
        AStar logic = new AStar(new AStarLinkCost(DrmTransport.VEHICLE));
        SparseMapMatching matchingLogic = new SparseMapMatching(logic);
        try {
            try (Connection con = pgLoader.connect()) {
                long t0 = System.currentTimeMillis();
                IQueryCondition queryCondition = new DrmQueryCondition().setBounds(rect, 3000.0);
                Network network = new PgSeiDrmLoader().setConnection(con).setTableName("seidrm2017.drm_32_table").setQueryCondition(queryCondition).setGeometryFlag(true).load();
                long t1 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t1 - t0) / 1000.0);
                Route route = matchingLogic.runSparseMapMatching(network, points);
                long t2 = System.currentTimeMillis();
                System.out.printf("time duration: %.03f(sec)\n", (double)(t2 - t1) / 1000.0);
                if (route == null) {
                    System.out.println("fail to get result");
                } else {
                    int idx = 0;
                    List<ILonLat> traj = route.getTrajectory();
                    for (ILonLat point : traj) {
                        System.out.printf("%04d,%.06f,%.06f\n", idx++, point.getLon(), point.getLat());
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

    private static List<ILonLatTime> load(File inputFile) {
        ArrayList<ILonLatTime> points = new ArrayList<ILonLatTime>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
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
        catch (IOException | ParseException exp) {
            exp.printStackTrace();
        }
        return points;
    }
}

