/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.drm.binary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import jp.ac.ut.csis.pflow.drm.binary.AParseDRM;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM21;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM22;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM23;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM31;
import jp.ac.ut.csis.pflow.drm.binary.ParseDRM32;
import org.apache.commons.lang.StringUtils;

public class ParseDRM {
    public static void main(String[] args) {
        int typeCode = Integer.parseInt(args[0]);
        File indir = new File(args[1]);
        File outdir = new File(args[2]);
        AParseDRM inst = ParseDRM.getInstance(typeCode);
        if (inst == null) {
            System.err.println("invalid type code::" + typeCode);
            System.exit(1);
        }
        File[] dirsMesh01 = indir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        final String fileExt = String.format(".%d", typeCode);
        File[] fileArray = dirsMesh01;
        int n = dirsMesh01.length;
        int n2 = 0;
        while (n2 < n) {
            File[] dirsMesh02;
            final File dir01 = fileArray[n2];
            System.out.println(String.valueOf(dir01.getName()) + "\tstarts");
            File[] fileArray2 = dirsMesh02 = dir01.listFiles(new FileFilter(){

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() && f.getName().startsWith(dir01.getName());
                }
            });
            int n3 = dirsMesh02.length;
            int n4 = 0;
            while (n4 < n3) {
                File[] files;
                File dir02 = fileArray2[n4];
                System.out.println("\t" + dir02.getName() + "\tstarts");
                File[] fileArray3 = files = dir02.listFiles(new FileFilter(){

                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(fileExt);
                    }
                });
                int n5 = files.length;
                int n6 = 0;
                while (n6 < n5) {
                    File file = fileArray3[n6];
                    System.out.println("\t\t" + file.getAbsolutePath() + "\tstarts");
                    if (inst.accept(file)) {
                        long dtstart = System.currentTimeMillis();
                        ParseDRM.convert(inst, file, new File(outdir, String.format("%s.tsv", file.getName())));
                        long dtend = System.currentTimeMillis();
                        System.out.println("\t\t\ttime : " + (double)(dtend - dtstart) / 1000.0 + "[sec]");
                    } else {
                        System.out.println(String.valueOf(file.getName()) + " is not accepted");
                    }
                    ++n6;
                }
                ++n4;
            }
            ++n2;
        }
    }

    protected static AParseDRM getInstance(int typeCode) {
        AParseDRM inst = null;
        switch (typeCode) {
            case 21: {
                inst = new ParseDRM21();
                break;
            }
            case 22: {
                inst = new ParseDRM22();
                break;
            }
            case 23: {
                inst = new ParseDRM23();
                break;
            }
            case 31: {
                inst = new ParseDRM31();
                break;
            }
            case 32: {
                inst = new ParseDRM32();
            }
        }
        return inst;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected static void convert(AParseDRM parser, File infile, File outfile) {
        try {
            Throwable throwable = null;
            Object var4_6 = null;
            try {
                FileInputStream stream = new FileInputStream(infile);
                try {
                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(outfile, false), "UTF-8"));){
                        bw.write(StringUtils.join(parser.listColumns(), (String)"\t"));
                        bw.newLine();
                        int len = parser.getRecordSize();
                        while (stream.available() > 0) {
                            byte[] buf = new byte[len];
                            int i = 0;
                            while (i < len) {
                                buf[i] = (byte)stream.read();
                                ++i;
                            }
                            List<String> record = parser.parseRecord(buf);
                            if (record == null) continue;
                            String stdout = StringUtils.join(record, (String)"\t");
                            bw.write(stdout);
                            bw.newLine();
                        }
                    }
                    if (stream == null) return;
                }
                catch (Throwable throwable2) {
                    if (throwable == null) {
                        throwable = throwable2;
                    } else if (throwable != throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    if (stream == null) throw throwable;
                    stream.close();
                    throw throwable;
                }
                stream.close();
                return;
            }
            catch (Throwable throwable3) {
                if (throwable == null) {
                    throwable = throwable3;
                    throw throwable;
                } else {
                    if (throwable == throwable3) throw throwable;
                    throwable.addSuppressed(throwable3);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }
}

