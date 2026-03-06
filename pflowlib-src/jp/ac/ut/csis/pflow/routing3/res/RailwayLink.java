/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.RailwayNode;

public class RailwayLink
extends Link {
    private static final long serialVersionUID = -4345835134412171449L;
    private int _lineCode;

    public RailwayLink(String linkID, int lineCode, RailwayNode source, RailwayNode target, double length) {
        this(linkID, lineCode, source, target, length, null);
    }

    public RailwayLink(String linkID, int lineCode, RailwayNode source, RailwayNode target, double length, List<LonLat> geom) {
        super(linkID, source, target, length, length, length, false, geom);
        this._lineCode = lineCode;
    }

    public int getLineCode() {
        return this._lineCode;
    }

    @Override
    public String toString() {
        return String.format("linkID=%s,lineCode=%d,source=%s,target=%s,length=%f", this.getLinkID(), this.getLineCode(), this.getTailNode(), this.getHeadNode(), this.getLength());
    }

    @Override
    public RailwayLink clone(Node tail, Node head) {
        return new RailwayLink(this.getLinkID(), this.getLineCode(), (RailwayNode)RailwayNode.class.cast(tail), (RailwayNode)RailwayNode.class.cast(head), this.getLength(), this.getLineString());
    }
}

