/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.commons.lang3.text.StrTokenizer
 *  org.apache.commons.lang3.time.DateUtils
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package jp.ac.ut.csis.pflow.interpolation.tripformatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.interpolation.trip.GpsTripParser;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.FormatMove2Move;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.FormatMove2Stay;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.FormatStay2Move;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.FormatStay2Stay;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.ITripFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TripFormatter {
    private static final Log LOGGER = LogFactory.getLog(TripFormatter.class);
    public static final double DISTANCE_THRESHOLD_FOR_STAY = 10.0;
    private List<ITripFormatter> _formatterList = new ArrayList<ITripFormatter>();

    /*
     * WARNING - void declaration
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static void main(String[] args) throws IOException {
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        System.out.println(infile.getAbsolutePath());
        try (BufferedReader br = Files.newBufferedReader(infile.toPath());
             BufferedWriter bw = Files.newBufferedWriter(outfile.toPath(), new OpenOption[0])) {
            int count = 0;
            StrTokenizer st = StrTokenizer.getCSVInstance();
            String line = null;
            String prev = null;
            ArrayList arrayList = new ArrayList();
            while ((line = br.readLine()) != null) {
                String[] tokens = st.reset(line).getTokenArray();
                String uid = tokens[0];
                String ts = String.valueOf(tokens[2]) + " " + tokens[8];
                String te = String.valueOf(tokens[2]) + " " + tokens[9];
                String mode = tokens[4];
                String traj = tokens[14];
                if (prev != null && !prev.equals(uid)) {
                    System.out.println(prev);
                    ++count;
                    List<List<String>> output = new TripFormatter().evaluate(uid, (List<List<String>>)arrayList);
                    for (List<String> out : output) {
                        bw.write(String.valueOf(prev) + "\t" + StringUtils.join(out, (String)"\t"));
                        bw.newLine();
                    }
                    arrayList = new ArrayList();
                }
                arrayList.add(Arrays.asList(mode, ts, te, traj));
                prev = uid;
            }
            System.out.println(prev);
            ++count;
            List<List<String>> output = new TripFormatter().evaluate(prev, (List<List<String>>)arrayList);
            for (List<String> out : output) {
                bw.write(String.valueOf(prev) + "\t" + StringUtils.join(out, (String)"\t"));
                bw.newLine();
            }
            System.out.println(count);
        }
        catch (IOException exp) {
            LOGGER.error((Object)"error", (Throwable)exp);
        }
    }

    public TripFormatter() {
        this._formatterList.add(new FormatStay2Stay());
        this._formatterList.add(new FormatStay2Move());
        this._formatterList.add(new FormatMove2Stay());
        this._formatterList.add(new FormatMove2Move());
    }

    public List<List<String>> evaluate(String uid, List<List<String>> trips) {
        ArrayList<List<String>> results = new ArrayList<List<String>>();
        Collections.sort(trips, new Comparator<List<String>>(){

            @Override
            public int compare(List<String> a, List<String> b) {
                String ta = String.valueOf(a.get(0)) + " " + a.get(9);
                String tb = String.valueOf(b.get(0)) + " " + b.get(9);
                return ta.compareTo(tb);
            }
        });
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<ITrip> tripSegment = new ArrayList<ITrip>();
        Date datePrevTe = null;
        for (List<String> tripTokens : trips) {
            try {
                String strMode = tripTokens.get(2);
                String strTs = String.valueOf(tripTokens.get(0)) + " " + tripTokens.get(9);
                String strTe = String.valueOf(tripTokens.get(0)) + " " + tripTokens.get(10);
                String strTraj = tripTokens.get(15);
                TransportMode transport = TransportMode.valueOf(strMode);
                Date dateTs = sdf.parse(strTs);
                Date dateTe = sdf.parse(strTe);
                List<ILonLatTime> pointList = GpsTripParser.parseTrajectory(strTraj);
                if (pointList == null || pointList.isEmpty()) continue;
                ITrip trip = null;
                if (transport == TransportMode.STAY) {
                    ILonLatTime psTime = pointList.get(0);
                    trip = new Stay(uid, dateTs, dateTe, psTime.getLon(), psTime.getLat());
                } else {
                    trip = new Move(uid, transport, pointList);
                }
                if (datePrevTe != null && this.diffDate(datePrevTe, dateTs) > 1) {
                    List<ITrip> orgArray2 = this.fillTime(tripSegment);
                    results.addAll(this.formatOutput(orgArray2));
                    tripSegment = new ArrayList();
                }
                tripSegment.add(trip);
                datePrevTe = dateTe;
            }
            catch (ParseException exp) {
                LOGGER.error((Object)"fail to parse data", (Throwable)exp);
            }
        }
        if (tripSegment != null && !tripSegment.isEmpty()) {
            List<ITrip> orgArray1 = this.formatTrips(tripSegment);
            List<ITrip> orgArray2 = this.fillTime(orgArray1);
            results.addAll(this.formatOutput(orgArray2));
        }
        return results;
    }

    private int diffDate(Date date0, Date date1) {
        long t0 = DateUtils.getFragmentInDays((Date)date0, (int)1);
        long t1 = DateUtils.getFragmentInDays((Date)date1, (int)1);
        return (int)(t1 - t0);
    }

    private List<List<String>> formatOutput(List<ITrip> trips) {
        ArrayList<List<String>> output = new ArrayList<List<String>>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (ITrip trip : trips) {
            Date t0 = trip.getStartTime();
            Date t1 = trip.getEndTime();
            output.add(Arrays.asList(trip.getTransportMode().toString(), sdf.format(t0), sdf.format(t1), GpsTripParser.composeTrajectoryString(trip)));
        }
        return output;
    }

    private List<ITrip> formatTrips(List<ITrip> trips) {
        ITrip prev = null;
        ArrayList<ITrip> result = new ArrayList<ITrip>();
        int i = 0;
        while (i < trips.size()) {
            ITrip trip = trips.get(i);
            if (trip.getTransportMode() != TransportMode.STAY && ((Move)Move.class.cast(trip)).getTrajectory().size() == 1) {
                LOGGER.info((Object)"[MOVE]conversion");
                ILonLatTime p = trip.getStartPoint();
                trip = new Stay(trip.getUid(), trip.getStartTime(), trip.getEndTime(), p.getLon(), p.getLat());
            }
            if (prev == null) {
                result.add(trip);
                prev = trip;
            } else {
                List<ITrip> temp = null;
                for (ITripFormatter formatter : this._formatterList) {
                    if (!formatter.check(prev, trip)) continue;
                    temp = formatter.format(prev, trip);
                    break;
                }
                result.remove(result.size() - 1);
                result.addAll(temp);
                prev = (ITrip)result.get(result.size() - 1);
            }
            ++i;
        }
        return result;
    }

    protected List<ITrip> fillTime(List<ITrip> trips) {
        ITrip trip0 = trips.get(0);
        ILonLatTime org = trip0.getStartPoint();
        Date ts0 = DateUtils.setHours((Date)DateUtils.setMinutes((Date)DateUtils.setSeconds((Date)DateUtils.setMilliseconds((Date)org.getTimeStamp(), (int)0), (int)0), (int)0), (int)0);
        Date te0 = null;
        if (trip0.getTransportMode() == TransportMode.STAY) {
            te0 = (Date)Date.class.cast(trip0.getEndTime().clone());
            trip0.setStartTime(ts0);
            trip0.setEndTime(te0);
        } else {
            te0 = (Date)Date.class.cast(trip0.getStartTime().clone());
            trips.add(0, new Stay(trip0.getUid(), ts0, te0, org.getLon(), org.getLat()));
        }
        ITrip trip1 = trips.get(trips.size() - 1);
        ILonLatTime dst = trip1.getEndPoint();
        Date ts1 = null;
        Date te1 = DateUtils.setHours((Date)DateUtils.setMinutes((Date)DateUtils.setSeconds((Date)DateUtils.setMilliseconds((Date)dst.getTimeStamp(), (int)999), (int)59), (int)59), (int)23);
        if (trip1.getTransportMode() == TransportMode.STAY) {
            ts1 = (Date)Date.class.cast(trip1.getStartTime().clone());
            trip1.setStartTime(ts1);
            trip1.setEndTime(te1);
        } else {
            ts1 = (Date)Date.class.cast(trip1.getEndTime().clone());
            trips.add(new Stay(trip1.getUid(), ts1, te1, dst.getLon(), dst.getLat()));
        }
        return trips;
    }
}

