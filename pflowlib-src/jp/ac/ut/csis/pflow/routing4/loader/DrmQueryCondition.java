/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IDrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing4.res.DrmLink;

public class DrmQueryCondition
extends QueryCondition
implements IDrmQueryCondition {
    private int[] _roadTypes;

    public DrmQueryCondition() {
        this(null, DrmLink.ROAD_TYPES);
    }

    public DrmQueryCondition(int[] roadTypes) {
        this(null, roadTypes);
    }

    public DrmQueryCondition(double[] rect, int[] roadTypes) {
        this(rect, 3000.0, roadTypes);
    }

    public DrmQueryCondition(double[] rect, double bufSize) {
        this(rect, bufSize, null);
    }

    public DrmQueryCondition(double[] rect, double bufSize, int[] roadTypes) {
        super(rect, bufSize);
        this._roadTypes = roadTypes;
    }

    @Override
    public int[] getRoadTypes() {
        return this._roadTypes;
    }

    @Override
    public IDrmQueryCondition setRoadTypes(int[] roadTypes) {
        this._roadTypes = roadTypes;
        return this;
    }
}

