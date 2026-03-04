/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom2.LonLatTime;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public class STNetworkPoint
extends LonLatTime {
    private static final long serialVersionUID = -8092822994449356550L;
    private Node _node;
    private Link _link;
    private double _ratio;

    public STNetworkPoint(Date ts, double lon, double lat, Node node, Link link, double ratio) {
        super(lon, lat, ts);
        this._node = node;
        this._link = link;
        this._ratio = ratio;
    }

    public Node getNode() {
        return this._node;
    }

    public Link getLink() {
        return this._link;
    }

    public double getRatio() {
        return this._ratio;
    }
}

