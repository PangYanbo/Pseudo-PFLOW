/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.clustering2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;

public class Cluster<T extends ILonLat>
implements Serializable {
    private static final long serialVersionUID = -644810731225128192L;
    private List<T> _points = new ArrayList<T>();

    public void addPoint(T point) {
        this._points.add(point);
    }

    public List<T> listPoints() {
        return this._points;
    }
}

