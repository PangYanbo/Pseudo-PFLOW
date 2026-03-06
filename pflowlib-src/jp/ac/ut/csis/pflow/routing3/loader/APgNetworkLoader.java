/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.routing3.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.res.Network;

public abstract class APgNetworkLoader
extends ANetworkLoader {
    public static final String GEOMETRY_COLUMN = "geom";
    public static final String LINK_ID_COLUMN = "gid";
    public static final String SOURCE_NODE_COLUMN = "source";
    public static final String TARGET_NODE_COLUMN = "target";
    public static final String LENGTH_COLUMN = "length";
    public static final String COST_COLUMN = "cost";
    public static final String REVERSER_COST_COLUMN = "reverse_cost";
    private String _tablename;

    protected APgNetworkLoader(String tablename) {
        this._tablename = tablename;
    }

    public String getTableName() {
        return this._tablename;
    }

    @Override
    public Network load(Network network, QueryCondition[] conds, boolean needGeom) {
        try (PgLoader loader = new PgLoader();){
            try {
                Throwable throwable = null;
                Object var6_8 = null;
                try (Connection con = loader.getConnection();){
                    network = con == null ? null : this.load(con, conds, needGeom);
                }
                catch (Throwable throwable2) {
                    if (throwable == null) {
                        throwable = throwable2;
                    } else if (throwable != throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            }
            catch (SQLException exp) {
                exp.printStackTrace();
                loader.close();
            }
        }
        return network;
    }

    public Network load(Connection con, double x0, double y0, double x1, double y1, boolean needGeom) {
        return this.load(con, new double[]{x0, y0, x1, y1}, needGeom);
    }

    public Network load(Connection con) {
        return this.load(con, new double[0], true);
    }

    public Network load(Connection con, boolean needGeom) {
        return this.load(con, new double[0], needGeom);
    }

    public Network load(Connection con, double[] rect, boolean needGeom) {
        return this.load(con, new QueryCondition(rect, 3000.0), needGeom);
    }

    public Network load(Connection con, double[] rect, double bufSize, boolean needGeom) {
        return this.load(con, new QueryCondition(rect, bufSize), needGeom);
    }

    public Network load(Connection con, QueryCondition cond) {
        return this.load(con, new QueryCondition[]{cond});
    }

    public Network load(Network network, Connection con, QueryCondition cond) {
        return this.load(network, con, new QueryCondition[]{cond});
    }

    public Network load(Connection con, QueryCondition cond, boolean needGeom) {
        return this.load(con, new QueryCondition[]{cond}, needGeom);
    }

    public Network load(Network network, Connection con, QueryCondition cond, boolean needGeom) {
        return this.load(network, con, new QueryCondition[]{cond}, needGeom);
    }

    public Network load(Connection con, QueryCondition[] conds) {
        return this.load(new Network(), con, conds);
    }

    public Network load(Connection con, QueryCondition[] conds, boolean needGeom) {
        return this.load(new Network(), con, conds, needGeom);
    }

    public Network load(Network network, Connection con) {
        return this.load(network, con, new QueryCondition[0]);
    }

    public Network load(Network network, Connection con, QueryCondition[] conds) {
        String sql = this.createQuery(conds);
        return this.load(network, con, sql, true);
    }

    public Network load(Network network, Connection con, QueryCondition[] conds, boolean needGeom) {
        String sql = this.createQuery(conds);
        return this.load(network, con, sql, needGeom);
    }

    protected abstract String createQuery(QueryCondition[] var1);

    public abstract Network load(Network var1, Connection var2, String var3, boolean var4);

    public Network load(Connection con, String sql, boolean needGeom) {
        return this.load(new Network(), con, sql, needGeom);
    }
}

