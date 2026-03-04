/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.tripformatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.tripformatter.ITripFormatter;

public class FormatNone
implements ITripFormatter {
    @Override
    public boolean check(ITrip tripPrev, ITrip tripNext) {
        Date t1;
        ILonLatTime p0 = tripPrev.getEndPoint();
        ILonLatTime p1 = tripNext.getStartPoint();
        Date t0 = tripPrev.getEndTime();
        return t0.equals(t1 = tripNext.getStartTime()) || p0.getLon() == p1.getLon() && p0.getLat() == p1.getLat();
    }

    @Override
    public List<ITrip> format(ITrip tripPrev, ITrip tripNext) {
        ArrayList<ITrip> trips = new ArrayList<ITrip>();
        trips.add(tripPrev);
        trips.add(tripNext);
        return trips;
    }
}

