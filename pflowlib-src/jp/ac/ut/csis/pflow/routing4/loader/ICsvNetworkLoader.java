/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.io.File;
import jp.ac.ut.csis.pflow.routing4.loader.INetworkLoader;

public interface ICsvNetworkLoader
extends INetworkLoader {
    public Delimiter getDelimiter();

    public ICsvNetworkLoader setDelimiter(Delimiter var1);

    public File getNetworkFile();

    public ICsvNetworkLoader setNetworkFile(File var1);

    public boolean hasHeader();

    public ICsvNetworkLoader setHeaderFlag(boolean var1);

    public static enum Delimiter {
        CSV,
        TSV;

    }
}

