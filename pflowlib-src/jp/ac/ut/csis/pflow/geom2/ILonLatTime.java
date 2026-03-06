/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ITime;

public interface ILonLatTime
extends ILonLat,
ITime {
    public ILonLatTime clone();
}

