/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import jp.ac.ut.csis.pflow.routing2.res.HighwayNode;
import jp.ac.ut.csis.pflow.routing2.res.Link;

public class HighwayLink
extends Link {
    private static final long serialVersionUID = -2601693176550157031L;
    private int _lineSeq;
    private String _lineName;

    public HighwayLink(String linkID, int lineSeq, String lineName, HighwayNode source, HighwayNode target, double length) {
        super(linkID, source, target, length, false);
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
        return String.format("linkID=%s,lineSeq=%d,lineName=%s, source=%s,target=%s,length=%f", this.getLinkID(), this.getLineSeq(), this.getLineName(), this.getTailNode(), this.getHeadNode(), this.getCost());
    }
}

