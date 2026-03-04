/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.matching;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.matching.IMatching;
import jp.ac.ut.csis.pflow.routing2.matching.MatchingResult;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class MapMatching
implements IMatching {
    @Override
    public <T extends LonLat> List<MatchingResult> runMatching(Network network, List<T> points) {
        return this.runMatching(network, points, 3000.0);
    }

    @Override
    public <T extends LonLat> List<MatchingResult> runMatching(Network network, List<T> points, double range) {
        ArrayList<MatchingResult> result = new ArrayList<MatchingResult>(points.size());
        for (LonLat point : points) {
            result.add(this.runMatching(network, point, range));
        }
        return result;
    }

    @Override
    public <T extends LonLat> MatchingResult runMatching(Network network, T point) {
        return this.runMatching(network, point, 3000.0);
    }

    @Override
    public <T extends LonLat> MatchingResult runMatching(Network network, T point, double range) {
        return this.runMatchingToLink(network, point, range);
    }

    public <T extends LonLat> MatchingResult runMatchingToLink(Network network, T point, double range) {
        List<Link> links = network.queryLink(point.getLon(), point.getLat(), range);
        double distance = Double.MAX_VALUE;
        Link nearestLink = null;
        LonLat nearestPoint = null;
        for (Link candidate : links) {
            double d;
            LonLat p = DistanceUtils.nearestPoint(candidate.getLineString(), point);
            if (p == null || !((d = DistanceUtils.distance(p, point)) < distance)) continue;
            nearestLink = candidate;
            nearestPoint = p;
            distance = d;
        }
        return new MatchingResult(point, nearestPoint, nearestLink, distance);
    }

    public <T extends LonLat> MatchingResult runMatchingToNode(Network network, T point, double range) {
        List<Node> nodes = network.queryNode(point.getLon(), point.getLat(), range);
        double distance = Double.MAX_VALUE;
        Node nearestPoint = null;
        for (Node candidate : nodes) {
            double d = DistanceUtils.distance(candidate, point);
            if (!(d < distance)) continue;
            nearestPoint = candidate;
            distance = d;
        }
        return new MatchingResult(point, nearestPoint, distance);
    }
}

