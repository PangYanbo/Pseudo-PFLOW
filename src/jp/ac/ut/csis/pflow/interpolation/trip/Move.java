/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.SegmentMode;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;

public class Move
implements ITrip {
    private String _uid;
    private TransportMode _transportMode;
    private SegmentMode _segmentMode;
    private List<ILonLatTime> _trajectory;

    public Move() {
        this(null, null, null);
    }

    public Move(String uid, List<ILonLatTime> trajectory) {
        this(uid, TransportMode.UNKNOWN, trajectory);
    }

    public Move(String uid, TransportMode transportMode, List<ILonLatTime> trajectory) {
        this._uid = uid;
        this._transportMode = transportMode;
        this._trajectory = trajectory;
    }

    @Override
    public String getUid() {
        return this._uid;
    }

    @Override
    public TransportMode getTransportMode() {
        return this._transportMode;
    }

    @Override
    public SegmentMode getSegmentMode() {
        return this._segmentMode;
    }

    public List<ILonLatTime> getTrajectory() {
        return this._trajectory;
    }

    public void setTrajectory(List<ILonLatTime> trajectory) {
        this._trajectory = trajectory;
    }

    @Override
    public Date getStartTime() {
        return this._trajectory == null || this._trajectory.isEmpty() ? null : this._trajectory.get(0).getTimeStamp();
    }

    @Override
    public void setStartTime(Date startTime) {
        ILonLatTime startPoint = this.getStartPoint();
        if (startPoint != null) {
            startPoint.setTimeStamp(startTime);
        }
    }

    @Override
    public Date getEndTime() {
        int N = this._trajectory == null || this._trajectory.isEmpty() ? -1 : this._trajectory.size();
        return N > 0 ? this._trajectory.get(N - 1).getTimeStamp() : null;
    }

    @Override
    public void setEndTime(Date endTime) {
        ILonLatTime endPoint = this.getEndPoint();
        if (endPoint != null) {
            endPoint.setTimeStamp(endTime);
        }
    }

    @Override
    public long getDuration() {
        Date t0 = this.getStartTime();
        Date t1 = this.getEndTime();
        return t0 == null || t1 == null ? -1L : t1.getTime() - t0.getTime();
    }

    @Override
    public ILonLatTime getStartPoint() {
        return this._trajectory == null || this._trajectory.isEmpty() ? null : this._trajectory.get(0);
    }

    @Override
    public ILonLatTime getEndPoint() {
        int N = this._trajectory == null || this._trajectory.isEmpty() ? -1 : this._trajectory.size();
        return N > 0 ? this._trajectory.get(N - 1) : null;
    }

    @Override
    public double getTripDistance() {
        return TrajectoryUtils.length(this._trajectory);
    }

    @Override
    public double getAverageVelocity() {
        double dist = this.getTripDistance();
        double time = (double)this.getDuration() / 1000.0;
        return dist / time;
    }

    public String toString() {
        return String.format("uid=%s, mode=%s, traj=%s", new Object[]{this.getUid(), this.getTransportMode(), this.getTrajectory()});
    }

    public Move clone() {
        List<ILonLatTime> trajectory = this.getTrajectory();
        if (trajectory == null) {
            return new Move();
        }
        ArrayList<ILonLatTime> points = new ArrayList<ILonLatTime>(trajectory.size());
        for (ILonLatTime point : trajectory) {
            points.add(point.clone());
        }
        return new Move(this.getUid(), this.getTransportMode(), points);
    }
}

