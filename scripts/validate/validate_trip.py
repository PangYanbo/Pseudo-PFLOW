#!/usr/bin/env python3
"""Validate trip CSV files produced by TripGenerator.

Trip CSV columns (no header):
  0: person_id (int)
  1: dep_time (int, seconds from midnight)
  2: origin_lon (float)
  3: origin_lat (float)
  4: dest_lon (float)
  5: dest_lat (float)
  6: transport_id (int: 0=NOT_DEFINED,1=WALK,2=BICYCLE,3=CAR,4=TRAIN,5=BUS,6=MIX,7=COMMUNITY)
  7: purpose_id (int)
  8: labor_id (int)
"""

import csv
import json
import math
import os
import sys
from collections import Counter, defaultdict
from pathlib import Path

# ── Thresholds ──────────────────────────────────────────────────────────────
MIN_ROW_COUNT = 10
LON_RANGE = (120.0, 155.0)
LAT_RANGE = (20.0, 50.0)
VALID_TRANSPORTS = {0, 1, 2, 3, 4, 5, 6, 7}
TRANSPORT_NAMES = {0: "NOT_DEFINED", 1: "WALK", 2: "BICYCLE", 3: "CAR", 4: "TRAIN", 5: "BUS", 6: "MIX", 7: "COMMUNITY"}
WARN_NOT_DEFINED_RATIO = 0.05
WARN_ZERO_DISTANCE_RATIO = 0.20
MAX_TRIP_DISTANCE_KM = 2000  # extreme outlier threshold
MAX_DEP_TIME = 86400 * 2  # 48h


def haversine_km(lon1, lat1, lon2, lat2):
    """Approximate distance in km."""
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2) ** 2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) ** 2
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def validate_file(filepath, activity_person_ids=None):
    """Validate a single trip CSV file. Returns a dict of results."""
    result = {
        "file": str(filepath),
        "status": "PASS",
        "errors": [],
        "warnings": [],
        "stats": {},
    }

    if not os.path.exists(filepath):
        result["status"] = "FAIL"
        result["errors"].append("File not found")
        return result

    size = os.path.getsize(filepath)
    if size == 0:
        result["status"] = "FAIL"
        result["errors"].append("File is empty")
        return result

    rows = []
    parse_errors = 0
    with open(filepath, "r") as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) < 9:
                parse_errors += 1
                continue
            rows.append(row)

    if parse_errors > 0:
        result["warnings"].append(f"{parse_errors} rows with <9 columns")

    row_count = len(rows)
    result["stats"]["row_count"] = row_count
    result["stats"]["file_size_bytes"] = size

    if row_count < MIN_ROW_COUNT:
        result["status"] = "FAIL"
        result["errors"].append(f"Row count {row_count} < {MIN_ROW_COUNT}")
        return result

    persons = defaultdict(list)
    transport_counter = Counter()
    bad_coords = 0
    zero_distance = 0
    extreme_distance = 0
    not_defined_count = 0
    bad_dep_times = 0
    distances_km = []

    for row in rows:
        try:
            pid = int(row[0])
            dep_time = int(row[1])
            olon, olat = float(row[2]), float(row[3])
            dlon, dlat = float(row[4]), float(row[5])
            transport = int(row[6])
            purpose = int(row[7])
        except (ValueError, IndexError):
            parse_errors += 1
            continue

        persons[pid].append({"dep_time": dep_time, "transport": transport})
        transport_counter[transport] += 1

        if transport == 0:
            not_defined_count += 1

        if not (LON_RANGE[0] <= olon <= LON_RANGE[1] and LAT_RANGE[0] <= olat <= LAT_RANGE[1]):
            bad_coords += 1
        if not (LON_RANGE[0] <= dlon <= LON_RANGE[1] and LAT_RANGE[0] <= dlat <= LAT_RANGE[1]):
            bad_coords += 1

        dist = haversine_km(olon, olat, dlon, dlat)
        distances_km.append(dist)
        if dist < 0.001:
            zero_distance += 1
        if dist > MAX_TRIP_DISTANCE_KM:
            extreme_distance += 1

        if dep_time < 0 or dep_time > MAX_DEP_TIME:
            bad_dep_times += 1

    num_persons = len(persons)
    trip_counts = [len(v) for v in persons.values()]
    result["stats"]["num_persons"] = num_persons
    result["stats"]["avg_trips_per_person"] = round(sum(trip_counts) / num_persons, 2) if num_persons else 0
    result["stats"]["max_trips_per_person"] = max(trip_counts) if trip_counts else 0
    result["stats"]["transport_distribution"] = {
        TRANSPORT_NAMES.get(k, str(k)): v for k, v in sorted(transport_counter.items())
    }

    if distances_km:
        result["stats"]["distance_km_median"] = round(sorted(distances_km)[len(distances_km) // 2], 2)
        result["stats"]["distance_km_p95"] = round(sorted(distances_km)[int(len(distances_km) * 0.95)], 2)

    # Departure time ordering per person
    time_violations = 0
    for pid, trips in persons.items():
        for i in range(1, len(trips)):
            if trips[i]["dep_time"] < trips[i - 1]["dep_time"]:
                time_violations += 1
                break

    # Person coverage vs activity
    if activity_person_ids is not None:
        trip_pids = set(persons.keys())
        missing = activity_person_ids - trip_pids
        extra = trip_pids - activity_person_ids
        coverage = len(trip_pids & activity_person_ids) / len(activity_person_ids) * 100 if activity_person_ids else 0
        result["stats"]["person_coverage_pct"] = round(coverage, 1)
        if missing:
            result["stats"]["missing_from_activity"] = len(missing)
        if extra:
            result["stats"]["extra_vs_activity"] = len(extra)

    # Checks
    if bad_coords > 0:
        pct = bad_coords / (row_count * 2) * 100
        msg = f"{bad_coords} coordinate values outside Japan bbox ({pct:.1f}%)"
        if pct > 5:
            result["errors"].append(msg)
        else:
            result["warnings"].append(msg)

    if not_defined_count > 0:
        ratio = not_defined_count / row_count
        msg = f"{not_defined_count} trips ({ratio*100:.1f}%) with NOT_DEFINED transport"
        if ratio > WARN_NOT_DEFINED_RATIO:
            result["errors"].append(msg)
        else:
            result["warnings"].append(msg)

    if zero_distance > 0:
        ratio = zero_distance / row_count
        msg = f"{zero_distance} trips ({ratio*100:.1f}%) with zero distance"
        if ratio > WARN_ZERO_DISTANCE_RATIO:
            result["warnings"].append(msg)

    if extreme_distance > 0:
        result["warnings"].append(f"{extreme_distance} trips exceed {MAX_TRIP_DISTANCE_KM}km")

    if bad_dep_times > 0:
        result["warnings"].append(f"{bad_dep_times} trips with dep_time out of range")

    if time_violations > 0:
        pct = time_violations / num_persons * 100
        msg = f"{time_violations} persons ({pct:.1f}%) with non-monotonic departure times"
        if pct > 20:
            result["warnings"].append(msg)

    if result["errors"]:
        result["status"] = "FAIL"
    elif result["warnings"]:
        result["status"] = "WARN"

    return result


def load_activity_person_ids(activity_dir, city_code):
    """Load person IDs from the matching activity file."""
    candidates = list(Path(activity_dir).glob(f"*{city_code}*.csv"))
    if not candidates:
        return None
    pids = set()
    with open(candidates[0], "r") as f:
        for row in csv.reader(f):
            if len(row) >= 1:
                try:
                    pids.add(int(row[0]))
                except ValueError:
                    pass
    return pids


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <trip_dir> [output_json] [activity_dir]", file=sys.stderr)
        sys.exit(1)

    trip_dir = Path(sys.argv[1])
    output_json = sys.argv[2] if len(sys.argv) > 2 else None
    activity_dir = sys.argv[3] if len(sys.argv) > 3 else None

    if not trip_dir.is_dir():
        print(f"ERROR: {trip_dir} is not a directory", file=sys.stderr)
        sys.exit(1)

    files = sorted(trip_dir.glob("*.csv"))
    if not files:
        print(f"ERROR: No CSV files found in {trip_dir}", file=sys.stderr)
        sys.exit(1)

    results = []
    for f in files:
        # Try to cross-reference with activity file
        act_pids = None
        if activity_dir:
            # Extract city code from filename: trip_XXXXX.csv -> XXXXX
            name = f.stem
            city_code = name.replace("trip_", "")
            act_pids = load_activity_person_ids(activity_dir, city_code)

        r = validate_file(f, act_pids)
        results.append(r)
        status = r["status"]
        print(f"  [{status:4s}] {f.name}: {r['stats'].get('num_persons', 0)} persons, {r['stats'].get('row_count', 0)} rows")

    summary = {
        "type": "trip",
        "directory": str(trip_dir),
        "file_count": len(results),
        "pass": sum(1 for r in results if r["status"] == "PASS"),
        "warn": sum(1 for r in results if r["status"] == "WARN"),
        "fail": sum(1 for r in results if r["status"] == "FAIL"),
        "total_rows": sum(r["stats"].get("row_count", 0) for r in results),
        "total_persons": sum(r["stats"].get("num_persons", 0) for r in results),
        "files": results,
    }

    if output_json:
        os.makedirs(os.path.dirname(output_json) or ".", exist_ok=True)
        with open(output_json, "w") as f:
            json.dump(summary, f, indent=2)
        print(f"  Report written to {output_json}")

    return summary


if __name__ == "__main__":
    main()
