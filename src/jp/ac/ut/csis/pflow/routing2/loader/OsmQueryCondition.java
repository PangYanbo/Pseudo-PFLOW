/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.OsmLink;

public class OsmQueryCondition
extends QueryCondition {
    private int[] _roadTypes;

    public OsmQueryCondition() {
        this(null, OsmLink.ROAD_TYPES);
    }

    public OsmQueryCondition(int[] roadTypes) {
        this(roadTypes, false);
    }

    public OsmQueryCondition(int[] roadTypes, boolean needGeom) {
        this(null, 3000.0, roadTypes, needGeom);
    }

    public OsmQueryCondition(double[] rect, int[] roadTypes) {
        this(rect, 3000.0, roadTypes);
    }

    public OsmQueryCondition(double[] rect, double bufSize, int[] roadTypes) {
        this(rect, bufSize, roadTypes, false);
    }

    public OsmQueryCondition(double[] rect, int[] roadTypes, boolean needGeom) {
        this(rect, 3000.0, roadTypes, needGeom);
    }

    public OsmQueryCondition(double[] rect, boolean needGeom) {
        this(rect, 3000.0, needGeom);
    }

    public OsmQueryCondition(double[] rect, double bufSize, boolean needGeom) {
        this(rect, bufSize, null, needGeom);
    }

    public OsmQueryCondition(double[] rect, double bufSize, int[] roadTypes, boolean needGeom) {
        super(rect, bufSize, needGeom);
        this._roadTypes = roadTypes;
    }

    public int[] getRoadTypes() {
        return this._roadTypes;
    }
}

