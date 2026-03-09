#!/usr/bin/env python3
"""Validate trajectory CSV files produced by TrajectoryGenerator.

Trajectory CSV columns (no header):
  0: person_id (int)
  1: unix_time_ms (long, milliseconds)
  2: datetime_str (str, yyyy-MM-dd HH:mm:ss)
  3: lon (float)
  4: lat (float)
  5: transport_id (int)
  6: purpose_id (int)
  7: labor_str (str, e.g. WORKER)
  8: link_id (str, may be empty)
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
TRANSPORT_NAMES = {0: "NOT_DEFINED", 1: "WALK", 2: "BICYCLE", 3: "CAR", 4: "TRAIN", 5: "BUS", 6: "MIX", 7: "COMMUNITY"}
MAX_JUMP_KM = 50  # spatial jump threshold between consecutive points
WARN_MISSING_LINK_RATIO = 0.30  # first point per trip has no link, so some ratio is normal
WARN_DUPLICATE_ROW_RATIO = 0.01
MAX_SPEED_KMH = 500  # above this = suspicious (even shinkansen ~320)


def haversine_km(lon1, lat1, lon2, lat2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2) ** 2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) ** 2
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def validate_file(filepath):
    """Validate a single trajectory CSV file. Returns a dict of results."""
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
            if len(row) < 8:
                parse_errors += 1
                continue
            rows.append(row)

    if parse_errors > 0:
        result["warnings"].append(f"{parse_errors} rows with <8 columns")

    row_count = len(rows)
    result["stats"]["row_count"] = row_count
    result["stats"]["file_size_bytes"] = size

    if row_count < MIN_ROW_COUNT:
        result["status"] = "FAIL"
        result["errors"].append(f"Row count {row_count} < {MIN_ROW_COUNT}")
        return result

    # Parse
    persons = defaultdict(list)
    transport_counter = Counter()
    bad_coords = 0
    missing_links = 0
    duplicate_rows = 0
    seen_rows = set()

    for row in rows:
        try:
            pid = int(row[0])
            unix_ms = int(row[1])
            lon = float(row[3])
            lat = float(row[4])
            transport = int(row[5])
            link_id = row[8].strip() if len(row) > 8 else ""
        except (ValueError, IndexError):
            parse_errors += 1
            continue

        # Duplicate detection (pid + timestamp + coords)
        row_key = (pid, unix_ms, row[3], row[4])
        if row_key in seen_rows:
            duplicate_rows += 1
        seen_rows.add(row_key)

        persons[pid].append({
            "unix_ms": unix_ms,
            "lon": lon,
            "lat": lat,
            "transport": transport,
            "link_id": link_id,
        })

        transport_counter[transport] += 1

        if not (LON_RANGE[0] <= lon <= LON_RANGE[1] and LAT_RANGE[0] <= lat <= LAT_RANGE[1]):
            bad_coords += 1

        if not link_id:
            missing_links += 1

    num_persons = len(persons)
    points_per_person = [len(v) for v in persons.values()]
    result["stats"]["num_persons"] = num_persons
    result["stats"]["avg_points_per_person"] = round(sum(points_per_person) / num_persons, 1) if num_persons else 0
    result["stats"]["transport_distribution"] = {
        TRANSPORT_NAMES.get(k, str(k)): v for k, v in sorted(transport_counter.items())
    }

    # Per-person checks: timestamp monotonicity, spatial jumps, speed
    time_violations = 0
    spatial_jumps = 0
    speed_violations = 0
    persons_with_issues = set()

    for pid, pts in persons.items():
        for i in range(1, len(pts)):
            prev, curr = pts[i - 1], pts[i]

            # Timestamp monotonicity
            if curr["unix_ms"] < prev["unix_ms"]:
                time_violations += 1
                persons_with_issues.add(pid)

            # Spatial jump
            dist_km = haversine_km(prev["lon"], prev["lat"], curr["lon"], curr["lat"])
            if dist_km > MAX_JUMP_KM:
                spatial_jumps += 1
                persons_with_issues.add(pid)

            # Speed check
            dt_hours = (curr["unix_ms"] - prev["unix_ms"]) / 3_600_000
            if dt_hours > 0:
                speed = dist_km / dt_hours
                if speed > MAX_SPEED_KMH:
                    speed_violations += 1

    # Checks
    if bad_coords > 0:
        pct = bad_coords / row_count * 100
        msg = f"{bad_coords} points ({pct:.1f}%) outside Japan bbox"
        if pct > 5:
            result["errors"].append(msg)
        else:
            result["warnings"].append(msg)

    if time_violations > 0:
        msg = f"{time_violations} timestamp monotonicity violations"
        result["errors"].append(msg)

    if spatial_jumps > 0:
        msg = f"{spatial_jumps} spatial jumps > {MAX_JUMP_KM}km"
        if spatial_jumps > row_count * 0.01:
            result["errors"].append(msg)
        else:
            result["warnings"].append(msg)

    if speed_violations > 0:
        result["warnings"].append(f"{speed_violations} point pairs with speed > {MAX_SPEED_KMH} km/h")

    if missing_links > 0:
        ratio = missing_links / row_count
        result["stats"]["missing_link_ratio"] = round(ratio, 3)
        if ratio > WARN_MISSING_LINK_RATIO:
            result["warnings"].append(f"{missing_links} points ({ratio*100:.1f}%) missing link_id")

    if duplicate_rows > 0:
        ratio = duplicate_rows / row_count
        msg = f"{duplicate_rows} duplicate rows ({ratio*100:.1f}%)"
        if ratio > WARN_DUPLICATE_ROW_RATIO:
            result["warnings"].append(msg)

    if persons_with_issues:
        result["stats"]["persons_with_issues"] = len(persons_with_issues)

    if result["errors"]:
        result["status"] = "FAIL"
    elif result["warnings"]:
        result["status"] = "WARN"

    return result


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <trajectory_dir> [output_json]", file=sys.stderr)
        sys.exit(1)

    traj_dir = Path(sys.argv[1])
    output_json = sys.argv[2] if len(sys.argv) > 2 else None

    if not traj_dir.is_dir():
        print(f"ERROR: {traj_dir} is not a directory", file=sys.stderr)
        sys.exit(1)

    files = sorted(traj_dir.glob("*.csv"))
    if not files:
        print(f"ERROR: No CSV files found in {traj_dir}", file=sys.stderr)
        sys.exit(1)

    results = []
    for f in files:
        r = validate_file(f)
        results.append(r)
        status = r["status"]
        print(f"  [{status:4s}] {f.name}: {r['stats'].get('num_persons', 0)} persons, {r['stats'].get('row_count', 0)} rows")

    summary = {
        "type": "trajectory",
        "directory": str(traj_dir),
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
