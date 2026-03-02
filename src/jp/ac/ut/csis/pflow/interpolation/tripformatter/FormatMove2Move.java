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
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.ITripFormatter;
import org.apache.commons.lang3.time.DateUtils;

public class FormatMove2Move
implements ITripFormatter {
    @Override
    public boolean check(ITrip tripPrev, ITrip tripNext) {
        return tripPrev.getTransportMode() != TransportMode.STAY && tripNext.getTransportMode() != TransportMode.STAY;
    }

    @Override
    public List<ITrip> format(ITrip tripPrev, ITrip tripNext) {
        Move prevMove = (Move)Move.class.cast(tripPrev);
        Move nextMove = (Move)Move.class.cast(tripNext);
        String uid = prevMove.getUid();
        ILonLatTime p0 = prevMove.getEndPoint();
        ILonLatTime p1 = nextMove.getStartPoint();
        double dd = DistanceUtils.distance(p0, p1);
        LonLat pc = new LonLat((p0.getLon() + p1.getLon()) / 2.0, (p0.getLat() + p1.getLat()) / 2.0);
        Date t0e = prevMove.getEndTime();
        Date t1s = nextMove.getStartTime();
        long dt0 = prevMove.getDuration() / 1000L;
        long dt1 = nextMove.getDuration() / 1000L;
        long dt = DateTimeUtils.getDuration(t0e, t1s) / 1000L;
        double vel = dd / (double)dt;
        ArrayList<ITrip> trips = new ArrayList<ITrip>();
        if (dt < 3600L) {
            Date tc = DateUtils.addSeconds((Date)t0e, (int)((int)(dt / 2L)));
            List<ILonLatTime> traj0 = prevMove.getTrajectory();
            if (dd > 0.0) {
                traj0.add(new LonLatTime(pc.getLon(), pc.getLat(), tc));
            }
            trips.add(new Move(uid, prevMove.getTransportMode(), traj0));
            List<ILonLatTime> traj1 = nextMove.getTrajectory();
            if (dd > 0.0) {
                traj1.add(0, new LonLatTime(pc.getLon(), pc.getLat(), tc));
            }
            trips.add(new Move(uid, nextMove.getTransportMode(), traj1));
        } else if (dt >= 3600L) {
            List<ILonLatTime> traj0 = prevMove.getTrajectory();
            double length0 = TrajectoryUtils.length(traj0);
            Date tc0 = null;
            if (length0 == 0.0) {
                tc0 = (Date)Date.class.cast(t0e.clone());
            } else {
                double velocity0 = length0 / (double)dt0;
                if (velocity0 < vel) {
                    velocity0 = vel;
                }
                int duration0 = (int)(DistanceUtils.distance(p0, pc) / velocity0);
                tc0 = DateUtils.addSeconds((Date)t0e, (int)duration0);
            }
            if (!t0e.equals(tc0)) {
                traj0.add(new LonLatTime(pc.getLon(), pc.getLat(), tc0));
            }
            Move prev = new Move(uid, prevMove.getTransportMode(), traj0);
            List<ILonLatTime> traj1 = nextMove.getTrajectory();
            double length1 = TrajectoryUtils.length(traj1);
            Date tc1 = null;
            if (length1 == 0.0) {
                tc1 = (Date)Date.class.cast(t1s.clone());
            } else {
                double velocity1 = length1 / (double)dt1;
                if (velocity1 < vel) {
                    velocity1 = vel;
                }
                int duration1 = (int)(DistanceUtils.distance(pc, p1) / velocity1);
                tc1 = DateUtils.addSeconds((Date)t1s, (int)(-duration1));
            }
            if (!t1s.equals(tc1)) {
                traj1.add(0, new LonLatTime(pc.getLon(), pc.getLat(), tc1));
            }
            Move next = new Move(uid, nextMove.getTransportMode(), traj1);
            trips.add(prev);
            if (!tc0.equals(tc1)) {
                Stay midStay = new Stay(uid, tc0, tc1, pc.getLon(), pc.getLat());
                trips.add(midStay);
            }
            trips.add(next);
        }
        return trips;
    }
}

