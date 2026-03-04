/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.awt.geom.Rectangle2D;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;

public class QueryCondition
implements IQueryCondition {
    private Rectangle2D _rect;
    private Rectangle2D _bounds;
    private double _buffer;

    public QueryCondition() {
        this._rect = null;
        this._bounds = null;
        this._buffer = 3000.0;
    }

    public QueryCondition(double x0, double y0, double x1, double y1, double bufSize) {
        this(new double[]{x0, y0, x1, y1}, bufSize);
    }

    public QueryCondition(double[] rect, double bufSize) {
        this.setBounds(rect, bufSize);
    }

    public QueryCondition(Rectangle2D rect, double bufSize) {
        this.setBounds(rect, bufSize);
    }

    public IQueryCondition setBounds(double x0, double y0, double x1, double y1, double bufSize) {
        return this.setBounds(new double[]{x0, y0, x1, y1}, bufSize);
    }

    @Override
    public IQueryCondition setBounds(double[] rect, double bufSize) {
        if (rect == null || rect.length != 4) {
            this._rect = null;
            this._bounds = null;
        } else {
            double xmin = Math.min(rect[0], rect[2]);
            double xmax = Math.max(rect[0], rect[2]);
            double ymin = Math.min(rect[1], rect[3]);
            double ymax = Math.max(rect[1], rect[3]);
            this._rect = new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
            double buf = bufSize * 1.2E-5;
            double xminb = this._rect.getMinX() - buf;
            double xmaxb = this._rect.getMaxX() + buf;
            double yminb = this._rect.getMinY() - buf;
            double ymaxb = this._rect.getMaxY() + buf;
            this._bounds = new Rectangle2D.Double(xminb, yminb, xmaxb - xminb, ymaxb - yminb);
        }
        this._buffer = bufSize;
        return this;
    }

    @Override
    public IQueryCondition setBounds(double[] rect) {
        return this.setBounds(rect, 3000.0);
    }

    @Override
    public IQueryCondition setBounds(Rectangle2D rect, double bufSize) {
        if (rect == null) {
            this._rect = rect;
            this._bounds = null;
        } else {
            this._rect = rect;
            double buf = bufSize * 1.2E-5;
            double xminb = this._rect.getMinX() - buf;
            double xmaxb = this._rect.getMaxX() + buf;
            double yminb = this._rect.getMinY() - buf;
            double ymaxb = this._rect.getMaxY() + buf;
            this._bounds = new Rectangle2D.Double(xminb, yminb, xmaxb - xminb, ymaxb - yminb);
        }
        this._buffer = bufSize;
        return this;
    }

    @Override
    public IQueryCondition setBounds(Rectangle2D rect) {
        return this.setBounds(rect, 3000.0);
    }

    @Override
    public Rectangle2D getBounds() {
        return this._bounds;
    }

    @Override
    public Rectangle2D getRect() {
        return this._rect;
    }

    @Override
    public double getBuffer() {
        return this._buffer;
    }
}

