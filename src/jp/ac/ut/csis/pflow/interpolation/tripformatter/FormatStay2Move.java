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

public class FormatStay2Move
implements ITripFormatter {
    @Override
    public boolean check(ITrip tripPrev, ITrip tripNext) {
        return tripPrev.getTransportMode() == TransportMode.STAY && tripNext.getTransportMode() != TransportMode.STAY;
    }

    @Override
    public List<ITrip> format(ITrip tripPrev, ITrip tripNext) {
        Stay prevStay = (Stay)Stay.class.cast(tripPrev);
        Move nextMove = (Move)Move.class.cast(tripNext);
        String uid = tripPrev.getUid();
        ILonLatTime p0 = prevStay.getEndPoint();
        ILonLatTime p1 = nextMove.getStartPoint();
        double dd = DistanceUtils.distance(p0, p1);
        Date t0s = prevStay.getStartTime();
        Date t0e = prevStay.getEndTime();
        Date t1s = nextMove.getStartTime();
        Date t1e = nextMove.getEndTime();
        long dt = DateTimeUtils.getDuration(t0e, t1s) / 1000L;
        double vel = dd / (double)dt;
        ArrayList<ITrip> trips = new ArrayList<ITrip>();
        List<ILonLatTime> traj = nextMove.getTrajectory();
        double tripLength = tripNext.getTripDistance();
        double tripVelocity = tripNext.getAverageVelocity();
        if (tripVelocity < vel) {
            tripVelocity = vel;
        }
        Date midTime = null;
        if (tripLength == 0.0) {
            Stay nextStay = new Stay(nextMove.getUid(), t1s, t1e, p1.getLon(), p1.getLat());
            return new FormatStay2Stay().format(prevStay, nextStay);
        }
        int subDuration = (int)(dd / tripVelocity);
        int stayDuration = (int)(dt - (long)subDuration);
        midTime = DateUtils.addSeconds((Date)t0e, (int)stayDuration);
        Stay prev = new Stay(prevStay.getUid(), t0s, midTime, p0.getLon(), p0.getLat());
        if (!midTime.equals(t1s) || dd > 10.0) {
            traj.add(0, new LonLatTime(p0.getLon(), p0.getLat(), midTime));
        }
        Move next = new Move(uid, nextMove.getTransportMode(), traj);
        trips.add(prev);
        trips.add(next);
        return trips;
    }
}

