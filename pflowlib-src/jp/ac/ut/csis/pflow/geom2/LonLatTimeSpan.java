/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom2.ILonLatTimeSpan;
import jp.ac.ut.csis.pflow.geom2.LonLat;

public class LonLatTimeSpan
extends LonLat
implements ILonLatTimeSpan {
    private static final long serialVersionUID = -4267348997515095264L;
    private Date _startTime;
    private Date _endTime;

    public LonLatTimeSpan(double lon, double lat, Date startTime, Date endTime) {
        super(lon, lat);
        this._startTime = startTime;
        this._endTime = endTime;
    }

    public LonLatTimeSpan() {
        this._startTime = null;
        this._endTime = null;
    }

    @Override
    public Date getStartTime() {
        return this._startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        this._startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return this._endTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        this._endTime = endTime;
    }

    @Override
    public long getDuration() {
        return this._startTime == null || this._endTime == null ? -1L : this._endTime.getTime() - this._startTime.getTime();
    }

    @Override
    public String toString() {
        return String.format("(%.08f, %.08f, %s - %s)", this.getLon(), this.getLat(), this.getStartTime(), this.getEndTime());
    }

    @Override
    public ILonLatTimeSpan clone() {
        Date ts = this._startTime != null ? (Date)this._startTime.clone() : null;
        Date te = this._endTime != null ? (Date)this._endTime.clone() : null;
        return new LonLatTimeSpan(this.getLon(), this.getLat(), ts, te);
    }
}

