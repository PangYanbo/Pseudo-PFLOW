#!/usr/bin/env python3
"""Aggregate per-file JSON validation reports into a prefecture-level Markdown summary."""

import json
import os
import sys
from pathlib import Path


def load_json(path):
    if not os.path.exists(path):
        return None
    with open(path) as f:
        return json.load(f)


def status_icon(status):
    if status == "PASS":
        return "PASS"
    elif status == "WARN":
        return "WARN"
    else:
        return "FAIL"


def render_section(title, report):
    lines = []
    lines.append(f"## {title}")
    lines.append("")

    if report is None:
        lines.append("No validation data available.")
        lines.append("")
        return lines

    total = report["file_count"]
    p, w, f = report["pass"], report["warn"], report["fail"]
    overall = "PASS" if f == 0 and w == 0 else ("WARN" if f == 0 else "FAIL")

    lines.append(f"**Overall: {status_icon(overall)}** | Files: {total} | PASS: {p} | WARN: {w} | FAIL: {f}")
    lines.append(f"Total rows: {report.get('total_rows', 'N/A')} | Total persons: {report.get('total_persons', 'N/A')}")
    lines.append("")

    # Table of files with issues
    problem_files = [r for r in report.get("files", []) if r["status"] != "PASS"]
    if problem_files:
        lines.append("### Files with issues")
        lines.append("")
        lines.append("| File | Status | Issues |")
        lines.append("|------|--------|--------|")
        for r in problem_files[:20]:  # cap at 20
            fname = os.path.basename(r["file"])
            issues = "; ".join(r.get("errors", []) + r.get("warnings", []))
            if len(issues) > 120:
                issues = issues[:117] + "..."
            lines.append(f"| {fname} | {status_icon(r['status'])} | {issues} |")
        if len(problem_files) > 20:
            lines.append(f"| ... | | ({len(problem_files) - 20} more files with issues) |")
        lines.append("")

    # Aggregate stats
    all_files = report.get("files", [])
    if all_files and all_files[0].get("stats"):
        # Show distribution from first file as sample, or aggregate
        if report["type"] == "activity":
            # Aggregate purpose distribution
            agg_purpose = {}
            for r in all_files:
                for k, v in r["stats"].get("purpose_distribution", {}).items():
                    agg_purpose[k] = agg_purpose.get(k, 0) + v
            if agg_purpose:
                lines.append("### Purpose distribution (aggregate)")
                lines.append("")
                purpose_names = {"1": "HOME", "2": "OFFICE", "3": "SCHOOL", "4": "RETURN_OFFICE",
                                 "100": "SHOPPING", "200": "EATING", "300": "HOSPITAL", "400": "FREE", "500": "BUSINESS"}
                total_acts = sum(agg_purpose.values())
                for k in sorted(agg_purpose.keys(), key=lambda x: int(x)):
                    name = purpose_names.get(k, k)
                    pct = agg_purpose[k] / total_acts * 100
                    lines.append(f"- {name} ({k}): {agg_purpose[k]:,} ({pct:.1f}%)")
                lines.append("")

        elif report["type"] == "trip":
            # ── PRIMARY: trip-level mode share via rep_mode ───────────────
            # This is the metric comparable to tuning targets in transport_share_targets.csv.
            # Deduplicated on (person_id, trip_id); uses each trip's representative mode.
            agg_trip_mode = {}
            total_tuning_trips = 0
            for r in all_files:
                trip_share = r["stats"].get("trip_level_mode_share", {})
                for name, info in trip_share.items():
                    agg_trip_mode[name] = agg_trip_mode.get(name, 0) + info.get("count", 0)
                total_tuning_trips += r["stats"].get("num_trips", 0)

            if agg_trip_mode:
                lines.append("### [PRIMARY] Trip-level mode share (rep_mode)")
                lines.append("")
                lines.append("_Comparable to transport_share_targets.csv. Use this metric to judge tuning/realism._")
                lines.append("")
                total_trip_count = sum(agg_trip_mode.values())
                for k in sorted(agg_trip_mode.keys()):
                    pct = agg_trip_mode[k] / total_trip_count * 100 if total_trip_count else 0
                    lines.append(f"- {k}: {agg_trip_mode[k]:,} ({pct:.1f}%)")
                lines.append("")
                lines.append(f"_Total trips (deduplicated): {total_trip_count:,}_")
                lines.append("")
            else:
                lines.append("### [PRIMARY] Trip-level mode share (rep_mode)")
                lines.append("")
                lines.append("_Not available: trip files use the legacy 9-column format without trip_id/rep_mode._")
                lines.append("")

            # Aggregate NOT_DEFINED breakdown
            agg_nd_total = sum(r["stats"].get("not_defined", {}).get("total", 0) for r in all_files)
            agg_nd_placeholder = sum(r["stats"].get("not_defined", {}).get("placeholder_zero_dist", 0) for r in all_files)
            agg_nd_unexpected = sum(r["stats"].get("not_defined", {}).get("unexpected_nonzero_dist", 0) for r in all_files)
            total_rows = report.get("total_rows", 0) or 1
            if agg_nd_total > 0:
                lines.append("### NOT_DEFINED breakdown (aggregate, subtrip-level)")
                lines.append("")
                lines.append(f"- Total NOT_DEFINED: {agg_nd_total:,} ({agg_nd_total/total_rows*100:.1f}%)")
                lines.append(f"  - Placeholder (zero-distance): {agg_nd_placeholder:,} ({agg_nd_placeholder/total_rows*100:.1f}%)")
                lines.append(f"  - Unexpected (nonzero-distance): {agg_nd_unexpected:,} ({agg_nd_unexpected/total_rows*100:.1f}%)")
                lines.append("")

            # ── SECONDARY: subtrip-level transport_id distribution ────────
            # Useful for counting individual bus/train segments, transfer structure, etc.
            # NOT comparable to tuning targets because multi-leg trips are counted multiple times.
            agg_mode = {}
            for r in all_files:
                for name, info in r["stats"].get("mode_share", {}).items():
                    agg_mode[name] = agg_mode.get(name, 0) + info.get("count", 0)
            if agg_mode:
                lines.append("### [SECONDARY] Subtrip-level mode distribution (transport_id)")
                lines.append("")
                lines.append("_For segment-level analysis (transit usage, transfers). NOT directly comparable to tuning targets._")
                lines.append("")
                total_sub = sum(agg_mode.values())
                for k in sorted(agg_mode.keys()):
                    pct = agg_mode[k] / total_sub * 100
                    lines.append(f"- {k}: {agg_mode[k]:,} ({pct:.1f}%)")
                lines.append("")

            # Aggregate real-movement mode share (still subtrip-level; kept for backward compat)
            agg_real = {}
            for r in all_files:
                for name, info in r["stats"].get("real_movement_mode_share", {}).items():
                    agg_real[name] = agg_real.get(name, 0) + info.get("count", 0)
            if agg_real:
                lines.append("### [SECONDARY] Subtrip real-movement mode share (excluding placeholder NOT_DEFINED)")
                lines.append("")
                total_real = sum(agg_real.values())
                for k in sorted(agg_real.keys()):
                    pct = agg_real[k] / total_real * 100 if total_real else 0
                    lines.append(f"- {k}: {agg_real[k]:,} ({pct:.1f}%)")
                lines.append("")

        elif report["type"] == "trajectory":
            agg_transport = {}
            for r in all_files:
                for k, v in r["stats"].get("transport_distribution", {}).items():
                    agg_transport[k] = agg_transport.get(k, 0) + v
            if agg_transport:
                lines.append("### Transport mode distribution (aggregate)")
                lines.append("")
                total_pts = sum(agg_transport.values())
                for k in sorted(agg_transport.keys()):
                    pct = agg_transport[k] / total_pts * 100
                    lines.append(f"- {k}: {agg_transport[k]:,} ({pct:.1f}%)")
                lines.append("")

    return lines


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <report_dir> <pref_code> [output_md]", file=sys.stderr)
        sys.exit(1)

    report_dir = Path(sys.argv[1])
    pref_code = sys.argv[2]
    output_md = sys.argv[3] if len(sys.argv) > 3 else None

    activity_report = load_json(report_dir / "activity.json")
    trip_report = load_json(report_dir / "trip.json")
    trajectory_report = load_json(report_dir / "trajectory.json")

    lines = []
    lines.append(f"# Validation Report: Prefecture {pref_code}")
    lines.append("")

    # Overall summary
    all_reports = [r for r in [activity_report, trip_report, trajectory_report] if r is not None]
    total_fail = sum(r.get("fail", 0) for r in all_reports)
    total_warn = sum(r.get("warn", 0) for r in all_reports)
    total_pass = sum(r.get("pass", 0) for r in all_reports)
    overall = "PASS" if total_fail == 0 and total_warn == 0 else ("WARN" if total_fail == 0 else "FAIL")

    lines.append(f"**Overall: {status_icon(overall)}** | PASS: {total_pass} | WARN: {total_warn} | FAIL: {total_fail}")
    lines.append("")

    # Sections
    lines.extend(render_section("Activity Validation", activity_report))
    lines.extend(render_section("Trip Validation", trip_report))
    lines.extend(render_section("Trajectory Validation", trajectory_report))

    md_text = "\n".join(lines)

    if output_md:
        os.makedirs(os.path.dirname(output_md) or ".", exist_ok=True)
        with open(output_md, "w") as f:
            f.write(md_text)
        print(f"Summary written to {output_md}")
    else:
        print(md_text)


if __name__ == "__main__":
    main()
