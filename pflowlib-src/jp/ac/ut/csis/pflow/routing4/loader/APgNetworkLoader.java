/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.sql.Connection;
import jp.ac.ut.csis.pflow.routing4.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IPgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;

public abstract class APgNetworkLoader
extends ANetworkLoader
implements IPgNetworkLoader {
    private Connection _dbConn;
    private String _tablename;

    protected APgNetworkLoader() {
        this(null, null);
    }

    protected APgNetworkLoader(Connection dbConn, String tablename) {
        this.setConnection(dbConn);
        this.setTableName(tablename);
    }

    @Override
    public Connection getConnection() {
        return this._dbConn;
    }

    @Override
    public IPgNetworkLoader setConnection(Connection dbConn) {
        this._dbConn = dbConn;
        return this;
    }

    @Override
    public String getTableName() {
        return this._tablename;
    }

    @Override
    public IPgNetworkLoader setTableName(String tablename) {
        this._tablename = tablename;
        return this;
    }

    protected abstract String createSqlQuery() throws NetworkLoadingException;
}

