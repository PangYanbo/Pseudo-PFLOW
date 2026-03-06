/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.Point
 *  org.apache.commons.lang.ArrayUtils
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing3.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing3.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import jp.ac.ut.csis.pflow.routing3.res.OsmLink;
import org.apache.commons.lang.ArrayUtils;

public class CsvOsmLoader
extends ACsvNetworkLoader {
    public CsvOsmLoader(File networkfile, boolean hasHeader, ACsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvOsmLoader(File networkfile, ACsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, delimiter);
    }

    public CsvOsmLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvOsmLoader(File networkfile) {
        super(networkfile);
    }

    @Override
    protected Link parseLine(Network network, String line, boolean needGeom) {
        String[] tokens = this.getTokenizer().reset(line).getTokenArray();
        String gid = tokens[0];
        String src = tokens[8];
        String tgt = tokens[9];
        double spd = (double)Integer.parseInt(tokens[11]) * 1000.0 / 3600.0;
        double cst = Double.parseDouble(tokens[10]) * 1000.0;
        double rcst = Double.parseDouble(tokens[10]) * 1000.0;
        int clz = Integer.parseInt(tokens[6]);
        String wkb = tokens[18];
        boolean way = false;
        LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
        Point p0 = linestring.getStartPoint();
        Point p1 = linestring.getEndPoint();
        List<LonLat> list = needGeom ? GeometryUtils.createPointList(linestring) : null;
        double length = TrajectoryUtils.length(list);
        Node n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
        Node n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
        return new OsmLink(gid, n0, n1, length, cst, rcst, way, clz, spd, list);
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

