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
            agg_transport = {}
            for r in all_files:
                for k, v in r["stats"].get("transport_distribution", {}).items():
                    agg_transport[k] = agg_transport.get(k, 0) + v
            if agg_transport:
                lines.append("### Transport mode distribution (aggregate)")
                lines.append("")
                total_trips = sum(agg_transport.values())
                for k in sorted(agg_transport.keys()):
                    pct = agg_transport[k] / total_trips * 100
                    lines.append(f"- {k}: {agg_transport[k]:,} ({pct:.1f}%)")
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
