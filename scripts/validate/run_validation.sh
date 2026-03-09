#!/usr/bin/env bash
# Validate pipeline outputs for a given prefecture.
#
# Usage:
#   ./run_validation.sh <pref_code> <output_root> [report_dir]
#
# Example:
#   ./run_validation.sh 22 /tmp/pflow_smoke
#   ./run_validation.sh 22 /mnt/large/data/PseudoPFLOW /tmp/reports/22
#
# The script expects output_root to contain:
#   activity/<pref>/  — activity CSV files
#   trip/<pref>/      — trip CSV files
#   trajectory/<pref>/ — trajectory CSV files
#
# Reports are written to report_dir (default: <output_root>/validation/<pref>/)

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <pref_code> <output_root> [report_dir]" >&2
    exit 1
fi

PREF="$1"
ROOT="$2"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="${3:-${ROOT}/validation/${PREF}}"

mkdir -p "$REPORT_DIR"

echo "=== Pseudo-PFLOW Validation: Prefecture $PREF ==="
echo "Output root: $ROOT"
echo "Report dir:  $REPORT_DIR"
echo ""

ACTIVITY_DIR="$ROOT/activity/$PREF"
TRIP_DIR="$ROOT/trip/$PREF"
TRAJ_DIR="$ROOT/trajectory/$PREF"

# Activity validation
if [ -d "$ACTIVITY_DIR" ]; then
    echo "--- Activity validation ---"
    python3 "$SCRIPT_DIR/validate_activity.py" "$ACTIVITY_DIR" "$REPORT_DIR/activity.json"
    echo ""
else
    echo "--- Activity: SKIP (directory not found: $ACTIVITY_DIR) ---"
    echo ""
fi

# Trip validation (with activity cross-reference if available)
if [ -d "$TRIP_DIR" ]; then
    echo "--- Trip validation ---"
    if [ -d "$ACTIVITY_DIR" ]; then
        python3 "$SCRIPT_DIR/validate_trip.py" "$TRIP_DIR" "$REPORT_DIR/trip.json" "$ACTIVITY_DIR"
    else
        python3 "$SCRIPT_DIR/validate_trip.py" "$TRIP_DIR" "$REPORT_DIR/trip.json"
    fi
    echo ""
else
    echo "--- Trip: SKIP (directory not found: $TRIP_DIR) ---"
    echo ""
fi

# Trajectory validation
if [ -d "$TRAJ_DIR" ]; then
    echo "--- Trajectory validation ---"
    python3 "$SCRIPT_DIR/validate_trajectory.py" "$TRAJ_DIR" "$REPORT_DIR/trajectory.json"
    echo ""
else
    echo "--- Trajectory: SKIP (directory not found: $TRAJ_DIR) ---"
    echo ""
fi

# Summary
echo "--- Generating summary ---"
python3 "$SCRIPT_DIR/validate_summary.py" "$REPORT_DIR" "$PREF" "$REPORT_DIR/summary.md"
echo ""
echo "=== Done ==="
