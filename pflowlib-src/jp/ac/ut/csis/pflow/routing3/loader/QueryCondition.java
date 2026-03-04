/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import java.awt.geom.Rectangle2D;

public class QueryCondition {
    private double[] _rect;
    private double _buffer;

    public QueryCondition() {
        this(null, 3000.0);
    }

    public QueryCondition(double[] rect, double bufSize) {
        this._rect = rect;
        this._buffer = bufSize;
    }

    public Rectangle2D getRects() {
        if (this._rect == null || this._rect.length < 4) {
            return null;
        }
        double xmin = Math.min(this._rect[0], this._rect[2]);
        double xmax = Math.max(this._rect[0], this._rect[2]);
        double ymin = Math.min(this._rect[1], this._rect[3]);
        double ymax = Math.max(this._rect[1], this._rect[3]);
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    public Rectangle2D getBounds() {
        if (this._rect == null || this._rect.length < 4) {
            return null;
        }
        double buf = this._buffer * 1.2E-5;
        double xmin = Math.min(this._rect[0], this._rect[2]) - buf;
        double xmax = Math.max(this._rect[0], this._rect[2]) + buf;
        double ymin = Math.min(this._rect[1], this._rect[3]) - buf;
        double ymax = Math.max(this._rect[1], this._rect[3]) + buf;
        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    public double getBuffer() {
        return this._buffer;
    }
}

