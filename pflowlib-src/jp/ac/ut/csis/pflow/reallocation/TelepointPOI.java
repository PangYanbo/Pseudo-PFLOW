/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.reallocation;

import jp.ac.ut.csis.pflow.reallocation.POI;

public class TelepointPOI
extends POI {
    private static final long serialVersionUID = -7264152470976720029L;

    protected TelepointPOI(String id, String zonecode, double lon, double lat) {
        super(id, zonecode, Double.NaN, lon, lat);
    }

    public String getZoneCode() {
        return this.getName();
    }

    @Override
    public String toString() {
        return String.format("(%S)[id=%s, name=%s, lon=%f, lat=%f]", this.getClass().getName(), this.getId(), this.getZoneCode(), this.getLon(), this.getLat());
    }
}

