/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.DrmLink;

public class DrmQueryCondition
extends QueryCondition {
    public static final int[] ORDINARY_ROAD_TYPES;
    public static final int[] WIDE_ROAD_TYPES_WITH_HIGHWAY;
    public static final int[] WIDE_ROAD_TYPES_WITHOUT_HIGHWAY;
    private int[] _roadTypes;

    static {
        int[] nArray = new int[7];
        nArray[0] = 3;
        nArray[1] = 4;
        nArray[2] = 5;
        nArray[3] = 6;
        nArray[4] = 7;
        nArray[5] = 9;
        ORDINARY_ROAD_TYPES = nArray;
        WIDE_ROAD_TYPES_WITH_HIGHWAY = new int[]{1, 2, 3, 4, 5};
        WIDE_ROAD_TYPES_WITHOUT_HIGHWAY = new int[]{3, 4, 5};
    }

    public DrmQueryCondition() {
        this(null, DrmLink.ROAD_TYPES);
    }

    public DrmQueryCondition(int[] roadTypes) {
        this(roadTypes, false);
    }

    public DrmQueryCondition(int[] roadTypes, boolean needGeom) {
        this(null, 3000.0, roadTypes, needGeom);
    }

    public DrmQueryCondition(double[] rect, int[] roadTypes) {
        this(rect, 3000.0, roadTypes);
    }

    public DrmQueryCondition(double[] rect, double bufSize, int[] roadTypes) {
        this(rect, bufSize, roadTypes, false);
    }

    public DrmQueryCondition(double[] rect, int[] roadTypes, boolean needGeom) {
        this(rect, 3000.0, roadTypes, needGeom);
    }

    public DrmQueryCondition(double[] rect, boolean needGeom) {
        this(rect, 3000.0, needGeom);
    }

    public DrmQueryCondition(double[] rect, double bufSize, boolean needGeom) {
        this(rect, bufSize, null, needGeom);
    }

    public DrmQueryCondition(double[] rect, double bufSize, int[] roadTypes, boolean needGeom) {
        super(rect, bufSize, needGeom);
        this._roadTypes = roadTypes;
    }

    public int[] getRoadTypes() {
        return this._roadTypes;
    }
}

