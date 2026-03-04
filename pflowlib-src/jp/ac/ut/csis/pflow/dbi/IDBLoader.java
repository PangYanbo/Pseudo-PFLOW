/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.dbi;

import java.sql.Connection;

public interface IDBLoader {
    public Connection connect();

    public String getUsername();

    public IDBLoader setUsername(String var1);

    public String getPassword();

    public IDBLoader setPassword(String var1);

    public int getPort();

    public IDBLoader setPort(int var1);

    public String getHost();

    public IDBLoader setHost(String var1);

    public String getDBName();

    public IDBLoader setDBName(String var1);

    public String getEncoding();

    public IDBLoader setEncoding(String var1);
}

