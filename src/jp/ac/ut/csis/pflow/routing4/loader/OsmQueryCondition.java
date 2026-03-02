/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IOsmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing4.res.OsmLink;

public class OsmQueryCondition
extends QueryCondition
implements IOsmQueryCondition {
    private int[] _roadTypes;

    public OsmQueryCondition() {
        this(null, OsmLink.ROAD_TYPES);
    }

    public OsmQueryCondition(int[] roadTypes) {
        this(null, roadTypes);
    }

    public OsmQueryCondition(double[] rect, int[] roadTypes) {
        this(rect, 3000.0, roadTypes);
    }

    public OsmQueryCondition(double[] rect, double bufSize) {
        this(rect, bufSize, null);
    }

    public OsmQueryCondition(double[] rect, double bufSize, int[] roadTypes) {
        super(rect, bufSize);
        this._roadTypes = roadTypes;
    }

    @Override
    public int[] getRoadTypes() {
        return this._roadTypes;
    }

    @Override
    public IOsmQueryCondition setRoadTypes(int[] roadTypes) {
        this._roadTypes = roadTypes;
        return this;
    }
}

