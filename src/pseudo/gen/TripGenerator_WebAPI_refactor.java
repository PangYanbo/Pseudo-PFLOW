package pseudo.gen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.ac.ut.csis.pflow.geom2.DistanceUtils;
import jp.ac.ut.csis.pflow.geom2.ILonLat;
import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.TrajectoryUtils;
import jp.ac.ut.csis.pflow.routing4.logic.Dijkstra;
import jp.ac.ut.csis.pflow.routing4.logic.linkcost.LinkCost;
import jp.ac.ut.csis.pflow.routing4.logic.transport.ITransport;
import jp.ac.ut.csis.pflow.routing4.logic.transport.Transport;
import jp.ac.ut.csis.pflow.routing4.res.Link;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import jp.ac.ut.csis.pflow.routing4.res.Route;
import network.DrmLoader;
import network.RailLoader;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import java.util.concurrent.ThreadLocalRandom;
import pseudo.acs.DataAccessor;
import pseudo.acs.PersonAccessor;
import pseudo.res.*;
import utils.ConfigLoader;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class TripGenerator_WebAPI_refactor {

    private final Network drm;
	private final Network railway;

	private final SSLContext sslContext;
	private final PoolingHttpClientConnectionManager connManager;
	private final CloseableHttpClient httpClient;
	private final String sessionId;

	private final double MIN_TRANSIT_DISTANCE;
	private final double FARE_PER_KILOMETER;
	private final double FARE_PER_HOUR;
	private final double FATIGUE_INDEX_WALK;
	private final double FATIGUE_INDEX_BICYCLE;
	private final double FARE_INIT;
	private final double CAR_AVAILABILITY;
	private final String appDate;
	private final String maxRadius;
	private final String maxRoutes;
	private final String transportCode;

	// Mixed-route diagnostic counters (shared across TripTask threads)
	private final AtomicInteger mixedQueryCount = new AtomicInteger();
	private final AtomicInteger mixedNoStationCount = new AtomicInteger();
	private final AtomicInteger mixedNoFareCount = new AtomicInteger();
	private final AtomicInteger mixedTransitAvailable = new AtomicInteger();
	private final AtomicInteger mixedSelectedCount = new AtomicInteger();
	private final AtomicInteger mixedBelowDistThreshold = new AtomicInteger();

	public TripGenerator_WebAPI_refactor(Country japan, Network drm, Network railway) throws Exception {
		super();
        this.drm = drm;
		this.railway = railway;
		this.MIN_TRANSIT_DISTANCE = Double.parseDouble(prop.getProperty("min.transit.distance", "1000"));
		this.FARE_PER_KILOMETER = Double.parseDouble(prop.getProperty("fare.per.kilometer", "10"));
		this.FARE_PER_HOUR = Double.parseDouble(prop.getProperty("fare.per.hour", "1000"));
		this.FATIGUE_INDEX_WALK = Double.parseDouble(prop.getProperty("fatigue.walk", "1.5"));
		this.FATIGUE_INDEX_BICYCLE = Double.parseDouble(prop.getProperty("fatigue.bicycle", "1.2"));
		this.FARE_INIT = Double.parseDouble(prop.getProperty("fare.init", "200"));
		this.CAR_AVAILABILITY = Double.parseDouble(prop.getProperty("car.availability", "0.4"));
		this.appDate = prop.getProperty("api.appDate", "20240401");
		this.maxRadius = prop.getProperty("api.maxRadius", "1000");
		this.maxRoutes = prop.getProperty("api.maxRoutes", "9");
		this.transportCode = prop.getProperty("api.transportCode", "3");
		if (!"1".equals(transportCode) && !"3".equals(transportCode)) {
			throw new IllegalArgumentException("api.transportCode must be 1 (train) or 3 (bus), got: " + transportCode);
		}
		this.sslContext = createSSLContext();
		this.connManager = createConnManager();
		this.httpClient = createHttpClient();
		this.sessionId = createSession();
	}

	private SSLContext createSSLContext() throws Exception {
		return SSLContextBuilder.create()
				.loadTrustMaterial(new TrustSelfSignedStrategy())
				.build();
	}

	private PoolingHttpClientConnectionManager createConnManager() {
		SSLContext sslContext;
		try {
			sslContext = SSLContextBuilder.create()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize SSL context", e);
		}

		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
				sslContext,
				new String[]{"TLSv1.2", "TLSv1.3"},
				null,
				NoopHostnameVerifier.INSTANCE);

		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
				RegistryBuilder.<ConnectionSocketFactory>create()
						.register("https", sslSocketFactory)
						.register("http", PlainConnectionSocketFactory.INSTANCE)
						.build());

		connManager.setMaxTotal(32); // Adjust based on your expected total number of concurrent connections
		connManager.setDefaultMaxPerRoute(100); // Adjust per route limits based on your API and use case

		return connManager;
	}
	private CloseableHttpClient createHttpClient() {

		return HttpClients.custom()
				.setSSLContext(this.sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.setConnectionManager(this.connManager)
				.setDefaultRequestConfig(RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.build())
				.build();
	}

	private static HttpResponse executePostRequest(CloseableHttpClient httpClient, HttpPost postRequest) throws Exception {
		return httpClient.execute(postRequest);
	}

	public String createSession() throws Exception{

		String createSessionURL = prop.getProperty("api.createSessionURL");
		if (createSessionURL == null) {
			throw new IllegalStateException("Missing config key: api.createSessionURL");
		}
		HttpPost createSessionPost = new HttpPost(createSessionURL);

		String apiUser = System.getenv("PFLOW_API_USER");
		String apiPass = System.getenv("PFLOW_API_PASS");
		if (apiUser == null || apiUser.isBlank()) {
			throw new IllegalStateException("Environment variable PFLOW_API_USER is not set");
		}
		if (apiPass == null || apiPass.isBlank()) {
			throw new IllegalStateException("Environment variable PFLOW_API_PASS is not set");
		}
		List<NameValuePair> sessionParams = new ArrayList<>();
		sessionParams.add(new BasicNameValuePair("UserID", apiUser));
		sessionParams.add(new BasicNameValuePair("Password", apiPass));
		createSessionPost.setEntity(new UrlEncodedFormEntity(sessionParams));

		HttpResponse sessionResponse = executePostRequest(this.httpClient, createSessionPost);
		if (sessionResponse.getStatusLine().getStatusCode() == 200) {
			String sessionResponseBody = EntityUtils.toString(sessionResponse.getEntity());
			System.out.println("Session created successfully");
			System.out.println(sessionResponseBody);
			String[] parts = sessionResponseBody.split(",");
			String sessionId = (parts.length > 1 ? parts[1] : parts[0]).trim().replace("\r", "").replace("\n", "");
			return sessionId;
		} else {
			throw new RuntimeException("Failed to create WebAPI session: HTTP " + sessionResponse.getStatusLine().getStatusCode());
		}
	}
	
	protected double getRandom() {
		return ThreadLocalRandom.current().nextDouble();
	}
	
	private class TripTask implements Callable<Integer> {
		private int id;
		private final List<Person> listAgents;
		private int error;
		private int total;
		LinkCost linkCost = new LinkCost();
		Dijkstra routing = new Dijkstra(linkCost);

		public TripTask(int id, List<Person> listAgents){
			this.id = id;
			this.listAgents = listAgents;
			this.total = error = 0;
		}	
		
		private EPurpose convertHomeMode(ELabor labor) {
			switch(labor) {
			case WORKER:
				return EPurpose.OFFICE;
			case JOBLESS:
			case NO_LABOR:
			case UNDEFINED:
			case INFANT:
				return EPurpose.FREE;
			case PRE_SCHOOL:
			case PRIMARY_SCHOOL:
			case SECONDARY_SCHOOL:
			case HIGH_SCHOOL:
			case COLLEGE:
			case JUNIOR_COLLEGE:
			default:
				return EPurpose.SCHOOL;
			}
		}

		private ETransport getTransport(int mode) { // mode defined by WebAPI
			switch (mode) {
				case 4:		return ETransport.WALK;
				case 3:	return ETransport.BUS;
                case 2:		return ETransport.TRAIN;
				case 0: return ETransport.NOT_DEFINED;
				default:		return ETransport.CAR;
			}
		}

		private int calculateMultiplier(ETransport mode) {
			switch (mode) {
				case WALK: return 6;
				case BICYCLE: return 3;
				case CAR: return 1;
				default: return 1;
			}
		}

		private void configureCalendar(Calendar calendar, Date date) {
			TimeZone timeZone = TimeZone.getTimeZone("Asia/Tokyo");
			calendar.setTime(date);
			calendar.setTimeZone(timeZone);
			calendar.add(Calendar.MILLISECOND, -timeZone.getOffset(calendar.getTimeInMillis()));
			calendar.add(Calendar.YEAR, 45);
			calendar.add(Calendar.MONTH, 9);
		}

		private ETransport determineTransportMode(Person person, double distance, Route route, Map<String, String> mixedparams, JsonNode[] mixedResultsHolder){
			ETransport nextMode;

			Map<ETransport, Double> choices = new LinkedHashMap<>();

			if(route!=null){
				double roadtime = route.getCost(); // seconds
				double roadfare = FARE_INIT + route.getLength() / 1000 * FARE_PER_KILOMETER; // length in meters, 150 as initial cost to avoid short distance car travel
				double roadcost = roadfare + roadtime / 3600 * FARE_PER_HOUR;
				if(person.hasCar() || getRandom() < CAR_AVAILABILITY){
					choices.put(ETransport.CAR, roadcost);
				}

				double walktime = route.getLength() / 1.38;
				double walkcost = walktime / 3600 * FARE_PER_HOUR * FATIGUE_INDEX_WALK;
				choices.put(ETransport.WALK, walkcost);

				if(person.hasBike()){
					double biketime = walktime / 2;
					double bikecost = biketime / 3600 * FARE_PER_HOUR * FATIGUE_INDEX_BICYCLE;
					choices.put(ETransport.BICYCLE, bikecost);
				}
			}

			if(distance>MIN_TRANSIT_DISTANCE){
				mixedQueryCount.incrementAndGet();
				mixedResultsHolder[0] = getMixedRoute(httpClient, sessionId, mixedparams);
				int numStation = mixedResultsHolder[0].path("num_station").asInt();
				int fare = mixedResultsHolder[0].path("fare").asInt();
				boolean publicTransit = numStation > 0 && fare > 0;
				if (!publicTransit) {
					if (numStation == 0) mixedNoStationCount.incrementAndGet();
					if (fare == 0) mixedNoFareCount.incrementAndGet();
				} else {
					mixedTransitAvailable.incrementAndGet();
					double mixedfare = mixedResultsHolder[0].get("fare").asDouble();
					double mixedtime = mixedResultsHolder[0].get("total_time").asDouble(); // Travel time from WebAPI is in minute
					double mixedcost = mixedfare + mixedtime / 60 * FARE_PER_HOUR;
					choices.put(ETransport.MIX, mixedcost);
				}
			} else {
				mixedBelowDistThreshold.incrementAndGet();
			}

			nextMode = choices.entrySet()
					.stream()
					.min(Comparator.comparing(Map.Entry::getValue))
					.map(Map.Entry::getKey)
					.orElse(ETransport.NOT_DEFINED);

			if (nextMode == ETransport.MIX) {
				mixedSelectedCount.incrementAndGet();
			}

			return nextMode;
		}

		// Methods to refactor and modularize the code
		private long calculateTravelTime(Route route, int multiplier) {
			if (route != null) {
				return (long) route.getCost() * multiplier;
			} else {
				return 3600L;
			}
		}

		private void addSubpoints(List<Node> nodes, Map<Node, Date> timeMap, ETransport nextMode, EPurpose purpose, List<SPoint> subpoints) {
			for (ILonLat node : nodes) {
				Date date = timeMap.get(node);
				Calendar cl = Calendar.getInstance();
				configureCalendar(cl, date);
				date = cl.getTime();
				SPoint point = new SPoint(node.getLon(), node.getLat(), date, nextMode, purpose);
				subpoints.add(point);
			}
		}

		private void assignLinksToSubpoints(List<SPoint> subpoints, List<Link> links) {
			for (int k = 1; k <= links.size(); k++) {
				subpoints.get(k).setLink(links.get(k - 1).getLinkID());
			}
		}

		private void handleMixedTransport(ETransport nextMode, EPurpose purpose, JsonNode[] mixedResultsHolder, List<SPoint> subpoints, List<SPoint> points, Person person, Activity next, Route route, long startTime, long endTime, LonLat oll, LonLat dll) {
			long mixedTime = mixedResultsHolder[0].path("total_time").asLong() * 60;
			endTime += mixedTime;
			long travelTime = mixedTime;
			long depTime = next.getStartTime() - travelTime;

			List<ETransport> nodeModes = new ArrayList<>();
			List<Node> nodes = extractNodesFromMixedResults(mixedResultsHolder, nodeModes);
			boolean publicTransit = mixedResultsHolder[0].path("num_station").asInt() > 0;
			List<JsonNode> currentSubtrip = new ArrayList<>();
			int lastMode = determineInitialTransportMode(mixedResultsHolder);

			processMixedTransportFeatures(mixedResultsHolder, currentSubtrip, lastMode, publicTransit, depTime, person, purpose);
			if (!nodes.isEmpty()) {
				addTimeStampedSubpoints(nodes, nodeModes, startTime, endTime, purpose, subpoints);
			}
			points.addAll(subpoints);
		}

//		private List<Node> extractNodesFromMixedResults(JsonNode[] mixedResultsHolder) {
//			List<Node> nodes = new ArrayList<>();
//			JsonNode routeData = mixedResultsHolder[0].path("features");
//			for (JsonNode feature : routeData) {
//				JsonNode coordinates = feature.path("geometry").path("coordinates");
//				String stationName =   feature.path("properties").path("station").toString();
//				if (coordinates.isArray()) {
//					double lon = coordinates.get(0).asDouble();
//					double lat = coordinates.get(1).asDouble();
//					nodes.add(new Node(feature.path("properties").path("id").toString(), lon, lat));
//				} else {
//					System.out.println("Coordinate from WebAPI is not an array!!");
//				}
//			}
//			return nodes;
//		}

		private List<Node> extractNodesFromMixedResults(JsonNode[] mixedResultsHolder, List<ETransport> nodeModes) {
			List<Node> nodes = new ArrayList<>();
			Node firstStationNode = null;
			Node lastStationNode = null;

			JsonNode routeData = mixedResultsHolder[0].path("features");
			boolean previousHasStation = false;
			int previousTransportation = -1;

			for (JsonNode feature : routeData) {
				JsonNode coordinates = feature.path("geometry").path("coordinates");
				String stationName = feature.path("properties").path("station").asText();
				int currentTransportation = feature.path("properties").path("transportation").asInt();
				boolean currentHasStation = !"null".equals(stationName);

				if ((previousHasStation && !currentHasStation) ||
					(previousTransportation != -1 && previousTransportation != currentTransportation)) {

					if (firstStationNode != null && lastStationNode != null && !firstStationNode.equals(lastStationNode)){
						LinkCost linkCost = new LinkCost(Transport.RAILWAY);
						Dijkstra routing = new Dijkstra(linkCost);
						Route route = routing.getRoute(railway,	firstStationNode.getLon(), firstStationNode.getLat(), lastStationNode.getLon(), lastStationNode.getLat());

						if (route != null && route.numNodes() > 0){
							List<Node> rail_nodes = route.listNodes();
							nodes.addAll(rail_nodes);
							for (int j = 0; j < rail_nodes.size(); j++) nodeModes.add(ETransport.TRAIN);
						}
					}
					firstStationNode = null;
					lastStationNode = null;
				}

				if (!currentHasStation && coordinates.isArray()) {
					double lon = coordinates.get(0).asDouble();
					double lat = coordinates.get(1).asDouble();
					Node node = new Node(feature.path("properties").path("id").asText(), lon, lat);
					nodes.add(node);
					nodeModes.add(getTransport(currentTransportation));
				}

				if (currentHasStation && coordinates.isArray()) {
					double lon = coordinates.get(0).asDouble();
					double lat = coordinates.get(1).asDouble();
					Node node = new Node(feature.path("properties").path("id").asText(), lon, lat);

					if (firstStationNode == null || previousTransportation != currentTransportation) {
						firstStationNode = node;
					}
					lastStationNode = node;
				}

				previousHasStation = currentHasStation;
				previousTransportation = currentTransportation;
			}

			// process final station segment
			if (firstStationNode != null && lastStationNode != null && !firstStationNode.equals(lastStationNode)) {
				LinkCost linkCost = new LinkCost(Transport.RAILWAY);
				Dijkstra routing = new Dijkstra(linkCost);
				Route route = routing.getRoute(railway, firstStationNode.getLon(), firstStationNode.getLat(), lastStationNode.getLon(), lastStationNode.getLat());
				if (route != null && route.numNodes() > 0) {
					List<Node> rail_nodes = route.listNodes();
					nodes.addAll(rail_nodes);
					for (int j = 0; j < rail_nodes.size(); j++) nodeModes.add(ETransport.TRAIN);
				}
			}
			// nodes may be empty for bus-only routes without railway network coverage
			return nodes;
		}

		private int determineInitialTransportMode(JsonNode[] mixedResultsHolder) {
			return mixedResultsHolder[0].path("features").get(0).path("properties").path("transportation").asInt();
		}

		private void processMixedTransportFeatures(JsonNode[] mixedResultsHolder, List<JsonNode> currentSubtrip, int lastMode, boolean publicTransit, long depTime, Person person, EPurpose purpose) {
			JsonNode routeData = mixedResultsHolder[0].path("features");
			JsonNode prevNode = null;
			long currentTime = depTime;
			for (JsonNode feature : routeData) {
				int currentMode = feature.path("properties").path("transportation").asInt();

				if (currentMode != lastMode) {
					if (currentSubtrip.size() > 1) {
						currentTime += estimateSegmentTime(currentSubtrip, lastMode);
						addTripForSubtrip(currentSubtrip, lastMode, publicTransit, currentTime, person, purpose);
					}
					currentSubtrip = new ArrayList<>();
					currentSubtrip.add(prevNode);
					lastMode = currentMode;
				}
				currentSubtrip.add(feature);
				prevNode = feature;
			}
			// emit final segment (was missing — last segment after final mode change was dropped)
			if (currentSubtrip.size() > 1) {
				currentTime += estimateSegmentTime(currentSubtrip, lastMode);
				addTripForSubtrip(currentSubtrip, lastMode, publicTransit, currentTime, person, purpose);
			}
		}

		private long estimateSegmentTime(List<JsonNode> subtrip, int mode) {
			JsonNode first = subtrip.get(0).path("geometry").path("coordinates");
			JsonNode last = subtrip.get(subtrip.size() - 1).path("geometry").path("coordinates");
			if (!first.isArray() || !last.isArray()) return 300L;
			LonLat a = new LonLat(first.get(0).asDouble(), first.get(1).asDouble());
			LonLat b = new LonLat(last.get(0).asDouble(), last.get(1).asDouble());
			double distance = DistanceUtils.distance(a, b);
			double speed = Speed.get(getTransport(mode));
			if (speed <= 0) speed = Speed.WALK;
			return Math.max(1L, (long)(distance / speed));
		}

		private void addTripForSubtrip(List<JsonNode> currentSubtrip, int lastMode, boolean publicTransit, long depTime, Person person, EPurpose purpose) {
			JsonNode ollCoords = currentSubtrip.get(0).path("geometry").path("coordinates");
			JsonNode dllCoords = currentSubtrip.get(currentSubtrip.size() - 1).path("geometry").path("coordinates");
//			ETransport mode = getTransport(currentSubtrip.get(1).path("properties").path("transportation").asInt());

			// 2025-02-20 feature railway interpolation with 1.2 algorithm
			int firstMode = currentSubtrip.get(0).path("properties").path("transportation").asInt();
			boolean allSame = true, hasMode2 = false, hasMode3 = false;

			for (JsonNode node : currentSubtrip) {
				int transportation = node.path("properties").path("transportation").asInt();
				if (transportation != firstMode) allSame = false;
				if (transportation == 2) hasMode2 = true;
				if (transportation == 3) hasMode3 = true;
			}

			int finalMode = allSame ? firstMode : (hasMode2 ? 2 : (hasMode3 ? 3 : lastMode));
			ETransport mode = getTransport(finalMode);

			if (mode == ETransport.CAR && publicTransit) {
				mode = ETransport.WALK;
			}
			if (ollCoords.isArray() && dllCoords.isArray()) {
				LonLat moll = new LonLat(ollCoords.get(0).asDouble(), ollCoords.get(1).asDouble());
				LonLat mdll = new LonLat(dllCoords.get(0).asDouble(), dllCoords.get(1).asDouble());
				// depTime += (long) (DistanceUtils.distance(moll, mdll) / getTravelSpeed(mode.getId()));
				person.addTrip(new Trip(mode, purpose, depTime, moll, mdll));
			} else {
				System.out.println("No coordinate from API!");
			}
		}

		private void addTimeStampedSubpoints(List<Node> nodes, long startTime, long endTime, ETransport nextMode, EPurpose purpose, List<SPoint> subpoints) {
			Map<Node, Date> timeMap = TrajectoryUtils.putTimeStamp(nodes, new Date(startTime * 1000), new Date(endTime * 1000));
			addSubpoints(nodes, timeMap, nextMode, purpose, subpoints);
		}

		private void addTimeStampedSubpoints(List<Node> nodes, List<ETransport> nodeModes, long startTime, long endTime, EPurpose purpose, List<SPoint> subpoints) {
			Map<Node, Date> timeMap = TrajectoryUtils.putTimeStamp(nodes, new Date(startTime * 1000), new Date(endTime * 1000));
			addSubpoints(nodes, timeMap, nodeModes, purpose, subpoints);
		}

		private void addSubpoints(List<Node> nodes, Map<Node, Date> timeMap, List<ETransport> nodeModes, EPurpose purpose, List<SPoint> subpoints) {
			for (int idx = 0; idx < nodes.size(); idx++) {
				Node node = nodes.get(idx);
				Date date = timeMap.get(node);
				Calendar cl = Calendar.getInstance();
				configureCalendar(cl, date);
				date = cl.getTime();
				ETransport mode = idx < nodeModes.size() ? nodeModes.get(idx) : ETransport.MIX;
				SPoint point = new SPoint(node.getLon(), node.getLat(), date, mode, purpose);
				subpoints.add(point);
			}
		}

		public String convertSecondsToHHMM(double totalSeconds) {
			// Calculate hours and minutes
			int hours = (int) (totalSeconds / 3600);
			int minutes = (int) ((totalSeconds % 3600) / 60);

			// Format hours and minutes to HHMM as WebAPI requests
			return String.format("%02d%02d", hours, minutes);
		}

		private int process(Person person) {
			List<SPoint> points = new ArrayList<>();

			List<Activity> activities = person.getActivities();
			Activity pre = activities.get(0);

			try {
				if (activities.size() == 1) {
					person.addTrip(new Trip(ETransport.NOT_DEFINED, EPurpose.HOME, 0, pre.getLocation(), pre.getLocation()));

					Calendar cl = Calendar.getInstance();
					Date startDate = new Date(0);
					configureCalendar(cl, startDate);
					startDate = cl.getTime();
					points.add(new SPoint(pre.getLocation().getLon(), pre.getLocation().getLat(), startDate, ETransport.NOT_DEFINED, EPurpose.HOME));
					Date endDate = new Date(86399000);
					configureCalendar(cl, endDate);
					endDate = cl.getTime();
					points.add(new SPoint(pre.getLocation().getLon(), pre.getLocation().getLat(), endDate, ETransport.NOT_DEFINED, EPurpose.HOME));
					person.addTrajectory(points);
				} else {
					for (int i = 1; i < activities.size(); i++) {
						List<SPoint> subpoints = new ArrayList<>();

						Activity next = activities.get(i);
						GLonLat oll = pre.getLocation();
						GLonLat dll = next.getLocation();

						long startTime = next.getStartTime();
						long endTime = startTime;

						EPurpose purpose = next.getPurpose();
						double distance = DistanceUtils.distance(oll, dll);

						if (distance > 0) {
							ETransport nextMode;
							Map<String, String> mixedparams = getStringStringMap(oll, dll, startTime);

							JsonNode[] mixedResultsHolder = new JsonNode[1];

							Route route = routing.getRoute(drm, oll.getLon(), oll.getLat(), dll.getLon(), dll.getLat());
							nextMode = determineTransportMode(person, distance, route, mixedparams, mixedResultsHolder);

							int multiplier = calculateMultiplier(nextMode);
							long travelTime = 0;

							if (nextMode == ETransport.WALK || nextMode == ETransport.BICYCLE || nextMode == ETransport.CAR) {
								travelTime = calculateTravelTime(route, multiplier);
								endTime += travelTime;

								List<Node> nodes = route.listNodes();
								Map<Node, Date> timeMap = TrajectoryUtils.putTimeStamp(nodes, new Date(startTime * 1000), new Date(endTime * 1000));
								addSubpoints(nodes, timeMap, nextMode, purpose, subpoints);

								List<Link> links = route.listLinks();
								assignLinksToSubpoints(subpoints, links);
								points.addAll(subpoints);

								long depTime = next.getStartTime() - travelTime;
								person.addTrip(new Trip(nextMode, purpose, depTime, oll, dll));
							} else if (nextMode == ETransport.MIX) {
								if (mixedResultsHolder[0].path("features").get(0) == null) {
									System.out.println("empty mixed results!");
								}
								handleMixedTransport(nextMode, purpose, mixedResultsHolder, subpoints, points, person, next, route, startTime, endTime, oll, dll);
							} else {
								person.addTrip(new Trip(ETransport.NOT_DEFINED, next.getPurpose(), next.getStartTime(), pre.getLocation(), pre.getLocation()));
								Calendar cl = Calendar.getInstance();
								Date startDate = new Date(next.getStartTime());
								configureCalendar(cl, startDate);
								startDate = cl.getTime();
								points.add(new SPoint(pre.getLocation().getLon(), pre.getLocation().getLat(), startDate, ETransport.NOT_DEFINED, EPurpose.HOME));
								Date endDate = new Date(next.getStartTime() + 300);
								configureCalendar(cl, endDate);
								endDate = cl.getTime();
								points.add(new SPoint(pre.getLocation().getLon(), pre.getLocation().getLat(), endDate, ETransport.NOT_DEFINED, EPurpose.HOME));
							}
						} else {
							person.addTrip(new Trip(ETransport.NOT_DEFINED, next.getPurpose(), next.getStartTime(), pre.getLocation(), pre.getLocation()));
							Calendar cl = Calendar.getInstance();
							Date startDate = new Date(next.getStartTime());
							configureCalendar(cl, startDate);
							startDate = cl.getTime();
							points.add(new SPoint(pre.getLocation().getLon(), pre.getLocation().getLat(), startDate, ETransport.NOT_DEFINED, next.getPurpose()));
						}

						pre = next;
					}
				}
				person.addTrajectory(points);
			} catch (Exception e){
				throw new RuntimeException("Exception processing person: " + person, e);
			}

			return 0;
		}

		private Map<String, String> getStringStringMap(GLonLat oll, GLonLat dll, long startTime) {
			Map<String, String> params = new HashMap<>();
			params.put("UnitTypeCode", "2");
			params.put("StartLongitude", String.valueOf(oll.getLon()));
			params.put("StartLatitude", String.valueOf(oll.getLat()));
			params.put("GoalLongitude", String.valueOf(dll.getLon()));
			params.put("GoalLatitude", String.valueOf(dll.getLat()));

			Map<String, String> mixedparams = new HashMap<>(params);
			mixedparams.put("TransportCode", transportCode);

			mixedparams.put("AppDate", appDate);
			mixedparams.put("AppTime", convertSecondsToHHMM(startTime));
			mixedparams.put("MaxRadius", maxRadius);
			mixedparams.put("MaxRoutes", maxRoutes);
			return mixedparams;
		}

		@Override
		public Integer call() throws Exception {
			try {
			for (Person p : listAgents) {
				int res = process(p);
				if (res < 0) {
					this.error++;
				}
				this.total++;
			}
			} catch (Throwable t) {
				System.err.println("[WebAPI TripTask " + id + "] failed: " + t);
				t.printStackTrace();
				if (t instanceof Exception) throw (Exception) t;
				throw new RuntimeException(t);
			}
			// System.out.printf("[%d]-%d-%d%n",id, error, total);
			return 0;
		}
	}
	
	public void generate(List<Person> agents) {
		// prepare thread processing
		int numThreads = Runtime.getRuntime().availableProcessors();
		System.out.println("NumOfThreads:" + numThreads);
		
		List<Callable<Integer> > listTasks = new ArrayList<>();
		int listSize = agents.size();
		int taskNum = numThreads;
		int stepSize = listSize / taskNum + (listSize % taskNum != 0 ? 1 : 0);
		for (int i = 0; i < listSize; i+= stepSize){
			int end = i + stepSize;
			end = Math.min(listSize, end);
			List<Person> subList = agents.subList(i, end);
			listTasks.add(new TripTask(i, subList));
		}
		System.out.println("NumOfTasks:" + listTasks.size());
		
		// execute thread processing
		ExecutorService es = Executors.newFixedThreadPool(numThreads);
		List<Future<Integer>> futures;
		try {
			futures = es.invokeAll(listTasks);
			es.shutdown();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("WebAPI trip generation tasks interrupted", ex);
		}
		for (Future<Integer> f : futures) {
			try {
				f.get();
			} catch (ExecutionException ex) {
				throw new RuntimeException("WebAPI trip generation task failed", ex.getCause());
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("WebAPI trip generation task interrupted", ex);
			}
		}
	}

	private static JsonNode getMixedRoute(CloseableHttpClient httpClient, String sessionid, Map<String, String> params) {
		String mixedRouteURL = prop.getProperty("api.getMixedRouteURL");
		if (mixedRouteURL == null) {
			throw new IllegalStateException("Missing config key: api.getMixedRouteURL");
		}
		HttpPost mixedRoutePost = new HttpPost(mixedRouteURL);

		List<NameValuePair> mixedRouteParams = new ArrayList<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			mixedRouteParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

        try {
            mixedRoutePost.setEntity(new UrlEncodedFormEntity(mixedRouteParams));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        mixedRoutePost.setHeader("Cookie", "WebApiSessionID=" + sessionid);

        HttpResponse mixedRouteResponse = null;
        try {
            mixedRouteResponse = executePostRequest(httpClient, mixedRoutePost);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ObjectMapper mapper = new ObjectMapper();

		if (mixedRouteResponse.getStatusLine().getStatusCode() == 200) {
            String mixedRouteResponseBody = null;
            try {
                mixedRouteResponseBody = EntityUtils.toString(mixedRouteResponse.getEntity());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                return mapper.readTree(mixedRouteResponseBody);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
			System.out.println("Failed to get mixed route: " + mixedRouteResponse.getStatusLine().getStatusCode());
            try {
                return mapper.readTree("");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
	}

	private static JsonNode getRoadRoute(CloseableHttpClient httpClient, String sessionid, Map<String, String> params) throws Exception {
		String roadRouteURL = prop.getProperty("api.getRoadRouteURL");
		if (roadRouteURL == null) {
			throw new IllegalStateException("Missing config key: api.getRoadRouteURL");
		}
		HttpPost roadRoutePost = new HttpPost(roadRouteURL);

		List<NameValuePair> roadRouteParams = new ArrayList<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			roadRouteParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		roadRoutePost.setEntity(new UrlEncodedFormEntity(roadRouteParams));
		roadRoutePost.setHeader("Cookie", "WebApiSessionID=" + sessionid);

		HttpResponse roadRouteResponse = executePostRequest(httpClient, roadRoutePost);
		ObjectMapper mapper = new ObjectMapper();

		if (roadRouteResponse.getStatusLine().getStatusCode() == 200) {
			String roadRouteResponseBody = EntityUtils.toString(roadRouteResponse.getEntity());
			return mapper.readTree(roadRouteResponseBody);
		} else {
			System.out.println("Failed to get road route: " + roadRouteResponse.getStatusLine().getStatusCode());
			return mapper.readTree("");
		}
	}
	private static Properties prop;

	public static void main(String[] args) throws Exception {

		int start = 1;
		int end = 47;
		int mfactor = 1;
		if (args.length >= 1) {
			start = end = Integer.parseInt(args[0]);
		}
		if (args.length >= 2) {
			mfactor = Integer.parseInt(args[1]);
		}

		prop = ConfigLoader.load(start);

		String root = prop.getProperty("root");
		String inputDir = prop.getProperty("inputDir");
		System.out.println("Root Directory: " + root);
		System.out.println("Input Directory: " + inputDir);
		
		Country japan = new Country();

		// load data
		String railFile = String.format("%srailnetwork.tsv", inputDir+"/network/");
		Network railway = RailLoader.load(railFile);


		String cityFile = String.format("%scity_boundary.csv", inputDir);
		DataAccessor.loadCityData(cityFile, japan);

		String stationFile = String.format("%sbase_station.csv", inputDir);
		Network station = DataAccessor.loadLocationData(stationFile);
		japan.setStation(station);

		String outputDir = prop.getProperty("outputDir", root);

		for (int i = start; i <= end; i++){

			File tripDir = new File(outputDir+"trip/", String.valueOf(i));
			File trajDir = new File(outputDir+"trajectory/", String.valueOf(i));
			System.out.println("Start prefecture:" + i +" "+ tripDir.mkdirs() +" "+ trajDir.mkdirs());

			String roadFile = String.format("%sdrm_%02d.tsv", inputDir+"/network/", i);

			Network road = DrmLoader.load(roadFile);
			String carKey = "car." + i;
			String carVal = prop.getProperty(carKey);
			if (carVal == null) {
				System.err.println("Missing config key: " + carKey + " -- skipping prefecture " + i);
				continue;
			}
			Double carRatio = Double.parseDouble(carVal);
			String bikeKey = "bike." + i;
			String bikeVal = prop.getProperty(bikeKey);
			if (bikeVal == null) {
				System.err.println("Missing config key: " + bikeKey + " -- defaulting bike ratio to 0.0 for prefecture " + i);
			}
			Double bikeRatio = bikeVal != null ? Double.parseDouble(bikeVal) : 0.0;

			// one session per prefecture (was per city file)
			TripGenerator_WebAPI_refactor worker = new TripGenerator_WebAPI_refactor(japan, road, railway);

			// Try outputDir first (chained mainline), fall back to root (legacy/external activity data)
			// When chained, activity is already sampled — use mfactor=1 to avoid double-sampling
			File actDir = new File(outputDir + "activity/", String.valueOf(i));
			int loadScale = 1; // chained: already sampled
			if (!actDir.isDirectory()) {
				actDir = new File(root + "activity/", String.valueOf(i));
				loadScale = mfactor; // fallback: apply sampling here
				if (actDir.isDirectory()) {
					System.out.println("Activity input: " + actDir.getAbsolutePath() + " (fallback to root, mfactor=" + mfactor + ")");
				}
			} else {
				System.out.println("Activity input: " + actDir.getAbsolutePath() + " (chained, mfactor=1)");
			}
			File[] actFiles = actDir.listFiles();
			if (actFiles == null) {
				System.err.println("Activity directory not found: " + actDir.getAbsolutePath());
				continue;
			}
			for(File file: actFiles){
				if (file.getName().contains(".csv")) {
					// Extract city code: "person_22101.csv" -> "22101", "person_22101_labor.csv" -> "22101"
					String baseName = file.getName().replace(".csv", "");
					String cityCode = baseName.substring("person_".length());
					int underscorePos = cityCode.indexOf('_');
					if (underscorePos > 0) {
						cityCode = cityCode.substring(0, underscorePos);
					}
					String tripFileName = outputDir + "trip/" + i + "/trip_" + cityCode + ".csv";

					String trajectoryFileName = outputDir + "trajectory/" + i + "/trajectory_" + cityCode + ".csv";


					// Check if the files already exist
//					if (new File(tripFileName).exists() || new File(trajectoryFileName).exists()) {
//						continue; // Skip to the next iteration
//					}

					long starttime = System.currentTimeMillis();
					List<Person> agents = PersonAccessor.loadActivity(file.getAbsolutePath(), loadScale, carRatio, bikeRatio);
					System.out.printf("%s%n", file.getName());
					worker.generate(agents);
					PersonAccessor.writeTrips(tripFileName, agents);
					PersonAccessor.writeTrajectory(trajectoryFileName, agents);
					long endtime = System.currentTimeMillis();
					System.out.println(file.getName() + ": " + (endtime - starttime));
				}
			}

			// Print mixed-route diagnostics for this prefecture
			System.out.println("--- Mixed-route diagnostics (pref " + i + ") ---");
			System.out.println("  AppDate: " + worker.appDate);
			System.out.println("  Trips below distance threshold (<" + worker.MIN_TRANSIT_DISTANCE + "m): " + worker.mixedBelowDistThreshold.get());
			System.out.println("  Mixed-route API queries: " + worker.mixedQueryCount.get());
			System.out.println("  Transit available (num_station>0 && fare>0): " + worker.mixedTransitAvailable.get());
			System.out.println("  No station (num_station==0): " + worker.mixedNoStationCount.get());
			System.out.println("  No fare (fare==0): " + worker.mixedNoFareCount.get());
			System.out.println("  MIX mode selected (won cost comparison): " + worker.mixedSelectedCount.get());
			System.out.println("---");

		}
		System.out.println("end");
	}	
}
