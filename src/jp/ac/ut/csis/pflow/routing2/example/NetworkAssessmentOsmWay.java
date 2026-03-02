/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing2.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.routing2.loader.PgOsmWayLoader;
import jp.ac.ut.csis.pflow.routing2.logic.NetworkAssessment;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkAssessmentOsmWay {
    private static final Logger LOGGER = LogManager.getLogger(NetworkAssessmentOsmWay.class);

    public static void main(String[] args) {
        File outfile = new File(args[0]);
        try (PgLoader pgloader = new PgLoader();){
            try (Connection con = pgloader.getConnection()) {
                PgOsmWayLoader osmLoader = new PgOsmWayLoader();
                Network roadNetwork = osmLoader.load(new Network(false, false), con);
                LOGGER.debug("[before]" + roadNetwork.listLinks().size());
                List<Set<Node>> results = new NetworkAssessment().validateIsolation(roadNetwork);
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
                    bw.write("groupNo\tnodeId\tnodeNum\twkt");
                    bw.newLine();
                    int groupno = 1;
                    for (Set<Node> set : results) {
                        for (Node node : set) {
                            bw.write(String.format("%d\t%s\t%d\tSRID=4326;POINT(%.06f %.06f)", groupno, node.getNodeID(), set.size(), node.getLon(), node.getLat()));
                            bw.newLine();
                        }
                        LOGGER.debug(String.format("[Group %d] has %d nodes", groupno, set.size()));
                        ++groupno;
                    }
                }
                catch (IOException exp) {
                    exp.printStackTrace();
                }
                LOGGER.debug("[after]" + roadNetwork.listLinks().size());
            }
            catch (SQLException exp) {
                exp.printStackTrace();
                pgloader.close();
            }
        }
    }
}

