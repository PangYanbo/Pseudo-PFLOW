/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.math.fraction.Fraction
 */
package jp.ac.ut.csis.pflow.geom2;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.Mesh;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import org.apache.commons.math.fraction.Fraction;

public class MeshUtils {
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

    public static void main(String[] args) {
        Mesh mesh0 = MeshUtils.createNiesMesh(4, 139.7743793168338, 35.707964910915045);
        System.out.println(mesh0);
    }

    public static Mesh getUpperMesh(Mesh mesh) {
        String code = mesh.getCode();
        switch (mesh.getLevel()) {
            default: {
                return null;
            }
            case 2: {
                return MeshUtils.createMesh(code.substring(0, 4));
            }
            case 3: {
                return MeshUtils.createMesh(code.substring(0, 6));
            }
            case 4: {
                return MeshUtils.createMesh(code.substring(0, 8));
            }
            case 5: {
                return MeshUtils.createMesh(code.substring(0, 9));
            }
            case 6: 
        }
        return MeshUtils.createMesh(code.substring(0, 10));
    }

    public static List<Mesh> listLowerMeshes(Mesh mesh) {
        String code = mesh.getCode();
        ArrayList<Mesh> list = new ArrayList<Mesh>();
        switch (mesh.getLevel()) {
            case 1: {
                int i = 0;
                while (i < 8) {
                    int j = 0;
                    while (j < 8) {
                        list.add(MeshUtils.createMesh(String.format("%s%d%d", code, i, j)));
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
                        list.add(MeshUtils.createMesh(String.format("%s%d%d", code, i, j)));
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
                    list.add(MeshUtils.createMesh(String.format("%s%d", code, i)));
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

    public static List<Mesh> list8Neighbors(Mesh mesh) {
        return MeshUtils.listNeighbors(mesh, 8);
    }

    public static List<Mesh> list4Neighbors(Mesh mesh) {
        return MeshUtils.listNeighbors(mesh, 4);
    }

    private static List<Mesh> listNeighbors(Mesh mesh, int n) {
        ArrayList<Mesh> list = new ArrayList<Mesh>();
        ILonLat cp = mesh.getCenter();
        int level = mesh.getLevel();
        Fraction dif_x = MeshUtils.getWidthInDegree(level);
        Fraction dif_y = MeshUtils.getHeightInDegree(level);
        list.add(MeshUtils.createMesh(level, cp.getLon(), cp.getLat() + dif_y.doubleValue()));
        list.add(MeshUtils.createMesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat()));
        list.add(MeshUtils.createMesh(level, cp.getLon(), cp.getLat() - dif_y.doubleValue()));
        list.add(MeshUtils.createMesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat()));
        if (n == 8) {
            list.add(MeshUtils.createMesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat() + dif_y.doubleValue()));
            list.add(MeshUtils.createMesh(level, cp.getLon() + dif_x.doubleValue(), cp.getLat() - dif_y.doubleValue()));
            list.add(MeshUtils.createMesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat() - dif_y.doubleValue()));
            list.add(MeshUtils.createMesh(level, cp.getLon() - dif_x.doubleValue(), cp.getLat() + dif_y.doubleValue()));
        }
        return list;
    }

    private static Fraction getHeightInDegree(int level) {
        switch (level) {
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

    private static Fraction getWidthInDegree(int level) {
        switch (level) {
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

    public static boolean is8NeighborOf(Mesh mesh0, Mesh mesh1) {
        for (Mesh m : MeshUtils.list8Neighbors(mesh0)) {
            if (!mesh1.getCode().equals(m.getCode())) continue;
            return true;
        }
        return false;
    }

    public static boolean is4NeighborOf(Mesh mesh0, Mesh mesh1) {
        for (Mesh m : MeshUtils.list4Neighbors(mesh0)) {
            if (!mesh1.getCode().equals(m.getCode())) continue;
            return true;
        }
        return false;
    }

    public static Mesh createMesh(int level, double lon, double lat) {
        if (level < 0 || 6 < level || !TrajectoryUtils.validateLonLat(lon, lat)) {
            return null;
        }
        String code = null;
        double x = 0.0;
        double y = 0.0;
        Rectangle2D.Double rect = new Rectangle2D.Double();
        int lat_1 = (int)(lat * 1.5) % 100;
        int lng_1 = (int)(lon - 100.0);
        code = String.format("%02d%02d", lat_1, lng_1);
        ((Rectangle2D)rect).setRect(x += (double)(lng_1 + 100), y += LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue(), LNG_WIDTH_MESH1.doubleValue(), LAT_HEIGHT_MESH1.doubleValue());
        if (level == 1) {
            return new Mesh(code, level, lon, lat, rect);
        }
        int lat_2 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue()) / LAT_HEIGHT_MESH2.doubleValue());
        int lng_2 = (int)((lon - (double)(lng_1 + 100)) / LNG_WIDTH_MESH2.doubleValue());
        code = String.format("%02d%02d%d%d", lat_1, lng_1, lat_2, lng_2);
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_MESH2.multiply(lng_2).doubleValue(), y += LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue(), LNG_WIDTH_MESH2.doubleValue(), LAT_HEIGHT_MESH2.doubleValue());
        if (level == 2) {
            return new Mesh(code, level, lon, lat, rect);
        }
        int lat_3 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue()) / LAT_HEIGHT_MESH3.doubleValue());
        int lng_3 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue()) / LNG_WIDTH_MESH3.doubleValue());
        code = String.format("%02d%02d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3);
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_MESH3.multiply(lng_3).doubleValue(), y += LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue(), LNG_WIDTH_MESH3.doubleValue(), LAT_HEIGHT_MESH3.doubleValue());
        if (level == 3) {
            return new Mesh(code, level, lon, lat, rect);
        }
        int lat_4 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue()) / LAT_HEIGHT_MESH4.doubleValue());
        int lng_4 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue()) / LNG_WIDTH_MESH4.doubleValue());
        code = String.format("%02d%02d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, MeshUtils.composeCode(lat_4, lng_4));
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_MESH4.multiply(lng_4).doubleValue(), y += LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue(), LNG_WIDTH_MESH4.doubleValue(), LAT_HEIGHT_MESH4.doubleValue());
        if (level == 4) {
            return new Mesh(code, level, lon, lat, rect);
        }
        int lat_5 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue() - LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue()) / LAT_HEIGHT_MESH5.doubleValue());
        int lng_5 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue() - LNG_WIDTH_MESH4.multiply(lng_4).doubleValue()) / LNG_WIDTH_MESH5.doubleValue());
        code = String.format("%02d%02d%d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, MeshUtils.composeCode(lat_4, lng_4), MeshUtils.composeCode(lat_5, lng_5));
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_MESH5.multiply(lng_5).doubleValue(), y += LAT_HEIGHT_MESH5.multiply(lat_5).doubleValue(), LNG_WIDTH_MESH5.doubleValue(), LAT_HEIGHT_MESH5.doubleValue());
        if (level == 5) {
            return new Mesh(code, level, lon, lat, rect);
        }
        int lat_6 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue() - LAT_HEIGHT_MESH4.multiply(lat_4).doubleValue() - LAT_HEIGHT_MESH5.multiply(lat_5).doubleValue()) / LAT_HEIGHT_MESH6.doubleValue());
        int lng_6 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue() - LNG_WIDTH_MESH4.multiply(lng_4).doubleValue() - LNG_WIDTH_MESH5.multiply(lng_5).doubleValue()) / LNG_WIDTH_MESH6.doubleValue());
        code = String.format("%02d%02d%d%d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, MeshUtils.composeCode(lat_4, lng_4), MeshUtils.composeCode(lat_5, lng_5), MeshUtils.composeCode(lat_6, lng_6));
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_MESH6.multiply(lng_6).doubleValue(), y += LAT_HEIGHT_MESH6.multiply(lat_6).doubleValue(), LNG_WIDTH_MESH6.doubleValue(), LAT_HEIGHT_MESH6.doubleValue());
        return new Mesh(code, level, lon, lat, rect);
    }

    public static Mesh createNiesMesh(int level, double lon, double lat) {
        if (level < 0 || 4 < level || !TrajectoryUtils.validateLonLat(lon, lat)) {
            return null;
        }
        String code = null;
        double x = 0.0;
        double y = 0.0;
        Rectangle2D.Double rect = new Rectangle2D.Double();
        if (1 <= level && level <= 3) {
            return MeshUtils.createMesh(level, lon, lat);
        }
        int lat_1 = (int)(lat * 1.5) % 100;
        int lng_1 = (int)(lon - 100.0);
        y += LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue();
        x += (double)(lng_1 + 100);
        int lat_2 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue()) / LAT_HEIGHT_MESH2.doubleValue());
        int lng_2 = (int)((lon - (double)(lng_1 + 100)) / LNG_WIDTH_MESH2.doubleValue());
        y += LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue();
        x += LNG_WIDTH_MESH2.multiply(lng_2).doubleValue();
        int lat_3 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue()) / LAT_HEIGHT_MESH3.doubleValue());
        int lng_3 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue()) / LNG_WIDTH_MESH3.doubleValue());
        y += LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue();
        x += LNG_WIDTH_MESH3.multiply(lng_3).doubleValue();
        Fraction LAT_HEIGHT_NIES_MESH4 = LAT_HEIGHT_MESH3.divide(10);
        Fraction LNG_WIDTH_NIES_MESH4 = LNG_WIDTH_MESH3.divide(10);
        int lat_4 = (int)((lat - LAT_HEIGHT_MESH1.multiply(lat_1).doubleValue() - LAT_HEIGHT_MESH2.multiply(lat_2).doubleValue() - LAT_HEIGHT_MESH3.multiply(lat_3).doubleValue()) / LAT_HEIGHT_NIES_MESH4.doubleValue());
        int lng_4 = (int)((lon - (double)(lng_1 + 100) - LNG_WIDTH_MESH2.multiply(lng_2).doubleValue() - LNG_WIDTH_MESH3.multiply(lng_3).doubleValue()) / LNG_WIDTH_NIES_MESH4.doubleValue());
        code = String.format("%02d%02d%d%d%d%d%d%d", lat_1, lng_1, lat_2, lng_2, lat_3, lng_3, lat_4, lng_4);
        ((Rectangle2D)rect).setRect(x += LNG_WIDTH_NIES_MESH4.multiply(lng_4).doubleValue(), y += LAT_HEIGHT_NIES_MESH4.multiply(lat_4).doubleValue(), LNG_WIDTH_NIES_MESH4.doubleValue(), LAT_HEIGHT_NIES_MESH4.doubleValue());
        if (level == 4) {
            return new Mesh(code, level, lon, lat, rect);
        }
        return null;
    }

    private static int composeCode(int latIndex, int lonIndex) {
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

    public static Mesh createMesh(String meshcode) {
        int n;
        String str = meshcode.replaceAll("-", "");
        int strlen = str.length();
        if (strlen == 0 || strlen > 11) {
            return null;
        }
        int level = 0;
        double x = 1.0E-10;
        double y = 1.0E-10;
        if (strlen >= 4) {
            y += LAT_HEIGHT_MESH1.multiply(new Integer(str.substring(0, 2)).intValue()).doubleValue();
            x += (double)(100 + new Integer(str.substring(2, 4)));
            level = 1;
        }
        if (strlen >= 6) {
            y += LAT_HEIGHT_MESH2.multiply(new Integer(str.substring(4, 5)).intValue()).doubleValue();
            x += LNG_WIDTH_MESH2.multiply(new Integer(str.substring(5, 6)).intValue()).doubleValue();
            level = 2;
        }
        if (strlen >= 8) {
            y += LAT_HEIGHT_MESH3.multiply(new Integer(str.substring(6, 7)).intValue()).doubleValue();
            x += LNG_WIDTH_MESH3.multiply(new Integer(str.substring(7, 8)).intValue()).doubleValue();
            level = 3;
        }
        if (strlen >= 9) {
            n = new Integer(str.substring(8, 9));
            y += LAT_HEIGHT_MESH4.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH4.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            level = 4;
        }
        if (strlen >= 10) {
            n = new Integer(str.substring(9, 10));
            y += LAT_HEIGHT_MESH5.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH5.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            level = 5;
        }
        if (strlen >= 11) {
            n = new Integer(str.substring(10, 11));
            y += LAT_HEIGHT_MESH6.multiply(n <= 2 ? 0 : 1).doubleValue();
            x += LNG_WIDTH_MESH6.multiply(n % 2 == 1 ? 0 : 1).doubleValue();
            level = 6;
        }
        return MeshUtils.createMesh(level, x, y);
    }
}

