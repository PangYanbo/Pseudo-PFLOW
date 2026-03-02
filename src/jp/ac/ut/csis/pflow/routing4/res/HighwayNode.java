/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.res;

import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class HighwayNode
extends Node {
    private static final long serialVersionUID = -4342455408699257467L;
    protected static final int[] ON_OFF_TYPES = new int[]{1, 2, 4};
    protected static final int[] SA_PA_TYPES = new int[]{4, 8};
    private int _icSeq;
    private String _roadCode;
    private int _icCode;
    private String _icName;
    private int _typeFlag;
    private int[] _etcCode1;
    private int[] _etcCode2;
    private String _censusCode;
    private String _prefCode;

    public HighwayNode(int icSeq, String nodeNo, String roadCode, int icCode, String icName, int typeFlag, int[] etcCode1, int[] etcCode2, String censusCode, String prefCode, double longitude, double latitude) {
        super(nodeNo, longitude, latitude);
        this._icSeq = icSeq;
        this._roadCode = roadCode;
        this._icCode = icCode;
        this._icName = icName;
        this._typeFlag = typeFlag;
        this._etcCode1 = etcCode1;
        this._etcCode2 = etcCode2;
        this._censusCode = censusCode;
        this._prefCode = prefCode;
    }

    public boolean isCensus() {
        return StringUtils.isNotBlank((String)this._censusCode);
    }

    public boolean canOnOff() {
        return ArrayUtils.contains((int[])ON_OFF_TYPES, (int)this._typeFlag) || this._typeFlag < 0;
    }

    public boolean isSAPA() {
        return ArrayUtils.contains((int[])SA_PA_TYPES, (int)this._typeFlag);
    }

    public int getIcSeq() {
        return this._icSeq;
    }

    public String getRoadCode() {
        return this._roadCode;
    }

    public int getIcCode() {
        return this._icCode;
    }

    public String getIcName() {
        return this._icName;
    }

    public int getTypeFlag() {
        return this._typeFlag;
    }

    public int[] getEtcCode1() {
        return this._etcCode1;
    }

    public int[] getEtcCode2() {
        return this._etcCode2;
    }

    public String getCensusCode() {
        return this._censusCode;
    }

    public String getPrefCode() {
        return this._prefCode;
    }

    @Override
    public String toString() {
        return String.format("id=%s,name=%s,%f,%f", this.getNodeID(), this.getIcName(), this.getLon(), this.getLat());
    }
}

