/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.tools;

public class TextUtils {
    public static final String DELIMITER = System.getProperty("pflow.textutils.delimiter", ",");

    public static String toString(int[] array) {
        return TextUtils.toString(array, DELIMITER);
    }

    public static String toString(int[] array, String delim) {
        StringBuffer buf = new StringBuffer();
        int[] nArray = array;
        int n = array.length;
        int n2 = 0;
        while (n2 < n) {
            int d = nArray[n2];
            buf.append(delim).append(d);
            ++n2;
        }
        return buf.substring(delim.length());
    }

    public static String toString(long[] array) {
        return TextUtils.toString(array, DELIMITER);
    }

    public static String toString(long[] array, String delim) {
        StringBuffer buf = new StringBuffer();
        long[] lArray = array;
        int n = array.length;
        int n2 = 0;
        while (n2 < n) {
            long d = lArray[n2];
            buf.append(delim).append(d);
            ++n2;
        }
        return buf.substring(delim.length());
    }

    public static String toString(double[] array) {
        return TextUtils.toString(array, DELIMITER);
    }

    public static String toString(double[] array, String delim) {
        StringBuffer buf = new StringBuffer();
        double[] dArray = array;
        int n = array.length;
        int n2 = 0;
        while (n2 < n) {
            double d = dArray[n2];
            buf.append(delim).append(d);
            ++n2;
        }
        return buf.substring(delim.length());
    }
}

