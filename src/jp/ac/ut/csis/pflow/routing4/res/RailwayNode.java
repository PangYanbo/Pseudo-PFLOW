/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.res;

import java.util.HashSet;
import java.util.Set;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class RailwayNode
extends Node {
    private static final long serialVersionUID = -4455890106098316483L;
    protected static final int[] COMPANY_CODES_JR = new int[]{11, 100, 101, 102, 103, 104, 105, 106, 107};
    private int _companyCode;
    private int _lineCode;
    private int _lineOrderCode;
    private int _stationOrderCode;
    private int _stationGroupCode;
    private int _stationType;
    private String _companyName;
    private String _lineName;
    private String _stationName;
    private int _prefCode;
    private Set<RailwayNode> _groupNodes;

    public RailwayNode(int companyCode, int lineCode, int stationCode, int stationGroupCode, String companyName, String lineName, String stationName, int stationPref, double lon, double lat) {
        this(companyCode, lineCode, stationCode, -1, -1, stationGroupCode, -1, companyName, lineName, stationName, stationPref, lon, lat);
    }

    public RailwayNode(int companyCode, int lineCode, int stationCode, int lineOrderCode, int stationOrderCode, int stationGroupCode, int stationType, String companyName, String lineName, String stationName, int prefCode, double lon, double lat) {
        super(String.valueOf(stationCode), lon, lat);
        this._companyCode = companyCode;
        this._lineCode = lineCode;
        this._lineOrderCode = lineOrderCode;
        this._stationOrderCode = stationOrderCode;
        this._stationGroupCode = stationGroupCode;
        this._stationType = stationType;
        this._companyName = companyName;
        this._lineName = lineName;
        this._stationName = stationName;
        this._prefCode = prefCode;
        this._groupNodes = new HashSet<RailwayNode>();
    }

    public int getStationCode() {
        return Integer.parseInt(this.getNodeID());
    }

    public int getCompanyCode() {
        return this._companyCode;
    }

    public int getLineCode() {
        return this._lineCode;
    }

    public int getLineOrderCode() {
        return this._lineOrderCode;
    }

    public int getStationOrderCode() {
        return this._stationOrderCode;
    }

    public int getStationGroupCode() {
        return this._stationGroupCode;
    }

    public int getStationType() {
        return this._stationType;
    }

    public String getCompanyName() {
        return this._companyName;
    }

    public String getLineName() {
        return this._lineName;
    }

    public String getStationName() {
        return this._stationName;
    }

    public int getPrefCode() {
        return this._prefCode;
    }

    public Set<RailwayNode> getGroupNodes() {
        return this._groupNodes;
    }

    public void setGroupNodes(Set<RailwayNode> groupNodes) {
        this._groupNodes = groupNodes;
    }

    public boolean isJR() {
        return ArrayUtils.contains((int[])COMPANY_CODES_JR, (int)this.getCompanyCode());
    }

    public boolean isJRNormal() {
        return this.getCompanyCode() == 11;
    }

    public boolean isShinkansen() {
        return this.getCompanyCode() >= 100;
    }

    @Override
    public String toString() {
        Object[] tokens = new String[]{String.format("CompanyCode=%d", this._companyCode), String.format("LineCode=%d", this._lineCode), String.format("StationCode=%s", this.getNodeID()), String.format("LineOrderCode=%d", this._lineOrderCode), String.format("StationOrderCode=%d", this._stationOrderCode), String.format("StationGroupCode=%d", this._stationGroupCode), String.format("StationType=%d", this._stationType), String.format("CompanyName=%s", this._companyName), String.format("LineName=%s", this._lineName), String.format("StationName=%s", this._stationName), String.format("PrefCode=%d", this._prefCode), String.format("LonLat=(%f %f)", this.getLon(), this.getLat())};
        return StringUtils.join((Object[])tokens, (String)", ");
    }

    @Override
    public RailwayNode clone() {
        return new RailwayNode(this.getCompanyCode(), this.getLineCode(), this.getStationCode(), this.getLineOrderCode(), this.getStationOrderCode(), this.getStationGroupCode(), this.getStationType(), this.getCompanyName(), this.getLineName(), this.getStationName(), this.getPrefCode(), this.getLon(), this.getLat());
    }
}

