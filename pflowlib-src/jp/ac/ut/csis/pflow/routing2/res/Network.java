/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.Envelope
 *  com.vividsolutions.jts.geom.Geometry
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.Point
 *  com.vividsolutions.jts.geom.Polygon
 *  com.vividsolutions.jts.geom.prep.PreparedGeometry
 *  com.vividsolutions.jts.geom.prep.PreparedGeometryFactory
 *  com.vividsolutions.jts.index.quadtree.Quadtree
 */
package jp.ac.ut.csis.pflow.routing2.res;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class Network {
    private static final double APPROX_1KM = 1.2E-5;
    private Map<String, Node> _nodes = new HashMap<String, Node>();
    private Map<String, Link> _links = new HashMap<String, Link>();
    private Quadtree _nodeIndex;
    private Quadtree _linkIndex;

    public Network() {
        this(true, true);
    }

    public Network(boolean makeNodeIndex, boolean makeLinkIndex) {
        this._nodeIndex = makeNodeIndex ? new Quadtree() : null;
        this._linkIndex = makeLinkIndex ? new Quadtree() : null;
    }

    public boolean isEmpty() {
        return this._nodes.isEmpty() || this._links.isEmpty();
    }

    public List<Node> listNodes() {
        return new ArrayList<Node>(this._nodes.values());
    }

    public void addLink(Link link) {
        Node tail = link.getTailNode();
        Node head = link.getHeadNode();
        if (!this._nodes.containsKey(tail.getNodeID())) {
            this._nodes.put(tail.getNodeID(), tail);
            this.addIndex(tail);
        }
        if (!this._nodes.containsKey(head.getNodeID())) {
            this._nodes.put(head.getNodeID(), head);
            this.addIndex(head);
        }
        if (!this._links.containsKey(link.getLinkID())) {
            this._links.put(link.getLinkID(), link);
            this.addIndex(link);
        }
    }

    public void addNode(Node node) {
        if (!this._nodes.containsKey(node.getNodeID())) {
            this._nodes.put(node.getNodeID(), node);
            this.addIndex(node);
        }
    }

    private void addIndex(Node node) {
        if (this._nodeIndex != null && node.isValid()) {
            Point point = GeometryUtils.createPoint(node.getLon(), node.getLat());
            PreparedGeometry prepgeom = PreparedGeometryFactory.prepare((Geometry)point);
            Envelope envelope = prepgeom.getGeometry().getEnvelopeInternal();
            this._nodeIndex.insert(envelope, (Object)node);
        }
    }

    private void removeIndex(Node node) {
        if (this._nodeIndex != null && node.isValid()) {
            Point point = GeometryUtils.createPoint(node.getLon(), node.getLat());
            PreparedGeometry prepgeom = PreparedGeometryFactory.prepare((Geometry)point);
            Envelope envelope = prepgeom.getGeometry().getEnvelopeInternal();
            this._nodeIndex.remove(envelope, (Object)node);
        }
    }

    public List<Node> query(double x0, double y0, double x1, double y1) {
        return this.queryNode(x0, y0, x1, y1);
    }

    public List<Node> queryNode(double x0, double y0, double x1, double y1) {
        if (this._nodeIndex == null) {
            ArrayList<Node> nodes = new ArrayList<Node>();
            Polygon bounds = GeometryUtils.createPolygon((LonLat[])new LonLat[]{new LonLat(x0, y0), new LonLat(x0, y1), new LonLat(x1, y1), new LonLat(x1, y0), new LonLat(x0, y0)});
            for (Node node : this.listNodes()) {
                Point point = GeometryUtils.createPoint(node.getLon(), node.getLat());
                if (!bounds.intersects((Geometry)point)) continue;
                nodes.add(node);
            }
            return nodes;
        }
        Envelope search = new Envelope(x0, x1, y0, y1);
        List nodes = this._nodeIndex.query(search);
        return nodes;
    }

    public List<Node> query(double x, double y, double r) {
        return this.queryNode(x, y, r);
    }

    public List<Node> queryNode(double x, double y, double r) {
        double w = r * 1.2E-5;
        double h = r * 1.2E-5;
        LonLat cntr = new LonLat(x, y);
        List<Node> nodes = this.queryNode(x - w, y - h, x + w, y + h);
        Iterator<Node> itr = nodes.iterator();
        while (itr.hasNext()) {
            Node node = itr.next();
            if (!(DistanceUtils.distance(cntr, node) > r)) continue;
            itr.remove();
        }
        return nodes;
    }

    private void addIndex(Link link) {
        if (this._linkIndex != null) {
            LineString linestring = null;
            if (link.hasGeometry()) {
                linestring = GeometryUtils.createLineString(link.getLineString());
            }
            if (linestring != null) {
                PreparedGeometry prepgeom = PreparedGeometryFactory.prepare((Geometry)linestring);
                Envelope envelope = prepgeom.getGeometry().getEnvelopeInternal();
                this._linkIndex.insert(envelope, (Object)link);
            }
        }
    }

    private void removeIndex(Link link) {
        if (this._linkIndex != null) {
            LineString linestring = null;
            if (link.hasGeometry()) {
                linestring = GeometryUtils.createLineString(link.getLineString());
            }
            if (linestring != null) {
                PreparedGeometry prepgeom = PreparedGeometryFactory.prepare((Geometry)linestring);
                Envelope envelope = prepgeom.getGeometry().getEnvelopeInternal();
                this._linkIndex.remove(envelope, (Object)link);
            }
        }
    }

    public List<Link> queryLink(double x0, double y0, double x1, double y1) {
        if (this._linkIndex == null) {
            ArrayList<Link> links = new ArrayList<Link>();
            Polygon bounds = GeometryUtils.createPolygon((LonLat[])new LonLat[]{new LonLat(x0, y0), new LonLat(x0, y1), new LonLat(x1, y1), new LonLat(x1, y0), new LonLat(x0, y0)});
            for (Link link : this.listLinks()) {
                LineString line = GeometryUtils.createLineString(link.getLineString());
                if (!bounds.intersects((Geometry)line)) continue;
                links.add(link);
            }
            return links;
        }
        Envelope search = new Envelope(x0, x1, y0, y1);
        List links = this._linkIndex.query(search);
        return links;
    }

    public List<Link> queryLink(double x, double y, double r) {
        double w = r * 1.2E-5;
        double h = r * 1.2E-5;
        LonLat cntr = new LonLat(x, y);
        List<Link> links = this.queryLink(x - w, y - h, x + w, y + h);
        Iterator<Link> itr = links.iterator();
        while (itr.hasNext()) {
            Link link = itr.next();
            if (!(DistanceUtils.distance(link.getLineString(), cntr) > r)) continue;
            itr.remove();
        }
        return links;
    }

    public List<Link> listLinks() {
        return new ArrayList<Link>(this._links.values());
    }

    public void clear() {
        this._links.clear();
        this._nodes.clear();
        if (this._nodeIndex != null) {
            this._nodeIndex = new Quadtree();
        }
        if (this._linkIndex != null) {
            this._linkIndex = new Quadtree();
        }
    }

    public void remove(Node node) {
        for (Link link : node.listInLinks()) {
            link.getTailNode().removeOutLink(link);
            this.removeIndex(link);
            this._links.remove(link.getLinkID());
        }
        for (Link link : node.listOutLinks()) {
            link.getHeadNode().removeInLink(link);
            this.removeIndex(link);
            this._links.remove(link.getLinkID());
        }
        this._nodes.remove(node.getNodeID());
        this.removeIndex(node);
    }

    public void remove(Link link) {
        Node head = link.getHeadNode();
        head.removeInLink(link);
        if (!link.isOneWay()) {
            head.removeOutLink(link);
        }
        if (head.isIsolated()) {
            this._nodes.remove(head.getNodeID());
            this.removeIndex(head);
        }
        Node tail = link.getTailNode();
        tail.removeOutLink(link);
        if (!link.isOneWay()) {
            tail.removeInLink(link);
        }
        if (tail.isIsolated()) {
            this._nodes.remove(tail.getNodeID());
            this.removeIndex(tail);
        }
        this._links.remove(link.getLinkID());
        this.removeIndex(link);
    }

    public Node getNode(String id) {
        return this._nodes.get(id);
    }

    public boolean hasNode(String id) {
        return this._nodes.containsKey(id);
    }

    public Link getLink(String id) {
        return this._links.get(id);
    }

    public Link getLink(Node tail, Node head) {
        for (Link link : tail.listOutLinks()) {
            Node h = link.getHeadNode();
            Node t = link.getTailNode();
            if (!h.equals(head) && (!t.equals(head) || link.isOneWay())) continue;
            return link;
        }
        return null;
    }
}

