/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing2.logic;

import java.util.Arrays;
import jp.ac.ut.csis.pflow.routing2.logic.LinkCost;
import jp.ac.ut.csis.pflow.routing2.res.Link;
import jp.ac.ut.csis.pflow.routing2.res.Node;

public class HighwayLinkCost
extends LinkCost {
    private static final String[][] RESTRICTED_NODES = new String[][]{{"957", "1169", "411"}, {"411", "1169", "957"}, {"1628", "2223", "1609"}, {"1608", "1609", "2223"}, {"1629", "2223", "1609"}, {"1609", "2223", "1629"}, {"1611", "1632", "1631"}, {"1631", "1632", "1611"}, {"1632", "1611", "1612"}, {"1612", "1611", "1632"}, {"1611", "1632", "1631"}, {"1631", "1632", "1611"}, {"1632", "1611", "1612"}, {"1612", "1611", "1632"}, {"1748", "1747", "1619"}, {"1619", "1747", "1748"}, {"35", "2088", "2078"}, {"2078", "2088", "35"}, {"36", "2089", "2084"}, {"2084", "2089", "36"}, {"2068", "2046", "2045"}, {"2047", "2046", "2068"}, {"2047", "2046", "2045"}, {"2044", "2043", "2221"}, {"2044", "2043", "2058"}, {"2058", "2043", "2044"}, {"2058", "2043", "2221"}, {"2221", "2043", "2058"}, {"2043", "2221", "2895"}, {"2043", "2221", "2042"}, {"2895", "2221", "2042"}, {"2039", "2048", "2049"}, {"2039", "2048", "2047"}, {"2049", "2048", "2047"}, {"2041", "2040", "2039"}, {"2041", "2040", "2058"}, {"2058", "2040", "2041"}, {"2058", "2040", "2039"}, {"2039", "2040", "2058"}, {"2059", "2041", "2040"}, {"2042", "2041", "2059"}, {"2042", "2041", "2040"}};

    @Override
    public double getCost(Link prevlink, Node crntNode, Link nextlink) {
        Node nextNode;
        if (prevlink == null) {
            return nextlink.getCost();
        }
        Node prevNode = prevlink.getTailNode();
        if (prevNode.equals(crntNode)) {
            prevNode = prevlink.getHeadNode();
        }
        if ((nextNode = nextlink.getHeadNode()).equals(crntNode)) {
            nextNode = nextlink.getTailNode();
        }
        boolean ngFlag = false;
        Object[] nodeOrder = new String[]{prevNode.getNodeID(), crntNode.getNodeID(), nextNode.getNodeID()};
        String[][] stringArray = RESTRICTED_NODES;
        int n = RESTRICTED_NODES.length;
        int n2 = 0;
        while (n2 < n) {
            Object[] restriction = stringArray[n2];
            if (ngFlag |= Arrays.equals(nodeOrder, restriction)) break;
            ++n2;
        }
        if (ngFlag) {
            return 2.147483647E9;
        }
        return nextlink.getCost();
    }

    @Override
    public double getReverseCost(Link prevlink, Node crntNode, Link nextlink) {
        Node nextNode;
        if (prevlink == null) {
            return nextlink.getReverseCost();
        }
        Node prevNode = prevlink.getTailNode();
        if (prevNode.equals(crntNode)) {
            prevNode = prevlink.getHeadNode();
        }
        if ((nextNode = nextlink.getHeadNode()).equals(crntNode)) {
            nextNode = nextlink.getTailNode();
        }
        boolean ngFlag = false;
        Object[] nodeOrder = new String[]{prevNode.getNodeID(), crntNode.getNodeID(), nextNode.getNodeID()};
        String[][] stringArray = RESTRICTED_NODES;
        int n = RESTRICTED_NODES.length;
        int n2 = 0;
        while (n2 < n) {
            Object[] restriction = stringArray[n2];
            if (ngFlag |= Arrays.equals(nodeOrder, restriction)) break;
            ++n2;
        }
        if (ngFlag) {
            return 2.147483647E9;
        }
        return nextlink.getReverseCost();
    }
}

