/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.math3.fraction.Fraction
 */
package jp.ac.ut.csis.pflow.geom;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import org.apache.commons.math3.fraction.Fraction;

public class Mesh
implements Serializable {
    private static final long serialVersionUID = -1130751934507725776L;
    public static final Fraction LAT_HEIGHT_MESH1 = new Fraction(2, 3);
    public static final Fraction LNG_WIDTH_MESH1 = new Fraction(1, 1);
    public static final Fraction LAT_HEIGHT_MESH2 = LAT_HEIGHT_MESH1.divide(8);
    public static final Fraction LNG_WIDTH_MESH2 = LNG_WIDTH_MESH1.divide(8);
    public static final Fraction LAT_HEIGHT_MESH3 = LAT_HEIGHT_MESH2.divide(10);
    public static final Fraction LNG_WIDTH_MESH3 = LNG_WIDTH_MESH2.divide(10);
    public static final Fraction LAT_HEIGHT_MESH4 = LAT_HEIGHT_MESH3.divide(2);
    public static final Fraction LNG_WIDTH_MESH4 = LNG_WIDTH_MESH3.divide(2);
    public static final Fraction LAT_HEIGHT_MESH5 = LAT_HEIGHT_MESH4.divide(2);
    public static final Fraction LNG_WIDTH_MESH5 = LNG_WIDTH_MESH4.divide(2);
    public static final Fraction LAT_HEIGHT_MESH6 = LAT_HEIGHT_MESH5.divide(2);
    public static final Fraction LNG_WIDTH_MESH6 = LNG_WIDTH_MESH5.divide(2);
    private Rectangle2D.Double _rect = new Rectangle2D.Double();
    private LonLat _point = new LonLat();
    private String _code = "";
    private int _level = 1;

    public Mesh() {
    }

    public Mesh(String code) {
        this();
        this.setCode(code);
    }

    public Mesh(int level, double lon, double lat) {
        this();
        this._level = level;
        this.setPoint(lon, lat);
    }

    public int getLevel() {
        return this._level;
    }

    public void setLevel(int level) {
        this._level = level;
        if (!this.getCode().isEmpty()) {
            this.updateMeshInfo();
        }
    }

    public String getCode() {
        return this._code;
    }

    public void setCode(String code) {
        this._code = code;
        LonLat pt = this.parseMeshCode(code);
        if (pt == null) {
            return;
        }
        this.setPoint(pt.getLon(), pt.getLat());
        pt.setLocation(this.getCenter());
    }

    public void setPoint(double x, double y) {
        this._point.setLocation(x, y);
        this.updateMeshInfo();
    }

    public LonLat getPoint() {
        return this._point;
    }

    public void setRect(double x, double y, double w, double h) {
        this._rect.setRect(x, y, w, h);
    }

    public Rectangle2D.Double getRect() {
        return this._rect;
    }

    public LonLat getCenter() {
        return new LonLat(this._rect.getCenterX(), this._rect.getCenterY());
    }

    public final Fraction getHeightInDegree() {
        switch (this.getLevel()) {
            default: {
                return LAT_HEIGHT_MESH1;
            }
            case 2: {
                return LAT_HEIGHT_MESH2;
            }
            case 3: {
                return LAT_HEIGHT_MESH3;
            }
            case 4: {
                return LAT_HEIGHT_MESH4;
            }
            case 5: {
                return LAT_HEIGHT_MESH5;
            }
            case 6: 
        }
        return LAT_HEIGHT_MESH6;
    }

    public Fraction getWidthInDegree() {
        switch (this.getLevel()) {
            default: {
                return LNG_WIDTH_MESH1;
            }
            case 2: {
                return LNG_WIDTH_MESH2;
            }
            case 3: {
                return LNG_WIDTH_MESH3;
            }
            case 4: {
                return LNG_WIDTH_MESH4;
            }
            case 5: {
                return LNG_WIDTH_MESH5;
            }
            case 6: 
        }
        return LNG_WIDTH_MESH6;
    }

    public Mesh getUpperMesh() {
        String code = this.getCode();
        switch (this.getLevel()) {
            default: {
                return null;
            }
            case 2: {
                return new Mesh(code.substring(0, 4));
            }
            case 3: {
                return new Mesh(code.substring(0, 6));
            }
            case 4: {
                return new Mesh(code.substring(0, 8));
            }
            case 5: {
                return new Mesh(code.substring(0, 9));
            }
            case 6: 
        }
        return new Mesh(code.substring(0, 10));
    }

    public List<Mesh> listLowerMeshes() {
        String code = this.getCode();
        ArrayList<Mesh> list = new ArrayList<Mesh>();
        switch (this.getLevel()) {
            case 1: {
                int i = 0;
                while (i < 8) {
                    int j = 0;
                    while (j < 8) {
                        list.add(new Mesh(String.format("%s%d%d", code, i, j)));
                        ++j;
                    }
                    ++i;
                }
                break;
            }
            case 2: {
                int i = 0;
                while (i < 10) {
                    int j = 0;
                    while (j < 10) {
                        list.add(new Mesh(String.format("%s%d%d", code, i, j)));
                        ++j;
                    }
                    ++i;
                }
                break;
            }
            case 3: 
            case 4: 
            case 5: {
                int i = 1;
                while (i <= 4) {
                    list.add(new Mesh(String.format("%s%d", code, i)));
                    ++i;
                }
                break;
            }
            default: {
                list = null;
            }
        }
        return list;
    }

    public List<Mesh> list8Neighbors() {
        return this.listNeighbors(8);
    }

    public List<Mesh> list4Neighbors() {
        return this.listNeighbors(4);
    }

    private List<Mesh> listNeighbors(int n) {
        ArrayList<Mesh> list = new ArrayList<Mesh>();
        LonLat cp = this.getCenter();
        Fraction dif_x = this.getWidthInDegree();
        Fraction dif_y = this.getHeightInDegree();
        int level = this.getLevel();
        list.add(new Mesh(level, cp.getLon(), cp.getLat() + dif_y.doubleValue()));
        list.add(new Mesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat()));
        list.add(new Mesh(level, cp.getLon(), cp.getLat() - dif_y.doubleValue()));
        list.add(new Mesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat()));
        if (n == 8) {
            list.add(new Mesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat() + dif_y.doubleValue()));
            list.add(new Mesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat() - dif_y.doubleValue()));
            list.add(new Mesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat() - dif_y.doubleValue()));
            list.add(new Mesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat() + dif_y.doubleValue()));
        }
        return list;
    }

    public boolean is8NeighborOf(Mesh mesh) {
        for (Mesh m : this.list8Neighbors()) {
            if (!mesh.getCode().equals(m.getCode())) continue;
            return true;
        }
        return false;
    }

    public boolean is4NeighborOf(Mesh mesh) {
        for (Mesh m : this.list4Neighbors()) {
            if (!mesh.getCode().equals(m.getCode())) continue;
            return true;
        }
        return false;
    }

    public LonLat getPositionOf(int x, int y) {
        Rectangle2D.Double rect = this.getRect();
        double lon = rect.getMinX() + rect.getWidth() * (double)x / 10000.0;
        double lat = rect.getMinY() + rect.getHeight() * (double)y / 10000.0;
        return new LonLat(lon, lat);
    }

    private void updateMeshInfo() {
        if (this.getLevel() < 0 || this.getLevel() > 6) {
            return;
        }
        double x = 0.0;
        double y = 0.0;
        int lat_1 = (int)(this.getPoint().getLat() * 1.5) % 100;
        int lng_1 = (int)(this.getPoint().getLon() - 100.0);
        if (this.getLevel() >= 1) {
            this.setRect(x += (double)(lng_1 + 100), y += LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue(), LNG_WIDTH_MESH1.doubleValue(), LAT_HEIGHT_MESH1.doubleValue());
            this._code = String.format("%02d%02d", lat_1, lng_1);
        }
        if (this.getLevel() < 2) {
            return;
        }
        int lat_2 = (int)((this.getPoint().getLat() - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue()) / LAT_HEIGHT_MESH2.doubleValue());
        int lng_2 = (int)((this.getPoint().getLon() - (double)(lng_1 + 100)) / LNG_WIDTH_MESH2.doubleValue());
        this.setRect(x += LNG_WIDTH_MESH2.multiply(lng_2).doubleValue(), y += LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue(), LNG_WIDTH_MESH2.doubleValue(), LAT_HEIGHT_MESH2.doubleValue());
        this._code = String.format("%02d%02d%d%d", lat_1, lng_1, lat_2, lng_2);
        if (this.getLevel() < 3) {
            return;
        }
        int lat_3 = (int)((this.getPoint().getLat() - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue()) / LAT_HEIGHT_MESH3.doubleValue());
        int lng_3 = (int)((this.getPoint().getLon() - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue()) / LNG_WIDTH_MESH3.doubleValue());
        this.setRect(x += LNG_WIDTH_MESH3.multiply(lng_3).doubleValue(), y += LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue(), LNG_WIDTH_MESH3.doubleValue(), LAT_HEIGHT_MESH3.doubleValue());
        this._code = String.format("%02d%02d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3);
        if (this.getLevel() < 4) {
            return;
        }
        int lat_4 = (int)((this.getPoint().getLat() - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue()) / LAT_HEIGHT_MESH4.doubleValue());
        int lng_4 = (int)((this.getPoint().getLon() - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue()) / LNG_WIDTH_MESH4.doubleValue());
        this.setRect(x += LNG_WIDTH_MESH4.multiply(lng_4).doubleValue(), y += LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue(), LNG_WIDTH_MESH4.doubleValue(), LAT_HEIGHT_MESH4.doubleValue());
        this._code = String.format("%02d%02d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, this.composeCode(lat_4, lng_4));
        if (this.getLevel() < 5) {
            return;
        }
        int lat_5 = (int)((this.getPoint().getLat() - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue() - LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue()) / LAT_HEIGHT_MESH5.doubleValue());
        int lng_5 = (int)((this.getPoint().getLon() - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue() - LNG_WIDTH_MESH4.multiply(lng_4).doubleValue()) / LNG_WIDTH_MESH5.doubleValue());
        this.setRect(x += LNG_WIDTH_MESH5.multiply(lng_5).doubleValue(), y += LAT_HEIGHT_MESH5.multiply(lat_5).doubleValue(), LNG_WIDTH_MESH5.doubleValue(), LAT_HEIGHT_MESH5.doubleValue());
        this._code = String.format("%02d%02d%d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, this.composeCode(lat_4, lng_4), this.composeCode(lat_5, lng_5));
        if (this.getLevel() < 6) {
            return;
        }
        int lat_6 = (int)((this.getPoint().getLat() - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue() - LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue() - LAT_HEIGHT_MESH5.multiply(lat_5).doubleValue()) / LAT_HEIGHT_MESH6.doubleValue());
        int lng_6 = (int)((this.getPoint().getLon() - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue() - LNG_WIDTH_MESH4.multiply(lng_4).doubleValue() - LNG_WIDTH_MESH5.multiply(lng_5).doubleValue()) / LNG_WIDTH_MESH6.doubleValue());
        this.setRect(x += LNG_WIDTH_MESH6.multiply(lng_6).doubleValue(), y += LAT_HEIGHT_MESH6.multiply(lat_6).doubleValue(), LNG_WIDTH_MESH6.doubleValue(), LAT_HEIGHT_MESH6.doubleValue());
        this._code = String.format("%02d%02d%d%d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, this.composeCode(lat_4, lng_4), this.composeCode(lat_5, lng_5), this.composeCode(lat_6, lng_6));
    }

    private int composeCode(int latIndex, int lonIndex) {
        if (latIndex == 0 && lonIndex == 0) {
            return 1;
        }
        if (latIndex == 0 && lonIndex == 1) {
            return 2;
        }
        if (latIndex == 1 && lonIndex == 0) {
            return 3;
        }
        if (latIndex == 1 && lonIndex == 1) {
            return 4;
        }
        return 0;
    }

    private LonLat parseMeshCode(String meshcode) {
        int n;
        String str = meshcode.replaceAll("-", "");
        int strlen = str.length();
        if (strlen == 0 || strlen > 11) {
            return null;
        }
        double x = 1.0E-10;
        double y = 1.0E-10;
        if (strlen >= 4) {
            y += LAT_HEIGHT_MESH1.multiply(new Integer(str.substring(0, 2)).intValue()).doubleValue();
            x += (double)(100 + new Integer(str.substring(2, 4)));
            this._level = 1;
        }
        if (strlen >= 6) {
            y += LAT_HEIGHT_MESH2.multiply(new Integer(str.substring(4, 5)).intValue()).doubleValue();
            x += LNG_WIDTH_MESH2.multiply(new Integer(str.substring(5, 6)).intValue()).doubleValue();
            this._level = 2;
        }
        if (strlen >= 8) {
            y += LAT_HEIGHT_MESH3.multiply(new Integer(str.substring(6, 7)).intValue()).doubleValue();
            x += LNG_WIDTH_MESH3.multiply(new Integer(str.substring(7, 8)).intValue()).doubleValue();
            this._level = 3;
        }
        if (strlen >= 9) {
            n = new Integer(str.substring(8, 9));
            y += LAT_HEIGHT_MESH4.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH4.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            this._level = 4;
        }
        if (strlen >= 10) {
            n = new Integer(str.substring(9, 10));
            y += LAT_HEIGHT_MESH5.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH5.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            this._level = 5;
        }
        if (strlen >= 11) {
            n = new Integer(str.substring(10, 11));
            y += LAT_HEIGHT_MESH6.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH6.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            this._level = 6;
        }
        return new LonLat(x, y);
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
        LonLat pt = this.getPoint();
        return new Mesh(this.getLevel(), pt.getLon(), pt.getLat());
    }
}

