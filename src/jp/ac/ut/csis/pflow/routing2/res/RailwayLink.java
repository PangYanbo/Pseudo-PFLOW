/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.RailwayNode;

public class RailwayLink
extends Link {
    private static final long serialVersionUID = -4590157452230664642L;
    private int _lineCode;

    public RailwayLink(String linkID, int lineCode, RailwayNode source, RailwayNode target, double length) {
        super(linkID, source, target, length, false);
        this._lineCode = lineCode;
    }

    public int getLineCode() {
        return this._lineCode;
    }

    @Override
    public String toString() {
        return String.format("linkID=%s,lineCode=%d,source=%s,target=%s,length=%f", this.getLinkID(), this.getLineCode(), this.getTailNode(), this.getHeadNode(), this.getCost());
    }
}

