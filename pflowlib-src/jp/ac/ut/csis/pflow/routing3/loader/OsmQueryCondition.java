/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import jp.ac.ut.csis.pflow.routing2.res.OsmLink;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;

public class OsmQueryCondition
extends QueryCondition {
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

    public int[] getRoadTypes() {
        return this._roadTypes;
    }
}

