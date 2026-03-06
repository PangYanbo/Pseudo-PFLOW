/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;

public interface IRailwayQueryCondition
extends IQueryCondition {
    public int getCompCode();

    public IRailwayQueryCondition setCompCode(int var1);

    public String getCompName();

    public IRailwayQueryCondition setCompName(String var1);

    public int getLineCode();

    public IRailwayQueryCondition setLineCode(int var1);

    public String getLineName();

    public IRailwayQueryCondition setLineName(String var1);
}

