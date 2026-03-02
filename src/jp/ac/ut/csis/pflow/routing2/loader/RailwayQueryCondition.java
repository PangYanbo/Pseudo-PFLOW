/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;

public class RailwayQueryCondition
extends QueryCondition {
    private int _compCode;
    private String _compName;
    private int _lineCode;
    private String _lineName;

    public RailwayQueryCondition() {
        this(false);
    }

    public RailwayQueryCondition(boolean needGeom) {
        this(null, null, needGeom);
    }

    public RailwayQueryCondition(int compCode, int lineCode) {
        this(null, 3000.0, compCode, lineCode, false);
    }

    public RailwayQueryCondition(String compName, String lineName) {
        this(null, 3000.0, compName, lineName, false);
    }

    public RailwayQueryCondition(int compCode, int lineCode, boolean needGeom) {
        this(null, 3000.0, compCode, lineCode, needGeom);
    }

    public RailwayQueryCondition(String compName, String lineName, boolean needGeom) {
        this(null, 3000.0, compName, lineName, needGeom);
    }

    public RailwayQueryCondition(double[] rect, boolean needGeom) {
        this(rect, 3000.0, needGeom);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, boolean needGeom) {
        this(rect, bufSize, null, null, needGeom);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, int compCode, int lineCode, boolean needGeom) {
        this(rect, bufSize, compCode, null, lineCode, null, needGeom);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, String compName, String lineName, boolean needGeom) {
        this(rect, bufSize, -1, compName, -1, lineName, needGeom);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, int compCode, String compName, int lineCode, String lineName, boolean needGeom) {
        super(rect, bufSize, needGeom);
        this._compCode = compCode;
        this._compName = compName;
        this._lineCode = lineCode;
        this._lineName = lineName;
    }

    public int getCompCode() {
        return this._compCode;
    }

    public String getCompName() {
        return this._compName;
    }

    public int getLineCode() {
        return this._lineCode;
    }

    public String getLineName() {
        return this._lineName;
    }
}

