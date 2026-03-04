/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.commons.lang3.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.interpolation.trip.Blank;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;

public class GpsTripParser {
    public static ITrip parse(String line) {
        String[] tokens = StrTokenizer.getTSVInstance((String)line).getTokenArray();
        String uid = tokens[0];
        String mode = tokens[1];
        String ts = tokens[2];
        String te = tokens[3];
        String traj = tokens[4];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TransportMode transport = TransportMode.valueOf(mode);
        Date startTime = null;
        Date endTime = null;
        try {
            startTime = sdf.parse(ts);
            endTime = sdf.parse(te);
        }
        catch (ParseException exp) {
            exp.printStackTrace();
            return null;
        }
        List<ILonLatTime> trajectory = GpsTripParser.parseTrajectory(traj);
        if (transport.equals((Object)TransportMode.STAY) || trajectory.size() == 1) {
            ILonLatTime lonlatTime = trajectory.get(0);
            return new Stay(uid, startTime, endTime, lonlatTime.getLon(), lonlatTime.getLat());
        }
        if (transport.equals((Object)TransportMode.BLANK)) {
            ILonLatTime lonlatTime = trajectory.get(0);
            return new Blank(uid, startTime, endTime, lonlatTime.getLon(), lonlatTime.getLat());
        }
        return new Move(uid, transport, trajectory);
    }

    public static List<ILonLatTime> parseTrajectory(String trajectoryString) {
        String[] strs = StringUtils.split((String)trajectoryString, (char)';');
        ArrayList<ILonLatTime> traj = new ArrayList<ILonLatTime>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] stringArray = strs;
        int n = strs.length;
        int n2 = 0;
        while (n2 < n) {
            String str = stringArray[n2];
            String[] vals = StringUtils.split((String)str, (char)'|');
            try {
                Date t = sdf.parse(vals[1]);
                double x = Double.parseDouble(vals[3]);
                double y = Double.parseDouble(vals[2]);
                if (x != 0.0 && y != 0.0) {
                    traj.add(new LonLatTime(x, y, t));
                }
            }
            catch (ParseException exp) {
                exp.printStackTrace();
            }
            ++n2;
        }
        return traj;
    }

    public static String composeTrajectoryString(ITrip trip) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuffer buf = new StringBuffer();
        if (trip.getTransportMode() != TransportMode.STAY) {
            Move move = (Move)Move.class.cast(trip);
            int n = 1;
            for (ILonLatTime p : move.getTrajectory()) {
                buf.append(String.format(";%d|%s|%s|%s", n++, sdf.format(p.getTimeStamp()), p.getLat(), p.getLon()));
            }
        } else {
            ILonLatTime p = trip.getStartPoint();
            buf.append(String.format(";%d|%s|%s|%s", 1, sdf.format(p.getTimeStamp()), p.getLat(), p.getLon()));
        }
        return buf.substring(1);
    }
}

