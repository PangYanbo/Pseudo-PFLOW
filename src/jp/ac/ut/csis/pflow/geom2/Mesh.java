/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;

public class Mesh
implements Serializable {
    private static final long serialVersionUID = -7279391629294766466L;
    private Rectangle2D _rect;
    private LonLat _point;
    private String _code;
    private int _level;

    protected Mesh(String code, int level, double lon, double lat, Rectangle2D rect) {
        this._code = code;
        this._level = level;
        this._point = new LonLat(lon, lat);
        this._rect = rect;
    }

    public int getLevel() {
        return this._level;
    }

    public String getCode() {
        return this._code;
    }

    public ILonLat getPoint() {
        return this._point;
    }

    public Rectangle2D getRect() {
        return this._rect;
    }

    public ILonLat getCenter() {
        return new LonLat(this._rect.getCenterX(), this._rect.getCenterY());
    }

    public boolean equals(Object obj) {
        if (obj instanceof Mesh) {
            String code1 = this.getCode().replaceAll("-", "");
            String code2 = ((Mesh)Mesh.class.cast(obj)).getCode().replaceAll("-", "");
            return code1.equals(code2);
        }
        return false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.getClass().getName()).append("[");
        buf.append("code=").append(this.getCode()).append(",");
        buf.append("rect=").append(this.getRect()).append(",");
        buf.append("point=").append(this.getPoint());
        buf.append("]");
        return buf.toString();
    }

    public Mesh clone() {
        ILonLat pt = this.getPoint();
        return new Mesh(this.getCode(), this.getLevel(), pt.getLon(), pt.getLat(), (Rectangle2D)this.getRect().clone());
    }
}

