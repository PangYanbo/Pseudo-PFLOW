/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.ATrip;
import jp.ac.ut.csis.pflow.geom.ITrip;
import jp.ac.ut.csis.pflow.geom.STPoint;

public class Move
extends ATrip {
    public Move() {
        this(null);
    }

    public Move(List<STPoint> trajectory) {
        super(trajectory);
    }

    @Override
    public ITrip.TripType getType() {
        return ITrip.TripType.MOVE;
    }

    @Override
    public String toString() {
        return String.valueOf(this.getClass().getName()) + "::" + this.getTrajectory();
    }

    public Move clone() {
        List<STPoint> trajectory = this.getTrajectory();
        if (trajectory == null) {
            return new Move();
        }
        ArrayList<STPoint> points = new ArrayList<STPoint>(trajectory.size());
        for (STPoint point : trajectory) {
            points.add(point.clone());
        }
        return new Move(points);
    }
}

