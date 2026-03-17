#!/usr/bin/env python3
"""Generate transport tuning summary report.

Reads score JSONs from output/tuning/<pref>/scores/ and produces
a markdown report at output/tuning/<pref>/report.md.

Usage:
    python transport_report.py <pref_code>
"""

import argparse
import json
import os
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

PROJECT_ROOT = SCRIPT_DIR.parent.parent
OUTPUT_BASE = PROJECT_ROOT / "output" / "tuning"

MODES = ["CAR", "TRAIN", "BUS", "BICYCLE", "WALK"]


def load_config_params(config_path):
    """Read a .properties file into a dict."""
    params = {}
    with open(config_path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                key, _, val = line.partition("=")
                key = key.strip()
                if key != "outputDir":
                    params[key] = val.strip()
    return params


def generate_report(pref_code, stage_num=1):
    """Generate markdown report for a prefecture's tuning results."""
    out_dir = OUTPUT_BASE / str(pref_code)
    scores_dir = out_dir / "scores"
    configs_dir = out_dir / "configs"

    # Load ranking
    ranking_file = scores_dir / f"ranking_s{stage_num}.json"
    if not ranking_file.exists():
        print(f"No ranking file at {ranking_file}")
        return None

    with open(ranking_file) as f:
        ranking = json.load(f)

    # Load individual scores and config params
    entries = []
    for r in ranking:
        cfg_id = r["config_id"]
        score_file = scores_dir / f"score_{cfg_id}_s{stage_num}.json"
        config_file = configs_dir / f"config_{cfg_id}.properties"

        score = {}
        if score_file.exists():
            with open(score_file) as f:
                score = json.load(f)

        params = {}
        if config_file.exists():
            params = load_config_params(config_file)

        entries.append({
            "rank": r["rank"],
            "config_id": cfg_id,
            "loss": r["loss"],
            "score": score,
            "params": params,
        })

    # Build report
    lines = []
    lines.append(f"# Transport Tuning Report: Prefecture {pref_code}\n")
    lines.append(f"Stage: {stage_num}\n")

    # Best parameter set
    best = entries[0]
    lines.append("## Best parameter set\n")
    lines.append(f"Config: `config_{best['config_id']}.properties` (loss: {best['loss']:.2f})\n")
    lines.append("| Parameter | Value |")
    lines.append("|-----------|-------|")
    for k, v in sorted(best["params"].items()):
        lines.append(f"| {k} | {v} |")
    lines.append("")

    # Mode share comparison across cities
    if "city_scores" in best["score"]:
        lines.append("## Mode share comparison\n")
        lines.append("Placeholder NOT_DEFINED excluded from denominator.\n")

        city_scores = best["score"]["city_scores"]
        # Build header
        header = "| Mode |"
        sep = "|------|"
        for code in sorted(city_scores.keys()):
            cs = city_scores[code]
            name = cs.get("city_name", code)
            header += f" Target ({name}) | Gen ({name}) | Error |"
            sep += "---|---|---|"
        lines.append(header)
        lines.append(sep)

        for mode in MODES:
            row = f"| {mode} |"
            for code in sorted(city_scores.keys()):
                cs = city_scores[code]
                if "mode_errors" in cs:
                    err = cs["mode_errors"][mode]
                    row += f" {err['target']:.1f}% | {err['generated']:.1f}% | {err['error_pct']:+.1f}% |"
                else:
                    row += " - | - | - |"
            lines.append(row)
        lines.append("")

        # Diagnostics
        lines.append("## Diagnostics\n")
        lines.append("| City | Placeholder ND rate | Unexpected ND rate | Real trips | Loss |")
        lines.append("|------|--------------------|--------------------|------------|------|")
        for code in sorted(city_scores.keys()):
            cs = city_scores[code]
            if "error" in cs:
                lines.append(f"| {code} ({cs.get('city_name', '')}) | - | - | - | ERROR: {cs['error']} |")
            else:
                lines.append(
                    f"| {code} ({cs.get('city_name', '')}) "
                    f"| {cs.get('placeholder_nd_rate', 0):.1f}% "
                    f"| {cs.get('unexpected_nd_rate', 0):.1f}% "
                    f"| {cs.get('real_trips', 0)} "
                    f"| {cs.get('loss', 0):.2f} |"
                )
        lines.append("")

    # Full ranking table
    lines.append("## All candidates\n")
    param_keys = sorted(best["params"].keys()) if best["params"] else []
    header = "| Rank | Config | Loss |"
    sep = "|------|--------|------|"
    for k in param_keys:
        short = k.split(".")[-1]
        header += f" {short} |"
        sep += "---|"
    lines.append(header)
    lines.append(sep)

    for e in entries:
        row = f"| {e['rank']} | {e['config_id']} | {e['loss']:.2f} |"
        for k in param_keys:
            row += f" {e['params'].get(k, '-')} |"
        lines.append(row)
    lines.append("")

    # Write report
    report_path = out_dir / "report.md"
    report_content = "\n".join(lines)
    with open(report_path, "w") as f:
        f.write(report_content)

    print(f"Report written to {report_path}")
    return report_path


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate transport tuning report")
    parser.add_argument("pref_code", type=int, help="Prefecture code")
    parser.add_argument("--stage", type=int, default=1, help="Stage number (default: 1)")
    args = parser.parse_args()

    generate_report(args.pref_code, stage_num=args.stage)
