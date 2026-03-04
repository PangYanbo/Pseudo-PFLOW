/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.reallocation;

import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.reallocation.IReallocatator;
import jp.ac.ut.csis.pflow.reallocation.POI;

public abstract class AReallocator
implements IReallocatator {
    public static final double DEFAULT_RADIUS = 1000.0;
    private Method _method;

    protected AReallocator() {
        this(Method.RANDOM);
    }

    protected AReallocator(Method method) {
        this._method = method;
    }

    public Method getMethod() {
        return this._method;
    }

    public void setMethod(Method method) {
        this._method = method;
    }

    @Override
    public POI reallocate(LonLat point) {
        return this.reallocate(point, 1000.0);
    }

    public static enum Method {
        RANDOM,
        AREA_PROB;

    }
}

