/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;

public interface IOsmQueryCondition
extends IQueryCondition {
    public int[] getRoadTypes();

    public IOsmQueryCondition setRoadTypes(int[] var1);
}

