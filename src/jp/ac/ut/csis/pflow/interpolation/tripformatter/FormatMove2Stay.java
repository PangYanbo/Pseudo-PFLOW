/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.time.DateUtils
 */
package jp.ac.ut.csis.pflow.interpolation.tripformatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.DateTimeUtils;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.FormatStay2Stay;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.ITripFormatter;
import org.apache.commons.lang3.time.DateUtils;

public class FormatMove2Stay
implements ITripFormatter {
    @Override
    public boolean check(ITrip tripPrev, ITrip tripNext) {
        return tripPrev.getTransportMode() != TransportMode.STAY && tripNext.getTransportMode() == TransportMode.STAY;
    }

    @Override
    public List<ITrip> format(ITrip tripPrev, ITrip tripNext) {
        Move prevMove = (Move)Move.class.cast(tripPrev);
        Stay nextStay = (Stay)Stay.class.cast(tripNext);
        String uid = prevMove.getUid();
        ILonLatTime p0 = prevMove.getEndPoint();
        ILonLatTime p1 = nextStay.getStartPoint();
        double dd = DistanceUtils.distance(p0, p1);
        Date t0s = prevMove.getStartTime();
        Date t0e = prevMove.getEndTime();
        Date t1s = nextStay.getStartTime();
        Date t1e = nextStay.getEndTime();
        long dt = DateTimeUtils.getDuration(t0e, t1s) / 1000L;
        double vel = dd / (double)dt;
        ArrayList<ITrip> trips = new ArrayList<ITrip>();
        List<ILonLatTime> traj = prevMove.getTrajectory();
        double tripLength = tripPrev.getTripDistance();
        double tripVelocity = tripPrev.getAverageVelocity();
        if (tripVelocity < vel) {
            tripVelocity = vel;
        }
        Date midTime = null;
        if (tripLength == 0.0) {
            Stay prevStay = new Stay(uid, t0s, t0e, p0.getLon(), p0.getLat());
            return new FormatStay2Stay().format(prevStay, nextStay);
        }
        int subDuration = (int)(dd / tripVelocity);
        midTime = DateUtils.addSeconds((Date)t0e, (int)subDuration);
        if (!t0e.equals(midTime) || dd > 10.0) {
            traj.add(new LonLatTime(p1.getLon(), p1.getLat(), midTime));
        }
        Move prev = new Move(uid, prevMove.getTransportMode(), traj);
        Stay next = new Stay(uid, midTime, t1e, p1.getLon(), p1.getLat());
        trips.add(prev);
        trips.add(next);
        return trips;
    }
}

