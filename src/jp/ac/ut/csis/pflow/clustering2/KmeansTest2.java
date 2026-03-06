/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.apache.commons.lang3.text.StrTokenizer
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
import java.util.Map;
import jp.ac.ut.csis.pflow.clustering2.CanopyKmeans;
import jp.ac.ut.csis.pflow.clustering2.Kmeans;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import org.apache.commons.lang3.text.StrTokenizer;

public class KmeansTest2 {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        ArrayList<LonLatTime> points = new ArrayList<LonLatTime>();
        try (BufferedReader br = new BufferedReader(new FileReader(infile))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                Date ts = new Date();
                double lon = Double.parseDouble(tokens[1]);
                double lat = Double.parseDouble(tokens[0]);
                points.add(new LonLatTime(lon, lat, ts));
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map<ILonLat, List<ILonLatTime>> clusters = (Map<ILonLat, List<ILonLatTime>>)(Map<?,?>) new CanopyKmeans(3000.0, 5000.0).clustering(points);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write("clusterNo,clon,clat,time,lon,lat");
            bw.newLine();
            int No = 1;
            for (Map.Entry<ILonLat, List<ILonLatTime>> entry : clusters.entrySet()) {
                ILonLat centroid = entry.getKey();
                List<ILonLatTime> cluster = entry.getValue();
                for (ILonLatTime point : cluster) {
                    bw.write(String.format("%d,%.08f,%.08f,%s,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), sdf.format(point.getTimeStamp()), point.getLon(), point.getLat()));
                    bw.newLine();
                }
                ++No;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void main_(String[] args) {
        int k = Integer.parseInt(args[0]);
        File infile = new File(args[1]);
        File outfile = new File(args[2]);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<LonLatTime> points = new ArrayList<LonLatTime>();
        try (BufferedReader br = new BufferedReader(new FileReader(infile))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                Date ts = sdf.parse(tokens[0]);
                double lon = Double.parseDouble(tokens[1]);
                double lat = Double.parseDouble(tokens[2]);
                points.add(new LonLatTime(lon, lat, ts));
            }
        }
        catch (ParseException exp) {
            exp.printStackTrace();
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map<ILonLat, List<ILonLatTime>> clusters = (Map<ILonLat, List<ILonLatTime>>)(Map<?,?>) new Kmeans(k, Kmeans.SPATIALLY_GRID_DISTRIB).clustering(points);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write("clusterNo,clon,clat,time,lon,lat");
            bw.newLine();
            int No = 1;
            for (Map.Entry<ILonLat, List<ILonLatTime>> entry : clusters.entrySet()) {
                ILonLat centroid = entry.getKey();
                List<ILonLatTime> cluster = entry.getValue();
                for (ILonLatTime point : cluster) {
                    bw.write(String.format("%d,%.08f,%.08f,%s,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), sdf.format(point.getTimeStamp()), point.getLon(), point.getLat()));
                    bw.newLine();
                }
                ++No;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void main__(String[] args) {
        int k = Integer.parseInt(args[0]);
        File infile = new File(args[1]);
        File outfile = new File(args[2]);
        ArrayList<LonLat> points = new ArrayList<LonLat>();
        try (BufferedReader br = new BufferedReader(new FileReader(infile))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                double lon = Double.parseDouble(tokens[3]);
                double lat = Double.parseDouble(tokens[2]);
                points.add(new LonLat(lon, lat));
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map<ILonLat, List<ILonLat>> clusters = (Map<ILonLat, List<ILonLat>>)(Map<?,?>) new Kmeans(k, Kmeans.SPATIALLY_GRID_DISTRIB).clustering(points);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            bw.write("clusterNo,clon,clat,lon,lat");
            bw.newLine();
            int No = 1;
            for (Map.Entry<ILonLat, List<ILonLat>> entry : clusters.entrySet()) {
                ILonLat centroid = entry.getKey();
                List<ILonLat> cluster = entry.getValue();
                for (ILonLat point : cluster) {
                    bw.write(String.format("%d,%.08f,%.08f,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), point.getLon(), point.getLat()));
                    bw.newLine();
                }
                ++No;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }
}
