/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.apache.commons.lang3.ArrayUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import java.io.File;
import java.util.Collections;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.DrmLink;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang3.ArrayUtils;

public class CsvDrmBasicRoadLoader
extends ACsvNetworkLoader {
    public CsvDrmBasicRoadLoader(File networkfile, boolean hasHeader, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvDrmBasicRoadLoader(File networkfile, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, delimiter);
    }

    public CsvDrmBasicRoadLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvDrmBasicRoadLoader(File networkfile) {
        super(networkfile);
    }

    public CsvDrmBasicRoadLoader() {
        super(null);
    }

    @Override
    protected Link parseLine(String line) throws NetworkLoadingException {
        Node n1;
        Node n0;
        double length;
        Network network = this.getNetwork();
        boolean needGeom = this.needGeom();
        String[] tokens = this.getTokenizer().reset(line).getTokenArray();
        String newLinkId = tokens[0];
        int type = Integer.parseInt(tokens[6]);
        double rLength = length = Double.parseDouble(tokens[11]);
        int width = Integer.parseInt(tokens[21]);
        int lanes = Integer.parseInt(tokens[22]);
        int reg = Integer.parseInt(tokens[29]);
        boolean way = DrmLink.isOneway(reg);
        String wkb = tokens[44];
        String src = tokens[41];
        String tgt = tokens[42];
        LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
        Point p0 = linestring.getStartPoint();
        Point p1 = linestring.getEndPoint();
        List<ILonLat> list = GeometryUtils.createPointList(linestring);
        if (DrmLink.isOnewayAndReverse(reg)) {
            n0 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
            Node node = n1 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
            if (list != null && !list.isEmpty()) {
                Collections.reverse(list);
            }
        } else {
            n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
            n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
        }
        return new DrmLink(String.valueOf(newLinkId), n0, n1, length, length, rLength, way, type, width, lanes, needGeom ? list : null);
    }

    @Override
    protected boolean validate(Link link) {
        if (!super.validate(link)) {
            return false;
        }
        List<IQueryCondition> queries = this.listQueries();
        if (queries == null || queries.isEmpty()) {
            return true;
        }
        for (IQueryCondition qc : queries) {
            DrmLink drmLink;
            int[] types;
            if (!(qc instanceof DrmQueryCondition) || !ArrayUtils.contains((int[])(types = ((DrmQueryCondition)DrmQueryCondition.class.cast(qc)).getRoadTypes()), (int)(drmLink = (DrmLink)DrmLink.class.cast(link)).getRoadType())) continue;
            return true;
        }
        return false;
    }
}

