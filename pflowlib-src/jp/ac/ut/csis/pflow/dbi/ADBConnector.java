/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.dbi;

import jp.ac.ut.csis.pflow.dbi.IDBConnector;

public abstract class ADBConnector
implements IDBConnector {
    private String _username;
    private String _password;
    private String _host;
    private String _dbname;
    private int _port;
    private String _encoding;

    @Override
    public String getUsername() {
        return this._username;
    }

    @Override
    public IDBConnector setUsername(String username) {
        this._username = username;
        return this;
    }

    @Override
    public String getPassword() {
        return this._password;
    }

    @Override
    public IDBConnector setPassword(String password) {
        this._password = password;
        return this;
    }

    @Override
    public int getPort() {
        return this._port;
    }

    @Override
    public IDBConnector setPort(int port) {
        this._port = port;
        return this;
    }

    @Override
    public String getHost() {
        return this._host;
    }

    @Override
    public IDBConnector setHost(String host) {
        this._host = host;
        return this;
    }

    @Override
    public String getDBName() {
        return this._dbname;
    }

    @Override
    public IDBConnector setDBName(String dbname) {
        this._dbname = dbname;
        return this;
    }

    @Override
    public String getEncoding() {
        return this._encoding;
    }

    @Override
    public IDBConnector setEncoding(String encoding) {
        this._encoding = encoding;
        return this;
    }
}

