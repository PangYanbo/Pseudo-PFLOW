/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing3.loader;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import jp.ac.ut.csis.pflow.routing3.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing3.loader.NetworkLoadException;
import jp.ac.ut.csis.pflow.routing3.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing3.res.Link;
import jp.ac.ut.csis.pflow.routing3.res.Network;
import jp.ac.ut.csis.pflow.routing3.res.Node;
import org.apache.commons.lang3.text.StrTokenizer;

public abstract class ACsvNetworkLoader
extends ANetworkLoader {
    private File _networkFile;
    private boolean _hasHeader;
    private Delimiter _delimiter;
    private StrTokenizer _tokenizer;

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
        this._tokenizer = this._delimiter == Delimiter.CSV ? StrTokenizer.getCSVInstance() : StrTokenizer.getTSVInstance();
    }

    protected abstract Link parseLine(Network var1, String var2, boolean var3) throws NetworkLoadException;

    protected Delimiter getDelimiter() {
        return this._delimiter;
    }

    protected StrTokenizer getTokenizer() {
        return this._tokenizer;
    }

    @Override
    public Network load(QueryCondition[] qcs) {
        return this.load(qcs, true);
    }

    @Override
    public Network load(Network network, QueryCondition[] qcs, boolean needGeom) {
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(this._networkFile))) {
            if (this._hasHeader) {
                line = br.readLine();
            }
            while ((line = br.readLine()) != null) {
                Link link = this.parseLine(network, line, needGeom);
                if (link == null) continue;
                if (qcs != null && qcs.length > 0) {
                    if (!this.validate(qcs, link)) continue;
                    network.addLink(link);
                    continue;
                }
                network.addLink(link);
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
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
            if (bounds == null || bounds.contains(head.getLon(), head.getLat()) || bounds.contains(tail.getLon(), tail.getLat())) {
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

