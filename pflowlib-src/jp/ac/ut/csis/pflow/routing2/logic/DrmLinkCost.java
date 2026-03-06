/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.DrmLink;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class DrmLinkCost
extends LinkCost {
    private static final double[][] VELOCITY_MATRIX = new double[][]{{6.0, 10.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0}, {0.0, 0.0, 0.0, 80.0, 100.0, 100.0, 100.0, 90.0, 90.0}, {0.0, 0.0, 0.0, 60.0, 70.0, 70.0, 70.0, 60.0, 60.0}, {6.0, 10.0, 30.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0}, {6.0, 10.0, 30.0, 40.0, 45.0, 45.0, 45.0, 45.0, 45.0}, {6.0, 10.0, 30.0, 40.0, 45.0, 45.0, 45.0, 45.0, 45.0}, {6.0, 10.0, 30.0, 40.0, 40.0, 40.0, 40.0, 40.0, 40.0}, {6.0, 10.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0}, {6.0, 10.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0}, {6.0, 10.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0}};
    private Mode _mode;

    public DrmLinkCost(Mode mode) {
        this._mode = mode;
    }

    public void setMode(Mode mode) {
        this._mode = mode;
    }

    public Mode getMode() {
        return this._mode;
    }

    @Override
    public double getCost(Link link) {
        return this.getCost(null, null, link);
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        if (nextLink instanceof DrmLink) {
            DrmLink drmlink = (DrmLink)DrmLink.class.cast(nextLink);
            double vel_m_s = VELOCITY_MATRIX[drmlink.getRoadType()][this._mode.getMode()] * 1000.0 / 3600.0;
            return nextLink.getCost() / vel_m_s;
        }
        return nextLink.getCost();
    }

    @Override
    public double getReverseCost(Link link) {
        return this.getReverseCost(null, null, link);
    }

    @Override
    public double getReverseCost(Link prevLink, Node crntNode, Link nextLink) {
        if (nextLink instanceof DrmLink) {
            DrmLink drmlink = (DrmLink)DrmLink.class.cast(nextLink);
            double vel_m_s = VELOCITY_MATRIX[drmlink.getRoadType()][this._mode.getMode()] * 1000.0 / 3600.0;
            return nextLink.getReverseCost() / vel_m_s;
        }
        return nextLink.getReverseCost();
    }

    public static enum Mode {
        WALK(0),
        BICYCLE(1),
        MOTORBIKE(2),
        BIKE(3),
        TAXI(4),
        VEHICLE(5),
        LIGHT_VEHICLE(6),
        SHIPPING_CAR(7),
        BUS(8);

        private int _mode;

        private Mode(int mode) {
            this._mode = mode;
        }

        public int getMode() {
            return this._mode;
        }

        public static Mode valueOf(String string) {
            return Enum.valueOf(Mode.class, string);
        }
    }
}

