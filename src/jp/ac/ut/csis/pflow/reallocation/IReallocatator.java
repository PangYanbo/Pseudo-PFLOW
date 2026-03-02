/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.Polygon
 */
package jp.ac.ut.csis.pflow.reallocation;

import com.vividsolutions.jts.geom.Polygon;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.reallocation.POI;

public interface IReallocatator {
    public POI reallocate(LonLat var1);

    public POI reallocate(LonLat var1, double var2);

    public POI reallocate(Polygon var1);
}

