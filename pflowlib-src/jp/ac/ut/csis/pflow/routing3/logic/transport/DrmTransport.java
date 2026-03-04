/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.logic.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jp.ac.ut.csis.pflow.routing3.logic.transport.ITransport;

public final class DrmTransport
implements ITransport {
    public static final ITransport WALK;
    public static final ITransport BICYCLE;
    public static final ITransport MOTOR_BIKE;
    public static final ITransport BIKE;
    public static final ITransport VEHICLE;
    public static final ITransport LIGHT_VEHICLE;
    public static final ITransport SHIPPING_CAR;
    public static final ITransport PRIVATE_BUS;
    public static final ITransport BUS;
    private String _name;
    private Map<Integer, Double> _linkVelocity;

    static {
        HashMap<Integer, Double> map = new HashMap<Integer, Double>();
        map.put(1, Double.NaN);
        map.put(2, Double.NaN);
        map.put(3, 1.6666666666666667);
        map.put(4, 1.6666666666666667);
        map.put(5, 1.6666666666666667);
        map.put(6, 1.6666666666666667);
        map.put(7, 1.6666666666666667);
        map.put(8, 1.6666666666666667);
        map.put(9, 1.6666666666666667);
        map.put(-1, 1.6666666666666667);
        WALK = new DrmTransport("WALK", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, Double.NaN);
        map.put(2, Double.NaN);
        map.put(3, 2.7777777777777777);
        map.put(4, 2.7777777777777777);
        map.put(5, 2.7777777777777777);
        map.put(6, 2.7777777777777777);
        map.put(7, 2.7777777777777777);
        map.put(8, 2.7777777777777777);
        map.put(9, 2.7777777777777777);
        map.put(-1, 2.7777777777777777);
        BICYCLE = new DrmTransport("BICYCLE", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, Double.NaN);
        map.put(2, Double.NaN);
        map.put(3, 8.333333333333334);
        map.put(4, 8.333333333333334);
        map.put(5, 8.333333333333334);
        map.put(6, 8.333333333333334);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 8.333333333333334);
        MOTOR_BIKE = new DrmTransport("MOTOR_BIKE", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 22.22222222222222);
        map.put(2, 16.666666666666668);
        map.put(3, 13.88888888888889);
        map.put(4, 11.11111111111111);
        map.put(5, 11.11111111111111);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.0);
        BIKE = new DrmTransport("BIKE", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 27.77777777777778);
        map.put(2, 19.444444444444446);
        map.put(3, 13.88888888888889);
        map.put(4, 12.5);
        map.put(5, 12.5);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.555555555555555);
        VEHICLE = new DrmTransport("VEHICLE", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 27.77777777777778);
        map.put(2, 19.444444444444446);
        map.put(3, 13.88888888888889);
        map.put(4, 12.5);
        map.put(5, 12.5);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.555555555555555);
        LIGHT_VEHICLE = new DrmTransport("LIGHT_VEHICLE", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 27.77777777777778);
        map.put(2, 19.444444444444446);
        map.put(3, 13.88888888888889);
        map.put(4, 12.5);
        map.put(5, 12.5);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.555555555555555);
        SHIPPING_CAR = new DrmTransport("SHIPPING_CAR", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 25.0);
        map.put(2, 16.666666666666668);
        map.put(3, 13.88888888888889);
        map.put(4, 12.5);
        map.put(5, 12.5);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.555555555555555);
        PRIVATE_BUS = new DrmTransport("PRIVATE_BUS", Collections.unmodifiableMap(map));
        map = new HashMap();
        map.put(1, 25.0);
        map.put(2, 16.666666666666668);
        map.put(3, 13.88888888888889);
        map.put(4, 12.5);
        map.put(5, 12.5);
        map.put(6, 11.11111111111111);
        map.put(7, 8.333333333333334);
        map.put(8, 8.333333333333334);
        map.put(9, 8.333333333333334);
        map.put(-1, 10.555555555555555);
        BUS = new DrmTransport("BUS", Collections.unmodifiableMap(map));
    }

    private DrmTransport(String name, Map<Integer, Double> linkVelocity) {
        this._name = name;
        this._linkVelocity = linkVelocity;
    }

    @Override
    public String getName() {
        return this._name;
    }

    @Override
    public double getOrdinaryVelocity() {
        return this.getVelocity(-1);
    }

    @Override
    public double getVelocity(int linkType) {
        if (this._linkVelocity.containsKey(linkType)) {
            return this._linkVelocity.get(linkType);
        }
        return this._linkVelocity.get(-1);
    }
}

