/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.sample;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import jp.ac.ut.csis.pflow.routing4.loader.CsvOsmLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.logic.NetworkAssessment;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;

public class OsmNetworkAssessment {
    public static void main(String[] args) {
        File inputNetworkTsv = new File(args[0]);
        File outputFileTsv = new File(args[1]);
        Network roadNetwork = new CsvOsmLoader().setNetworkFile(inputNetworkTsv).setHeaderFlag(true).setDelimiter(ICsvNetworkLoader.Delimiter.TSV).setGeometryFlag(false).setNetwork(new Network(false, false)).load();
        System.out.println("[before]" + roadNetwork.linkCount());
        List<Set<Node>> results = new NetworkAssessment().validateIsolation(roadNetwork);
        try {
            Throwable throwable = null;
            Object var6_8 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileTsv));){
                bw.write("groupNo\tnodeId\tnodeNum\twkt");
                bw.newLine();
                int groupno = 1;
                for (Set<Node> set : results) {
                    for (Node node : set) {
                        bw.write(String.format("%d\t%s\t%d\tSRID=4326;POINT(%.08f %.08f)", groupno, node.getNodeID(), set.size(), node.getLon(), node.getLat()));
                        bw.newLine();
                    }
                    System.out.printf("[Group %d] has %d nodes\n", groupno, set.size());
                    ++groupno;
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
            exp.printStackTrace();
        }
    }
}

