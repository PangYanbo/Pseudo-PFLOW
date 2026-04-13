#!/usr/bin/env python3
"""Transport tuning orchestrator.

Runs the two-stage transport mode share tuning pipeline:
  Stage 1: 10 configs (baseline + 9 LHS) on 1 representative city
  Stage 2: top N configs on all target cities at 2x sample

Usage:
    # Generate configs + run Stage 1 for pref 22
    python transport_tune.py 22 --stage 1

    # Score existing Stage 1 runs (no Maven, just scoring)
    python transport_tune.py 22 --stage 1 --score-only

    # Generate configs only (dry run)
    python transport_tune.py 22 --generate-only
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path


def _resolve_mvn():
    """Resolve the Maven executable in a cross-platform-safe way.

    On Windows, the Maven launcher is `mvn.cmd` (a batch file). Python's
    subprocess.run with a list and shell=False uses CreateProcess, which does
    not search PATHEXT and therefore fails with [WinError 2] when given just
    "mvn". shutil.which() correctly finds `mvn.cmd` on Windows and `mvn` on
    Linux/macOS.
    """
    path = shutil.which("mvn")
    if path:
        return path
    # Fallback: try mvn.cmd explicitly on Windows
    if os.name == "nt":
        path = shutil.which("mvn.cmd")
        if path:
            return path
    # Last resort: return the bare name and let subprocess raise its own error
    return "mvn"


MVN = _resolve_mvn()

# Add scripts/tuning to path for sibling imports
SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from city_align import (
    get_pref_targets,
    load_transport_targets,
    pick_representative_city,
    resolve_activity_files,
)
from lhs import generate_configs, load_search_space
from transport_score import score_prefecture

# Project root (two levels up from scripts/tuning/)
PROJECT_ROOT = SCRIPT_DIR.parent.parent

# Paths relative to project root
TARGET_CSV = PROJECT_ROOT / "data" / "tuning" / "transport_share_targets.csv"
SEARCH_SPACE = PROJECT_ROOT / "config" / "tuning" / "transport_search_space.yaml"
OUTPUT_BASE = PROJECT_ROOT / "output" / "tuning"

# Java environment for Maven subprocess calls.
#
# Policy:
#   - Windows: ALWAYS inherit the user's environment verbatim. The Windows VM
#     has JAVA_HOME pre-configured (e.g. C:\Program Files\Java\jdk-21.0.10) and
#     we must not override it.
#   - Linux/macOS: prefer an inherited JAVA_HOME that points to an existing
#     directory. If none is set OR the set value is invalid (e.g. the dev
#     shell has a stale path), fall back to the known dev-machine JDK at
#     /usr/lib/jvm/temurin-21-jdk-amd64.
#
# WARNING: the previous version hardcoded JAVA_HOME to the Linux dev path
# unconditionally, which broke the Windows tuning path — mvn.cmd saw a
# non-existent Linux directory and bailed with "JAVA_HOME is not defined
# correctly" even though the interactive PowerShell session had a valid
# JAVA_HOME set.
if os.name == "nt":
    # Windows: trust the inherited environment
    MVN_ENV = dict(os.environ)
else:
    _inherited = os.environ.get("JAVA_HOME")
    if _inherited and os.path.isdir(_inherited):
        MVN_ENV = dict(os.environ)
    else:
        MVN_ENV = {**os.environ, "JAVA_HOME": "/usr/lib/jvm/temurin-21-jdk-amd64"}


def get_output_dir(pref_code):
    return OUTPUT_BASE / str(pref_code)


def generate_tuning_configs(pref_code, stage=1, seed=42):
    """Generate config files for a tuning run.

    Returns list of (config_id, config_path, params_dict).
    """
    out_dir = get_output_dir(pref_code)
    configs_dir = out_dir / "configs"
    stage_dir = out_dir / f"stage{stage}"

    n_lhs = 9 if stage == 1 else 3

    configs = generate_configs(
        str(SEARCH_SPACE),
        str(configs_dir),
        n=n_lhs,
        seed=seed,
        output_base=str(stage_dir),
    )

    return configs


def run_activity_generator(pref_code, mfactor, output_dir):
    """Run ActivityGenerator once (shared across all configs).

    Activity output goes to output_dir/activity/<pref>/.
    mfactor controls sampling: higher = fewer persons.
    At mfactor=200, pref-22 representative city (22100, 3 wards) yields ~3.5K persons.
    """
    activity_config = output_dir / "activity_config.properties"
    activity_config.parent.mkdir(parents=True, exist_ok=True)
    # Java Properties treats backslash as an escape character (\t, \n, etc.),
    # so raw Windows paths like C:\Pseudo-PFLOW\... get mangled on load.
    # Always serialize paths with forward slashes — Java accepts them on
    # every platform including Windows.
    output_dir_fs = str(output_dir).replace("\\", "/")
    with open(activity_config, "w") as f:
        f.write(f"# Activity generation config\n")
        f.write(f"outputDir={output_dir_fs}/\n")

    cmd = [
        MVN, "-q", "exec:java",
        f"-Dexec.mainClass=pseudo.gen.ActivityGenerator",
        f"-Dexec.args={pref_code} {mfactor}",
        f"-Dconfig.file={activity_config}",
    ]

    print(f"[activity] Running ActivityGenerator pref={pref_code} mfactor={mfactor}")
    print(f"[activity] Command: {' '.join(cmd)}")

    log_dir = output_dir / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    log_file = log_dir / "activity.log"

    start = time.time()
    with open(log_file, "w") as log:
        result = subprocess.run(
            cmd, cwd=str(PROJECT_ROOT), stdout=log, stderr=subprocess.STDOUT,
            env=MVN_ENV, timeout=14400,
        )
    elapsed = time.time() - start

    if result.returncode != 0:
        print(f"[activity] FAILED (exit {result.returncode}, {elapsed:.0f}s). See {log_file}")
        return False

    print(f"[activity] Done ({elapsed:.0f}s). Log: {log_file}")
    return True


def setup_sampled_activity(candidate_dir, pref_code, shared_activity_dir,
                            target_codes, cap_per_city=2000, seed=42):
    """Create a filtered activity dir with per-city person-level sampling.

    Two-stage sampling policy:
      Stage 1 (already done): ActivityGenerator at mfactor=200 (0.5% global)
      Stage 2 (this function): per target city, if persons > cap_per_city,
               sample down to cap_per_city. Cities at or below cap are kept in full.

    For aggregated target cities (multiple ward files), the cap applies to the
    city as a whole. Budget is distributed proportionally across ward files.
    Sampling is person-level (all activity lines for a person are kept together).
    Fixed seed for reproducibility.

    Returns (total_persons, details_str).
    """
    import random
    from collections import OrderedDict

    filtered_dir = candidate_dir / "activity" / str(pref_code)
    if filtered_dir.is_dir() and any(filtered_dir.iterdir()):
        existing = len(list(filtered_dir.glob("*.csv")))
        return existing, "(reused existing)"
    filtered_dir.mkdir(parents=True, exist_ok=True)

    rng = random.Random(seed)
    total_persons = 0
    details = []

    for code in target_codes:
        files = resolve_activity_files(code, str(shared_activity_dir))
        if not files:
            continue

        # Group lines by person_id (column 0) per file
        file_persons = OrderedDict()  # filepath -> OrderedDict{pid -> [lines]}
        city_person_count = 0
        for f in files:
            persons = OrderedDict()
            with open(f) as fh:
                for line in fh:
                    pid = line.split(",", 1)[0].strip()
                    if pid not in persons:
                        persons[pid] = []
                    persons[pid].append(line)
            file_persons[f] = persons
            city_person_count += len(persons)

        # Decide: keep all or cap
        needs_cap = city_person_count > cap_per_city
        city_sampled = 0

        if not needs_cap:
            # City is at or below cap — copy all files in full
            for f, persons in file_persons.items():
                out_path = filtered_dir / Path(f).name
                with open(out_path, "w") as out:
                    for pid_lines in persons.values():
                        out.writelines(pid_lines)
                city_sampled += len(persons)
            tag = "kept all"
        else:
            # City exceeds cap — proportional person-level sampling across wards
            for f, persons in file_persons.items():
                n_persons = len(persons)
                file_budget = max(1, round(cap_per_city * n_persons / city_person_count))
                file_budget = min(file_budget, n_persons)

                pids = list(persons.keys())
                sampled_pids = rng.sample(pids, file_budget)

                out_path = filtered_dir / Path(f).name
                with open(out_path, "w") as out:
                    for pid in sampled_pids:
                        out.writelines(persons[pid])
                city_sampled += len(sampled_pids)
            tag = f"capped {city_person_count}->{city_sampled}"

        total_persons += city_sampled
        details.append(f"{code}: {city_sampled} ({tag})")

    return total_persons, ", ".join(details)


def compute_tuning_load_scale(shared_activity_dir, pref_code, target_codes,
                               budget_per_city=1500):
    """Compute tuning.loadScale to cap sample at ~budget_per_city per target city.

    Counts total persons across all linked activity files for the target cities,
    then computes a loadScale so the aggregate stays within budget.

    Returns (loadScale, total_persons, effective_persons).
    """
    total = 0
    for code in target_codes:
        files = resolve_activity_files(code, str(shared_activity_dir))
        for f in files:
            total += sum(1 for _ in open(f))

    budget = budget_per_city * len(target_codes)
    load_scale = max(1, total // budget)
    effective = total // load_scale if load_scale > 0 else total
    return load_scale, total, effective


def inject_load_scale(config_path, load_scale):
    """Append tuning.loadScale to a config file if not already present."""
    content = Path(config_path).read_text()
    if "tuning.loadScale" in content:
        return  # already set
    with open(config_path, "a") as f:
        f.write(f"\n# Tuning sample control (computed per-run)\n")
        f.write(f"tuning.loadScale={load_scale}\n")


def run_trip_generator(pref_code, config_id, config_path, mfactor, stage_dir, target_codes):
    """Run TripGenerator_WebAPI_refactor with a specific config overlay.

    Creates pre-sampled activity copies (capped at ~2000 per target city)
    in the candidate dir, so TripGenerator processes only the sampled data.
    """
    candidate_dir = stage_dir / config_id
    candidate_dir.mkdir(parents=True, exist_ok=True)

    # Create per-city sampled activity copies (not symlinks)
    shared_activity = stage_dir.parent / "activity" / str(pref_code)
    n_written, details = setup_sampled_activity(
        candidate_dir, pref_code, shared_activity, target_codes)
    print(f"[trip:{config_id}] Sampled activity: {n_written} persons ({details})")

    cmd = [
        MVN, "-q", "exec:java",
        f"-Dexec.mainClass=pseudo.gen.TripGenerator_WebAPI_refactor",
        f"-Dexec.args={pref_code} {mfactor}",
        f"-Dconfig.file={config_path}",
    ]

    print(f"[trip:{config_id}] Running TripGenerator pref={pref_code}")

    log_dir = stage_dir.parent / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    log_file = log_dir / f"trip_{config_id}.log"

    start = time.time()
    with open(log_file, "w") as log:
        result = subprocess.run(
            cmd, cwd=str(PROJECT_ROOT), stdout=log, stderr=subprocess.STDOUT,
            env=MVN_ENV, timeout=14400,
        )
    elapsed = time.time() - start

    if result.returncode != 0:
        print(f"[trip:{config_id}] FAILED (exit {result.returncode}, {elapsed:.0f}s). See {log_file}")
        return False

    print(f"[trip:{config_id}] Done ({elapsed:.0f}s). Log: {log_file}")
    return True


def score_candidate(pref_code, config_id, stage_dir, stage_num):
    """Score a candidate's trip output against targets.

    Returns score dict or None on failure.
    """
    trip_dir = stage_dir / config_id / "trip" / str(pref_code)
    if not trip_dir.is_dir():
        print(f"[score:{config_id}] No trip directory at {trip_dir}")
        return None

    result = score_prefecture(str(TARGET_CSV), str(trip_dir), str(pref_code))

    # Save score JSON
    scores_dir = stage_dir.parent / "scores"
    scores_dir.mkdir(parents=True, exist_ok=True)
    score_file = scores_dir / f"score_{config_id}_s{stage_num}.json"
    with open(score_file, "w") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    loss = result.get("weighted_avg_loss", float("inf"))
    matched = result.get("matched_cities", 0)
    print(f"[score:{config_id}] Loss={loss:.2f} ({matched} cities matched)")
    return result


def run_stage(pref_code, stage_num, mfactor=200, score_only=False, seed=42):
    """Run a complete tuning stage.

    Args:
        pref_code: Prefecture code.
        stage_num: 1 or 2.
        mfactor: Sampling factor for ActivityGenerator (higher = fewer persons).
        score_only: If True, skip Maven runs and just score existing output.
        seed: Random seed for LHS.

    Returns:
        List of (config_id, loss, score_dict) sorted by loss.
    """
    out_dir = get_output_dir(pref_code)
    stage_dir = out_dir / f"stage{stage_num}"

    # Load targets
    targets = load_transport_targets(str(TARGET_CSV))
    pref_targets = get_pref_targets(targets, pref_code)
    if not pref_targets:
        print(f"ERROR: No targets for pref {pref_code}")
        return []

    rep_city = pick_representative_city(pref_targets)

    # Generate trips for ALL target cities so per-city scoring works
    gen_cities = list(pref_targets.keys())

    print(f"Prefecture {pref_code}: {len(pref_targets)} target cities, "
          f"representative={rep_city} ({pref_targets[rep_city]['city_name']})")
    print(f"Stage {stage_num}: generating trips for {len(gen_cities)} cities, "
          f"scoring all {len(pref_targets)} target cities")

    # Generate configs
    configs = generate_tuning_configs(pref_code, stage=stage_num, seed=seed)
    print(f"Generated {len(configs)} configs in {out_dir / 'configs'}/")

    if not score_only:
        activity_dir = out_dir / "activity" / str(pref_code)

        # Run ActivityGenerator if not already done
        if not activity_dir.is_dir() or not any(activity_dir.iterdir()):
            print(f"[activity] Generating activity data (mfactor={mfactor})")
            if not run_activity_generator(pref_code, mfactor, out_dir):
                return []
        else:
            n_files = len(list(activity_dir.glob("*.csv")))
            print(f"[activity] Using existing activity data ({n_files} files)")

        # Run TripGenerator for each config
        for config_id, config_path, params in configs:
            # Check if trip output already exists (resume support)
            trip_dir = stage_dir / config_id / "trip" / str(pref_code)
            if trip_dir.is_dir() and any(trip_dir.iterdir()):
                print(f"[trip:{config_id}] Output exists, skipping (delete to rerun)")
                continue

            success = run_trip_generator(
                pref_code, config_id, config_path, mfactor, stage_dir, gen_cities)
            if not success:
                print(f"[trip:{config_id}] Failed, continuing with next config")

    # Score all candidates
    print(f"\n--- Scoring Stage {stage_num} ---")
    results = []
    for config_id, config_path, params in configs:
        score = score_candidate(pref_code, config_id, stage_dir, stage_num)
        if score and "weighted_avg_loss" in score:
            results.append((config_id, score["weighted_avg_loss"], score))
        else:
            results.append((config_id, float("inf"), score))

    # Sort by loss
    results.sort(key=lambda x: x[1])

    # Print ranking
    print(f"\n=== Stage {stage_num} Ranking ===")
    print(f"{'Rank':>4} {'Config':>8} {'Loss':>10}")
    print("-" * 26)
    for rank, (cfg_id, loss, _) in enumerate(results, 1):
        marker = " *" if loss == float("inf") else ""
        print(f"{rank:>4} {cfg_id:>8} {loss:>10.2f}{marker}")

    # Save ranking summary
    ranking_file = out_dir / "scores" / f"ranking_s{stage_num}.json"
    ranking_file.parent.mkdir(parents=True, exist_ok=True)
    ranking = [
        {"rank": i + 1, "config_id": r[0], "loss": r[1]}
        for i, r in enumerate(results)
    ]
    with open(ranking_file, "w") as f:
        json.dump(ranking, f, indent=2)
    print(f"\nRanking saved to {ranking_file}")

    return results


def main():
    parser = argparse.ArgumentParser(description="Transport tuning orchestrator")
    parser.add_argument("pref_code", type=int, help="Prefecture code (e.g., 22)")
    parser.add_argument("--stage", type=int, default=1, choices=[1, 2],
                        help="Stage to run (default: 1)")
    parser.add_argument("--mfactor", type=int, default=200,
                        help="Sampling factor for ActivityGenerator (default: 200 = 0.5%%)")
    parser.add_argument("--score-only", action="store_true",
                        help="Score existing output without running generators")
    parser.add_argument("--generate-only", action="store_true",
                        help="Generate configs only (dry run)")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed for LHS (default: 42)")

    args = parser.parse_args()

    if args.generate_only:
        configs = generate_tuning_configs(args.pref_code, stage=args.stage, seed=args.seed)
        print(f"Generated {len(configs)} configs:")
        for cfg_id, cfg_path, params in configs:
            print(f"  {cfg_id}: {cfg_path}")
            for k, v in sorted(params.items()):
                print(f"    {k} = {v}")
        return

    results = run_stage(
        args.pref_code,
        stage_num=args.stage,
        mfactor=args.mfactor,
        score_only=args.score_only,
        seed=args.seed,
    )

    if not results:
        print("No results produced.")
        sys.exit(1)

    best_id, best_loss, _ = results[0]
    print(f"\nBest: config_{best_id} (loss={best_loss:.2f})")


if __name__ == "__main__":
    main()
