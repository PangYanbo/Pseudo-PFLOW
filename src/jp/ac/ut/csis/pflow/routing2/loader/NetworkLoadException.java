/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.loader;

public class NetworkLoadException
extends RuntimeException {
    private static final long serialVersionUID = -6475871164592009744L;

    public NetworkLoadException(String msg, Exception exp) {
        super(msg, exp);
    }
}

