/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.ITrip;
import jp.ac.ut.csis.pflow.geom.STPoint;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;

public abstract class ATrip
implements ITrip,
Cloneable {
    private List<STPoint> _trajectory;

    public ATrip() {
        this(null);
    }

    public ATrip(List<STPoint> trajectory) {
        this._trajectory = trajectory;
    }

    public List<STPoint> getTrajectory() {
        return this._trajectory;
    }

    public void setTrajectory(List<STPoint> trajectory) {
        this._trajectory = trajectory;
    }

    @Override
    public Date getStartTime() {
        return this._trajectory == null || this._trajectory.isEmpty() ? null : this._trajectory.get(0).getTimeStamp();
    }

    @Override
    public Date getEndTime() {
        int N = this._trajectory == null || this._trajectory.isEmpty() ? -1 : this._trajectory.size();
        return N > 0 ? this._trajectory.get(N - 1).getTimeStamp() : null;
    }

    @Override
    public long getTripDuration() {
        Date t0 = this.getStartTime();
        Date t1 = this.getEndTime();
        return t0 != null && t1 != null ? STPoint.getDuration(t0, t1) / 1000L : -1L;
    }

    @Override
    public STPoint getStartPoint() {
        return this._trajectory == null || this._trajectory.isEmpty() ? null : this._trajectory.get(0);
    }

    @Override
    public STPoint getEndPoint() {
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
        double time = this.getTripDuration();
        return dist / time;
    }

    public String toString() {
        return String.valueOf(this.getClass().getName()) + "::" + this._trajectory;
    }
}

