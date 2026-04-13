#!/usr/bin/env python3
"""Generate Markov transition matrices from the Akita 2005 (秋期PT) raw master file.

The Akita 簡易PT (simplified PT) master file format was reverse-engineered since
the layout PDF is not machine-readable. Field positions validated against:
  - chronologically consistent multi-trip chains per person
  - gender/age/occupation distributions matching plausible demographics
  - purpose codes matching time-of-day patterns (morning outbound vs evening return)

Layout (0-indexed):
    col  0-59:  person identifier prefix (household+person)
    col 43:     gender (1=M, 2=F)
    col 44-45:  age (0-99)
    col 46-47:  occupation (01-10)
    col 73-74:  trip index (NOT chronological — ignore)
    col 85:     dep AM/PM flag (1=AM, 2=PM)
    col 86-89:  dep HHMM (12-hour)
    col 100:    arr AM/PM flag
    col 101-104: arr HHMM
    col 105-106: purpose (01-15)

Same pipeline as generate_markov_kyushu.py:
  1. Parse fixed-width raw file with inferred layout
  2. Build 96-bin schedule per person
  3. Split by demographics
  4. Count transitions and compute smoothed probabilities
  5. Apply canonical postprocess (absorbing HOME at bins >=88)

Usage:
    python scripts/markov/generate_markov_akita.py [--raw RAW] [--output-dir OUT]
"""
import argparse
import os
import sys
from collections import Counter, defaultdict
from pathlib import Path

# ── State space (matches existing datasets) ──
STATES = ['000', '001', '002', '003', '100', '200', '300', '400', '500']
N_STATES = len(STATES)

# ── Akita purpose → 9-state mapping ──
# Validated against dep-hour distribution:
#   01: morn-peak 7h (86% morn) → WORK
#   02: morn-peak 7h (99% morn) → SCHOOL
#   03: eve-peak 17h (54% eve)  → HOME (return)
#   04-06: midday peak → shopping/eating/free
#   07: morn+eve mix → hospital/welfare
#   08-09: morn-peak → business/work-other
#   10-15: misc free/business
ACTIVITY_MAPPING = {
    '01': '002',  # WORK
    '02': '003',  # SCHOOL
    '03': '001',  # RETURN HOME
    '04': '100',  # shopping
    '05': '200',  # eating
    '06': '400',  # free/social
    '07': '300',  # hospital
    '08': '500',  # business
    '09': '500',  # work-other/business
    '10': '400',
    '11': '400',
    '12': '400',
    '13': '400',
    '14': '400',
    '15': '400',
    '00': '000',
    '0':  '000',
    '':   '000',
}


def decode_time(ampm, hhmm):
    """Convert Akita 12-hour AM/PM time to minutes-from-midnight. Returns None on invalid."""
    if not hhmm.isdigit() or ampm not in ('1', '2'):
        return None
    h = int(hhmm[:2])
    m = int(hhmm[2:4])
    if ampm == '2' and h < 12:
        h += 12
    elif ampm == '1' and h == 12:
        h = 0
    if not (0 <= h < 24 and 0 <= m < 60):
        return None
    return h * 60 + m


def parse_int(s, default=None):
    try:
        return int(s) if s else default
    except ValueError:
        return default


def parse_record(line):
    """Extract fields from one Akita raw record."""
    return {
        'person_prefix': line[0:60],
        'gender': line[43:44],
        'age': line[44:46].strip(),
        'occupation': line[46:48].strip(),
        'dep_ampm': line[85:86],
        'dep_hhmm': line[86:90],
        'arr_ampm': line[100:101],
        'arr_hhmm': line[101:105],
        'purpose': line[105:107].strip(),
    }


def person_category(gender, age, occupation):
    """Classify into one of 8 demographic categories.

    Akita occupation codes (validated by age/gender):
      1-5: labor
      6:   student1 (avg age 10)
      7:   student2 (avg age 18)
      8:   nolabor (housewife primarily, 97% F)
      9:   nolabor_senior (avg age 66)
      10:  nolabor (other)
    """
    g = gender
    a = parse_int(age, 0) or 0
    o = parse_int(occupation)

    if o is None:
        if a >= 65:
            return 'nolabor_male_senior' if g == '1' else 'nolabor_female_senior'
        return 'nolabor_male' if g == '1' else 'nolabor_female'

    if o == 6:
        return 'student1'
    if o == 7:
        return 'student2'

    if 1 <= o <= 5:
        return 'labor_male' if g == '1' else 'labor_female'

    if o == 9 or a >= 65:
        return 'nolabor_male_senior' if g == '1' else 'nolabor_female_senior'

    # Codes 8 (housewife), 10 (other): nolabor non-senior
    return 'nolabor_male' if g == '1' else 'nolabor_female'


def build_schedule(trips):
    """Build a 96-bin schedule with morning fill (same logic as Kyushu generator)."""
    bin_purposes = [defaultdict(float) for _ in range(96)]

    for trip in trips:
        dep_min = decode_time(trip['dep_ampm'], trip['dep_hhmm'])
        arr_min = decode_time(trip['arr_ampm'], trip['arr_hhmm'])
        if dep_min is None or arr_min is None:
            continue
        if arr_min <= dep_min:
            continue
        if dep_min < 0 or arr_min > 24 * 60:
            continue

        purpose_code = trip['purpose']

        t = (dep_min // 15) * 15
        while t < arr_min and t < 24 * 60:
            bin_end = t + 15
            overlap = max(0, min(bin_end, arr_min) - max(t, dep_min))
            if overlap > 0:
                bin_purposes[t // 15][purpose_code] += overlap
            t += 15

    # Resolve dominant purpose per bin
    schedule = ['000'] * 96
    for i, acts in enumerate(bin_purposes):
        if not acts:
            continue
        best = max(acts.items(), key=lambda kv: (kv[1], kv[0] != '0'))
        schedule[i] = ACTIVITY_MAPPING.get(best[0], '000')

    # Force bin 0 to HOME
    if schedule[0] == '000':
        schedule[0] = '001'

    # Morning fill: pre-first-trip bins → HOME
    first_trip_bin = 96
    for i in range(1, 96):
        if schedule[i] != '000':
            first_trip_bin = i
            break
    for i in range(1, first_trip_bin):
        schedule[i] = '001'

    return schedule


def compute_probabilities(counts, alpha=0.1, late_home_bin=88):
    """Same canonical logic as generate_markov_kyushu.py."""
    probs = {}
    for bin_idx in range(96):
        probs[bin_idx] = {}
        for from_s in STATES:
            total = sum(counts[bin_idx][from_s].get(ts, 0) for ts in STATES)
            row = {}
            if total == 0:
                for s in STATES:
                    row[s] = 0.0
                if bin_idx >= late_home_bin:
                    row['001'] = 1.0
                else:
                    row[from_s] = 1.0
            else:
                smoothed_total = total + alpha * N_STATES
                for s in STATES:
                    raw = counts[bin_idx][from_s].get(s, 0)
                    row[s] = (raw + alpha) / smoothed_total
            probs[bin_idx][from_s] = row
    return probs


def write_markov_csv(probs, output_path):
    with open(output_path, 'w') as f:
        for bin_idx in range(96):
            parts = [f"{bin_idx:03d}"]
            for s1 in STATES[1:]:
                tokens = [s1]
                for s2 in STATES:
                    tokens.append(s2)
                    tokens.append(str(probs[bin_idx][s1][s2]))
                parts.append(' '.join(tokens) + ' ')
            f.write(','.join(parts) + '\n')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--raw', default='/mnt/large/data/00_PTData/17_秋田H17/10_rawdata/提供資料/秋期PT/秋期PT_34,391レコード.txt')
    ap.add_argument('--output-dir', default='/tmp/markov_v2_raw')
    ap.add_argument('--sample', type=int, default=None)
    args = ap.parse_args()

    persons = defaultdict(list)
    person_meta = {}

    print(f"Reading {args.raw}")
    with open(args.raw, encoding='ascii', errors='replace') as f:
        for i, line in enumerate(f):
            if args.sample and i >= args.sample:
                break
            if len(line.rstrip()) < 107:
                continue
            rec = parse_record(line)
            pid = rec['person_prefix']
            persons[pid].append(rec)
            if pid not in person_meta:
                person_meta[pid] = (rec['gender'], rec['age'], rec['occupation'])

    print(f"Parsed {len(persons)} persons from {sum(len(v) for v in persons.values())} trip records")

    cat_counts = {}
    cat_person_counts = Counter()

    # Also collect diagnostics
    singleton_persons = 0   # persons with only 1 trip (could indicate grouping collisions)

    for uid, trips in persons.items():
        g, a, o = person_meta[uid]
        cat = person_category(g, a, o)
        cat_person_counts[cat] += 1
        if len(trips) < 2:
            singleton_persons += 1

        schedule = build_schedule(trips)

        if cat not in cat_counts:
            cat_counts[cat] = [
                {s: defaultdict(int) for s in STATES} for _ in range(96)
            ]
        counts = cat_counts[cat]
        for i in range(95):
            from_s = schedule[i]
            to_s = schedule[i + 1]
            counts[i][from_s][to_s] += 1

    print(f"\nPersons per category:")
    for cat, n in cat_person_counts.most_common():
        print(f"  {cat}: {n}")
    print(f"\nSingleton persons (only 1 trip): {singleton_persons}/{len(persons)} ({100*singleton_persons/len(persons):.1f}%)")

    # Write raw CSVs
    os.makedirs(args.output_dir, exist_ok=True)
    for cat, counts in cat_counts.items():
        probs = compute_probabilities(counts)
        out_path = Path(args.output_dir) / f"akita2005_trip_{cat}_prob.csv"
        write_markov_csv(probs, out_path)
        print(f"  wrote: {out_path}")


if __name__ == '__main__':
    main()
