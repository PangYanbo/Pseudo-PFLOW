/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.time.DateUtils
 */
package jp.ac.ut.csis.pflow.interpolation;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom2.DateTimeUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.Mesh;
import jp.ac.ut.csis.pflow.geom2.MeshUtils;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.interpolation.trip.Blank;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.STNetworkPoint;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.INetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IPgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import org.apache.commons.lang.time.DateUtils;

public class TripInterpolation {
    private static final int[] ROAD_TYPES = new int[]{1, 2, 3, 4, 5, 6, 7};
    private PgLoader _pgLoader;
    private INetworkLoader _roadNetworkLoader;
    private Network _railwayNetwork;
    private Network _roadNetwork;
    private Map<String, Integer> _network_master;
    private SparseMapMatching _roadMatching;
    private Dijkstra _roadMatchingLogic = new Dijkstra(new LinkCost(DrmTransport.VEHICLE));
    private SparseMapMatching _railwayMatching;
    private List<ITrip> _trips;
    private boolean _enableTimeSplit = false;
    private long _timeInterval = 300L;

    public TripInterpolation() {
        this._roadMatching = new SparseMapMatching(this._roadMatchingLogic);
        this._railwayMatching = new SparseMapMatching(new Dijkstra(new RailwayLinkCost()));
    }

    public void setPgLoader(PgLoader pgLoader) {
        this._pgLoader = pgLoader;
    }

    public void setNetworkLoader(INetworkLoader networkLoader) {
        this._roadNetworkLoader = networkLoader;
    }

    public TripInterpolation setRoadNetwork(Network roadNetwork) {
        this._roadNetwork = roadNetwork;
        return this;
    }

    public TripInterpolation setNetworkMaster(Map<String, Integer> networkMaster) {
        this._network_master = networkMaster;
        return this;
    }

    public void setRailwayNetwork(Network railwayNetwork) {
        this._railwayNetwork = railwayNetwork;
    }

    public void setTrips(List<ITrip> trips) {
        this._trips = trips;
    }

    public void setTimeInterval(int interval) {
        this._enableTimeSplit = true;
        this._timeInterval = interval;
    }

    public List<List<String>> doMain() throws IOException {
        ArrayList<List<String>> output = new ArrayList<List<String>>();
        for (ITrip trip : this._trips) {
            List<String> out = null;
            try {
                switch (trip.getTransportMode()) {
                    case STAY: {
                        out = this.interpolateStay((Stay)Stay.class.cast(trip));
                        break;
                    }
                    case BLANK: {
                        out = this.interpolateBlank((Blank)Blank.class.cast(trip));
                        break;
                    }
                    case TRAIN: {
                        out = this.interpolateRailway((Move)Move.class.cast(trip));
                        break;
                    }
                    case WALK: {
                        this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.WALK);
                        out = this.interpolateRoad((Move)Move.class.cast(trip));
                        break;
                    }
                    case BIKE: {
                        this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.BIKE);
                        out = this.interpolateRoad((Move)Move.class.cast(trip));
                        break;
                    }
                    case CAR: {
                        this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.VEHICLE);
                        out = this.interpolateRoad((Move)Move.class.cast(trip));
                        break;
                    }
                    default: {
                        out = this.interpolateRoad((Move)Move.class.cast(trip));
                        break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println("Dijkstra error");
            }
            if (out == null) {
                ArrayList<String> err = new ArrayList<String>();
                err.add("error");
                Move m = (Move)trip;
                err.add(m.getUid());
                err.add(m.getTransportMode().toString());
                for (ILonLatTime illt : m.getTrajectory()) {
                    SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    err.add(String.valueOf(sf.format(illt.getTimeStamp())) + "," + illt.getLat() + "," + illt.getLon());
                }
                output.add(err);
                continue;
            }
            output.add(out);
        }
        return output;
    }

    public <T extends ILonLat> double[] createRect(T point) {
        return this.createRect(Arrays.asList(point));
    }

    private <T extends ILonLat> double[] createRect(List<T> points) {
        Rectangle2D.Double rect = TrajectoryUtils.makeMBR(points);
        return new double[]{rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY()};
    }

    private List<String> interpolateRoad(Move trip) {
        List<ILonLatTime> trajectory = trip.getTrajectory();
        ILonLatTime ps = trajectory.get(0);
        ILonLatTime pe = trajectory.get(trajectory.size() - 1);
        Network roadNetwork = null;
        if (this._roadNetworkLoader instanceof IPgNetworkLoader) {
            try {
                Throwable throwable = null;
                Object var7_9 = null;
                try (Connection con = this._pgLoader.getConnection();){
                    IQueryCondition[] conditions = new DrmQueryCondition[]{new DrmQueryCondition(this.createRect(ps), 3000.0), new DrmQueryCondition(this.createRect(trajectory), 3000.0, ROAD_TYPES), new DrmQueryCondition(this.createRect(pe), 3000.0)};
                    IPgNetworkLoader l = (IPgNetworkLoader)this._roadNetworkLoader;
                    roadNetwork = l.setConnection(con).setQueryConditions(conditions).load();
                    roadNetwork._networkId = this._network_master.get(l.getTableName());
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
            catch (NullPointerException | SQLException exp) {
                exp.printStackTrace();
                System.err.println(" [FATAL] postgress error ");
                System.exit(1);
            }
        } else {
            roadNetwork = this._roadNetwork;
        }
        Date st = new Date();
        Dijkstra logic = new Dijkstra(new LinkCost(DrmTransport.WALK));
        SparseMapMatching matchingLogic = new SparseMapMatching(logic);
        Route route = matchingLogic.setMatchingType(SparseMapMatching.MatchingType.LINK).setSearchRange(3000.0).runSparseMapMatching(roadNetwork, trajectory);
        Date ed = new Date();
        long duration = (ed.getTime() - st.getTime()) / 1000L;
        if (duration > 60L) {
            System.err.println(duration);
            for (ILonLatTime illt : trajectory) {
                System.err.println(illt.getTimeStamp() + "," + illt.getLat() + "," + illt.getLon());
            }
        }
        if (route != null && !route.isEmpty()) {
            List<Node> nodes = route.listNodes();
            List<Link> links = route.listLinks();
            if (nodes.size() >= 2) {
                List<ILonLat> routePoints = route.getTrajectory();
                routePoints.add(0, ps);
                routePoints.add(pe);
                List<STNetworkPoint> result = this.assignRouteTimestamp(trajectory, routePoints, nodes, links);
                if (this._enableTimeSplit) {
                    result = this.insertUnitTimePoint(result, this._timeInterval);
                }
                return this.format(trip, result, TrajectoryUtils.length(trajectory), roadNetwork._networkId);
            }
            return null;
        }
        return null;
    }

    private List<String> interpolateRailway(Move trip) {
        List<ILonLatTime> trajectory = trip.getTrajectory();
        try {
            Route route;
            if (this._railwayNetwork == null) {
                System.err.println("[FATAL] railwayNetowk is null");
                System.exit(1);
            }
            if ((route = this._railwayMatching.runSparseMapMatching(this._railwayNetwork, trajectory)) != null && !route.isEmpty()) {
                List<Node> nodes = route.listNodes();
                List<Link> links = route.listLinks();
                if (nodes.size() >= 2) {
                    List<ILonLat> routePoints = route.getTrajectory();
                    List<STNetworkPoint> result = this.assignRouteTimestamp(trajectory, routePoints, nodes, links);
                    if (this._enableTimeSplit) {
                        result = this.insertUnitTimePoint(result, this._timeInterval);
                    }
                    return this.format(trip, result, TrajectoryUtils.length(trajectory), this._railwayNetwork._networkId);
                }
                return null;
            }
            return null;
        }
        catch (Exception exp) {
            exp.printStackTrace();
            return null;
        }
    }

    private List<String> interpolateBlank(Blank trip) {
        ILonLatTime point = trip.getStartPoint();
        Mesh mesh = MeshUtils.createMesh(6, point.getLon(), point.getLat());
        int idx = 1;
        StringBuffer buf = new StringBuffer();
        buf.append(String.format("%d|%d|%.08f|%.08f|||%s", idx++, trip.getStartTime().getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
        if (this._enableTimeSplit) {
            List<Date> timeList = this.getTimestampWithin(trip.getStartTime(), trip.getEndTime(), this._timeInterval);
            for (Date dt : timeList) {
                buf.append(String.format(";%d|%d|%.08f|%.08f|||%s", idx++, dt.getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
            }
        }
        buf.append(String.format(";%d|%d|%.08f|%.08f|||%s", idx, trip.getEndTime().getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
        return Arrays.asList(trip.getTransportMode().toString(), "STAY", "", "", "0", String.valueOf(DateTimeUtils.getDuration(trip.getStartTime(), trip.getEndTime()) / 1000L), String.valueOf(trip.getStartTime().getTime() / 1000L), String.valueOf(trip.getEndTime().getTime() / 1000L), String.valueOf(idx), buf.toString(), "", "");
    }

    private List<String> interpolateStay(Stay trip) {
        ILonLatTime point = trip.getStartPoint();
        Mesh mesh = MeshUtils.createMesh(6, point.getLon(), point.getLat());
        int idx = 1;
        StringBuffer buf = new StringBuffer();
        buf.append(String.format("%d|%d|%.08f|%.08f|||%s", idx++, trip.getStartTime().getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
        if (this._enableTimeSplit) {
            List<Date> timeList = this.getTimestampWithin(trip.getStartTime(), trip.getEndTime(), this._timeInterval);
            for (Date dt : timeList) {
                buf.append(String.format(";%d|%d|%.08f|%.08f|||%s", idx++, dt.getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
            }
        }
        buf.append(String.format(";%d|%d|%.08f|%.08f|||%s", idx, trip.getEndTime().getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
        return Arrays.asList(trip.getTransportMode().toString(), "STAY", "", "", "0", String.valueOf(DateTimeUtils.getDuration(trip.getStartTime(), trip.getEndTime()) / 1000L), String.valueOf(trip.getStartTime().getTime() / 1000L), String.valueOf(trip.getEndTime().getTime() / 1000L), String.valueOf(idx), buf.toString(), "", "");
    }

    private List<String> format(ITrip trip, List<STNetworkPoint> result, double dist, int networkId) {
        StringBuffer buf = new StringBuffer();
        int num = result.size();
        int i = 0;
        while (i < num - 1) {
            STNetworkPoint point = result.get(i);
            Date ts = point.getTimeStamp();
            Node node = point.getNode();
            Link link = point.getLink();
            Mesh mesh = MeshUtils.createMesh(6, point.getLon(), point.getLat());
            String token = String.format(";%d|%d|%.08f|%.08f|%s|%s|%s", i + 1, ts.getTime() / 1000L, point.getLat(), point.getLon(), node != null ? node.getNodeID() : "", link != null ? link.getLinkID() : "", mesh.getCode());
            buf.append(token);
            ++i;
        }
        STNetworkPoint lastPoint = result.get(num - 1);
        Date ts = lastPoint.getTimeStamp();
        Node node = lastPoint.getNode();
        Mesh mesh = MeshUtils.createMesh(6, lastPoint.getLon(), lastPoint.getLat());
        buf.append(String.format(";%d|%d|%.08f|%.08f|%s||%s", result.size(), ts.getTime() / 1000L, lastPoint.getLat(), lastPoint.getLon(), node != null ? node.getNodeID() : "", mesh.getCode()));
        return Arrays.asList(trip.getTransportMode().toString(), "MOVE", "", "", String.format("%.03f", dist), String.valueOf(DateTimeUtils.getDuration(trip.getStartTime(), trip.getEndTime()) / 1000L), String.valueOf(trip.getStartTime().getTime() / 1000L), String.valueOf(trip.getEndTime().getTime() / 1000L), String.valueOf(result.size()), buf.substring(1), String.valueOf(networkId), "");
    }

    protected List<STNetworkPoint> assignRouteTimestamp(List<ILonLatTime> baseList, List<ILonLat> routePoints, List<Node> targetNodeList, List<Link> targetLinkList) {
        LinkedHashMap<Node, STNetworkPoint> result = new LinkedHashMap<Node, STNetworkPoint>();
        double length = TrajectoryUtils.length(routePoints);
        int index01 = 0;
        double[] baseRatioArray = new double[baseList.size()];
        for (ILonLatTime point : baseList) {
            baseRatioArray[index01++] = TrajectoryUtils.getLocatePointRatio(routePoints, point);
        }
        if (0.0 < baseRatioArray[0]) {
            baseRatioArray[0] = 0.0;
        }
        if (baseRatioArray[index01 - 1] < 1.0) {
            baseRatioArray[index01 - 1] = 1.0;
        }
        Arrays.sort(baseRatioArray);
        int index02 = 0;
        double[] nodeRatioArray = new double[targetNodeList.size()];
        for (Node node : targetNodeList) {
            nodeRatioArray[index02++] = TrajectoryUtils.getLocatePointRatio(routePoints, node);
        }
        ILonLatTime p0 = baseList.get(0);
        double r0 = baseRatioArray[0];
        int i = 1;
        while (i < baseList.size()) {
            ILonLatTime p1 = baseList.get(i);
            double r1 = baseRatioArray[i];
            double len = length * (r1 - r0);
            double vel = len / (double)(DateTimeUtils.getDuration(p0.getTimeStamp(), p1.getTimeStamp()) / 1000L);
            int j = 0;
            while (j < targetNodeList.size()) {
                Link link;
                double r = nodeRatioArray[j];
                Node node = targetNodeList.get(j);
                Link link2 = link = targetLinkList.size() > j ? targetLinkList.get(j) : null;
                if (!(r0 == r && result.containsKey(node) || r == r1 && result.containsKey(node) || !(r0 <= r) || !(r <= r1))) {
                    double l = length * (r - r0);
                    int td = (int)(l / vel);
                    Date t = DateUtils.addSeconds((Date)p0.getTimeStamp(), (int)td);
                    result.put(node, new STNetworkPoint(t, node.getLon(), node.getLat(), node, link, 0.0));
                }
                ++j;
            }
            p0 = p1;
            r0 = r1;
            ++i;
        }
        ILonLatTime ps = baseList.get(0);
        ILonLatTime pe = baseList.get(baseList.size() - 1);
        ArrayList<STNetworkPoint> output = new ArrayList<STNetworkPoint>(result.values());
        output.add(0, new STNetworkPoint(ps.getTimeStamp(), ps.getLon(), ps.getLat(), null, null, 0.0));
        output.add(new STNetworkPoint(pe.getTimeStamp(), pe.getLon(), pe.getLat(), null, null, 0.0));
        return output;
    }

    private List<STNetworkPoint> insertUnitTimePoint(List<STNetworkPoint> points, long spanSec) {
        ArrayList<STNetworkPoint> result = new ArrayList<STNetworkPoint>();
        STNetworkPoint ps = points.get(0);
        STNetworkPoint pe = points.get(points.size() - 1);
        List<Date> timeList = this.getTimestampWithin(ps.getTimeStamp(), pe.getTimeStamp(), spanSec);
        int idx = 0;
        block0: for (Date ts : timeList) {
            int i = idx;
            while (i < points.size() - 1) {
                STNetworkPoint p0 = points.get(i);
                STNetworkPoint p1 = points.get(i + 1);
                Date t0 = p0.getTimeStamp();
                Date t1 = p1.getTimeStamp();
                if (t0.before(ts) && t1.after(ts)) {
                    Link link = p0.getLink();
                    Node node = p0.getNode();
                    long spanDeno = DateTimeUtils.getDuration(t0, t1) / 1000L;
                    long spanNume = DateTimeUtils.getDuration(t0, ts) / 1000L;
                    double ratio = (double)spanNume / (double)spanDeno;
                    if (link != null && link.getHeadNode().equals(p0.getNode())) {
                        ratio = 1.0 - ratio;
                    }
                    ILonLat point = link != null ? TrajectoryUtils.getLineInterpolatePoint(link.getLineString(), ratio) : TrajectoryUtils.getLineInterpolatePoint(Arrays.asList(ps, pe), ratio);
                    result.add(new STNetworkPoint(ts, point.getLon(), point.getLat(), node, link, ratio));
                    idx = i;
                    continue block0;
                }
                ++i;
            }
        }
        result.addAll(points);
        Collections.sort(result, new Comparator<ILonLatTime>(){

            @Override
            public int compare(ILonLatTime a, ILonLatTime b) {
                Date ta = a.getTimeStamp();
                Date tb = b.getTimeStamp();
                return ta.compareTo(tb);
            }
        });
        return result;
    }

    private List<Date> getTimestampWithin(Date ts, Date te, long spanSec) {
        long t1;
        long t0 = ts.getTime() / 1000L;
        if (t0 != 0L) {
            t0 += spanSec - t0 % spanSec;
        }
        if ((t1 = te.getTime() / 1000L) != 0L) {
            t1 -= t0 % spanSec;
        }
        ArrayList<Date> output = new ArrayList<Date>();
        long t = t0;
        while (t <= t1) {
            output.add(new Date(t * 1000L));
            t += spanSec;
        }
        return output;
    }
}

