/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.dbcp.BasicDataSource
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.dbi;

import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.ADBLoader;
import jp.ac.ut.csis.pflow.dbi.IDBLoader;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;

public final class PgsqlLoader
extends ADBLoader {
    public static final String PGSQL_DRIVER = "org.postgresql.Driver";
    public static final String DB_HOST = System.getProperty("pflow.pgloader.host", "localhost");
    public static final String DB_USER = System.getProperty("pflow.pgloader.userid", "postgres");
    public static final String DB_PASS = System.getProperty("pflow.pgloader.userpw", "postgres");
    public static final String DB_NAME = System.getProperty("pflow.pgloader.dbname", "pflowdb");
    public static final int DB_PORT = Integer.getInteger("pflow.pgloader.port", 5432);
    public static final String DB_ENCODING = System.getProperty("pflow.pgloader.encoding", "UTF-8");
    public static final int POOL_SIZE = Integer.getInteger("pflow.pgloader.pool_size", 100);
    private BasicDataSource _dbPool = new BasicDataSource();
    private int _poolSize;

    public PgsqlLoader() {
        this._dbPool.setDriverClassName(PGSQL_DRIVER);
        this.setUsername(DB_USER);
        this.setPassword(DB_PASS);
        this.setHost(DB_HOST);
        this.setDBName(DB_NAME);
        this.setPort(DB_PORT);
        this.setEncoding(DB_ENCODING);
        this._poolSize = POOL_SIZE;
    }

    public int getPoolSize() {
        return this._poolSize;
    }

    public IDBLoader setPoolSize(int poolSize) {
        this._poolSize = poolSize;
        return this;
    }

    @Override
    public synchronized Connection connect() {
        String username = this.getUsername();
        if (StringUtils.isBlank((String)username)) {
            System.err.println("no username specified");
            return null;
        }
        String password = this.getPassword();
        if (StringUtils.isBlank((String)password)) {
            System.err.println("no password specified");
            return null;
        }
        String hostname = this.getHost();
        if (StringUtils.isBlank((String)hostname)) {
            System.err.println("no hostname specified");
            return null;
        }
        int port = this.getPort();
        if (port <= 0) {
            System.err.println("invalid port no::" + port);
            return null;
        }
        String dbname = this.getDBName();
        if (StringUtils.isBlank((String)dbname)) {
            System.err.println("no DB name specified");
            return null;
        }
        String encoding = this.getEncoding();
        if (StringUtils.isBlank((String)encoding)) {
            System.err.println("no encoding specified");
            return null;
        }
        int poolSize = this.getPoolSize();
        if (poolSize <= 0) {
            System.err.println("invalid pool size::" + poolSize);
            return null;
        }
        Connection con = null;
        try {
            this._dbPool.setUsername(username);
            this._dbPool.setPassword(password);
            this._dbPool.setUrl(String.format("jdbc:postgresql://%s:%d/%s?charSet=%s", hostname, port, dbname, encoding));
            this._dbPool.setMaxActive(poolSize);
            con = this._dbPool.getConnection();
        }
        catch (SQLException exp) {
            exp.printStackTrace();
            con = null;
        }
        return con;
    }

    public void close() {
        try {
            if (this._dbPool != null) {
                this._dbPool.close();
            }
        }
        catch (SQLException exp) {
            exp.printStackTrace();
        }
    }
}

