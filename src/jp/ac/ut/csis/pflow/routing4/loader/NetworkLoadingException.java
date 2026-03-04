/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

public class NetworkLoadingException
extends RuntimeException {
    private static final long serialVersionUID = -5412726366111612186L;

    public NetworkLoadingException(String msg, Throwable exp) {
        super(msg, exp);
    }

    public NetworkLoadingException(String msg) {
        super(msg);
    }
}

