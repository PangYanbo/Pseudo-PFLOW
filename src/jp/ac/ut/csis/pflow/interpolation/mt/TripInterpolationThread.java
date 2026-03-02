/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 *  org.apache.commons.lang.time.DateUtils
 */
package jp.ac.ut.csis.pflow.interpolation.mt;

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.geom2.DateTimeUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.ILonLatTime;
import jp.ac.ut.csis.pflow.geom2.Mesh;
import jp.ac.ut.csis.pflow.geom2.MeshUtils;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.interpolation.mt.TripInterpolationMain;
import jp.ac.ut.csis.pflow.interpolation.trip.ITrip;
import jp.ac.ut.csis.pflow.interpolation.trip.Move;
import jp.ac.ut.csis.pflow.interpolation.trip.STNetworkPoint;
import jp.ac.ut.csis.pflow.interpolation.trip.Stay;
import jp.ac.ut.csis.pflow.interpolation.trip.TransportMode;
import jp.ac.ut.csis.pflow.routing4.loader.DrmQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.IPgNetworkLoader;
import jp.ac.ut.csis.pflow.routing4.loader.IQueryCondition;
import jp.ac.ut.csis.pflow.routing4.loader.PgSeiDrmLoader;
import jp.ac.ut.csis.pflow.routing4.logic.AStar;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.AStarLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.RailwayLinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.DrmTransport;
import jp.ac.ut.csis.pflow.routing4.mapmatching.SparseMapMatching;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import jp.ac.ut.csis.pflow.thread.ARunningThread;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

public class TripInterpolationThread
extends ARunningThread {
    private static final int[] ROAD_TYPES = new int[]{1, 2, 3, 4, 5, 6, 7};
    public static final int NETWORK_TYPE_ORIGINAL_DRM2017 = 1;
    public static final int NETWORK_TYPE_SIMPLIFIED_DRM2017 = 2;
    public static final int NETWORK_TYPE_RAILWAY = 3;
    private PgLoader _pgLoader;
    private IPgNetworkLoader _drmLoader;
    private Network _railwayNetwork;
    private File _outputFile;
    private SparseMapMatching _roadMatching;
    private AStar _roadMatchingLogic;
    private SparseMapMatching _railwayMatching;
    private List<ITrip> _trips;
    private boolean _enableTimeSplit;

    protected TripInterpolationThread(TripInterpolationMain mainThread, PgLoader pgLoader, Network railwayNetwork, File outputFile, List<ITrip> trips) {
        this(mainThread, pgLoader, railwayNetwork, outputFile, trips, false);
    }

    protected TripInterpolationThread(TripInterpolationMain mainThread, PgLoader pgLoader, Network railwayNetwork, File outputFile, List<ITrip> trips, boolean enableTimeSplit) {
        super(mainThread);
        this._pgLoader = pgLoader;
        this._drmLoader = new PgSeiDrmLoader().setTableName("seidrm2017.drm_32_table");
        this._railwayNetwork = railwayNetwork;
        this._outputFile = outputFile;
        this._roadMatchingLogic = new AStar(new AStarLinkCost(DrmTransport.VEHICLE));
        this._roadMatching = new SparseMapMatching(this._roadMatchingLogic);
        this._railwayMatching = new SparseMapMatching(new Dijkstra(new RailwayLinkCost()));
        this._trips = trips;
        this._enableTimeSplit = enableTimeSplit;
    }

    /*
     * WARNING - void declaration
     */
    @Override
    protected void doMain() {
        ArrayList<List<String>> output = new ArrayList<List<String>>();
        ITrip prev = null;
        Iterator<ITrip> iterator = this._trips.iterator();
        while (iterator.hasNext()) {
            ITrip iTrip;
            prev = iTrip = iterator.next();
            List<String> out = null;
            switch (iTrip.getTransportMode()) {
                case STAY: {
                    out = this.interpolateStay((Stay)Stay.class.cast(iTrip));
                    break;
                }
                case TRAIN: {
                    out = this.interpolateRailway((Move)Move.class.cast(iTrip));
                    break;
                }
                case WALK: {
                    this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.WALK);
                    out = this.interpolateRoad((Move)Move.class.cast(iTrip));
                    break;
                }
                case BIKE: {
                    this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.BIKE);
                    out = this.interpolateRoad((Move)Move.class.cast(iTrip));
                    break;
                }
                case CAR: {
                    this._roadMatchingLogic.getLinkCost().setTransport(DrmTransport.VEHICLE);
                    out = this.interpolateRoad((Move)Move.class.cast(iTrip));
                    break;
                }
                default: {
                    out = this.interpolateRoad((Move)Move.class.cast(iTrip));
                }
            }
            if (out == null) {
                System.out.println(String.valueOf(iTrip.getUid()) + " was failed");
                return;
            }
            output.add(out);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this._outputFile))) {
            int idx = 1;
            String uid = prev.getUid();
            String date = new SimpleDateFormat("yyyy-MM-dd").format(prev.getStartTime());
            for (List list : output) {
                String line = String.format("%s,%s,%d,%s", uid, date, idx++, StringUtils.join((Collection)list, (String)","));
                writer.write(line);
                writer.newLine();
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public <T extends ILonLat> double[] createRect(T point) {
        return this.createRect(Arrays.asList(point));
    }

    private <T extends ILonLat> double[] createRect(List<T> points) {
        Rectangle2D.Double rect = TrajectoryUtils.makeMBR(points);
        return new double[]{rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY()};
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private List<String> interpolateRoad(Move trip) {
        List<ILonLatTime> trajectory = trip.getTrajectory();
        try (Connection con = this._pgLoader.getConnection()) {
            ILonLatTime ps = trajectory.get(0);
            ILonLatTime pe = trajectory.get(trajectory.size() - 1);
            IQueryCondition[] conditions = new DrmQueryCondition[]{new DrmQueryCondition(this.createRect(ps), 3000.0), new DrmQueryCondition(this.createRect(trajectory), 3000.0, ROAD_TYPES), new DrmQueryCondition(this.createRect(pe), 3000.0)};
            Network network = this._drmLoader.setConnection(con).setQueryConditions(conditions).load();
            Route route = this._roadMatching.runSparseMapMatching(network, trajectory);
            if (route != null && !route.isEmpty()) {
                List<Node> nodes = route.listNodes();
                List<Link> links = route.listLinks();
                if (nodes.size() >= 2) {
                    List<ILonLat> routePoints = route.getTrajectory();
                    routePoints.add(0, ps);
                    routePoints.add(pe);
                    List<STNetworkPoint> result = this.assignRouteTimestamp(trajectory, routePoints, nodes, links);
                    if (!this._enableTimeSplit) return this.format(trip, result, TrajectoryUtils.length(trajectory), 2);
                    result = this.insertUnitTimePoint(result, 300L);
                    return this.format(trip, result, TrajectoryUtils.length(trajectory), 2);
                }
                System.err.println("insufficient interpolated points on road");
                return null;
            }
            System.err.println("failed to interpolate road section");
            return null;
        }
        catch (NullPointerException | SQLException exp) {
            exp.printStackTrace();
            return null;
        }
    }

    private List<String> interpolateRailway(Move trip) {
        block5: {
            List<ILonLatTime> trajectory = trip.getTrajectory();
            try {
                Route route = this._railwayMatching.runSparseMapMatching(this._railwayNetwork, trajectory);
                if (route == null || route.isEmpty()) break block5;
                List<Node> nodes = route.listNodes();
                List<Link> links = route.listLinks();
                if (nodes.size() >= 2) {
                    List<ILonLat> routePoints = route.getTrajectory();
                    List<STNetworkPoint> result = this.assignRouteTimestamp(trajectory, routePoints, nodes, links);
                    if (this._enableTimeSplit) {
                        result = this.insertUnitTimePoint(result, 300L);
                    }
                    return this.format(trip, result, TrajectoryUtils.length(trajectory), 3);
                }
                System.err.println("insufficient interpolated points on railway");
                return null;
            }
            catch (Exception exp) {
                exp.printStackTrace();
                return null;
            }
        }
        System.err.println("failed to interpolate railway section");
        return null;
    }

    private List<String> interpolateStay(Stay trip) {
        ILonLatTime point = trip.getStartPoint();
        Mesh mesh = MeshUtils.createMesh(6, point.getLon(), point.getLat());
        int idx = 1;
        StringBuffer buf = new StringBuffer();
        buf.append(String.format("%d|%d|%.08f|%.08f|||%s", idx++, trip.getStartTime().getTime() / 1000L, point.getLat(), point.getLon(), mesh.getCode()));
        if (this._enableTimeSplit) {
            List<Date> timeList = this.getTimestampWithin(trip.getStartTime(), trip.getEndTime(), 300L);
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
        return Arrays.asList(trip.getTransportMode().toString(), trip.getTransportMode() != TransportMode.STAY ? "MOVE" : "STAY", "", "", String.format("%.03f", dist), String.valueOf(DateTimeUtils.getDuration(trip.getStartTime(), trip.getEndTime()) / 1000L), String.valueOf(trip.getStartTime().getTime() / 1000L), String.valueOf(trip.getEndTime().getTime() / 1000L), String.valueOf(result.size()), buf.substring(1), String.valueOf(networkId), "");
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

