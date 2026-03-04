/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom.ATrip;
import jp.ac.ut.csis.pflow.geom.ITrip;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.STPoint;

public class Stay
extends ATrip {
    public Stay() {
        this((STPoint)null);
    }

    public Stay(Date startTime, Date endTime, LonLat point) {
        this(new STPoint(startTime, endTime, point.getLon(), point.getLat()));
    }

    public Stay(STPoint stayPoint) {
        super(Arrays.asList(stayPoint));
    }

    @Override
    public ITrip.TripType getType() {
        return ITrip.TripType.STAY;
    }

    @Override
    public String toString() {
        return String.valueOf(this.getClass().getName()) + "::" + this.getTrajectory();
    }

    @Override
    public STPoint getStartPoint() {
        STPoint point = super.getStartPoint();
        return new STPoint(point.getDtStart(), point.getLon(), point.getLat());
    }

    @Override
    public STPoint getEndPoint() {
        STPoint point = super.getEndPoint();
        return new STPoint(point.getDtEnd(), point.getLon(), point.getLat());
    }

    @Override
    public Date getStartTime() {
        STPoint point = super.getStartPoint();
        return point == null ? null : point.getDtStart();
    }

    @Override
    public Date getEndTime() {
        STPoint point = super.getEndPoint();
        return point == null ? null : point.getDtEnd();
    }

    @Override
    public List<STPoint> getTrajectory() {
        return new ArrayList<STPoint>(Arrays.asList(this.getStartPoint(), this.getEndPoint()));
    }

    public Stay clone() {
        List<STPoint> trajectory = super.getTrajectory();
        if (trajectory == null || trajectory.isEmpty()) {
            return new Stay(null);
        }
        return new Stay(trajectory.get(0).clone());
    }
}

