/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;
import jp.ac.ut.csis.pflow.routing2.res.RailwayLink;
import jp.ac.ut.csis.pflow.routing2.res.RailwayNode;
import org.apache.commons.lang.StringUtils;

public class RailwayLinkCost
extends LinkCost {
    public static double VELOCITY_RAILWAY = 11.11111111111111;
    public static double VELOCITY_WALK = 0.8333333333333333;
    public static double TIME_TRANSFER = 60.0;
    private double _railwayVelocity;
    private double _walkingVelocity;
    private double _waitingTime;

    public RailwayLinkCost() {
        this(VELOCITY_RAILWAY, VELOCITY_WALK, TIME_TRANSFER);
    }

    public RailwayLinkCost(double railwayVelocity, double walkingVelocity, double waitingTime) {
        this._railwayVelocity = railwayVelocity;
        this._walkingVelocity = walkingVelocity;
        this._waitingTime = waitingTime;
    }

    public double getRailwayVelocity() {
        return this._railwayVelocity;
    }

    public double getWalkingVelocity() {
        return this._walkingVelocity;
    }

    public double getWaitingTime() {
        return this._waitingTime;
    }

    @Override
    public double getCost(Link link) {
        return this.getCost(null, null, link);
    }

    @Override
    public double getCost(Link prevLink, Node crntNode, Link nextLink) {
        if (prevLink instanceof RailwayLink && nextLink instanceof RailwayLink) {
            String nextLineName;
            Node prevNode = crntNode.equals(prevLink.getHeadNode()) ? prevLink.getTailNode() : prevLink.getHeadNode();
            Node nextNode = crntNode.equals(nextLink.getTailNode()) ? nextLink.getHeadNode() : nextLink.getTailNode();
            String prevLineName = ((RailwayNode)RailwayNode.class.cast(prevNode)).getLineName();
            if (StringUtils.equals((String)prevLineName, (String)(nextLineName = ((RailwayNode)RailwayNode.class.cast(nextNode)).getLineName()))) {
                return nextLink.getCost() / this.getRailwayVelocity();
            }
            return nextLink.getCost() / this.getWalkingVelocity() + this.getWaitingTime();
        }
        return nextLink.getCost() / this.getRailwayVelocity();
    }

    @Override
    public double getReverseCost(Link link) {
        return this.getReverseCost(null, null, link);
    }

    @Override
    public double getReverseCost(Link prevLink, Node crntNode, Link nextLink) {
        if (prevLink instanceof RailwayLink && nextLink instanceof RailwayLink) {
            String nextLineName;
            Node prevNode = crntNode.equals(prevLink.getHeadNode()) ? prevLink.getTailNode() : prevLink.getHeadNode();
            Node nextNode = crntNode.equals(nextLink.getTailNode()) ? nextLink.getHeadNode() : nextLink.getTailNode();
            String prevLineName = ((RailwayNode)RailwayNode.class.cast(prevNode)).getLineName();
            if (StringUtils.equals((String)prevLineName, (String)(nextLineName = ((RailwayNode)RailwayNode.class.cast(nextNode)).getLineName()))) {
                return nextLink.getReverseCost() / this.getRailwayVelocity();
            }
            return nextLink.getReverseCost() / this.getWalkingVelocity() + this.getWaitingTime();
        }
        return nextLink.getReverseCost() / this.getRailwayVelocity();
    }
}

