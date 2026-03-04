/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.res;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.res.Link;

public class Node
extends LonLat {
    private static final long serialVersionUID = -6249142357721556222L;
    String _id;
    List<Link> _inLinks;
    List<Link> _outLinks;

    public Node(String id, double lon, double lat) {
        super(lon, lat);
        this._id = id;
        this._inLinks = new ArrayList<Link>();
        this._outLinks = new ArrayList<Link>();
    }

    public Node(String id) {
        this(id, Double.NaN, Double.NaN);
    }

    public String getNodeID() {
        return this._id;
    }

    public void addInLink(Link link) {
        this._inLinks.add(link);
    }

    public boolean removeInLink(Link link) {
        return this._inLinks.remove(link);
    }

    public List<Link> listInLinks() {
        return this._inLinks;
    }

    public void addOutLink(Link link) {
        this._outLinks.add(link);
    }

    public boolean removeOutLink(Link link) {
        return this._outLinks.remove(link);
    }

    public List<Link> listOutLinks() {
        return this._outLinks;
    }

    public List<Link> listAllLinks() {
        LinkedHashSet<Link> district = new LinkedHashSet<Link>();
        district.addAll(this._inLinks);
        district.addAll(this._outLinks);
        return new ArrayList<Link>(district);
    }

    public boolean isIsolated() {
        return this._outLinks.isEmpty() && this._inLinks.isEmpty();
    }

    public boolean equals(Object obj) {
        return obj instanceof Node && this._id.equals(((Node)Node.class.cast(obj)).getNodeID());
    }

    @Override
    public String toString() {
        return this.getNodeID();
    }

    @Override
    public Node clone() {
        return new Node(this._id, this.getLon(), this.getLat());
    }
}

