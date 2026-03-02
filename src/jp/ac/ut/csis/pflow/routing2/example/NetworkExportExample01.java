/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing2.example;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.routing2.loader.PgOsmLoader;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.NetworkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkExportExample01 {
    private static final Logger LOGGER = LogManager.getLogger(NetworkExportExample01.class);

    public static void main(String[] args) {
        File outputFile = new File(args[0]);
        try (PgLoader pgloader = new PgLoader();){
            try (Connection con = pgloader.getConnection()) {
                PgOsmLoader osmLoader = new PgOsmLoader();
                Network roadNetwork = osmLoader.load(con, true);
                NetworkUtils.exportAsCsv(roadNetwork, outputFile);
            }
            catch (SQLException exp) {
                LOGGER.error("fail to load data from DB", (Throwable)exp);
                pgloader.close();
            }
        }
    }
}

