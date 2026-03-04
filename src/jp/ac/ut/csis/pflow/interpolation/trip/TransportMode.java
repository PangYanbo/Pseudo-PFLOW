/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.interpolation.trip;

public enum TransportMode {
    STAY(0.0),
    BLANK(0.0),
    CAR(30.0),
    TRAIN(30.0),
    BIKE(15.0),
    WALK(5.0),
    UNKNOWN(5.0);

    private double _velocity;

    private TransportMode(double velocity) {
        this._velocity = velocity;
    }

    public double getVelocity() {
        return this._velocity;
    }
}

