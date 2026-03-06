/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vividsolutions.jts.geom.LineString
 *  com.vividsolutions.jts.geom.Point
 *  org.apache.commons.lang.ArrayUtils
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.GeometryUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.OsmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.OsmLink;
import org.apache.commons.lang.ArrayUtils;

public class CsvOsmLoader
extends ACsvNetworkLoader {
    public CsvOsmLoader(File networkfile, boolean hasHeader, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, hasHeader, delimiter);
    }

    public CsvOsmLoader(File networkfile, ICsvNetworkLoader.Delimiter delimiter) {
        super(networkfile, delimiter);
    }

    public CsvOsmLoader(File networkfile, boolean hasHeader) {
        super(networkfile, hasHeader);
    }

    public CsvOsmLoader(File networkfile) {
        super(networkfile);
    }

    public CsvOsmLoader() {
        super(null);
    }

    @Override
    protected Link parseLine(String line) {
        Network network = this.getNetwork();
        boolean needGeom = this.needGeom();
        String[] tokens = this.getTokenizer().reset(line).getTokenArray();
        String gid = tokens[0];
        String src = tokens[8];
        String tgt = tokens[9];
        double spd = (double)Integer.parseInt(tokens[11]) * 1000.0 / 3600.0;
        double len = Double.parseDouble(tokens[10]) * 1000.0;
        double cst = Double.parseDouble(tokens[10]) * 1000.0;
        double rcst = Double.parseDouble(tokens[10]) * 1000.0;
        int clz = Integer.parseInt(tokens[6]);
        String wkb = tokens[18];
        boolean way = false;
        LineString linestring = (LineString)LineString.class.cast(GeometryUtils.parseWKB(wkb));
        Point p0 = linestring.getStartPoint();
        Point p1 = linestring.getEndPoint();
        List<ILonLat> list = needGeom ? GeometryUtils.createPointList(linestring) : null;
        Node n0 = network.hasNode(src) ? network.getNode(src) : new Node(src, p0.getX(), p0.getY());
        Node n1 = network.hasNode(tgt) ? network.getNode(tgt) : new Node(tgt, p1.getX(), p1.getY());
        return new OsmLink(gid, n0, n1, len, cst, rcst, way, clz, spd, list);
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
            int[] types;
            if (!(qc instanceof OsmQueryCondition) || !ArrayUtils.contains((int[])(types = ((OsmQueryCondition)OsmQueryCondition.class.cast(qc)).getRoadTypes()), (int)((OsmLink)OsmLink.class.cast(link)).getRoadClass())) continue;
            return true;
        }
        return false;
    }
}

