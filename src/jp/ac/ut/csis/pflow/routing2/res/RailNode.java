/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import jp.ac.ut.csis.pflow.routing2.res.Node;

public class RailNode
extends Node {
    private static final long serialVersionUID = 3387384334842446905L;
    private String _compName;
    private String _lineName;
    private String _stationName;

    public RailNode(String id, double lon, double lat, String stationName) {
        this(id, lon, lat, null, null, stationName);
    }

    public RailNode(String id, String stationName) {
        this(id, null, null, stationName);
    }

    public RailNode(String id, double lon, double lat, String compName, String lineName, String stationName) {
        super(id, lon, lat);
        this._compName = compName;
        this._lineName = lineName;
        this._stationName = stationName;
    }

    public RailNode(String id, String compName, String lineName, String stationName) {
        super(id);
        this._compName = compName;
        this._lineName = lineName;
        this._stationName = stationName;
    }

    public String getStationName() {
        return this._stationName;
    }

    public String getCompanyName() {
        return this._compName;
    }

    public String getLineName() {
        return this._lineName;
    }

    @Override
    public String toString() {
        return String.format("%s (%s %s %s)", super.toString(), this.getCompanyName(), this.getLineName(), this.getStationName());
    }
}

