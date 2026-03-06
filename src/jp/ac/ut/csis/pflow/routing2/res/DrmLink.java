/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.ArrayUtils
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.util.List;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.commons.lang3.ArrayUtils;

public class DrmLink
extends Link {
    private static final long serialVersionUID = -8451855189211348402L;
    public static final int[] ROAD_TYPES;
    public static final int[] ROAD_WIDTH;
    public static final int[] ROAD_ONEWAY;
    private int _roadtype;
    private int _width;
    private int _laneNum;

    static {
        int[] nArray = new int[9];
        nArray[0] = 1;
        nArray[1] = 2;
        nArray[2] = 3;
        nArray[3] = 4;
        nArray[4] = 5;
        nArray[5] = 6;
        nArray[6] = 7;
        nArray[7] = 9;
        ROAD_TYPES = nArray;
        ROAD_WIDTH = new int[]{1, 2, 3, 4, 5};
        ROAD_ONEWAY = new int[]{4, 5, 6, 7, 8};
    }

    public static boolean isOneway(int regulation_flag) {
        return ArrayUtils.contains((int[])ROAD_ONEWAY, (int)regulation_flag);
    }

    public static boolean isOnewayAndReverse(int regulation_flag) {
        return regulation_flag == 5 || regulation_flag == 7;
    }

    public DrmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int roadtype, int width) {
        this(linkid, tailNode, headNode, cost, revCost, oneway, roadtype, width, -1, null);
    }

    public DrmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int roadtype, int width, int laneNum) {
        this(linkid, tailNode, headNode, cost, revCost, oneway, roadtype, width, laneNum, null);
    }

    public DrmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int roadtype, int width, List<LonLat> geom) {
        this(linkid, tailNode, headNode, cost, revCost, oneway, roadtype, width, -1, null);
    }

    public DrmLink(String linkid, Node tailNode, Node headNode, double cost, double revCost, boolean oneway, int roadtype, int width, int laneNum, List<LonLat> geom) {
        super(linkid, tailNode, headNode, cost, revCost, oneway, geom);
        this._roadtype = roadtype;
        this._width = width;
        this._laneNum = laneNum;
    }

    public int getRoadType() {
        return this._roadtype;
    }

    public int getRoadWidth() {
        return this._width;
    }

    public int getLaneNum() {
        return this._laneNum;
    }
}

