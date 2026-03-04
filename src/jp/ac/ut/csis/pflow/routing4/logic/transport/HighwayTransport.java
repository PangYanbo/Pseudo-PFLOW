/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic.transport;

import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.logic.transport.Transport;

public final class HighwayTransport
extends Transport {
    public static final ITransport BIKE = new HighwayTransport("BIKE", 22.22222222222222);
    public static final ITransport VEHICLE = new HighwayTransport("VEHICLE", 27.77777777777778);

    protected HighwayTransport(String name, double velocity) {
        super(name, velocity);
    }
}

