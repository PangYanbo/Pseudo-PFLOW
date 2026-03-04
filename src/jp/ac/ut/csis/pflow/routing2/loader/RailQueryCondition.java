/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;

public class RailQueryCondition
extends QueryCondition {
    private String _compName;
    private String _lineName;

    public RailQueryCondition() {
        this(false);
    }

    public RailQueryCondition(boolean needGeom) {
        this(null, null, needGeom);
    }

    public RailQueryCondition(String compName, String lineName) {
        this(null, 3000.0, compName, lineName, false);
    }

    public RailQueryCondition(String compName, String lineName, boolean needGeom) {
        this(null, 3000.0, compName, lineName, needGeom);
    }

    public RailQueryCondition(double[] rect, boolean needGeom) {
        this(rect, 3000.0, needGeom);
    }

    public RailQueryCondition(double[] rect, double bufSize, boolean needGeom) {
        this(rect, bufSize, null, null, needGeom);
    }

    public RailQueryCondition(double[] rect, double bufSize, String compName, String lineName, boolean needGeom) {
        super(rect, bufSize, needGeom);
        this._compName = compName;
        this._lineName = lineName;
    }

    public String getCompName() {
        return this._compName;
    }

    public String getLineName() {
        return this._lineName;
    }
}

