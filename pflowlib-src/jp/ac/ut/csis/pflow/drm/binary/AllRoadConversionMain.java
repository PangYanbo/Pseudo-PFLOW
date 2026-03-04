/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang.StringUtils
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.drm.binary;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM31;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM32;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.geom.Mesh;
import jp.ac.ut.csis.pflow.geom.TrajectoryUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;

public class AllRoadConversionMain {
    public static void main(String[] args) throws IOException {
        File drm31dir = new File(args[0]);
        File drm32dir = new File(args[1]);
        File linkFile = new File(args[2]);
        File nodeFile = new File(args[3]);
        File stderr = new File(args[4]);
        try {
            Throwable throwable = null;
            Object var7_9 = null;
            try (PrintStream pw = new PrintStream(stderr);){
                System.setErr(pw);
                new AllRoadConversionMain().invoke(drm31dir, drm32dir, linkFile, nodeFile);
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

    protected void invoke(File drm31dir, File drm32dir, File linkFile, File nodeFile) {
        Map<String, String> bounds = this.loadBoundaryNodes(drm31dir);
        File[] files = drm32dir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".tsv");
            }
        });
        LinkedHashMap<String, Integer> nodelist = new LinkedHashMap<String, Integer>();
        StrTokenizer st = StrTokenizer.getTSVInstance();
        try {
            Throwable throwable = null;
            Object var10_12 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(linkFile));){
                bw.write(this.composeLinkOutputHeader());
                bw.newLine();
                File[] fileArray = files;
                int n = files.length;
                int n2 = 0;
                while (n2 < n) {
                    File f = fileArray[n2];
                    System.out.println("\tload link file: " + f.getName());
                    String mcode = this.extractMeshCode(f.getName());
                    try {
                        Throwable throwable2 = null;
                        Object var18_23 = null;
                        try (BufferedReader br = new BufferedReader(new FileReader(f));){
                            String line = br.readLine();
                            while ((line = br.readLine()) != null) {
                                String[] tokens = st.reset(line).getTokenArray();
                                int nodeNo01 = this.assignNodeNo(String.valueOf(mcode) + tokens[1], nodelist, bounds);
                                int nodeNo02 = this.assignNodeNo(String.valueOf(mcode) + tokens[2], nodelist, bounds);
                                String[] pretokens = tokens;
                                Mesh mesh = new Mesh(mcode);
                                List<LonLat> geom = this.composeGeometry(mesh, tokens, br);
                                if (geom.isEmpty()) {
                                    System.err.printf("emptry geometry: (%s) %s %s - %s %s\n", f.getName(), mcode, tokens[1], mcode, tokens[2]);
                                    continue;
                                }
                                String stdout = this.composeLinkOutputString(mcode, pretokens, nodeNo01, nodeNo02, geom);
                                bw.write(stdout);
                                bw.newLine();
                            }
                        }
                        catch (Throwable throwable3) {
                            if (throwable2 == null) {
                                throwable2 = throwable3;
                            } else if (throwable2 != throwable3) {
                                throwable2.addSuppressed(throwable3);
                            }
                            throw throwable2;
                        }
                    }
                    catch (IOException exp) {
                        exp.printStackTrace();
                    }
                    ++n2;
                }
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                } else if (throwable != throwable4) {
                    throwable.addSuppressed(throwable4);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
        this.exportNode(drm31dir, nodeFile, nodelist);
        System.out.println("all done");
    }

    private String extractMeshCode(String filename) {
        return StringUtils.substringBetween((String)filename, (String)"_", (String)".");
    }

    private List<LonLat> composeGeometry(Mesh mesh, String[] tokens, BufferedReader br) throws IOException {
        Rectangle2D.Double rect = mesh.getRect();
        ArrayList<LonLat> points = new ArrayList<LonLat>();
        int pnum = Integer.parseInt(tokens[14]);
        StrTokenizer st = StrTokenizer.getTSVInstance();
        int i = 0;
        while (i < pnum) {
            int n = i % 21;
            double x = rect.getMinX() + ((RectangularShape)rect).getWidth() * (double)Integer.parseInt(tokens[15 + n * 2]) / 10000.0;
            double y = rect.getMinY() + ((RectangularShape)rect).getHeight() * (double)Integer.parseInt(tokens[15 + (n * 2 + 1)]) / 10000.0;
            points.add(new LonLat(x, y));
            if ((i + 1) % 21 == 0 && i + 1 < pnum) {
                String nextLine = br.readLine();
                tokens = st.reset(nextLine).getTokenArray();
            }
            ++i;
        }
        return points;
    }

    private String composeLinkOutputHeader() {
        ArrayList<String> output = new ArrayList<String>();
        output.addAll(ParseDRM32.COLUMNS.subList(0, 14));
        output.addAll(ParseDRM32.COLUMNS.subList(57, 59));
        output.addAll(Arrays.asList("meshcode", "sourceNode", "targetNode", "ewkt"));
        return StringUtils.join(output, (String)"\t");
    }

    private String composeLinkOutputString(String meshcode, String[] tokens, int nodeNo01, int nodeNo02, List<LonLat> geom) {
        Object[] tokens00 = (String[])ArrayUtils.subarray((Object[])tokens, (int)0, (int)14);
        Object[] tokens01 = (String[])ArrayUtils.subarray((Object[])tokens, (int)57, (int)59);
        String wkt = "SRID=4301;" + TrajectoryUtils.asWKT(geom);
        String srcNode = String.valueOf(nodeNo01);
        String tgtNode = String.valueOf(nodeNo02);
        Object[] output = (String[])ArrayUtils.addAll((Object[])tokens00, (Object[])tokens01);
        output = (String[])ArrayUtils.addAll((Object[])output, (Object[])new String[]{meshcode, srcNode, tgtNode, wkt});
        return StringUtils.join((Object[])output, (String)"\t");
    }

    private int assignNodeNo(String node, Map<String, Integer> nodelist, Map<String, String> bounds) {
        int nodeNo = -1;
        if (nodelist.containsKey(node)) {
            nodeNo = nodelist.get(node);
        } else {
            String boundaryNode;
            if (bounds.containsKey(node) && nodelist.containsKey(boundaryNode = bounds.get(node))) {
                nodeNo = nodelist.get(boundaryNode);
                nodelist.put(node, nodeNo);
            }
            if (nodeNo < 0) {
                nodeNo = nodelist.size() + 1;
                nodelist.put(node, nodeNo);
            }
        }
        return nodeNo;
    }

    private Map<String, String> loadBoundaryNodes(File drm31dir) {
        LinkedHashMap<String, String> nodemap = new LinkedHashMap<String, String>();
        File[] files = drm31dir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".tsv");
            }
        });
        StrTokenizer st = StrTokenizer.getTSVInstance();
        File[] fileArray = files;
        int n = files.length;
        int n2 = 0;
        while (n2 < n) {
            File f = fileArray[n2];
            System.out.println("parsing " + f.getName());
            String meshcode = StringUtils.substringBeforeLast((String)f.getName(), (String)".");
            try {
                Throwable throwable = null;
                Object var11_13 = null;
                try (BufferedReader br = new BufferedReader(new FileReader(f));){
                    String line = br.readLine();
                    while ((line = br.readLine()) != null) {
                        String[] tokens = st.reset(line).getTokenArray();
                        String node0 = String.valueOf(meshcode) + tokens[1];
                        String node1 = String.valueOf(tokens[5]) + tokens[6];
                        if (node1.equals("0000000000")) continue;
                        nodemap.put(node0, node1);
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
            ++n2;
        }
        System.out.println("complete loading DRM31");
        return nodemap;
    }

    private void exportNode(File drm31dir, File nodefile, Map<String, Integer> nodelist) {
        File[] files = drm31dir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".tsv");
            }
        });
        try {
            Throwable throwable = null;
            Object var6_8 = null;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(nodefile));){
                bw.write(String.valueOf(StringUtils.join(ParseDRM31.COLUMNS, (String)"\t")) + "\tmeshcode\tnodeNo\tewkt");
                bw.newLine();
                StrTokenizer st = StrTokenizer.getTSVInstance();
                File[] fileArray = files;
                int n = files.length;
                int n2 = 0;
                while (n2 < n) {
                    File f = fileArray[n2];
                    System.out.println("\tnode output " + f.getName());
                    String meshcode = this.extractMeshCode(f.getName());
                    Mesh mesh = new Mesh(meshcode);
                    Rectangle2D.Double rect = mesh.getRect();
                    try {
                        Throwable throwable2 = null;
                        Object var17_22 = null;
                        try (BufferedReader br = new BufferedReader(new FileReader(f));){
                            String line = br.readLine();
                            while ((line = br.readLine()) != null) {
                                Object[] tokens = st.reset(line).getTokenArray();
                                int nameLen = Integer.parseInt(tokens[16]);
                                tokens[17] = nameLen == 0 ? "" : tokens[17].substring(0, nameLen);
                                String drmNodeNo = String.valueOf(meshcode) + tokens[1];
                                int nodeNo = nodelist.get(drmNodeNo);
                                double x = rect.getMinX() + ((RectangularShape)rect).getWidth() * (double)Integer.parseInt(tokens[2]) / 10000.0;
                                double y = rect.getMinY() + ((RectangularShape)rect).getHeight() * (double)Integer.parseInt(tokens[3]) / 10000.0;
                                String ewkt = String.format("SRID=4301;POINT(%.08f %.08f)", x, y);
                                bw.write(String.format("%s\t%s\t%d\t%s", StringUtils.join((Object[])tokens, (String)"\t"), meshcode, nodeNo, ewkt));
                                bw.newLine();
                            }
                        }
                        catch (Throwable throwable3) {
                            if (throwable2 == null) {
                                throwable2 = throwable3;
                            } else if (throwable2 != throwable3) {
                                throwable2.addSuppressed(throwable3);
                            }
                            throw throwable2;
                        }
                    }
                    catch (IOException exp) {
                        exp.printStackTrace();
                    }
                    ++n2;
                }
            }
            catch (Throwable throwable4) {
                if (throwable == null) {
                    throwable = throwable4;
                } else if (throwable != throwable4) {
                    throwable.addSuppressed(throwable4);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }
}

