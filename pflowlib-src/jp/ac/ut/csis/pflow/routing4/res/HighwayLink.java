/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.res.HighwayNode;
import jp.ac.ut.csis.pflow.routing4.res.Link;

public class HighwayLink
extends Link {
    private static final long serialVersionUID = 7967786400992118474L;
    private int _lineSeq;
    private String _lineName;

    public HighwayLink(String linkID, int lineSeq, String lineName, HighwayNode source, HighwayNode target, double length, double cost, double revCost) {
        this(linkID, lineSeq, lineName, source, target, length, cost, revCost, null);
    }

    public HighwayLink(String linkID, int lineSeq, String lineName, HighwayNode source, HighwayNode target, double length, double cost, double revCost, List<ILonLat> geom) {
        super(linkID, source, target, length, cost, revCost, false, geom);
        this._lineSeq = lineSeq;
        this._lineName = lineName;
    }

    public int getLineSeq() {
        return this._lineSeq;
    }

    public String getLineName() {
        return this._lineName;
    }

    @Override
    public String toString() {
        return String.format("linkID=%s,lineSeq=%d,lineName=%s, source=%s,target=%s,length=%f", this.getLinkID(), this.getLineSeq(), this.getLineName(), this.getTailNode(), this.getHeadNode(), this.getLength());
    }
}

