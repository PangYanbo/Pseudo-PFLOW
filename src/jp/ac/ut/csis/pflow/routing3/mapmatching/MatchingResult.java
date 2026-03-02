/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing3.mapmatching;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import org.apache.commons.lang.StringUtils;

public class MatchingResult {
    private LonLat _inputPoint;
    private LonLat _nearestPoint;
    private Link _nearestLink;
    private double _dist;
    private double _ratio;
    private List<String> _attrs;

    protected MatchingResult(LonLat in, LonLat out, double dist) {
        this(in, out, dist, null);
    }

    protected MatchingResult(LonLat in, LonLat out, Link link, double dist, double ratio) {
        this(in, out, link, dist, ratio, null);
    }

    protected MatchingResult(LonLat in, LonLat out, double dist, List<String> attrs) {
        this(in, out, null, dist, Double.NaN, attrs);
    }

    protected MatchingResult(LonLat in, LonLat out, Link link, double dist, double ratio, List<String> attrs) {
        this._inputPoint = in;
        this._nearestPoint = out;
        this._nearestLink = link;
        this._dist = dist;
        this._ratio = ratio;
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

    public double getRatio() {
        return this._ratio;
    }

    public List<String> getAttributes() {
        return this._attrs;
    }

    public String toResultString() {
        return this.toResultString("\t");
    }

    public String toResultString(String delim) {
        DecimalFormat df = new DecimalFormat("###.######");
        List<String> val = Arrays.asList(df.format(this._inputPoint.getLon()), df.format(this._inputPoint.getLat()), this.isValid() ? df.format(this._nearestPoint.getLon()) : "", this.isValid() ? df.format(this._nearestPoint.getLat()) : "", this.isValid() ? String.valueOf(this._dist) : "", this.isValid() && this._nearestLink != null ? String.valueOf(this._ratio) : "", this.isValid() && this._nearestLink != null ? this._nearestLink.getLinkID() : "");
        return String.valueOf(StringUtils.join(val, (String)delim)) + (this._attrs != null && !this._attrs.isEmpty() ? String.valueOf(delim) + StringUtils.join(this._attrs, (String)delim) : "");
    }
}

