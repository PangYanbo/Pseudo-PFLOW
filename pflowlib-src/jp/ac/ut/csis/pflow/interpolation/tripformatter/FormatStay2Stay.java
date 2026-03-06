/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.time.DateUtils
 */
package jp.ac.ut.csis.pflow.interpolation.tripformatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.DateTimeUtils;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.ITripFormatter;
import org.apache.commons.lang.time.DateUtils;

public class FormatStay2Stay
implements ITripFormatter {
    @Override
    public boolean check(ITrip tripPrev, ITrip tripNext) {
        return tripPrev.getTransportMode() == TransportMode.STAY && tripNext.getTransportMode() == TransportMode.STAY;
    }

    @Override
    public List<ITrip> format(ITrip tripPrev, ITrip tripNext) {
        String uid = tripPrev.getUid();
        ILonLatTime p0 = tripPrev.getEndPoint();
        ILonLatTime p1 = tripNext.getStartPoint();
        double dd = DistanceUtils.distance(p0, p1);
        LonLat pc = new LonLat((p0.getLon() + p1.getLon()) / 2.0, (p0.getLat() + p1.getLat()) / 2.0);
        Date t0s = tripPrev.getStartTime();
        Date t0e = tripPrev.getEndTime();
        Date t1s = tripNext.getStartTime();
        Date t1e = tripNext.getEndTime();
        long dt = DateTimeUtils.getDuration(t0e, t1s) / 1000L;
        ArrayList<ITrip> trips = new ArrayList<ITrip>();
        if (dd < 200.0) {
            trips.add(new Stay(uid, t0s, t1e, pc.getLon(), pc.getLat()));
        } else if (dd >= 200.0) {
            Date t0 = t0e;
            Date t1 = t1s;
            if (dt == 0L) {
                double time = (int)(dd / DEFAULT_VELOCITY);
                t0 = DateUtils.addSeconds((Date)t0, (int)((int)(-time / 2.0)));
                t1 = DateUtils.addSeconds((Date)t1, (int)((int)(time / 2.0)));
                p0.setTimeStamp(t0);
                p1.setTimeStamp(t1);
            }
            ArrayList<ILonLatTime> traj = new ArrayList<ILonLatTime>();
            traj.add(new LonLatTime(p0.getLon(), p0.getLat(), t0));
            traj.add(new LonLatTime(p1.getLon(), p1.getLat(), t1));
            Move mid = new Move(uid, TransportMode.UNKNOWN, traj);
            trips.add(tripPrev);
            trips.add(mid);
            trips.add(tripNext);
        }
        return trips;
    }
}

