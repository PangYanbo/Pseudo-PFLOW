/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing3.loader;

public class NetworkLoadException
extends RuntimeException {
    private static final long serialVersionUID = -1096198895886844980L;

    public NetworkLoadException(String msg, Exception exp) {
        super(msg, exp);
    }
}

