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
import jp.ac.ut.csis.pflow.routing2.loader.NetworkLoadException;
import jp.ac.ut.csis.pflow.routing2.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.OsmLink;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.StrTokenizer;

public class CsvOsmWayLoader
extends ACsvNetworkLoader {
    public CsvOsmWayLoader(File networkfile, boolean hasHeader, ACsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvOsmWayLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvOsmWayLoader(File networkfile) {
        super(networkfile);
    }

    @Override
    protected Link parseLine(Network network, String line) throws NetworkLoadException {
        OsmLink link = null;
        try {
            Node n1;
            String[] tokens = this.getDelimiter().equals((Object)ACsvNetworkLoader.Delimiter.CSV) ? StrTokenizer.getCSVInstance((String)line).getTokenArray() : StrTokenizer.getTSVInstance((String)line).getTokenArray();
            String gid = tokens[0];
            String src = tokens[5];
            String tgt = tokens[5];
            double spd = (double)Integer.parseInt(tokens[17]) * 1000.0 / 3600.0;
            double cst = Double.parseDouble(tokens[3]);
            double rcst = Double.parseDouble(tokens[3]);
            int clz = Integer.parseInt(tokens[1]);
            String wkb = tokens[23];
            int oneway = Integer.parseInt(tokens[16]);
            boolean way = oneway > 0;
            LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
            Point p0 = linestring.getStartPoint();
            Point p1 = linestring.getEndPoint();
            List<LonLat> list = GeometryUtils.createPointList(linestring);
            Node n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
            Node node = n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
            if (oneway <= 1) {
                link = new OsmLink(gid, n0, n1, cst, rcst, way, clz, spd, list);
            } else {
                Collections.reverse(list);
                link = new OsmLink(gid, n1, n0, cst, rcst, way, clz, spd, list);
            }
        }
        catch (Exception exp) {
            throw new NetworkLoadException("fail to load network", exp);
        }
        return link;
    }

    @Override
    protected boolean validate(QueryCondition[] qcs, Link link) {
        boolean bounds = super.validate(qcs, link);
        QueryCondition[] queryConditionArray = qcs;
        int n = qcs.length;
        int n2 = 0;
        while (n2 < n) {
            QueryCondition qc = queryConditionArray[n2];
            if (qc instanceof OsmQueryCondition) {
                int[] types = ((OsmQueryCondition)OsmQueryCondition.class.cast(qc)).getRoadTypes();
                if (bounds && ArrayUtils.contains((int[])types, (int)((OsmLink)OsmLink.class.cast(link)).getRoadClass())) {
                    return true;
                }
            }
            ++n2;
        }
        return false;
    }
}

