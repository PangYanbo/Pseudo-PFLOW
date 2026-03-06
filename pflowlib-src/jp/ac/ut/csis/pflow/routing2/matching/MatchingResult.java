/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing2.matching;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import org.apache.commons.lang.StringUtils;

public class MatchingResult {
    private LonLat _inputPoint;
    private LonLat _nearestPoint;
    private Link _nearestLink;
    private double _dist;
    private List<String> _attrs;

    protected MatchingResult(LonLat in, LonLat out, double dist) {
        this(in, out, null, dist, null);
    }

    protected MatchingResult(LonLat in, LonLat out, Link link, double dist) {
        this(in, out, link, dist, null);
    }

    protected MatchingResult(LonLat in, LonLat out, double dist, List<String> attrs) {
        this(in, out, null, dist, attrs);
    }

    protected MatchingResult(LonLat in, LonLat out, Link link, double dist, List<String> attrs) {
        this._dist = dist;
        this._inputPoint = in;
        this._nearestPoint = out;
        this._nearestLink = link;
        this._attrs = attrs;
    }

    public boolean isValid() {
        return this._nearestPoint != null;
    }

    public LonLat getInputPoint() {
        return this._inputPoint;
    }

    public LonLat getNearestPoint() {
        return this._nearestPoint;
    }

    public Link getNearestLink() {
        return this._nearestLink;
    }

    public double getDistance() {
        return this._dist;
    }

    public List<String> getAttributes() {
        return this._attrs;
    }

    public String toResultString() {
        return this.toResultString("\t");
    }

    public String toResultString(String delim) {
        DecimalFormat df = new DecimalFormat("###.######");
        List<String> val = Arrays.asList(df.format(this._inputPoint.getLon()), df.format(this._inputPoint.getLat()), this._nearestPoint != null ? df.format(this._nearestPoint.getLon()) : "", this._nearestPoint != null ? df.format(this._nearestPoint.getLat()) : "", this._nearestPoint != null ? String.valueOf(this._dist) : "", this._nearestLink != null ? this._nearestLink.getLinkID() : "");
        return String.valueOf(StringUtils.join(val, (String)delim)) + (this._attrs != null && !this._attrs.isEmpty() ? String.valueOf(delim) + StringUtils.join(this._attrs, (String)delim) : "");
    }
}

