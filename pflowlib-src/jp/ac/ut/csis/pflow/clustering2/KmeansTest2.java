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
import java.util.Map;
import jp.ac.ut.csis.pflow.clustering2.CanopyKmeans;
import jp.ac.ut.csis.pflow.clustering2.Kmeans;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import org.apache.commons.lang.text.StrTokenizer;

public class KmeansTest2 {
    public static void main(String[] args) {
        Throwable throwable;
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        ArrayList<LonLatTime> points = new ArrayList<LonLatTime>();
        try {
            Throwable throwable2 = null;
            throwable = null;
            try (BufferedReader br = new BufferedReader(new FileReader(infile));){
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                    Date ts = new Date();
                    double lon = Double.parseDouble(tokens[1]);
                    double lat = Double.parseDouble(tokens[0]);
                    points.add(new LonLatTime(lon, lat, ts));
                }
            }
            catch (Throwable throwable3) {
                if (throwable2 == null) {
                    throwable2 = throwable3;
                } else if (throwable2 != throwable3) {
                    throwable2.addSuppressed(throwable3);
                }
                throw throwable2;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map clusters = new CanopyKmeans(3000.0, 5000.0).clustering(points);
        try {
            throwable = null;
            Object var7_12 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));){
                bw.write("clusterNo,clon,clat,time,lon,lat");
                bw.newLine();
                int No = 1;
                for (Map.Entry entry : clusters.entrySet()) {
                    ILonLat centroid = entry.getKey();
                    List cluster = entry.getValue();
                    for (ILonLatTime point : cluster) {
                        bw.write(String.format("%d,%.08f,%.08f,%s,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), sdf.format(point.getTimeStamp()), point.getLon(), point.getLat()));
                        bw.newLine();
                    }
                    ++No;
                }
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                } else if (throwable != throwable4) {
                    throwable.addSuppressed(throwable4);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    public static void main_(String[] args) {
        Throwable throwable;
        int k = Integer.parseInt(args[0]);
        File infile = new File(args[1]);
        File outfile = new File(args[2]);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<LonLatTime> points = new ArrayList<LonLatTime>();
        try {
            Throwable throwable2 = null;
            throwable = null;
            try (BufferedReader br = new BufferedReader(new FileReader(infile));){
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                    Date ts = sdf.parse(tokens[0]);
                    double lon = Double.parseDouble(tokens[1]);
                    double lat = Double.parseDouble(tokens[2]);
                    points.add(new LonLatTime(lon, lat, ts));
                }
            }
            catch (Throwable throwable3) {
                if (throwable2 == null) {
                    throwable2 = throwable3;
                } else if (throwable2 != throwable3) {
                    throwable2.addSuppressed(throwable3);
                }
                throw throwable2;
            }
        }
        catch (ParseException exp) {
            exp.printStackTrace();
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map clusters = new Kmeans(k, Kmeans.SPATIALLY_GRID_DISTRIB).clustering(points);
        try {
            throwable = null;
            Object var8_14 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));){
                bw.write("clusterNo,clon,clat,time,lon,lat");
                bw.newLine();
                int No = 1;
                for (Map.Entry entry : clusters.entrySet()) {
                    ILonLat centroid = entry.getKey();
                    List cluster = entry.getValue();
                    for (ILonLatTime point : cluster) {
                        bw.write(String.format("%d,%.08f,%.08f,%s,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), sdf.format(point.getTimeStamp()), point.getLon(), point.getLat()));
                        bw.newLine();
                    }
                    ++No;
                }
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                } else if (throwable != throwable4) {
                    throwable.addSuppressed(throwable4);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    public static void main__(String[] args) {
        Throwable throwable;
        int k = Integer.parseInt(args[0]);
        File infile = new File(args[1]);
        File outfile = new File(args[2]);
        ArrayList<LonLat> points = new ArrayList<LonLat>();
        try {
            Throwable throwable2 = null;
            throwable = null;
            try (BufferedReader br = new BufferedReader(new FileReader(infile));){
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
                    double lon = Double.parseDouble(tokens[3]);
                    double lat = Double.parseDouble(tokens[2]);
                    points.add(new LonLat(lon, lat));
                }
            }
            catch (Throwable throwable3) {
                if (throwable2 == null) {
                    throwable2 = throwable3;
                } else if (throwable2 != throwable3) {
                    throwable2.addSuppressed(throwable3);
                }
                throw throwable2;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        Map clusters = new Kmeans(k, Kmeans.SPATIALLY_GRID_DISTRIB).clustering(points);
        try {
            throwable = null;
            Object var7_12 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));){
                bw.write("clusterNo,clon,clat,lon,lat");
                bw.newLine();
                int No = 1;
                for (Map.Entry entry : clusters.entrySet()) {
                    ILonLat centroid = entry.getKey();
                    List cluster = entry.getValue();
                    for (ILonLat point : cluster) {
                        bw.write(String.format("%d,%.08f,%.08f,%.08f,%.08f", No, centroid.getLon(), centroid.getLat(), point.getLon(), point.getLat()));
                        bw.newLine();
                    }
                    ++No;
                }
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                } else if (throwable != throwable4) {
                    throwable.addSuppressed(throwable4);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }
}

