/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic.transport;

public interface ITransport {
    public static final double RATIO = 0.2777777777777778;

    public String getName();

    public double getOrdinaryVelocity();

    public double getVelocity(int var1);
}

