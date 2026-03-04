/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.RailNode;

public class RailLink
extends Link {
    private static final long serialVersionUID = 3964249879871051800L;

    public RailLink(String linkid, RailNode tailNode, RailNode headNode, double cost, double revCost, boolean oneway, String compName, String lineName) {
        super(linkid, tailNode, headNode, cost, revCost, oneway);
    }

    public RailLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, String compName, String lineName, List<LonLat> geom) {
        super(linkid, tailNode, headNode, cost, revCost, oneway, geom);
    }

    public boolean isExchagne() {
        RailNode tailNode = (RailNode)RailNode.class.cast(this.getTailNode());
        RailNode headNode = (RailNode)RailNode.class.cast(this.getHeadNode());
        return !tailNode.getLineName().equals(headNode.getLineName());
    }
}

