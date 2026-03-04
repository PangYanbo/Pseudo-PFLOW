/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.routing4.mapmatching;

import java.util.List;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.routing4.mapmatching.MatchingResult;
import jp.ac.ut.csis.pflow.routing4.res.Network;

public interface IMatching {
    public static final double SEARCH_RANGE = 1000.0;

    public <T extends ILonLat> List<MatchingResult> runMatching(Network var1, List<T> var2);

    public <T extends ILonLat> List<MatchingResult> runMatching(Network var1, List<T> var2, double var3);

    public <T extends ILonLat> MatchingResult runMatching(Network var1, T var2);

    public <T extends ILonLat> MatchingResult runMatching(Network var1, T var2, double var3);
}

