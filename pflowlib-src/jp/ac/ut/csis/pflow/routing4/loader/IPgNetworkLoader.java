/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.sql.Connection;
import jp.ac.ut.csis.pflow.routing4.loader.INetworkLoader;

public interface IPgNetworkLoader
extends INetworkLoader {
    public static final String GEOMETRY_COLUMN = "geom";
    public static final String LINK_ID_COLUMN = "gid";
    public static final String SOURCE_NODE_COLUMN = "source";
    public static final String TARGET_NODE_COLUMN = "target";
    public static final String LENGTH_COLUMN = "length";
    public static final String COST_COLUMN = "cost";
    public static final String REVERSER_COST_COLUMN = "reverse_cost";

    public Connection getConnection();

    public IPgNetworkLoader setConnection(Connection var1);

    public String getTableName();

    public IPgNetworkLoader setTableName(String var1);
}

