/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.ITimeSpan;
import jp.ac.ut.csis.pflow.interpolation.trip.SegmentMode;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;

public interface ITrip
extends ITimeSpan {
    public String getUid();

    public TransportMode getTransportMode();

    public SegmentMode getSegmentMode();

    public ILonLatTime getStartPoint();

    public ILonLatTime getEndPoint();

    public double getTripDistance();

    public double getAverageVelocity();
}

