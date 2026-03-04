/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.tripformatter;

import java.util.List;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;

public interface ITripFormatter {
    public static final long TIME_THRESHOLD = 3600L;
    public static final double DISTANCE_THRESHOLD = 200.0;
    public static final double DEFAULT_VELOCITY = TransportMode.CAR.getVelocity() / 3.6;
    public static final double STAY_BIAS_DISTANCE = 10.0;

    public boolean check(ITrip var1, ITrip var2);

    public List<ITrip> format(ITrip var1, ITrip var2);
}

