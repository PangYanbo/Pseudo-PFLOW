#!/usr/bin/env python3
"""Resolve target city codes to generated file paths.

Handles three alignment cases:
  1. Direct match: target code = generated file code
  2. Ward aggregation: designated cities (e.g., 22100 -> 22101,22102,22103)
  3. Leading-zero padding: prefectures 1-9 (e.g., target 1203 -> file 01203)
"""

import csv
import os
from glob import glob
from pathlib import Path


def pad_city_code(city_code):
    """Zero-pad city code to 5 digits."""
    return city_code.zfill(5)


def resolve_city_files(target_code, generated_dir, file_prefix="person_", file_suffix=".csv"):
    """Return list of generated file paths for a target city code.

    Args:
        target_code: City code from target CSV (e.g., "22100", "1203")
        generated_dir: Directory containing generated files
        file_prefix: Filename prefix (e.g., "person_", "trip_")
        file_suffix: Filename suffix (e.g., ".csv")

    Returns:
        List of matching file paths, sorted. Empty if no match.
    """
    padded = pad_city_code(target_code)

    # Case 1: direct match
    direct = os.path.join(generated_dir, f"{file_prefix}{padded}{file_suffix}")
    if os.path.isfile(direct):
        return [direct]

    # Case 2: ward aggregation (first 4 digits of padded code)
    prefix = padded[:4]
    pattern = os.path.join(generated_dir, f"{file_prefix}{prefix}*{file_suffix}")
    ward_files = sorted(glob(pattern))
    if ward_files:
        return ward_files

    return []


def resolve_trip_files(target_code, trip_dir):
    """Resolve target code to trip CSV files."""
    return resolve_city_files(target_code, trip_dir, file_prefix="trip_")


def resolve_activity_files(target_code, activity_dir):
    """Resolve target code to activity/person CSV files."""
    return resolve_city_files(target_code, activity_dir, file_prefix="person_")


def load_transport_targets(target_csv):
    """Load transport share targets, keyed by city_code.

    Returns dict: city_code -> {
        pref_code, city_name, car_target, rail_target, bus_target,
        bicycle_target, walk_other_target, motorcycle_share
    }

    Motorcycle is merged into car_target by default.
    """
    targets = {}
    with open(target_csv, encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            code = row["city_code"]
            car_driver = float(row["car_driver_share"])
            car_passenger = float(row["car_passenger_share"])
            motorcycle = float(row["motorcycle_share"])
            targets[code] = {
                "pref_code": row["pref_code"],
                "pref_name": row["pref_name"],
                "city_name": row["city_name"],
                "city_segment": row["city_segment"],
                "center_periphery": row["center_periphery"],
                "car_target": car_driver + car_passenger + motorcycle,
                "rail_target": float(row["rail_share"]),
                "bus_target": float(row["bus_share"]),
                "bicycle_target": float(row["bicycle_share"]),
                "walk_other_target": float(row["walk_other_share"]),
                "motorcycle_share": motorcycle,
            }
    return targets


def get_pref_targets(targets, pref_code):
    """Filter targets for a single prefecture."""
    return {k: v for k, v in targets.items() if v["pref_code"] == str(pref_code)}


def pick_representative_city(pref_targets):
    """Pick the representative city for Stage 1 tuning.

    Prefer center cities (center_periphery == '中心都市') as they have
    the widest mode diversity. Fall back to the first city.
    """
    centers = [k for k, v in pref_targets.items() if v["center_periphery"] == "中心都市"]
    if centers:
        return centers[0]
    return next(iter(pref_targets))


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <target_csv> <generated_dir> [pref_code]", file=sys.stderr)
        sys.exit(1)

    target_csv = sys.argv[1]
    generated_dir = sys.argv[2]
    pref_filter = sys.argv[3] if len(sys.argv) > 3 else None

    targets = load_transport_targets(target_csv)
    if pref_filter:
        targets = get_pref_targets(targets, pref_filter)

    for code, info in sorted(targets.items()):
        files = resolve_city_files(code, generated_dir)
        status = f"{len(files)} files" if files else "NO MATCH"
        print(f"  {code} ({info['city_name']}): {status}")
        for f in files:
            print(f"    -> {f}")
