/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.LonLat;

public class LonLatTime
extends LonLat
implements ILonLatTime,
Comparable<LonLatTime> {
    private static final long serialVersionUID = -8350866770516728682L;
    private Date _timeStamp;

    public LonLatTime(double lon, double lat, Date timeStamp) {
        super(lon, lat);
        this._timeStamp = timeStamp;
    }

    public LonLatTime() {
        this._timeStamp = null;
    }

    @Override
    public Date getTimeStamp() {
        return this._timeStamp;
    }

    @Override
    public void setTimeStamp(Date timeStamp) {
        this._timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return String.format("(%.08f, %.08f, %s)", this.getLon(), this.getLat(), this._timeStamp);
    }

    @Override
    public ILonLatTime clone() {
        Date ts = this._timeStamp != null ? (Date)this._timeStamp.clone() : null;
        return new LonLatTime(this.getLon(), this.getLat(), ts);
    }

    @Override
    public int compareTo(LonLatTime obj) {
        if (obj == null) {
            return 1;
        }
        if (obj.getTimeStamp() == null) {
            return 1;
        }
        if (this == obj) {
            return 0;
        }
        if (this.getTimeStamp() == null) {
            return -1;
        }
        return this.getTimeStamp().compareTo(obj.getTimeStamp());
    }
}

