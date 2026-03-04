/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.Network;

public interface INetworkLoader {
    public static final double APPROX_1KM = 1.2E-5;
    public static final double DEFAULT_BUFFER_SIZE = 3000.0;
    public static final double MINIMUM_BUFFER_SIZE = 1000.0;
    public static final double MAXIMUM_BUFFER_SIZE = 5000.0;

    public Network load() throws NetworkLoadingException;

    public INetworkLoader setNetwork(Network var1);

    public INetworkLoader setBounds(double var1, double var3, double var5, double var7, double var9);

    public INetworkLoader setBounds(double var1, double var3, double var5, double var7);

    public INetworkLoader addBounds(double var1, double var3, double var5, double var7, double var9);

    public INetworkLoader addBounds(double var1, double var3, double var5, double var7);

    public INetworkLoader setBounds(double[] var1, double var2);

    public INetworkLoader setBounds(double[] var1);

    public INetworkLoader addBounds(double[] var1, double var2);

    public INetworkLoader addBounds(double[] var1);

    public INetworkLoader setGeometryFlag(boolean var1);

    public INetworkLoader setQueryCondition(IQueryCondition var1);

    public INetworkLoader setQueryConditions(IQueryCondition[] var1);

    public INetworkLoader addQueryCondition(IQueryCondition var1);
}

