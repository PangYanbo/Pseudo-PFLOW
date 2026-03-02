/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.math.LongRange
 *  org.apache.commons.lang.math.Range
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom.LonLat;
import org.apache.commons.lang.math.LongRange;
import org.apache.commons.lang.math.Range;

public class STPoint
extends LonLat
implements Comparable<STPoint> {
    private static final long serialVersionUID = -6096951274573950900L;
    private Date _dtstart;
    private Date _dtend;

    public static long getDuration(Date ts, Date te) {
        return ts == null || te == null ? -1L : te.getTime() - ts.getTime();
    }

    public STPoint(Date dtstart, Date dtend, double lon, double lat) {
        super(lon, lat);
        this.setTimeSpan(dtstart, dtend);
    }

    public STPoint(Date ts, double lon, double lat) {
        super(lon, lat);
        this.setTimeStamp(ts);
    }

    public STPoint() {
    }

    public Date getTimeStamp() {
        return this.isTimeStamp() ? this._dtstart : null;
    }

    public Date getDtStart() {
        return this._dtstart;
    }

    public Date getDtEnd() {
        return this._dtend;
    }

    public void setTimeStamp(Date time) {
        this.setTimeSpan(time, time);
    }

    public void setTimeSpan(Date dtstart, Date dtend) {
        this._dtstart = dtstart;
        this._dtend = dtend;
    }

    public void setValues(Date time, double lon, double lat) {
        this.setTimeStamp(time);
        this.setLocation(lon, lat);
    }

    public void setValues(Date dtstart, Date dtend, double lon, double lat) {
        this.setTimeSpan(dtstart, dtend);
        this.setLocation(lon, lat);
    }

    public boolean isTimeStamp() {
        return this._dtstart != null && this._dtend != null && this._dtstart.equals(this._dtend);
    }

    public boolean isTimeSpan() {
        return this._dtstart != null && this._dtend != null && !this._dtstart.equals(this._dtend);
    }

    public long getDuration() {
        return STPoint.getDuration(this._dtstart, this._dtend);
    }

    public boolean intersects(Date ts) {
        return this._dtstart != null && this._dtend != null && ts != null && new LongRange(this._dtstart.getTime(), this._dtend.getTime()).containsLong(ts.getTime());
    }

    public boolean intersects(Date ts, Date te) {
        return this._dtstart != null && this._dtend != null && ts != null && te != null && new LongRange(this._dtstart.getTime(), this._dtend.getTime()).overlapsRange((Range)new LongRange(ts.getTime(), te.getTime()));
    }

    @Override
    public int compareTo(STPoint p) {
        Date t0 = this.isTimeStamp() ? this.getTimeStamp() : this.getDtStart();
        Date t1 = p.isTimeStamp() ? p.getTimeStamp() : p.getDtStart();
        return t0.compareTo(t1);
    }

    @Override
    public String toString() {
        if (this.isTimeSpan()) {
            return String.format("%s - %s (%f,%f)", this._dtstart, this._dtend, this.getLon(), this.getLat());
        }
        return String.format("%s (%f,%f)", this._dtstart, this.getLon(), this.getLat());
    }

    @Override
    public STPoint clone() {
        return new STPoint((Date)Date.class.cast(this._dtstart.clone()), (Date)Date.class.cast(this._dtend.clone()), this.getLon(), this.getLat());
    }
}

