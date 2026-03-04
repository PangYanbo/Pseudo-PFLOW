/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.routing2.loader;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import jp.ac.ut.csis.pflow.routing2.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing2.loader.NetworkLoadException;
import jp.ac.ut.csis.pflow.routing2.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Network;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ACsvNetworkLoader
extends ANetworkLoader {
    private static final Logger LOGGER = LogManager.getLogger(ACsvNetworkLoader.class);
    private File _networkFile;
    private boolean _hasHeader;
    private Delimiter _delimiter;

    public ACsvNetworkLoader(File networkfile) {
        this(networkfile, true);
    }

    public ACsvNetworkLoader(File networkfile, boolean hasHeader) {
        this(networkfile, hasHeader, Delimiter.CSV);
    }

    public ACsvNetworkLoader(File networkfile, Delimiter delimType) {
        this(networkfile, true, delimType);
    }

    public ACsvNetworkLoader(File networkfile, boolean hasHeader, Delimiter delimType) {
        this._networkFile = networkfile;
        this._delimiter = delimType;
        this._hasHeader = hasHeader;
    }

    protected abstract Link parseLine(Network var1, String var2) throws NetworkLoadException;

    protected Delimiter getDelimiter() {
        return this._delimiter;
    }

    @Override
    public Network load(QueryCondition[] qcs) {
        return this.load(new Network(), qcs);
    }

    @Override
    public Network load(Network network, QueryCondition[] qcs) {
        String line = null;
        try {
            Throwable throwable = null;
            Object var5_7 = null;
            try (BufferedReader br = new BufferedReader(new FileReader(this._networkFile));){
                if (this._hasHeader) {
                    line = br.readLine();
                }
                while ((line = br.readLine()) != null) {
                    Link link = this.parseLine(network, line);
                    if (link == null) continue;
                    if (qcs != null && qcs.length > 0) {
                        if (!this.validate(qcs, link)) continue;
                        network.addLink(link);
                        continue;
                    }
                    network.addLink(link);
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
            LOGGER.error("fail to load network", (Throwable)exp);
        }
        return network;
    }

    protected boolean validate(QueryCondition[] qcs, Link link) {
        Node head = link.getHeadNode();
        Node tail = link.getTailNode();
        QueryCondition[] queryConditionArray = qcs;
        int n = qcs.length;
        int n2 = 0;
        while (n2 < n) {
            QueryCondition qc = queryConditionArray[n2];
            Rectangle2D bounds = qc.getBounds();
            if (bounds.contains(head.getLon(), head.getLat()) || bounds.contains(tail.getLon(), tail.getLat())) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    public static enum Delimiter {
        CSV,
        TSV;

    }
}

