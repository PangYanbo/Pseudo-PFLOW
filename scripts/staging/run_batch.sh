#!/usr/bin/env bash
# Run staging pipeline for multiple prefectures sequentially.
#
# Usage:
#   ./run_batch.sh [pref_codes...] [-- mfactor]
#
# Examples:
#   ./run_batch.sh                  # default: 22 13 26, mfactor=200
#   ./run_batch.sh 22 13            # two prefs, mfactor=200
#   ./run_batch.sh 22 13 26 -- 100  # three prefs, mfactor=100

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MFACTOR=200
PREFS=()

# Parse args: prefectures before --, mfactor after --
while [ $# -gt 0 ]; do
    if [ "$1" = "--" ]; then
        shift
        MFACTOR="${1:-200}"
        break
    fi
    PREFS+=("$1")
    shift
done

# Default prefectures if none given
if [ ${#PREFS[@]} -eq 0 ]; then
    PREFS=(22 13 26)
fi

echo "=== Staging batch: prefectures ${PREFS[*]}, mfactor=${MFACTOR} ==="
echo ""

PASSED=()
FAILED=()

for p in "${PREFS[@]}"; do
    echo "━━━ Prefecture $p ━━━"
    if "$SCRIPT_DIR/run_pref.sh" "$p" "$MFACTOR"; then
        PASSED+=("$p")
    else
        FAILED+=("$p")
        echo "*** Prefecture $p FAILED — continuing with next ***"
    fi
    echo ""
done

echo "=== Batch summary ==="
echo "Passed: ${PASSED[*]:-none}"
echo "Failed: ${FAILED[*]:-none}"

if [ ${#FAILED[@]} -gt 0 ]; then
    echo ""
    echo "To rerun failed prefectures:"
    echo "  $SCRIPT_DIR/run_batch.sh ${FAILED[*]} -- $MFACTOR"
    exit 1
fi
