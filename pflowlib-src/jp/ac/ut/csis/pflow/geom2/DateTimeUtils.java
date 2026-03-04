/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ITime;
import jp.ac.ut.csis.pflow.geom2.ITimeSpan;

public class DateTimeUtils {
    public static boolean isWeekDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(7);
        switch (day) {
            case 1: 
            case 7: {
                return false;
            }
        }
        return true;
    }

    public static long getDuration(Date ts, Date te) {
        return ts == null || te == null ? -1L : te.getTime() - ts.getTime();
    }

    public static long getDuration(ITime ts, ITime te) {
        Date ds = ts.getTimeStamp();
        Date de = te.getTimeStamp();
        return DateTimeUtils.getDuration(ds, de);
    }

    public static List<Date> getExactTimeWithin(Date ts, Date te, long spanSec) {
        long t0 = ts.getTime() / 1000L;
        if (t0 % spanSec != 0L) {
            t0 += spanSec - t0 % spanSec;
        }
        long t1 = te.getTime() / 1000L;
        t1 -= t0 % spanSec;
        ArrayList<Date> output = new ArrayList<Date>();
        long t = t0;
        while (t <= t1) {
            output.add(new Date(t * 1000L));
            t += spanSec;
        }
        return output;
    }

    public static boolean intersects(ITimeSpan timespan, ITime timestamp) {
        Date ts = timespan.getStartTime();
        Date te = timespan.getEndTime();
        Date t = timestamp.getTimeStamp();
        return DateTimeUtils.intersects(ts, te, t);
    }

    public static boolean intersects(Date ts, Date te, Date timestamp) {
        long tsUnixtime = ts.getTime();
        long teUnixtime = te.getTime();
        long tUnixtime = timestamp.getTime();
        return tsUnixtime <= tUnixtime && tUnixtime <= teUnixtime;
    }

    public static boolean intersects(ITimeSpan timespan0, ITimeSpan timespan1) {
        Date ts0 = timespan0.getStartTime();
        Date te0 = timespan0.getEndTime();
        Date ts1 = timespan1.getStartTime();
        Date te1 = timespan1.getEndTime();
        return DateTimeUtils.intersects(ts0, te0, ts1, te1);
    }

    public static boolean intersects(Date ts0, Date te0, Date ts1, Date te1) {
        long tsUnixtime0 = ts0.getTime();
        long teUnixtime0 = te0.getTime();
        long tsUnixtime1 = ts1.getTime();
        long teUnixtime1 = te1.getTime();
        return tsUnixtime0 <= tsUnixtime1 && tsUnixtime1 <= teUnixtime0 || tsUnixtime0 <= teUnixtime1 && teUnixtime1 <= teUnixtime0;
    }
}

