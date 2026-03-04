/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.logic;

import java.util.List;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;

public class AStar
extends Dijkstra {
    public AStar(double minDist, AStarLinkCost linkcost) {
        super(minDist, linkcost);
    }

    public AStar(AStarLinkCost linkcost) {
        this(3000.0, linkcost);
    }

    public AStar() {
        this(new AStarLinkCost());
    }

    @Override
    public String getName() {
        return "AStar";
    }

    @Override
    public AStarLinkCost getLinkCost() {
        return (AStarLinkCost)AStarLinkCost.class.cast(super.getLinkCost());
    }

    @Override
    public List<Route> getRoutes(Network network, Node depnode, Node arrnode, int N) {
        this.getLinkCost().setNodes(depnode, arrnode);
        return super.getRoutes(network, depnode, arrnode, N);
    }
}

