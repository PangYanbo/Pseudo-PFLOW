package pseudo.res;

import jp.ac.ut.csis.pflow.geom2.ILonLat;

public class Trip {
	private ETransport transport;
	private EPurpose purpose;
	private long depTime;
	private ILonLat origin;
	private ILonLat destination;
	private int tripId;
	private int subtripId;
	private ETransport repMode;

	public Trip(ETransport transport, EPurpose purpose, long depTime, ILonLat origin, ILonLat destination) {
		this.transport = transport;
		this.purpose = purpose;
		this.origin = origin;
		this.destination = destination;
		this.depTime = depTime;
		this.repMode = transport; // default: segment mode is representative mode
	}
	
	public EPurpose getPurpose() {
		return this.purpose;
	}

	public ETransport getTransport() {
		return transport;
	}

	public long getDepTime() {
		return depTime;
	}

	public void setDepTime(long time) {
		this.depTime = time;
	}
	
	public ILonLat getOrigin() {
		return origin;
	}

	public ILonLat getDestination() {
		return destination;
	}

	public int getTripId() {
		return tripId;
	}

	public void setTripId(int tripId) {
		this.tripId = tripId;
	}

	public int getSubtripId() {
		return subtripId;
	}

	public void setSubtripId(int subtripId) {
		this.subtripId = subtripId;
	}

	public ETransport getRepMode() {
		return repMode;
	}

	public void setRepMode(ETransport repMode) {
		this.repMode = repMode;
	}

	/**
	 * Compute representative mode for a trip using priority rule:
	 * TRAIN > BUS > CAR > BICYCLE > WALK > NOT_DEFINED.
	 * Call with each segment's mode; the highest-priority mode wins.
	 */
	public static ETransport computeRepMode(ETransport current, ETransport candidate) {
		return priority(candidate) > priority(current) ? candidate : current;
	}

	private static int priority(ETransport mode) {
		switch (mode) {
			case TRAIN:       return 5;
			case BUS:         return 4;
			case CAR:         return 3;
			case BICYCLE:     return 2;
			case WALK:        return 1;
			default:          return 0; // NOT_DEFINED, MIX, COMMUNITY
		}
	}
}
