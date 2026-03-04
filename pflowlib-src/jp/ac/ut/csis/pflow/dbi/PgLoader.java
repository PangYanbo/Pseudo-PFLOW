/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.dbcp.BasicDataSource
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.dbi;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PgLoader {
    private static final Logger LOGGER = LogManager.getLogger(PgLoader.class);
    public static final String PGSQL_DRIVER = "org.postgresql.Driver";
    public static final String DB_HOST = System.getProperty("pflow.pgloader.host", "localhost");
    public static final String DB_USER = System.getProperty("pflow.pgloader.userid", "postgres");
    public static final String DB_PASS = System.getProperty("pflow.pgloader.userpw", "postgres");
    public static final String DB_NAME = System.getProperty("pflow.pgloader.dbname", "pflowdb");
    public static final int DB_PORT = Integer.getInteger("pflow.pgloader.port", 5432);
    public static final String DB_ENCODING = System.getProperty("pflow.pgloader.encoding", "UTF-8");
    public static final int POOL_SIZE = Integer.getInteger("pflow.pgloader.pool_size", 100);
    private BasicDataSource _dbPool = null;

    public PgLoader() {
        this(DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME, DB_ENCODING, POOL_SIZE);
    }

    public PgLoader(String host, String id, String pw, String dbname) {
        this(host, DB_PORT, id, pw, dbname, DB_ENCODING, POOL_SIZE);
    }

    public PgLoader(String host, int port, String id, String pw, String dbname) {
        this(host, port, id, pw, dbname, DB_ENCODING, POOL_SIZE);
    }

    public PgLoader(String host, int port, String id, String pw, String dbname, String encoding) {
        this(host, port, id, pw, dbname, encoding, POOL_SIZE);
    }

    public PgLoader(String host, int port, String id, String pw, String dbname, String encoding, int maxNum) {
        this._dbPool = new BasicDataSource();
        this._dbPool.setDriverClassName(PGSQL_DRIVER);
        this._dbPool.setUsername(id);
        this._dbPool.setPassword(pw);
        this._dbPool.setUrl(String.format("jdbc:postgresql://%s:%d/%s?charSet=%s", host, port, dbname, encoding));
        this._dbPool.setMaxActive(maxNum);
    }

    public PgLoader(BasicDataSource dbPool) {
        this._dbPool = dbPool;
    }

    public synchronized Connection getConnection() {
        if (this._dbPool == null) {
            LOGGER.error("you should initialize connection pool first");
            return null;
        }
        Connection con = null;
        try {
            con = this._dbPool.getConnection();
        }
        catch (SQLException exp) {
            LOGGER.error("fail to get DB connection", (Throwable)exp);
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
            LOGGER.error("fail to close DB connection", (Throwable)exp);
        }
    }
}

