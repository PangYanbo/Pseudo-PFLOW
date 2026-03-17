#!/usr/bin/env python3
"""Score generated trip output against transport share targets.

Reads 12-column trip CSVs, computes trip-level mode shares using rep_mode,
and compares against PT survey target shares. Outputs per-city scores
and a weighted-SSE loss for the prefecture.

Usage:
    python transport_score.py <target_csv> <trip_dir> <pref_code>

    # Score with JSON output:
    python transport_score.py data/tuning/transport_share_targets.csv \
        /tmp/pflow_staging_tripid/trip/22 22 --json

Output: per-city mode shares, diagnostics, and weighted-SSE loss.
"""

import csv
import json
import math
import os
import sys
from collections import Counter
from pathlib import Path

from city_align import (
    get_pref_targets,
    load_transport_targets,
    resolve_trip_files,
)

# ETransport IDs
TRANSPORT_IDS = {
    0: "NOT_DEFINED",
    1: "WALK",
    2: "BICYCLE",
    3: "CAR",
    4: "TRAIN",
    5: "BUS",
    6: "MIX",
    7: "COMMUNITY",
}

# Mode weights for loss function (from TUNING_PLAN.md A1)
MODE_WEIGHTS = {
    "CAR": 3.0,
    "TRAIN": 2.0,
    "BUS": 2.0,
    "BICYCLE": 1.0,
    "WALK": 1.0,
}

# Mapping from generated mode names to target field names
MODE_TO_TARGET = {
    "CAR": "car_target",
    "TRAIN": "rail_target",
    "BUS": "bus_target",
    "BICYCLE": "bicycle_target",
    "WALK": "walk_other_target",
}


def read_trip_file(filepath):
    """Read a 12-column trip CSV and return rows as tuples.

    Returns list of (person_id, trip_id, subtip_id, rep_mode, transport,
                     o_lon, o_lat, d_lon, d_lat).
    """
    rows = []
    with open(filepath) as f:
        reader = csv.reader(f)
        for line in reader:
            if len(line) < 12:
                continue
            person_id = int(line[0])
            transport = int(line[6])
            trip_id = int(line[9])
            subtip_id = int(line[10])
            rep_mode = int(line[11])
            o_lon = float(line[2])
            o_lat = float(line[3])
            d_lon = float(line[4])
            d_lat = float(line[5])
            rows.append((person_id, trip_id, subtip_id, rep_mode, transport,
                         o_lon, o_lat, d_lon, d_lat))
    return rows


def is_placeholder_nd(o_lon, o_lat, d_lon, d_lat):
    """Check if a trip is a placeholder NOT_DEFINED (zero distance)."""
    return abs(o_lon - d_lon) < 1e-8 and abs(o_lat - d_lat) < 1e-8


def compute_city_score(trip_files, target_info):
    """Compute mode shares and loss for one target city.

    Args:
        trip_files: List of trip CSV file paths (may be multiple for ward aggregation).
        target_info: Dict with car_target, rail_target, etc.

    Returns:
        Dict with mode_shares, diagnostics, loss, and details.
    """
    # Read all trip rows from all files
    all_rows = []
    for f in trip_files:
        all_rows.extend(read_trip_file(f))

    if not all_rows:
        return {
            "error": "no trip data",
            "files": [os.path.basename(f) for f in trip_files],
        }

    # Deduplicate to trip level using (person_id, trip_id)
    # For each unique trip, take the rep_mode from the first subtrip (all share the same rep_mode)
    trip_modes = {}  # (person_id, trip_id) -> rep_mode
    trip_coords = {}  # (person_id, trip_id) -> (o_lon, o_lat, d_lon, d_lat) from subtripId=0
    for row in all_rows:
        person_id, trip_id, subtip_id, rep_mode = row[0], row[1], row[2], row[3]
        key = (person_id, trip_id)
        if key not in trip_modes:
            trip_modes[key] = rep_mode
        if subtip_id == 0:
            trip_coords[key] = row[5:9]

    total_trips = len(trip_modes)

    # Classify NOT_DEFINED trips
    placeholder_nd = 0
    unexpected_nd = 0
    for key, mode in trip_modes.items():
        if mode == 0:  # NOT_DEFINED
            coords = trip_coords.get(key)
            if coords and is_placeholder_nd(*coords):
                placeholder_nd += 1
            else:
                unexpected_nd += 1

    # Compute mode shares excluding placeholder NOT_DEFINED
    real_trips = total_trips - placeholder_nd
    if real_trips == 0:
        return {
            "error": "no real trips (all placeholder NOT_DEFINED)",
            "total_trips": total_trips,
            "placeholder_nd": placeholder_nd,
        }

    mode_counter = Counter()
    for key, mode in trip_modes.items():
        # Skip placeholder NOT_DEFINED
        if mode == 0:
            coords = trip_coords.get(key)
            if coords and is_placeholder_nd(*coords):
                continue
        mode_name = TRANSPORT_IDS.get(mode, f"UNKNOWN_{mode}")
        mode_counter[mode_name] += 1

    # Compute shares as fractions (0-1)
    mode_shares = {}
    for mode_name in ["CAR", "TRAIN", "BUS", "BICYCLE", "WALK"]:
        mode_shares[mode_name] = mode_counter.get(mode_name, 0) / real_trips

    # Include unexpected NOT_DEFINED and other modes in shares for reporting
    other_count = sum(c for m, c in mode_counter.items()
                      if m not in ["CAR", "TRAIN", "BUS", "BICYCLE", "WALK"])
    mode_shares["OTHER"] = other_count / real_trips

    # Compute weighted SSE loss against targets
    loss = 0.0
    mode_errors = {}
    for mode_name, weight in MODE_WEIGHTS.items():
        target_key = MODE_TO_TARGET[mode_name]
        target_share = target_info[target_key] / 100.0  # Convert % to fraction
        gen_share = mode_shares.get(mode_name, 0.0)
        error = gen_share - target_share
        mode_errors[mode_name] = {
            "generated": round(gen_share * 100, 2),
            "target": round(target_share * 100, 2),
            "error_pct": round(error * 100, 2),
            "weighted_sq_error": round(weight * error * error * 10000, 4),  # in %² units
        }
        loss += weight * error * error * 10000  # Scale to percentage-squared

    return {
        "files": [os.path.basename(f) for f in trip_files],
        "total_trips": total_trips,
        "real_trips": real_trips,
        "placeholder_nd": placeholder_nd,
        "placeholder_nd_rate": round(placeholder_nd / total_trips * 100, 2),
        "unexpected_nd": unexpected_nd,
        "unexpected_nd_rate": round(unexpected_nd / total_trips * 100, 2),
        "mode_shares": {k: round(v * 100, 2) for k, v in mode_shares.items()},
        "mode_errors": mode_errors,
        "loss": round(loss, 4),
    }


def score_prefecture(target_csv, trip_dir, pref_code):
    """Score all target cities in a prefecture.

    Returns:
        Dict with per-city scores and aggregate loss.
    """
    targets = load_transport_targets(target_csv)
    pref_targets = get_pref_targets(targets, pref_code)

    if not pref_targets:
        return {"error": f"no targets for pref {pref_code}"}

    city_scores = {}
    total_loss = 0.0
    total_real_trips = 0
    matched_cities = 0

    for code, info in sorted(pref_targets.items()):
        trip_files = resolve_trip_files(code, trip_dir)
        if not trip_files:
            city_scores[code] = {
                "city_name": info["city_name"],
                "error": "no matching trip files",
            }
            continue

        score = compute_city_score(trip_files, info)
        score["city_name"] = info["city_name"]
        score["city_segment"] = info["city_segment"]
        score["center_periphery"] = info["center_periphery"]
        city_scores[code] = score

        if "loss" in score:
            total_loss += score["loss"] * score["real_trips"]
            total_real_trips += score["real_trips"]
            matched_cities += 1

    # Population-weighted average loss
    avg_loss = total_loss / total_real_trips if total_real_trips > 0 else float("inf")

    return {
        "pref_code": str(pref_code),
        "num_target_cities": len(pref_targets),
        "matched_cities": matched_cities,
        "total_real_trips": total_real_trips,
        "weighted_avg_loss": round(avg_loss, 4),
        "city_scores": city_scores,
    }


def print_report(result):
    """Print a human-readable report."""
    print(f"\n=== Transport Score: Prefecture {result['pref_code']} ===")
    print(f"Target cities: {result['num_target_cities']}, "
          f"Matched: {result['matched_cities']}, "
          f"Total real trips: {result['total_real_trips']}")
    print(f"Weighted average loss: {result['weighted_avg_loss']}")

    for code, score in sorted(result["city_scores"].items()):
        print(f"\n--- {code} ({score.get('city_name', '?')}) ---")
        if "error" in score:
            print(f"  ERROR: {score['error']}")
            continue

        print(f"  Files: {', '.join(score['files'])}")
        print(f"  Trips: {score['total_trips']} total, {score['real_trips']} real")
        print(f"  Placeholder ND: {score['placeholder_nd']} ({score['placeholder_nd_rate']}%)")
        print(f"  Unexpected ND: {score['unexpected_nd']} ({score['unexpected_nd_rate']}%)")
        print(f"  Loss: {score['loss']}")
        print()
        print(f"  {'Mode':<10} {'Generated':>10} {'Target':>10} {'Error':>10} {'Wt.Sq.Err':>10}")
        print(f"  {'-'*50}")
        for mode in ["CAR", "TRAIN", "BUS", "BICYCLE", "WALK"]:
            err = score["mode_errors"][mode]
            print(f"  {mode:<10} {err['generated']:>9.1f}% {err['target']:>9.1f}% "
                  f"{err['error_pct']:>+9.1f}% {err['weighted_sq_error']:>10.2f}")
        other_pct = score["mode_shares"].get("OTHER", 0)
        if other_pct > 0:
            print(f"  {'OTHER':<10} {other_pct:>9.1f}%")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Score trip output against transport targets")
    parser.add_argument("target_csv", help="Path to transport_share_targets.csv")
    parser.add_argument("trip_dir", help="Directory containing trip_XXXXX.csv files")
    parser.add_argument("pref_code", help="Prefecture code (e.g., 22)")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    args = parser.parse_args()

    result = score_prefecture(args.target_csv, args.trip_dir, args.pref_code)

    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print_report(result)
