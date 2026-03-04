/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.Date;
import jp.ac.ut.csis.pflow.geom.STPoint;

public interface ITrip {
    public TripType getType();

    public Date getStartTime();

    public Date getEndTime();

    public long getTripDuration();

    public STPoint getStartPoint();

    public STPoint getEndPoint();

    public double getTripDistance();

    public double getAverageVelocity();

    public static enum TripType {
        STAY,
        MOVE;

    }
}

