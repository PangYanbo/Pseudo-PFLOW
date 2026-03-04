/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.tools;

public class MathUtils {
    public static int gcd(int a, int b) {
        int max = Math.max(a, b);
        int min = Math.min(a, b);
        if (min == 0) {
            return max;
        }
        return MathUtils.gcd(min, max % min);
    }
}

