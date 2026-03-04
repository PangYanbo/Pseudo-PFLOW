/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.dbi;

import java.sql.Connection;

public interface IDBConnector {
    public Connection connect();

    public void disconnect();

    public String getUsername();

    public IDBConnector setUsername(String var1);

    public String getPassword();

    public IDBConnector setPassword(String var1);

    public int getPort();

    public IDBConnector setPort(int var1);

    public String getHost();

    public IDBConnector setHost(String var1);

    public String getDBName();

    public IDBConnector setDBName(String var1);

    public String getEncoding();

    public IDBConnector setEncoding(String var1);
}

