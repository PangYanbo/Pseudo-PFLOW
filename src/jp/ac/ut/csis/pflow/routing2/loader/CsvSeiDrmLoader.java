/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.LineString
 *  org.locationtech.jts.geom.Point
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import java.io.File;
import java.util.Collections;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.DrmLink;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.StrTokenizer;

public class CsvSeiDrmLoader
extends ACsvNetworkLoader {
    public CsvSeiDrmLoader(File networkfile, boolean hasHeader, ACsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvSeiDrmLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvSeiDrmLoader(File networkfile) {
        super(networkfile);
    }

    @Override
    protected Link parseLine(Network network, String line) {
        Node n1;
        Node n0;
        String[] tokens = this.getDelimiter().equals((Object)ACsvNetworkLoader.Delimiter.CSV) ? StrTokenizer.getCSVInstance((String)line).getTokenArray() : StrTokenizer.getTSVInstance((String)line).getTokenArray();
        String gid = tokens[0];
        int type = Integer.parseInt(tokens[4]);
        double cst = Double.parseDouble(tokens[6]);
        double rcst = Double.parseDouble(tokens[6]);
        int width = Integer.parseInt(tokens[7]);
        int lanes = Integer.parseInt(tokens[8]);
        int reg = Integer.parseInt(tokens[9]);
        boolean way = DrmLink.isOneway(reg);
        String wkb = tokens[23];
        String src = tokens[25];
        String tgt = tokens[26];
        LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
        Point p0 = linestring.getStartPoint();
        Point p1 = linestring.getEndPoint();
        List<LonLat> list = GeometryUtils.createPointList(linestring);
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
        return new DrmLink(String.valueOf(gid), n0, n1, cst, rcst, way, type, width, lanes, list);
    }

    @Override
    protected boolean validate(QueryCondition[] qcs, Link link) {
        boolean bounds = super.validate(qcs, link);
        QueryCondition[] queryConditionArray = qcs;
        int n = qcs.length;
        int n2 = 0;
        while (n2 < n) {
            QueryCondition qc = queryConditionArray[n2];
            if (qc instanceof DrmQueryCondition) {
                int[] types = ((DrmQueryCondition)DrmQueryCondition.class.cast(qc)).getRoadTypes();
                if (bounds && ArrayUtils.contains((int[])types, (int)((DrmLink)DrmLink.class.cast(link)).getRoadType())) {
                    return true;
                }
            }
            ++n2;
        }
        return false;
    }
}

