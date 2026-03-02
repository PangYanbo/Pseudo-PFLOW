/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.ILonLatTimeSpan;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLatTimeSpan;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.SegmentMode;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;

public class Stay
implements ITrip {
    private String _uid;
    private ILonLatTimeSpan _lonlatTimespan;

    public Stay() {
        this(null, null);
    }

    public Stay(String uid, Date startTime, Date endTime, double lon, double lat) {
        this(uid, new LonLatTimeSpan(lon, lat, startTime, endTime));
    }

    public Stay(String uid, ILonLatTimeSpan lonlatTimespan) {
        this._uid = uid;
        this._lonlatTimespan = lonlatTimespan;
    }

    @Override
    public String getUid() {
        return this._uid;
    }

    @Override
    public TransportMode getTransportMode() {
        return TransportMode.STAY;
    }

    @Override
    public SegmentMode getSegmentMode() {
        return SegmentMode.STAY;
    }

    @Override
    public ILonLatTime getStartPoint() {
        return new LonLatTime(this._lonlatTimespan.getLon(), this._lonlatTimespan.getLat(), this._lonlatTimespan.getStartTime());
    }

    @Override
    public ILonLatTime getEndPoint() {
        return new LonLatTime(this._lonlatTimespan.getLon(), this._lonlatTimespan.getLat(), this._lonlatTimespan.getEndTime());
    }

    @Override
    public Date getStartTime() {
        return this._lonlatTimespan.getStartTime();
    }

    @Override
    public void setStartTime(Date startTime) {
        this._lonlatTimespan.setStartTime(startTime);
    }

    @Override
    public Date getEndTime() {
        return this._lonlatTimespan.getEndTime();
    }

    @Override
    public void setEndTime(Date endTime) {
        this._lonlatTimespan.setEndTime(endTime);
    }

    @Override
    public long getDuration() {
        return this._lonlatTimespan.getDuration();
    }

    @Override
    public double getTripDistance() {
        return 0.0;
    }

    @Override
    public double getAverageVelocity() {
        return 0.0;
    }

    public String toString() {
        return String.format("uid=%s, mode=%s, point=%s", new Object[]{this.getUid(), this.getTransportMode(), this._lonlatTimespan});
    }

    public Stay clone() {
        return new Stay(this.getUid(), this._lonlatTimespan.clone());
    }
}

