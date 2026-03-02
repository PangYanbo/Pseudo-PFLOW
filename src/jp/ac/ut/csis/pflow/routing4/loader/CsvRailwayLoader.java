/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing4.res.RailwayNode;
import org.apache.commons.lang3.StringUtils;

public class CsvRailwayLoader
extends ACsvNetworkLoader {
    public CsvRailwayLoader(File networkfile, boolean hasHeader, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvRailwayLoader(File networkfile, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, delimiter);
    }

    public CsvRailwayLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvRailwayLoader(File networkfile) {
        super(networkfile);
    }

    public CsvRailwayLoader() {
        super(null);
    }

    @Override
    protected Link parseLine(String line) throws NetworkLoadingException {
        Node tgtNode;
        Node srcNode;
        Network network = this.getNetwork();
        boolean needGeom = this.needGeom();
        String[] tokens = this.getTokenizer().reset(line).getTokenArray();
        String linkId = tokens[0];
        int compCode = StringUtils.isNotBlank((String)tokens[1]) ? Integer.parseInt(tokens[1]) : -1;
        String compName = tokens[2];
        int lineCode = StringUtils.isNotBlank((String)tokens[3]) ? Integer.parseInt(tokens[3]) : -1;
        String lineName = tokens[4];
        int source = Integer.parseInt(tokens[5]);
        String srcName = tokens[6];
        int srcGroup = Integer.parseInt(tokens[7]);
        int srcPref = Integer.parseInt(tokens[8]);
        int target = Integer.parseInt(tokens[9]);
        String tgtName = tokens[10];
        int tgtGroup = Integer.parseInt(tokens[11]);
        int tgtPref = Integer.parseInt(tokens[12]);
        double length = Double.parseDouble(tokens[13]);
        String wkb = tokens[14];
        LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
        Point p0 = linestring.getStartPoint();
        Point p1 = linestring.getEndPoint();
        List<ILonLat> geom = GeometryUtils.createPointList(linestring);
        if (!needGeom) {
            geom = null;
        }
        if ((srcNode = network.getNode(String.valueOf(source))) == null) {
            srcNode = new RailwayNode(compCode, lineCode, source, srcGroup, compName, lineName, srcName, srcPref, p0.getX(), p0.getY());
        }
        if ((tgtNode = network.getNode(String.valueOf(target))) == null) {
            tgtNode = new RailwayNode(compCode, lineCode, target, tgtGroup, compName, lineName, tgtName, tgtPref, p1.getX(), p1.getY());
        }
        RailwayLink link = new RailwayLink(String.valueOf(linkId), lineCode, (RailwayNode)srcNode, (RailwayNode)tgtNode, length);
        if (needGeom) {
            link.setLineString(geom);
        }
        return link;
    }

    @Override
    public Network load() throws NetworkLoadingException {
        Network network = super.load();
        return this.updateStationGroup(network);
    }

    private Network updateStationGroup(Network network) {
        TreeMap<Integer, Set<RailwayNode>> groups = new TreeMap<Integer, Set<RailwayNode>>();
        for (Node node : network.listNodes()) {
            RailwayNode ekiNode = (RailwayNode)RailwayNode.class.cast(node);
            int groupCode = ekiNode.getStationGroupCode();
            if (!groups.containsKey(groupCode)) {
                groups.put(groupCode, new HashSet<RailwayNode>());
            }
            groups.get(groupCode).add(ekiNode);
        }
        for (Set<RailwayNode> val : groups.values()) {
            for (RailwayNode ekiNode : val) {
                ekiNode.setGroupNodes(val);
            }
        }
        return network;
    }
}

