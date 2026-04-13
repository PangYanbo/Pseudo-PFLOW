#!/usr/bin/env python3
"""Minimal post-process of existing Markov CSV files to enforce end-of-day
HOME closure.

Unlike rebuild_markov.py (which reinterprets the raw per-person schedules and
changes transition semantics), this script takes the EXISTING Markov probability
files as input and modifies only the late-evening bins to absorb to HOME.

Changes made:
  1. For bins >= 88 (22:00 onwards), rewrite every source state's transition
     row so that ->HOME (001) = 1.0 and all others = 0.0.

Rationale: the only real structural problem in the v1 Markov data is that
evening closure to HOME is not enforced. The v1 morning dynamics (driven by
night-shift-returner bias in the HOME state) happen to produce plausible
morning-departure rates when sampled by the runtime generator, so we preserve
them. Only the end-of-day closure needs correction.

This is a minimal, reversible, low-risk fix.

Usage:
    python scripts/markov/postprocess_markov.py \\
        --src-dir /mnt/large/data/PseudoPFLOW/processing/markov \\
        --out-dir /tmp/markov_v2 \\
        --datasets asahikawa2002 chukyo2011
"""
import argparse
import os
import sys
from pathlib import Path

STATES = ['000', '001', '002', '003', '100', '200', '300', '400', '500']

FILE_SUFFIXES = [
    'trip_labor_male_prob',
    'trip_labor_female_prob',
    'trip_nolabor_male_prob',
    'trip_nolabor_female_prob',
    'trip_nolabor_male_senior_prob',
    'trip_nolabor_female_senior_prob',
    'trip_student1_prob',
    'trip_student2_prob',
]


def parse_row(line):
    """Parse one Markov row. Returns (bin_idx, {source_state: {dest_state: prob}})."""
    parts = line.rstrip('\n').split(',')
    bin_idx = int(parts[0])
    purpose_data = {}
    for cell in parts[1:]:
        toks = cell.strip().split()
        if not toks:
            continue
        cur = toks[0]
        trans = {}
        for i in range(1, len(toks), 2):
            try:
                trans[toks[i]] = float(toks[i + 1])
            except (ValueError, IndexError):
                pass
        purpose_data[cur] = trans
    return bin_idx, purpose_data


def format_row(bin_idx, purpose_data):
    """Format one row back to the original CSV format."""
    parts = [f"{bin_idx:03d}"]
    # Source states: skip '000' (convention from original notebook)
    for s1 in STATES[1:]:
        if s1 not in purpose_data:
            continue
        dests = purpose_data[s1]
        tokens = [s1]
        for s2 in STATES:
            tokens.append(s2)
            tokens.append(str(dests.get(s2, 0.0)))
        # Note: original notebook joins with spaces and has trailing space
        parts.append(' '.join(tokens) + ' ')
    return ','.join(parts)


def absorbing_home_row():
    """A transition row forcing HOME: P(001) = 1.0, others 0."""
    return {s: (1.0 if s == '001' else 0.0) for s in STATES}


def stay_row():
    """A transition row forcing STAY (no new activity emission): P(000) = 1.0."""
    return {s: (1.0 if s == '000' else 0.0) for s in STATES}


def postprocess_file(src_path, out_path, late_home_bin=88):
    """Read one Markov CSV, enforce absorbing HOME for bins >= late_home_bin,
    and write the result."""
    with open(src_path) as f:
        lines = f.readlines()

    out_lines = []
    for line in lines:
        if not line.strip():
            continue
        bin_idx, purpose_data = parse_row(line)
        if bin_idx >= late_home_bin:
            # Force absorbing HOME for non-HOME sources; HOME source stays
            # (STAY = don't emit a new activity) to avoid spamming duplicate
            # HOME activities for every bin 88..94.
            new_data = {}
            for s1 in STATES[1:]:
                if s1 == '001':
                    new_data[s1] = stay_row()
                else:
                    new_data[s1] = absorbing_home_row()
            purpose_data = new_data
        out_lines.append(format_row(bin_idx, purpose_data) + '\n')

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, 'w') as f:
        f.writelines(out_lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src-dir', default='/mnt/large/data/PseudoPFLOW/processing/markov')
    ap.add_argument('--out-dir', default='/tmp/markov_v2')
    ap.add_argument('--datasets', nargs='+', default=['asahikawa2002', 'chukyo2011'])
    ap.add_argument('--late-home-bin', type=int, default=88,
                    help='Bin index (inclusive) from which to force absorbing HOME (default 88 = 22:00)')
    args = ap.parse_args()

    for ds in args.datasets:
        print(f"\n=== {ds} (absorbing HOME at bin {args.late_home_bin}+) ===")
        for suffix in FILE_SUFFIXES:
            src = Path(args.src_dir) / f"{ds}_{suffix}.csv"
            out = Path(args.out_dir) / f"{ds}_{suffix}.csv"
            if not src.exists():
                print(f"  SKIP (missing): {src}")
                continue
            postprocess_file(str(src), str(out), late_home_bin=args.late_home_bin)
            print(f"  wrote: {out}")

    print("\nDone.")


if __name__ == '__main__':
    main()
