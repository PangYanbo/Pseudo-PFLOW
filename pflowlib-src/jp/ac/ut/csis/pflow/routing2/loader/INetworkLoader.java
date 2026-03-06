/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Network;

public interface INetworkLoader {
    public static final double APPROX_1KM = 1.2E-5;
    public static final double DEFAULT_BUFFER_SIZE = 3000.0;
    public static final double MINIMUM_BUFFER_SIZE = 1000.0;
    public static final double MAXIMUM_BUFFER_SIZE = 5000.0;

    public Network load(double var1, double var3, double var5, double var7);

    public Network load(Network var1, double var2, double var4, double var6, double var8);

    public Network load(double[] var1);

    public Network load(Network var1, double[] var2);

    public Network load(double var1, double var3, double var5, double var7, double var9);

    public Network load(Network var1, double var2, double var4, double var6, double var8, double var10);

    public Network load(double[] var1, double var2);

    public Network load(Network var1, double[] var2, double var3);

    public Network load(double var1, double var3, double var5, double var7, boolean var9);

    public Network load(Network var1, double var2, double var4, double var6, double var8, boolean var10);

    public Network load(double[] var1, boolean var2);

    public Network load(Network var1, double[] var2, boolean var3);

    public Network load(double var1, double var3, double var5, double var7, double var9, boolean var11);

    public Network load(Network var1, double var2, double var4, double var6, double var8, double var10, boolean var12);

    public Network load(double[] var1, double var2, boolean var4);

    public Network load(Network var1, double[] var2, double var3, boolean var5);

    public Network load(QueryCondition var1);

    public Network load(Network var1, QueryCondition var2);

    public Network load(QueryCondition[] var1);

    public Network load(Network var1, QueryCondition[] var2);

    public Network load();

    public Network load(Network var1);
}

