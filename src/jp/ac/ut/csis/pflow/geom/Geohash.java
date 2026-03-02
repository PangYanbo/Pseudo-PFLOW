/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import jp.ac.ut.csis.pflow.geom.LonLat;

public class Geohash
implements Serializable {
    private static final long serialVersionUID = -7305033420122736673L;
    private static final double[] LAT_RANGE = new double[]{-90.0, 90.0};
    private static final double[] LON_RANGE = new double[]{-180.0, 180.0};
    private static final String DIGITS = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static int GEOHASH_LENGTH = 7;
    private String _geohash;
    private LonLat _lonlat;
    private Rectangle2D.Double _rect;

    public static void setDefaultGeohashLength(int hashLength) {
        if (hashLength <= 0) {
            throw new IllegalArgumentException("unavailable value: " + hashLength);
        }
        GEOHASH_LENGTH = hashLength;
    }

    public static int getDefaultGeohashLength() {
        return GEOHASH_LENGTH;
    }

    public Geohash(int precision, double lon, double lat) {
        this.encode(precision, lon, lat);
    }

    public Geohash(double lon, double lat) {
        this(GEOHASH_LENGTH, lon, lat);
    }

    public Geohash(String geohash) {
        this.decode(geohash);
    }

    public String getGeohash() {
        return this._geohash;
    }

    public int getGeohashLength() {
        return this._geohash.length();
    }

    public LonLat getLonLat() {
        return this._lonlat;
    }

    public Rectangle2D.Double getRect() {
        return this._rect;
    }

    public LonLat getCenter() {
        return new LonLat(this._rect.getCenterX(), this._rect.getCenterY());
    }

    private void encode(int length, double lon, double lat) {
        if (length <= 0 || lon < LON_RANGE[0] || LON_RANGE[1] < lon || lat < LAT_RANGE[0] || LAT_RANGE[1] < lat) {
            throw new IllegalArgumentException("unavailable value");
        }
        int len = length * 5;
        double[] x = new double[]{LON_RANGE[0], LON_RANGE[1]};
        double[] y = new double[]{LAT_RANGE[0], LAT_RANGE[1]};
        int idx = 0;
        int buf = 0;
        StringBuffer hash = new StringBuffer();
        int i = 0;
        while (i < len) {
            double mid;
            if (i % 2 == 0) {
                mid = (x[0] + x[1]) / 2.0;
                if (lon <= mid) {
                    buf <<= 1;
                    x[1] = mid;
                } else {
                    buf = buf << 1 | 1;
                    x[0] = mid;
                }
            } else {
                mid = (y[0] + y[1]) / 2.0;
                if (lat <= mid) {
                    buf <<= 1;
                    y[1] = mid;
                } else {
                    buf = buf << 1 | 1;
                    y[0] = mid;
                }
            }
            if (++idx == 5) {
                hash.append(DIGITS.charAt(buf));
                idx = 0;
                buf = 0;
            }
            ++i;
        }
        this._geohash = hash.toString();
        this._lonlat = new LonLat(lon, lat);
        this._rect = new Rectangle2D.Double(x[0], y[0], x[1] - x[0], y[1] - y[0]);
    }

    private void decode(String hash) {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("unavailable value");
        }
        int len = hash.length();
        double[] x = new double[]{LON_RANGE[0], LON_RANGE[1]};
        double[] y = new double[]{LAT_RANGE[0], LAT_RANGE[1]};
        int idx = 0;
        int i = 0;
        while (i < len) {
            char c = hash.charAt(i);
            int dig = DIGITS.indexOf(c);
            int mask = 16;
            int j = 0;
            while (j < 5) {
                if (idx % 2 == 0) {
                    if ((dig & mask) == 0) {
                        x[1] = (x[0] + x[1]) / 2.0;
                    } else {
                        x[0] = (x[0] + x[1]) / 2.0;
                    }
                } else if ((dig & mask) == 0) {
                    y[1] = (y[0] + y[1]) / 2.0;
                } else {
                    y[0] = (y[0] + y[1]) / 2.0;
                }
                mask >>= 1;
                ++idx;
                ++j;
            }
            ++i;
        }
        this._geohash = hash;
        this._rect = new Rectangle2D.Double(x[0], y[0], x[1] - x[0], y[1] - y[0]);
        this._lonlat = new LonLat(this._rect.getCenterX(), this._rect.getCenterY());
    }

    public String toString() {
        return String.format("%s,%f,%f", this._geohash, this._lonlat.getLon(), this._lonlat.getLat());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Geohash) {
            String code1 = this.getGeohash();
            String code2 = ((Geohash)Geohash.class.cast(obj)).getGeohash();
            return code1.equals(code2);
        }
        return false;
    }

    public Geohash clone() {
        LonLat pt = this.getLonLat();
        return new Geohash(this.getGeohashLength(), pt.getLon(), pt.getLat());
    }
}

