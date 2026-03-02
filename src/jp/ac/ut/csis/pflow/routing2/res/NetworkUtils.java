/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing2.res;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing2.loader.ACsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkUtils {
    private static final Logger LOGGER = LogManager.getLogger(NetworkUtils.class);

    public static void exportAsCsv(Network network, File outputFile) {
        NetworkUtils.exportAsCsv(network, outputFile, ACsvNetworkLoader.Delimiter.TSV);
    }

    public static void exportAsCsv(Network network, File outputFile, ACsvNetworkLoader.Delimiter delim) {
        try {
            Throwable throwable = null;
            Object var4_6 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));){
                String delimString = delim.equals((Object)ACsvNetworkLoader.Delimiter.TSV) ? "\t" : ",";
                Object[] header = new String[]{"linkId", "tailNode", "headNode", "cost", "rCost", "geom"};
                bw.write(StringUtils.join((Object[])header, (String)delimString));
                bw.newLine();
                for (Link link : network.listLinks()) {
                    String linkId = link.getLinkID();
                    Node tailNode = link.getTailNode();
                    Node headNode = link.getHeadNode();
                    double cost = link.getCost();
                    double rCost = link.getReverseCost();
                    String wkt = link.hasGeometry() ? TrajectoryUtils.asWKT(link.getLineString()) : TrajectoryUtils.asWKT(Arrays.asList(tailNode, headNode));
                    Object[] tokens = new String[]{linkId, tailNode.getNodeID(), headNode.getNodeID(), String.valueOf(cost), String.valueOf(rCost), wkt};
                    bw.write(StringUtils.join((Object[])tokens, (String)delimString));
                    bw.newLine();
                }
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
        catch (IOException exp) {
            LOGGER.error("fail to export", (Throwable)exp);
        }
    }
}

