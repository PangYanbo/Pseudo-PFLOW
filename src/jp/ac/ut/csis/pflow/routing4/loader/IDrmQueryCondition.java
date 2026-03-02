/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;

public interface IDrmQueryCondition
extends IQueryCondition {
    public static final int[] BASIC_ROAD_TYPES_WITHOUT_HIGHWWAY;
    public static final int[] BASIC_ROAD_TYPES_WITH_HIGHWAY;
    public static final int[] WIDE_ROAD_TYPES_WITH_HIGHWAY;
    public static final int[] WIDE_ROAD_TYPES_WITHOUT_HIGHWAY;

    static {
        int[] nArray = new int[7];
        nArray[0] = 3;
        nArray[1] = 4;
        nArray[2] = 5;
        nArray[3] = 6;
        nArray[4] = 7;
        nArray[5] = 9;
        BASIC_ROAD_TYPES_WITHOUT_HIGHWWAY = nArray;
        BASIC_ROAD_TYPES_WITH_HIGHWAY = new int[]{1, 2, 3, 4, 5, 6, 7};
        WIDE_ROAD_TYPES_WITH_HIGHWAY = new int[]{1, 2, 3, 4, 5};
        WIDE_ROAD_TYPES_WITHOUT_HIGHWAY = new int[]{3, 4, 5};
    }

    public int[] getRoadTypes();

    public IDrmQueryCondition setRoadTypes(int[] var1);
}

