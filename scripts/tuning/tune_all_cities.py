#!/usr/bin/env python3
"""Per-target-city transport tuning orchestrator.

For each prefecture that has target cities in transport_share_targets.csv:
  1. Run ActivityGenerator once (shared across configs)
  2. Run TripGenerator_WebAPI_refactor 10 times (baseline + 9 LHS configs)
  3. Score each config against each target city individually
  4. Pick best config per target city → save as param group

Output:
  config/tuning/param_groups/<pref>_<city>.properties  (one per target city)
  output/tuning/final/best_params_by_city.csv
  output/tuning/final/tuning_summary.md

Usage:
    # Run all prefectures
    python tune_all_cities.py

    # Run a single prefecture
    python tune_all_cities.py --pref 22

    # Score only (reuse existing trip output)
    python tune_all_cities.py --score-only

    # Score a single pref
    python tune_all_cities.py --pref 22 --score-only
"""

import argparse
import csv
import json
import os
import sys
from collections import defaultdict
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from city_align import load_transport_targets, get_pref_targets
from transport_tune import (
    run_stage,
    get_output_dir,
    TARGET_CSV,
    generate_tuning_configs,
    run_activity_generator,
    run_trip_generator,
    score_candidate,
)

PROJECT_ROOT = SCRIPT_DIR.parent.parent
PARAM_GROUPS_DIR = PROJECT_ROOT / "config" / "tuning" / "param_groups"
FINAL_DIR = PROJECT_ROOT / "output" / "tuning" / "final"


def get_prefectures_with_targets(target_csv):
    """Return {pref_code: [city_codes]} for all prefectures with targets."""
    targets = load_transport_targets(target_csv)
    prefs = defaultdict(list)
    for code, info in targets.items():
        pref = info["pref_code"]
        if pref:
            prefs[pref].append(code)
    return dict(prefs)


def read_config_params(config_path):
    """Read model parameters from a .properties file (skip outputDir)."""
    params = {}
    with open(config_path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            k = k.strip()
            if k == "outputDir":
                continue
            params[k] = v.strip()
    return params


def score_per_city(pref_code, configs, stage_dir):
    """Score all configs and return per-city best.

    Returns:
        dict: city_code -> {best_config_id, best_loss, all_scores: [{config_id, loss}]}
    """
    targets = load_transport_targets(str(TARGET_CSV))
    pref_targets = get_pref_targets(targets, pref_code)

    # Collect per-city scores across all configs
    # city_code -> [(config_id, loss, score_dict)]
    city_results = defaultdict(list)

    for config_id, config_path, params in configs:
        score = score_candidate(pref_code, config_id, stage_dir, 1)
        if not score or "city_scores" not in score:
            continue

        for city_code, city_score in score["city_scores"].items():
            if "loss" in city_score:
                city_results[city_code].append(
                    (config_id, city_score["loss"], city_score)
                )

    # Pick best config per city
    best_per_city = {}
    for city_code, results in city_results.items():
        results.sort(key=lambda x: x[1])
        best_id, best_loss, best_score = results[0]
        best_per_city[city_code] = {
            "best_config_id": best_id,
            "best_loss": best_loss,
            "best_score": best_score,
            "all_scores": [
                {"config_id": r[0], "loss": round(r[1], 2)}
                for r in results
            ],
            "city_name": pref_targets.get(city_code, {}).get("city_name", "?"),
        }

    return best_per_city


def write_param_group_config(pref_code, city_code, city_name, config_id,
                              params, loss, output_dir):
    """Write a per-city param group .properties file.

    IMPORTANT: the group name must match the convention used by the canonical
    data/tuning/city_code_to_param_group.csv, which zero-pads city codes to
    5 digits (e.g. pref 5 city 5207 → "05207", group name "pref05_05207").
    transport_share_targets.csv stores city_code as a plain integer without
    zero padding (e.g. "5207"), so we must normalize here; otherwise the
    written filename "pref05_5207.properties" will mismatch the precheck's
    expected "pref05_05207.properties" and run_pref_with_tuning.ps1 will
    fail with "After tuning, param groups still incomplete".
    """
    city_code_5 = f"{int(city_code):05d}"
    group_name = f"pref{int(pref_code):02d}_{city_code_5}"
    path = output_dir / f"{group_name}.properties"
    with open(path, "w") as f:
        f.write(f"# Parameter group: {group_name}\n")
        f.write(f"# City: {city_name} ({city_code}), Prefecture {pref_code}\n")
        f.write(f"# Tuned: Stage 1 best config_{config_id} (loss={loss:.2f})\n")
        f.write(f"# api.maxRoutes=6 at tuning time\n")
        for k in sorted(params.keys()):
            f.write(f"{k}={params[k]}\n")
    return group_name


def run_prefecture(pref_code, mfactor=200, score_only=False, seed=42):
    """Run full tuning for one prefecture, return per-city best params.

    Returns:
        dict: city_code -> {group_name, config_id, loss, params}
        or None on failure.
    """
    out_dir = get_output_dir(int(pref_code))
    stage_dir = out_dir / "stage1"

    targets = load_transport_targets(str(TARGET_CSV))
    pref_targets = get_pref_targets(targets, pref_code)
    target_codes = list(pref_targets.keys())

    city_labels = [f"{c} ({pref_targets[c]['city_name']})" for c in target_codes]
    print(f"Prefecture {pref_code}: {len(target_codes)} target cities: "
          f"{', '.join(city_labels)}")

    # Generate configs
    configs = generate_tuning_configs(int(pref_code), stage=1, seed=seed)

    if not score_only:
        activity_dir = out_dir / "activity" / str(pref_code)

        # Run ActivityGenerator if not already done
        if not activity_dir.is_dir() or not any(activity_dir.iterdir()):
            if not run_activity_generator(int(pref_code), mfactor, out_dir):
                return None
        else:
            n_files = len(list(activity_dir.glob("*.csv")))
            print(f"[activity] Using existing activity data ({n_files} files)")

        # Run TripGenerator for each config — process ALL target cities
        for config_id, config_path, params in configs:
            trip_dir = stage_dir / config_id / "trip" / str(pref_code)
            if trip_dir.is_dir() and any(trip_dir.iterdir()):
                print(f"[trip:{config_id}] Output exists, skipping")
                continue

            success = run_trip_generator(
                int(pref_code), config_id, config_path, mfactor,
                stage_dir, target_codes)
            if not success:
                print(f"[trip:{config_id}] Failed, continuing")

    # Score per city
    print(f"\n--- Scoring per target city ---")
    best_per_city = score_per_city(int(pref_code), configs, stage_dir)

    # Write param group configs
    PARAM_GROUPS_DIR.mkdir(parents=True, exist_ok=True)
    results = {}
    for city_code, info in best_per_city.items():
        config_id = info["best_config_id"]
        loss = info["best_loss"]
        city_name = info["city_name"]

        # Read the winning config's params
        config_path = out_dir / "configs" / f"config_{config_id}.properties"
        params = read_config_params(str(config_path))

        group_name = write_param_group_config(
            pref_code, city_code, city_name, config_id,
            params, loss, PARAM_GROUPS_DIR)

        results[city_code] = {
            "group_name": group_name,
            "config_id": config_id,
            "loss": loss,
            "params": params,
            "city_name": city_name,
        }

        print(f"  {city_code} ({city_name}): best=config_{config_id}, "
              f"loss={loss:.2f} → {group_name}.properties")

    return results


def write_best_params_csv(all_results, output_dir):
    """Write best_params_by_city.csv."""
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / "best_params_by_city.csv"

    param_keys = ["fare.per.hour", "fare.init", "fare.per.kilometer",
                   "fatigue.walk", "fatigue.bicycle",
                   "api.transit.transferPenalty", "min.transit.distance"]

    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["pref_code", "city_code", "city_name", "param_group",
                     "config_id", "loss"] + param_keys)
        for pref_code in sorted(all_results.keys(), key=lambda x: int(x)):
            for city_code in sorted(all_results[pref_code].keys()):
                info = all_results[pref_code][city_code]
                row = [pref_code, city_code, info["city_name"],
                       info["group_name"], info["config_id"],
                       f"{info['loss']:.2f}"]
                for k in param_keys:
                    row.append(info["params"].get(k, ""))
                w.writerow(row)
    print(f"Written: {path}")


def write_summary(all_results, output_dir):
    """Write tuning_summary.md."""
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / "tuning_summary.md"

    param_keys = ["fare.per.hour", "fare.init", "fare.per.kilometer",
                   "fatigue.walk", "fatigue.bicycle",
                   "api.transit.transferPenalty", "min.transit.distance"]
    short_keys = [k.split(".")[-1] for k in param_keys]

    with open(path, "w") as f:
        f.write("# Transport Tuning Summary\n\n")
        f.write(f"Tuning method: Stage 1 (baseline + 9 LHS), per-target-city scoring\n")
        f.write(f"api.maxRoutes=6 at tuning time\n\n")

        total_cities = sum(len(v) for v in all_results.values())
        total_prefs = len(all_results)
        f.write(f"**{total_cities} target cities tuned across {total_prefs} prefectures**\n\n")

        # Per-city results table
        f.write("## Best parameters per target city\n\n")
        header = "| Pref | City | Name | Config | Loss |"
        for sk in short_keys:
            header += f" {sk} |"
        f.write(header + "\n")
        f.write("|" + "---|" * (5 + len(short_keys)) + "\n")

        for pref_code in sorted(all_results.keys(), key=lambda x: int(x)):
            for city_code in sorted(all_results[pref_code].keys()):
                info = all_results[pref_code][city_code]
                row = f"| {pref_code} | {city_code} | {info['city_name']} | {info['config_id']} | {info['loss']:.1f} |"
                for k in param_keys:
                    row += f" {info['params'].get(k, '')} |"
                f.write(row + "\n")

        # Failed prefectures
        f.write("\n## Status\n\n")
        for pref_code in sorted(all_results.keys(), key=lambda x: int(x)):
            n = len(all_results[pref_code])
            cities = ", ".join(f"{c}" for c in sorted(all_results[pref_code].keys()))
            f.write(f"- Pref {pref_code}: {n} cities tuned ({cities})\n")

    print(f"Written: {path}")


def main():
    parser = argparse.ArgumentParser(
        description="Per-target-city transport tuning")
    parser.add_argument("--pref", type=int, default=None,
                        help="Run only this prefecture")
    parser.add_argument("--score-only", action="store_true",
                        help="Score existing output without running generators")
    parser.add_argument("--mfactor", type=int, default=200,
                        help="Sampling factor (default: 200)")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed (default: 42)")
    args = parser.parse_args()

    pref_targets = get_prefectures_with_targets(str(TARGET_CSV))
    print(f"Target cities: {sum(len(v) for v in pref_targets.values())} "
          f"across {len(pref_targets)} prefectures\n")

    if args.pref:
        prefs_to_run = {str(args.pref): pref_targets.get(str(args.pref), [])}
        if not prefs_to_run[str(args.pref)]:
            print(f"ERROR: No target cities for pref {args.pref}")
            sys.exit(1)
    else:
        prefs_to_run = pref_targets

    all_results = {}
    failed_prefs = []

    for pref_code in sorted(prefs_to_run.keys(), key=lambda x: int(x)):
        print(f"\n{'='*60}")
        print(f"PREFECTURE {pref_code}")
        print(f"{'='*60}")

        try:
            results = run_prefecture(
                pref_code,
                mfactor=args.mfactor,
                score_only=args.score_only,
                seed=args.seed,
            )
            if results:
                all_results[pref_code] = results
            else:
                failed_prefs.append(pref_code)
                print(f"FAILED: pref {pref_code}")
        except Exception as e:
            failed_prefs.append(pref_code)
            print(f"FAILED: pref {pref_code}: {e}")
            import traceback
            traceback.print_exc()

    # Write outputs
    if all_results:
        FINAL_DIR.mkdir(parents=True, exist_ok=True)
        write_best_params_csv(all_results, FINAL_DIR)
        write_summary(all_results, FINAL_DIR)

    # Final status
    print(f"\n{'='*60}")
    print("FINAL STATUS")
    print(f"{'='*60}")
    total_ok = sum(len(v) for v in all_results.values())
    print(f"Tuned: {total_ok} cities across {len(all_results)} prefectures")
    if failed_prefs:
        print(f"Failed: prefectures {', '.join(failed_prefs)}")
        print(f"\nTo rerun failed:")
        for p in failed_prefs:
            print(f"  python tune_all_cities.py --pref {p}")
    print(f"\nParam groups: {PARAM_GROUPS_DIR}/")
    print(f"Summary: {FINAL_DIR}/tuning_summary.md")


if __name__ == "__main__":
    main()
