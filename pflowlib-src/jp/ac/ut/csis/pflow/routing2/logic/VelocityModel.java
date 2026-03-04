/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.Random;

public class VelocityModel {
    public static final double SIGMOID_GAIN = 5.0;
    private double _gain;

    public VelocityModel() {
        this(5.0);
    }

    public VelocityModel(double gain) {
        this._gain = gain;
    }

    public double estimateVelocity(double distanceInMeter) {
        Random rand;
        double prob;
        double distKM = distanceInMeter / 1000.0;
        double ratio = 1.0 - 1.0 / (1.0 + Math.exp(this._gain - distKM));
        return ratio > (prob = (rand = new Random()).nextDouble()) ? Velocity.WALK.getVelocity() : (rand.nextBoolean() ? Velocity.VEHICLE.getVelocity() : Velocity.BIKE.getVelocity());
    }

    public long estimateTimeDuration(double distanceInMeter) {
        double velocity = this.estimateVelocity(distanceInMeter) * 1000.0 / 3600.0;
        return (long)(distanceInMeter / velocity);
    }

    public static enum Velocity {
        WALK(5.0),
        BIKE(30.0),
        VEHICLE(50.0),
        UNKNOWN(-1.0);

        private double _velocity;

        private Velocity(double velocity) {
            this._velocity = velocity;
        }

        public double getVelocity() {
            return this._velocity;
        }
    }
}

