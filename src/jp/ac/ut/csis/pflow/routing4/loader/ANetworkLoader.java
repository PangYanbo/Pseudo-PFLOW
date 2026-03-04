/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.loader;

import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.routing4.loader.INetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.QueryCondition;
import jp.ac.ut.csis.pflow.routing4.res.Network;

public abstract class ANetworkLoader
implements INetworkLoader {
    private List<IQueryCondition> _queryList;
    private boolean _needGeom;
    private Network _network;

    public ANetworkLoader() {
        this.init();
    }

    public INetworkLoader init() {
        this._queryList = new ArrayList<IQueryCondition>();
        this._needGeom = true;
        this._network = null;
        return this;
    }

    protected List<IQueryCondition> listQueries() {
        return this._queryList;
    }

    protected boolean needGeom() {
        return this._needGeom;
    }

    protected Network getNetwork() {
        return this._network;
    }

    @Override
    public INetworkLoader setGeometryFlag(boolean needGeom) {
        this._needGeom = needGeom;
        return this;
    }

    @Override
    public INetworkLoader setNetwork(Network network) {
        this._network = network;
        return this;
    }

    @Override
    public INetworkLoader setBounds(double x0, double y0, double x1, double y1, double bufSize) {
        this._queryList.clear();
        this._queryList.add(new QueryCondition(x0, y0, x1, y1, bufSize));
        return this;
    }

    @Override
    public INetworkLoader setBounds(double x0, double y0, double x1, double y1) {
        return this.setBounds(x0, y0, x1, y1, 0.0);
    }

    @Override
    public INetworkLoader addBounds(double x0, double y0, double x1, double y1, double bufSize) {
        this._queryList.add(new QueryCondition(x0, y0, x1, y1, bufSize));
        return this;
    }

    @Override
    public INetworkLoader addBounds(double x0, double y0, double x1, double y1) {
        return this.addBounds(x0, y0, x1, y1, 0.0);
    }

    @Override
    public INetworkLoader setBounds(double[] rect, double bufSize) {
        this._queryList.clear();
        this._queryList.add(new QueryCondition(rect, bufSize));
        return this;
    }

    @Override
    public INetworkLoader setBounds(double[] rect) {
        return this.setBounds(rect, 0.0);
    }

    @Override
    public INetworkLoader addBounds(double[] rect, double bufSize) {
        this._queryList.add(new QueryCondition(rect, bufSize));
        return this;
    }

    @Override
    public INetworkLoader addBounds(double[] rect) {
        return this.addBounds(rect, 0.0);
    }

    @Override
    public INetworkLoader setQueryCondition(IQueryCondition cond) {
        this._queryList.clear();
        this._queryList.add(cond);
        return this;
    }

    @Override
    public INetworkLoader setQueryConditions(IQueryCondition[] conds) {
        this._queryList.clear();
        IQueryCondition[] iQueryConditionArray = conds;
        int n = conds.length;
        int n2 = 0;
        while (n2 < n) {
            IQueryCondition cond = iQueryConditionArray[n2];
            this._queryList.add(cond);
            ++n2;
        }
        return this;
    }

    @Override
    public INetworkLoader addQueryCondition(IQueryCondition cond) {
        this._queryList.add(cond);
        return this;
    }
}

