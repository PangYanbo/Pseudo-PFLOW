#!/usr/bin/env bash
# Run mainline pipeline for a single prefecture at a given sampling rate.
#
# Usage:
#   ./run_pref.sh <pref_code> [mfactor]
#
# Examples:
#   ./run_pref.sh 22         # mfactor=200 (0.5% default)
#   ./run_pref.sh 13 100     # mfactor=100 (1%)
#
# Requires: PFLOW_API_USER and PFLOW_API_PASS environment variables.
# Output written to /tmp/pflow_staging/pref_<N>/

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <pref_code> [mfactor]" >&2
    exit 1
fi

PREF="$1"
MFACTOR="${2:-200}"
ROOT="/tmp/pflow_staging/pref_${PREF}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
VALIDATE_DIR="$PROJECT_DIR/scripts/validate"

# Ensure API credentials are set
if [ -z "${PFLOW_API_USER:-}" ] || [ -z "${PFLOW_API_PASS:-}" ]; then
    echo "ERROR: PFLOW_API_USER and PFLOW_API_PASS must be set" >&2
    exit 1
fi

mkdir -p "$ROOT/logs"

# Staging config override (outputDir only; paths come from base config)
STAGING_CONFIG="/tmp/staging_config_${PREF}.properties"
cat > "$STAGING_CONFIG" <<EOF
outputDir=${ROOT}/
EOF

echo "=== Staging run: prefecture ${PREF}, mfactor=${MFACTOR} ($(awk "BEGIN{printf \"%.1f%%\", 100/$MFACTOR}") sample) ==="
echo "Output: $ROOT"
echo "Config override: $STAGING_CONFIG"
echo ""

MAVEN_OPTS="-Dconfig.file=$STAGING_CONFIG"

# Step 1: Activity generation
echo "[${PREF}] Step 1/3: Activity generation..."
START_T=$(date +%s)
(cd "$PROJECT_DIR" && JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 \
    mvn -q exec:java \
    -Dexec.mainClass="pseudo.gen.ActivityGenerator" \
    -Dexec.args="${PREF} ${MFACTOR}" \
    $MAVEN_OPTS) 2>&1 | tee "$ROOT/logs/activity.log"
END_T=$(date +%s)
ACT_FILES=$(find "$ROOT/activity/${PREF}/" -name "*.csv" 2>/dev/null | wc -l)
echo "[${PREF}] Activity done: ${ACT_FILES} files, $((END_T - START_T))s"
echo ""

# Step 2: Trip + trajectory generation (WebAPI mainline)
echo "[${PREF}] Step 2/3: Trip + trajectory generation (WebAPI)..."
START_T=$(date +%s)
(cd "$PROJECT_DIR" && JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 \
    mvn -q exec:java \
    -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" \
    -Dexec.args="${PREF} ${MFACTOR}" \
    $MAVEN_OPTS) 2>&1 | tee "$ROOT/logs/trip.log"
END_T=$(date +%s)
TRIP_FILES=$(find "$ROOT/trip/${PREF}/" -name "*.csv" 2>/dev/null | wc -l)
TRAJ_FILES=$(find "$ROOT/trajectory/${PREF}/" -name "*.csv" 2>/dev/null | wc -l)
echo "[${PREF}] Trip+trajectory done: ${TRIP_FILES} trip files, ${TRAJ_FILES} trajectory files, $((END_T - START_T))s"
echo ""

# Step 3: Validation
echo "[${PREF}] Step 3/3: Validation..."
if [ -x "$VALIDATE_DIR/run_validation.sh" ]; then
    "$VALIDATE_DIR/run_validation.sh" "$PREF" "$ROOT" "$ROOT/validation/${PREF}"
else
    bash "$VALIDATE_DIR/run_validation.sh" "$PREF" "$ROOT" "$ROOT/validation/${PREF}"
fi
echo ""

# Write manifest
MANIFEST="$ROOT/manifest.json"
python3 -c "
import json, os, datetime
m = {
    'prefecture': ${PREF},
    'mfactor': ${MFACTOR},
    'scale': '$(awk "BEGIN{printf \"%.1f%%\", 100/$MFACTOR}")',
    'timestamp': datetime.datetime.now().isoformat(timespec='seconds'),
    'output_root': '$ROOT',
    'files': {
        'activity': ${ACT_FILES},
        'trip': ${TRIP_FILES},
        'trajectory': ${TRAJ_FILES},
    },
    'logs': ['logs/activity.log', 'logs/trip.log'],
    'validation_summary': 'validation/${PREF}/summary.md',
}
with open('$MANIFEST', 'w') as f:
    json.dump(m, f, indent=2)
print(f'Manifest written to $MANIFEST')
"

echo "=== Prefecture ${PREF} complete ==="
echo "Summary: $ROOT/validation/${PREF}/summary.md"
