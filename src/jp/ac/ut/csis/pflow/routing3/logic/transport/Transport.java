/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.logic.transport;

import jp.ac.ut.csis.pflow.routing3.logic.transport.ITransport;

public final class Transport
implements ITransport {
    public static final ITransport WALK = new Transport("WALK", 1.3888888888888888);
    public static final ITransport BICYCLE = new Transport("BICYCLE", 2.7777777777777777);
    public static final ITransport BIKE = new Transport("BIKE", 8.333333333333334);
    public static final ITransport VEHICLE = new Transport("VEHICLE", 11.11111111111111);
    public static final ITransport RAILWAY = new Transport("RAILWAY", 11.11111111111111);
    private String _name;
    private double _velocity;

    private Transport(String name, double velocity) {
        this._name = name;
        this._velocity = velocity;
    }

    @Override
    public String getName() {
        return this._name;
    }

    @Override
    public double getOrdinaryVelocity() {
        return this._velocity;
    }

    @Override
    public double getVelocity(int linkType) {
        return this.getOrdinaryVelocity();
    }
}

