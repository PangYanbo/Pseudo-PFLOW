/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.commons.lang3.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.interpolation.mt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;

public class Conv2PointLine {
    private Set<String> _ids = new HashSet<String>();

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static void main(String[] args) {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            String line = null;
            Conv2PointLine inst = new Conv2PointLine();
            while ((line = br.readLine()) != null) {
                List<String> tripStrs = inst.parse(line);
                for (String tripStr : tripStrs) {
                    bw.write(tripStr);
                    bw.newLine();
                }
            }
            System.out.println("valid IDs:" + inst._ids.size());
        }
        catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    /*
     * WARNING - void declaration
     */
    public List<String> parse(String line) {
        String[] tokens = StrTokenizer.getCSVInstance((String)line).getTokenArray();
        String uid = tokens[0];
        String seq = tokens[2];
        String mode = tokens[3];
        String traj = tokens[12];
        String nwId = tokens[13];
        String sf = tokens[14];
        this._ids.add(uid);
        String[] strs = StringUtils.split((String)traj, (char)';');
        ArrayList<String[]> array = new ArrayList<String[]>();
        int idx = 1;
        String[] stringArray = strs;
        int n = strs.length;
        int var13_16 = 0;
        while (var13_16 < n) {
            String str = stringArray[var13_16];
            String[] vals = StringUtils.splitByWholeSeparatorPreserveAllTokens((String)str, (String)"|");
            String[] out = new String[]{String.valueOf(idx++), vals[1], vals[2], vals[3], vals[4], vals[5], vals[6]};
            array.add(out);
            ++var13_16;
        }
        ArrayList<String> output = new ArrayList<String>();
        for (Object[] objectArray : array) {
            output.add(String.format("%s,%s,%s,%s,%s,%s", uid, seq, mode, StringUtils.join((Object[])objectArray, (String)","), nwId, sf));
        }
        return output;
    }
}

