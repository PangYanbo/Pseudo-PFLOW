/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.text.StrTokenizer
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import jp.ac.ut.csis.pflow.routing4.loader.ANetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.ICsvNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.NetworkLoadingException;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.apache.commons.lang.text.StrTokenizer;

public abstract class ACsvNetworkLoader
extends ANetworkLoader
implements ICsvNetworkLoader {
    private File _networkFile;
    private boolean _hasHeader;
    private ICsvNetworkLoader.Delimiter _delimiter;

    public ACsvNetworkLoader(File networkfile) {
        this(networkfile, true, ICsvNetworkLoader.Delimiter.CSV);
    }

    public ACsvNetworkLoader(File networkfile, boolean hasHeader) {
        this(networkfile, hasHeader, ICsvNetworkLoader.Delimiter.CSV);
    }

    public ACsvNetworkLoader(File networkfile, ICsvNetworkLoader.Delimiter delimType) {
        this(networkfile, true, delimType);
    }

    public ACsvNetworkLoader(File networkfile, boolean hasHeader, ICsvNetworkLoader.Delimiter delimType) {
        this._networkFile = networkfile;
        this._delimiter = delimType;
        this._hasHeader = hasHeader;
    }

    protected abstract Link parseLine(String var1) throws NetworkLoadingException;

    @Override
    public ICsvNetworkLoader.Delimiter getDelimiter() {
        return this._delimiter;
    }

    @Override
    public ICsvNetworkLoader setDelimiter(ICsvNetworkLoader.Delimiter delimiter) {
        this._delimiter = delimiter;
        return this;
    }

    @Override
    public File getNetworkFile() {
        return this._networkFile;
    }

    @Override
    public ICsvNetworkLoader setNetworkFile(File networkFile) {
        this._networkFile = networkFile;
        return this;
    }

    @Override
    public boolean hasHeader() {
        return this._hasHeader;
    }

    @Override
    public ICsvNetworkLoader setHeaderFlag(boolean hasHeader) {
        this._hasHeader = hasHeader;
        return this;
    }

    protected StrTokenizer getTokenizer() {
        return this._delimiter == ICsvNetworkLoader.Delimiter.CSV ? StrTokenizer.getCSVInstance() : StrTokenizer.getTSVInstance();
    }

    @Override
    public Network load() throws NetworkLoadingException {
        File networkFile = this.getNetworkFile();
        boolean hasHeader = this.hasHeader();
        Network network = this.getNetwork();
        if (networkFile == null) {
            throw new NetworkLoadingException("no network file specified");
        }
        if (network == null) {
            network = new Network();
            this.setNetwork(network);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(this._networkFile))) {
            String line = null;
            if (hasHeader) {
                line = br.readLine();
            }
            while ((line = br.readLine()) != null) {
                Link link = this.parseLine(line);
                if (!this.validate(link)) continue;
                network.addLink(link);
            }
        }
        catch (IOException exp) {
            exp.printStackTrace();
            throw new NetworkLoadingException("failed to load network", exp);
        }
        return network;
    }

    protected boolean validate(Link link) {
        if (link == null) {
            return false;
        }
        List<IQueryCondition> queries = this.listQueries();
        if (queries == null || queries.isEmpty()) {
            return true;
        }
        Node head = link.getHeadNode();
        Node tail = link.getTailNode();
        for (IQueryCondition qc : queries) {
            Rectangle2D bounds = qc.getBounds();
            if (bounds != null && !bounds.contains(head.getLon(), head.getLat()) && !bounds.contains(tail.getLon(), tail.getLat())) continue;
            return true;
        }
        return false;
    }
}

