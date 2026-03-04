/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ITimeSpan;

public interface ILonLatTimeSpan
extends ILonLat,
ITimeSpan {
    public ILonLatTimeSpan clone();
}

