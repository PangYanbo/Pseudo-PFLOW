/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.clustering2.Cluster;
import jp.ac.ut.csis.pflow.clustering2.DBSCAN;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import org.apache.commons.lang.text.StrTokenizer;

public class DBSCANTest01 {
    public static void main(String[] args) {
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        List<ILonLatTime> points = DBSCANTest01.load(infile);
        System.out.println("num points:" + points.size());
        List<Cluster<ILonLatTime>> clusters = new DBSCAN(200.0, 3).clustering(points);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write("clusterNo,time,lon,lat");
            bw.newLine();
            int No = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Cluster<ILonLatTime> cluster : clusters) {
                for (ILonLatTime point : cluster.listPoints()) {
                    bw.write(String.format("%d,%s,%.08f,%.08f", No, sdf.format(point.getTimeStamp()), point.getLon(), point.getLat()));
                    bw.newLine();
                }
                ++No;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    private static List<ILonLatTime> load(File inputFile) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<ILonLatTime> points = new ArrayList<ILonLatTime>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                Date ts = sdf.parse(tokens[4]);
                double lon = Double.parseDouble(tokens[5]);
                double lat = Double.parseDouble(tokens[6]);
                points.add(new LonLatTime(lon, lat, ts));
            }
        }
        catch (IOException | ParseException exp) {
            exp.printStackTrace();
        }
        return points;
    }
}

