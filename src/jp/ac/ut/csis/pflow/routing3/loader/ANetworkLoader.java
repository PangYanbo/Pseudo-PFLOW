/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collection;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing3.loader.INetworkLoader;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.res.Network;

public abstract class ANetworkLoader
implements INetworkLoader {
    public static <T extends LonLat> double[] createRect(T point) {
        return ANetworkLoader.createRect(Arrays.asList(point));
    }

    public static <T extends LonLat> double[] createRect(Collection<T> points) {
        Rectangle2D.Double rect = LonLat.makeMBR(points);
        return new double[]{rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY()};
    }

    @Override
    public Network load(double x0, double y0, double x1, double y1) {
        return this.load(new Network(), x0, y0, x1, y1, 3000.0);
    }

    @Override
    public Network load(Network network, double x0, double y0, double x1, double y1) {
        return this.load(network, x0, y0, x1, y1, 3000.0);
    }

    @Override
    public Network load(double[] rect) {
        return this.load(new Network(), rect, 3000.0);
    }

    @Override
    public Network load(Network network, double[] rect) {
        return this.load(network, rect, 3000.0);
    }

    @Override
    public Network load(double x0, double y0, double x1, double y1, double bufSize) {
        return this.load(new Network(), x0, y0, x1, y1, bufSize, false);
    }

    @Override
    public Network load(Network network, double x0, double y0, double x1, double y1, double bufSize) {
        return this.load(network, x0, y0, x1, y1, bufSize, false);
    }

    @Override
    public Network load(double[] rect, double bufSize) {
        return this.load(new Network(), rect, bufSize, false);
    }

    @Override
    public Network load(Network network, double[] rect, double bufSize) {
        return this.load(network, rect, bufSize, false);
    }

    @Override
    public Network load(double x0, double y0, double x1, double y1, boolean needGeom) {
        return this.load(new Network(), x0, y0, x1, y1, 3000.0, needGeom);
    }

    @Override
    public Network load(Network network, double x0, double y0, double x1, double y1, boolean needGeom) {
        return this.load(network, x0, y0, x1, y1, 3000.0, needGeom);
    }

    @Override
    public Network load(double[] rect, boolean needGeom) {
        return this.load(new Network(), rect, 3000.0, needGeom);
    }

    @Override
    public Network load(Network network, double[] rect, boolean needGeom) {
        return this.load(network, rect, 3000.0, needGeom);
    }

    @Override
    public Network load(double x0, double y0, double x1, double y1, double bufSize, boolean needGeom) {
        return this.load(new Network(), new double[]{x0, y0, x1, y1}, bufSize, needGeom);
    }

    @Override
    public Network load(Network network, double x0, double y0, double x1, double y1, double bufSize, boolean needGeom) {
        return this.load(network, new double[]{x0, y0, x1, y1}, bufSize, needGeom);
    }

    @Override
    public Network load(double[] rect, double bufSize, boolean needGeom) {
        return this.load(new Network(), new QueryCondition(rect, bufSize), needGeom);
    }

    @Override
    public Network load(Network network, double[] rect, double bufSize, boolean needGeom) {
        return this.load(network, new QueryCondition(rect, bufSize), needGeom);
    }

    @Override
    public Network load(QueryCondition cond) {
        return this.load(new Network(), new QueryCondition[]{cond});
    }

    @Override
    public Network load(Network network, QueryCondition cond) {
        return this.load(network, new QueryCondition[]{cond});
    }

    @Override
    public Network load(QueryCondition cond, boolean needGeom) {
        return this.load(new QueryCondition[]{cond}, needGeom);
    }

    @Override
    public Network load(Network network, QueryCondition cond, boolean needGeom) {
        return this.load(network, new QueryCondition[]{cond}, needGeom);
    }

    @Override
    public Network load(QueryCondition[] cond) {
        return this.load(cond, true);
    }

    @Override
    public Network load(Network network, QueryCondition[] cond) {
        return this.load(network, cond, true);
    }

    @Override
    public Network load(QueryCondition[] cond, boolean needGeom) {
        return this.load(new Network(), cond, needGeom);
    }

    @Override
    public Network load() {
        QueryCondition[] conds = null;
        return this.load(new Network(), conds);
    }

    @Override
    public Network load(Network network) {
        QueryCondition[] conds = null;
        return this.load(network, conds);
    }
}

