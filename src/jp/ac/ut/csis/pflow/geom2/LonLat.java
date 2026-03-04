/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.io.Serializable;
import jp.ac.ut.csis.pflow.geom2.ILonLat;

public class LonLat
implements ILonLat,
Serializable,
Cloneable {
    private static final long serialVersionUID = -7004655097961499880L;
    private double _lon;
    private double _lat;

    public LonLat() {
        this(Double.NaN, Double.NaN);
    }

    public LonLat(double lon, double lat) {
        this._lon = lon;
        this._lat = lat;
    }

    @Override
    public double getLat() {
        return this._lat;
    }

    @Override
    public double getLon() {
        return this._lon;
    }

    @Override
    public void setLocation(double lon, double lat) {
        this._lon = lon;
        this._lat = lat;
    }

    public String toString() {
        return String.format("(%.08f,%.08f)", this.getLon(), this.getLat());
    }

    public ILonLat clone() {
        return new LonLat(this.getLon(), this.getLat());
    }
}

