/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.geom2;

import java.util.Date;

public interface ITimeSpan {
    public Date getStartTime();

    public void setStartTime(Date var1);

    public Date getEndTime();

    public void setEndTime(Date var1);

    public long getDuration();
}

