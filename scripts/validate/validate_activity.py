#!/usr/bin/env python3
"""Validate activity CSV files produced by ActivityGenerator.

Activity CSV columns (no header):
  0: person_id (int)
  1: age (int)
  2: gender_id (int: 1=MALE, 2=FEMALE)
  3: labor_id (int)
  4: start_time (int, seconds from midnight)
  5: duration (int, seconds)
  6: purpose_id (int: 1=HOME,2=OFFICE,3=SCHOOL,4=RETURN_OFFICE,100=SHOPPING,200=EATING,300=HOSPITAL,400=FREE,500=BUSINESS)
  7: lon (float)
  8: lat (float)
  9: gcode (str, 5-digit city code)
"""

import csv
import json
import os
import sys
from collections import Counter, defaultdict
from pathlib import Path

# ── Thresholds ──────────────────────────────────────────────────────────────
MIN_ROW_COUNT = 10
MAX_ACTIVITIES_PER_PERSON = 30
LON_RANGE = (120.0, 155.0)  # Japan bounding box
LAT_RANGE = (20.0, 50.0)
VALID_PURPOSES = {1, 2, 3, 4, 100, 200, 300, 400, 500}
VALID_GENDERS = {1, 2}
MAX_START_TIME = 86400 * 2  # 48h in seconds (allow next-day)
WARN_EMPTY_GCODE_RATIO = 0.01
WARN_DUPLICATE_ID_RATIO = 0.0  # any duplicates with identical rows = warn


def validate_file(filepath):
    """Validate a single activity CSV file. Returns a dict of results."""
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
        for lineno, row in enumerate(reader, 1):
            if len(row) < 10:
                parse_errors += 1
                continue
            rows.append(row)

    if parse_errors > 0:
        result["warnings"].append(f"{parse_errors} rows with <10 columns")

    row_count = len(rows)
    result["stats"]["row_count"] = row_count
    result["stats"]["file_size_bytes"] = size

    if row_count < MIN_ROW_COUNT:
        result["status"] = "FAIL"
        result["errors"].append(f"Row count {row_count} < {MIN_ROW_COUNT}")
        return result

    # Parse into structured data
    persons = defaultdict(list)
    purpose_counter = Counter()
    gender_counter = Counter()
    bad_coords = 0
    bad_purposes = 0
    bad_genders = 0
    empty_gcodes = 0
    bad_start_times = 0

    for row in rows:
        try:
            pid = int(row[0])
            age = int(row[1])
            gender = int(row[2])
            labor = int(row[3])
            start_time = int(row[4])
            duration = int(row[5])
            purpose = int(row[6])
            lon = float(row[7])
            lat = float(row[8])
            gcode = row[9].strip()
        except (ValueError, IndexError):
            parse_errors += 1
            continue

        persons[pid].append({
            "start_time": start_time,
            "duration": duration,
            "purpose": purpose,
            "lon": lon,
            "lat": lat,
        })

        purpose_counter[purpose] += 1
        gender_counter[gender] += 1

        if not (LON_RANGE[0] <= lon <= LON_RANGE[1] and LAT_RANGE[0] <= lat <= LAT_RANGE[1]):
            bad_coords += 1
        if purpose not in VALID_PURPOSES:
            bad_purposes += 1
        if gender not in VALID_GENDERS:
            bad_genders += 1
        if not gcode:
            empty_gcodes += 1
        if start_time < 0 or start_time > MAX_START_TIME:
            bad_start_times += 1

    num_persons = len(persons)
    result["stats"]["num_persons"] = num_persons
    result["stats"]["purpose_distribution"] = {str(k): v for k, v in sorted(purpose_counter.items())}
    result["stats"]["gender_distribution"] = {str(k): v for k, v in sorted(gender_counter.items())}

    # Activity counts per person
    act_counts = [len(v) for v in persons.values()]
    avg_acts = sum(act_counts) / num_persons if num_persons else 0
    max_acts = max(act_counts) if act_counts else 0
    single_act = sum(1 for c in act_counts if c == 1)
    result["stats"]["avg_activities_per_person"] = round(avg_acts, 2)
    result["stats"]["max_activities_per_person"] = max_acts
    result["stats"]["single_activity_persons"] = single_act

    # Check time ordering per person
    time_order_violations = 0
    for pid, acts in persons.items():
        for i in range(1, len(acts)):
            if acts[i]["start_time"] < acts[i - 1]["start_time"]:
                time_order_violations += 1
                break

    # Checks
    if bad_coords > 0:
        pct = bad_coords / row_count * 100
        msg = f"{bad_coords} rows ({pct:.1f}%) with coordinates outside Japan bbox"
        if pct > 5:
            result["errors"].append(msg)
            result["status"] = "FAIL"
        else:
            result["warnings"].append(msg)

    if bad_purposes > 0:
        result["warnings"].append(f"{bad_purposes} rows with unknown purpose_id")

    if bad_genders > 0:
        result["warnings"].append(f"{bad_genders} rows with unknown gender_id")

    if empty_gcodes > 0:
        ratio = empty_gcodes / row_count
        msg = f"{empty_gcodes} rows ({ratio*100:.1f}%) with empty gcode"
        if ratio > WARN_EMPTY_GCODE_RATIO:
            result["warnings"].append(msg)

    if bad_start_times > 0:
        result["warnings"].append(f"{bad_start_times} rows with start_time out of range")

    if time_order_violations > 0:
        pct = time_order_violations / num_persons * 100
        msg = f"{time_order_violations} persons ({pct:.1f}%) with non-monotonic activity start times"
        if pct > 10:
            result["warnings"].append(msg)

    if max_acts > MAX_ACTIVITIES_PER_PERSON:
        result["warnings"].append(f"Max activities per person = {max_acts} (threshold {MAX_ACTIVITIES_PER_PERSON})")

    if result["errors"]:
        result["status"] = "FAIL"
    elif result["warnings"]:
        result["status"] = "WARN"

    return result


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <activity_dir> [output_json]", file=sys.stderr)
        sys.exit(1)

    activity_dir = Path(sys.argv[1])
    output_json = sys.argv[2] if len(sys.argv) > 2 else None

    if not activity_dir.is_dir():
        print(f"ERROR: {activity_dir} is not a directory", file=sys.stderr)
        sys.exit(1)

    files = sorted(activity_dir.glob("*.csv"))
    if not files:
        print(f"ERROR: No CSV files found in {activity_dir}", file=sys.stderr)
        sys.exit(1)

    results = []
    for f in files:
        r = validate_file(f)
        results.append(r)
        status = r["status"]
        print(f"  [{status:4s}] {f.name}: {r['stats'].get('num_persons', 0)} persons, {r['stats'].get('row_count', 0)} rows")

    summary = {
        "type": "activity",
        "directory": str(activity_dir),
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
