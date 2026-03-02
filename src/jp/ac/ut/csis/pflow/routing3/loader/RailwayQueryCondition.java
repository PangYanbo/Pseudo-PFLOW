/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;

public class RailwayQueryCondition
extends QueryCondition {
    private int _compCode;
    private String _compName;
    private int _lineCode;
    private String _lineName;

    public RailwayQueryCondition() {
        this(null, null);
    }

    public RailwayQueryCondition(int compCode, int lineCode) {
        this(null, 3000.0, compCode, lineCode);
    }

    public RailwayQueryCondition(String compName, String lineName) {
        this(null, 3000.0, compName, lineName);
    }

    public RailwayQueryCondition(double[] rect) {
        this(rect, 3000.0);
    }

    public RailwayQueryCondition(double[] rect, double bufSize) {
        this(rect, bufSize, null, null);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, int compCode, int lineCode) {
        this(rect, bufSize, compCode, null, lineCode, null);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, String compName, String lineName) {
        this(rect, bufSize, -1, compName, -1, lineName);
    }

    public RailwayQueryCondition(double[] rect, double bufSize, int compCode, String compName, int lineCode, String lineName) {
        super(rect, bufSize);
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

