/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.reallocation;

import jp.ac.ut.csis.pflow.geom.LonLat;

public class POI
extends LonLat {
    private static final long serialVersionUID = 7198765881334598718L;
    private String _id;
    private String _name;
    private double _area;

    public POI(String id, double lon, double lat) {
        this(id, null, Double.NaN, lon, lat);
    }

    public POI(String id, String name, double area, double lon, double lat) {
        super(lon, lat);
        this._id = id;
        this._name = name;
        this._area = area;
    }

    public String getId() {
        return this._id;
    }

    public String getName() {
        return this._name;
    }

    public double getArea() {
        return this._area;
    }

    @Override
    public String toString() {
        return String.format("(%S)[id=%s, name=%s, area=%f, lon=%f, lat=%f]", this.getClass().getName(), this.getId(), this.getName(), this.getArea(), this.getLon(), this.getLat());
    }
}

