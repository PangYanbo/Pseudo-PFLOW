/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Collection;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;

public class LonLat
implements Serializable,
Cloneable {
    private static final long serialVersionUID = -4164943786825374186L;
    private double _lon;
    private double _lat;

    public static <T extends LonLat> Rectangle2D.Double makeMBR(Collection<T> set) {
        double xmin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        double ymin = Double.MAX_VALUE;
        double ymax = Double.MIN_VALUE;
        for (LonLat c : set) {
            xmin = Math.min(xmin, c.getLon());
            xmax = Math.max(xmax, c.getLon());
            ymin = Math.min(ymin, c.getLat());
            ymax = Math.max(ymax, c.getLat());
        }
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    public static boolean validate(LonLat position) {
        return LonLat.validate(position.getLon(), position.getLat());
    }

    public static boolean validate(double lon, double lat) {
        return -180.0 <= lon && lon <= 180.0 && -90.0 <= lat && lat <= 90.0;
    }

    public LonLat() {
        this(Double.NaN, Double.NaN);
    }

    public LonLat(double lon, double lat) {
        this._lon = lon;
        this._lat = lat;
    }

    public void setLat(double lat) {
        this._lat = lat;
    }

    public void setLon(double lon) {
        this._lon = lon;
    }

    public double getLat() {
        return this._lat;
    }

    public double getLon() {
        return this._lon;
    }

    public void setLocation(double lon, double lat) {
        this._lon = lon;
        this._lat = lat;
    }

    public void setLocation(LonLat lonlat) {
        this._lon = lonlat.getLon();
        this._lat = lonlat.getLat();
    }

    public boolean isValid() {
        return !Double.isNaN(this._lon) && !Double.isNaN(this._lat);
    }

    public boolean isNorth() {
        return this.getLat() > 0.0;
    }

    public boolean isSouth() {
        return this.getLat() < 0.0;
    }

    public boolean isEast() {
        return this.getLon() > 0.0;
    }

    public boolean isWest() {
        return this.getLon() < 0.0;
    }

    public double distance(LonLat p) {
        return DistanceUtils.distance(p, this);
    }

    public String toString() {
        return String.format("(%.08f,%.08f)", this.getLon(), this.getLat());
    }

    public LonLat clone() {
        return new LonLat(this.getLon(), this.getLat());
    }
}

