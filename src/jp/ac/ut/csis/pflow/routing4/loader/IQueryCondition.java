/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.awt.geom.Rectangle2D;

public interface IQueryCondition {
    public static final double APPROX_1KM = 1.2E-5;
    public static final double DEFAULT_BUFFER_SIZE = 3000.0;
    public static final double MINIMUM_BUFFER_SIZE = 1000.0;
    public static final double MAXIMUM_BUFFER_SIZE = 5000.0;

    public Rectangle2D getBounds();

    public Rectangle2D getRect();

    public double getBuffer();

    public IQueryCondition setBounds(Rectangle2D var1, double var2);

    public IQueryCondition setBounds(Rectangle2D var1);

    public IQueryCondition setBounds(double[] var1, double var2);

    public IQueryCondition setBounds(double[] var1);
}

