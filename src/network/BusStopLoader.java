package network;

import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads bus stop locations from National Land Numerical Information (NLNI/KSJ) GML zip files.
 *
 * Expected file: processing/bus_stop/P11-22_XX_GML.zip containing P11-22_XX.xml
 * Format: {@code <gml:Point gml:id="nN"><gml:pos>LAT LON</gml:pos></gml:Point>}
 * Coordinates are JGD2011 (lat, lon) — effectively WGS84 for our purposes.
 *
 * Duplicate coordinates are deduplicated (same physical stop served by multiple routes).
 */
public class BusStopLoader {

    private static final Pattern POS_PATTERN = Pattern.compile("<gml:pos>([\\d.]+)\\s+([\\d.]+)</gml:pos>");

    /**
     * Load bus stops for a single prefecture from its GML zip file.
     *
     * @param zipPath path to P11-22_XX_GML.zip
     * @return Network with one Node per unique bus stop location
     */
    public static Network load(String zipPath) {
        return load(zipPath, true);
    }

    /**
     * Load bus stops with optional node spatial index.
     */
    public static Network load(String zipPath, boolean makeNodeIndex) {
        Network net = new Network(makeNodeIndex, false);
        Set<String> seen = new HashSet<>();
        int total = 0;
        int deduped = 0;

        try (ZipFile zf = new ZipFile(zipPath)) {
            // Find the main XML file (P11-22_XX.xml, not KS-META)
            ZipEntry xmlEntry = null;
            for (ZipEntry entry : java.util.Collections.list(zf.entries())) {
                String name = entry.getName();
                if (name.endsWith(".xml") && !name.contains("KS-META")) {
                    xmlEntry = entry;
                    break;
                }
            }
            if (xmlEntry == null) {
                System.err.println("[BusStopLoader] No GML XML found in " + zipPath);
                return net;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(xmlEntry), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = POS_PATTERN.matcher(line);
                    while (m.find()) {
                        total++;
                        double lat = Double.parseDouble(m.group(1));
                        double lon = Double.parseDouble(m.group(2));
                        // Deduplicate by rounded coordinates (5 decimal places ≈ 1.1m)
                        String key = String.format("%.5f,%.5f", lat, lon);
                        if (seen.add(key)) {
                            String nodeId = "bus_" + seen.size();
                            net.addNode(new Node(nodeId, lon, lat));
                        } else {
                            deduped++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load bus stops from " + zipPath, e);
        }

        System.out.printf("[BusStopLoader] %s: %d stops total, %d unique, %d duplicates%n",
                zipPath, total, total - deduped, deduped);
        return net;
    }

    /**
     * Build the zip file path for a given prefecture code.
     *
     * @param busStopDir directory containing P11-22_XX_GML.zip files
     * @param prefCode   prefecture code (1-47)
     * @return path to the zip file
     */
    public static String getZipPath(String busStopDir, int prefCode) {
        return String.format("%s/P11-22_%02d_GML.zip", busStopDir, prefCode);
    }
}
