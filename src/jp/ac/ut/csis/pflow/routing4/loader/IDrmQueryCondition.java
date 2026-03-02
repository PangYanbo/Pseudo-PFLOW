/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;

public interface IDrmQueryCondition
extends IQueryCondition {
    public static final int[] BASIC_ROAD_TYPES_WITHOUT_HIGHWWAY = {3, 4, 5, 6, 7, 9, 0};
    public static final int[] BASIC_ROAD_TYPES_WITH_HIGHWAY = {1, 2, 3, 4, 5, 6, 7};
    public static final int[] WIDE_ROAD_TYPES_WITH_HIGHWAY = {1, 2, 3, 4, 5};
    public static final int[] WIDE_ROAD_TYPES_WITHOUT_HIGHWAY = {3, 4, 5};

    public int[] getRoadTypes();

    public IDrmQueryCondition setRoadTypes(int[] var1);
}

