/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.mt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.interpolation.mt.TripInterpolationThread;
import jp.ac.ut.csis.pflow.interpolation.trip.GpsTripParser;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.routing4.loader.PgRailwayLoader;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.thread.AMainThread;

public class TripInterpolationMain
extends AMainThread {
    private File _inputFile;
    private File _outputDir;
    private PgLoader _pgLoader;
    private Network _railwayNetwork;

    public static void main(String[] args) {
        int threadNum = Integer.parseInt(args[0]);
        File inputFile = new File(args[1]);
        File outputDir = new File(args[2]);
        File stdoutLog = new File(outputDir, "stdout.log");
        File stderrLog = new File(outputDir, "stderr.log");
        try (PrintStream stdout = new PrintStream(stdoutLog);
             PrintStream stderr = new PrintStream(stderrLog)) {
            System.setOut(stdout);
            System.setErr(stderr);
            long t0 = System.currentTimeMillis();
            new TripInterpolationMain(threadNum, inputFile, outputDir).invoke();
            long t1 = System.currentTimeMillis();
            System.out.printf("duration %.03f (sec)\n", (double)(t1 - t0) / 1000.0);
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    public TripInterpolationMain(int threadNum, File inputFile, File outputDir) {
        super(threadNum);
        this._inputFile = inputFile;
        this._outputDir = outputDir;
    }

    @Override
    protected void doMain() {
        try (BufferedReader br = new BufferedReader(new FileReader(this._inputFile))) {
            String line = null;
            String prev = null;
            int idx = 0;
            ArrayList<ITrip> trips = new ArrayList<ITrip>();
            while ((line = br.readLine()) != null) {
                ITrip trip = GpsTripParser.parse(line);
                String uid = trip.getUid();
                if (prev != null && !prev.equals(uid)) {
                    File outputFile = this.prepraeOutputFile(this._outputDir, prev);
                    this.append(new TripInterpolationThread(this, this._pgLoader, this._railwayNetwork, outputFile, trips, false));
                    if (idx >= this.getThreadNum()) {
                        idx = 0;
                    }
                    trips = new ArrayList();
                }
                trips.add(trip);
                prev = uid;
            }
            File outputFile = this.prepraeOutputFile(this._outputDir, prev);
            this.append(new TripInterpolationThread(this, this._pgLoader, this._railwayNetwork, outputFile, trips, false));
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    private File prepraeOutputFile(File outputDir, String uid) {
        int id = Integer.parseInt(uid);
        File groupDir = new File(outputDir, String.format("%06d", id / 1000));
        if (!groupDir.exists()) {
            groupDir.mkdir();
        }
        return new File(groupDir, String.format("%s.csv", uid));
    }

    @Override
    public void init() {
        super.init();
        this._pgLoader = new PgLoader("localhost", "postgres", "kashiwa64307", "pflowdrm");
        try (Connection con = this._pgLoader.getConnection()) {
            this._railwayNetwork = new PgRailwayLoader().setConnection(con).setTableName("rail.railway_network_v1").load();
        }
        catch (SQLException exp) {
            exp.printStackTrace();
        }
    }

    @Override
    public void close() {
        super.close();
        this._pgLoader.close();
    }
}

