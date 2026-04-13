#!/usr/bin/env python3
"""Generate Markov transition matrices from the Kyushu 2005 (北部九州PT H17) raw master file.

Pipeline (matches Process_PTMaster.ipynb logic for existing datasets):
  1. Parse fixed-width raw file using layout from 4PTマスターレイアウト.xls
  2. Split records by person; build 96-bin schedule per person using
     edited purpose field (cols 305-306) with 9-state activity_mapping
  3. Split persons into demographic categories (labor_male/labor_female/
     nolabor_male/nolabor_female/nolabor_male_senior/nolabor_female_senior/
     student1/student2)
  4. Count transitions between adjacent 15-min bins per category
  5. Write raw Markov CSVs to output/
  6. (Separate step) apply postprocess_markov.py for canonical closure fix

Output:
  /tmp/markov_v2/kyushu2005_trip_{category}_prob.csv  (raw)
  (then run postprocess_markov.py to add absorbing HOME)

Usage:
    python scripts/markov/generate_markov_kyushu.py [--raw RAW_FILE] [--output-dir OUTPUT]
"""
import argparse
import csv
import os
import sys
from collections import Counter, OrderedDict, defaultdict
from pathlib import Path

# ── Layout (1-indexed start, end inclusive, zero-indexed: start-1, end) ──
LAYOUT = {
    'addr_code':    (1, 8),     # 住所コード
    'household':    (9, 12),    # 世帯番号
    'person_num':   (13, 14),   # 個人番号 (within household)
    'trip_count':   (15, 16),   # トリップ数
    'trip_num':     (17, 18),   # トリップ番号
    'gender':       (47, 47),   # 性別 1=M 2=F
    'age':          (48, 50),   # 年齢
    'occupation':   (51, 52),   # 職業 1-14
    'dep_hour':     (105, 106), # 出発時間 時
    'dep_minute':   (107, 108), # 出発時間 分
    'arr_hour':     (126, 127), # 到着時間 時
    'arr_minute':   (128, 129), # 到着時間 分
    'raw_purpose':  (130, 131), # 目的 (raw, survey-specific)
    'edit_purpose': (305, 306), # 編集項目 目的 (SRC-normalized, 1-22)
    'uid':          (309, 316), # 個人番号 (globally unique)
    'resid_zone':   (317, 320), # 居住地ゾーン
}

# ── State space (matches existing datasets) ──
STATES = ['000', '001', '002', '003', '100', '200', '300', '400', '500']
N_STATES = len(STATES)
TIME_SLOTS = [f"{h:02d}:{m:02d}" for h in range(24) for m in range(0, 60, 15)]

# ── Activity mapping (Kyushu edited-purpose codes 1-22 → 9-state) ──
# Validated against departure-hour distribution analysis:
#   - Codes 01-03: morning peak → work/school outbound
#   - Codes 19, 22: BOTH are "return home" (帰宅), differing by time:
#       - 22 peaks 11:00-17:00 (midday return for housewives/retirees)
#       - 19 peaks 17:00-22:00 (evening return for commuters)
ACTIVITY_MAPPING = {
    '01': '002',  # WORK (勤務) — morning peak 7-8h
    '02': '003',  # SCHOOL (高校・大学 high school/college) — morning peak 7-9h
    '03': '003',  # SCHOOL (小中学校 elementary/junior) — morning peak 7-8h, 92% of occ-11 first trips
    '04': '100',  # shopping
    '05': '200',  # eating
    '06': '400',  # social (free)
    '07': '300',  # hospital
    '08': '300',  # welfare/medical
    '09': '400',  # free/private
    '10': '400',
    '11': '400',
    '12': '400',
    '13': '400',
    '14': '400',
    '15': '400',
    '16': '400',
    '17': '500',  # business
    '18': '500',  # business
    '19': '001',  # ← RETURN HOME (evening primary)
    '20': '500',
    '21': '500',
    '22': '001',  # ← RETURN HOME (midday)
    '00': '000',
    '0': '000',
    '': '000',
}


def slice_field(line, start, end):
    """Extract field using 1-indexed inclusive positions."""
    return line[start - 1:end].strip()


def parse_int(s, default=None):
    try:
        return int(s) if s else default
    except ValueError:
        return default


def parse_record(line):
    """Parse one raw line into a dict of fields."""
    rec = {}
    for name, (s, e) in LAYOUT.items():
        rec[name] = slice_field(line, s, e)
    return rec


def person_category(gender, age, occupation):
    """Classify a person into one of 8 demographic categories.

    Kyushu PT H17 職業 codes (validated against age/gender distribution):
        1-10: labor (workers, avg age 40-57)
        11: primary/junior students (avg age 10, range 5-19)
        12: high school / college students (avg age 19, range 15-75)
        13: housewife / nolabor (avg age 52, mostly female)
        14: retired / elderly nolabor (avg age 66)
    """
    g = parse_int(gender)
    a = parse_int(age, 0) or 0
    o = parse_int(occupation)

    if o is None:
        if a >= 65:
            return 'nolabor_male_senior' if g == 1 else 'nolabor_female_senior'
        return 'nolabor_male' if g == 1 else 'nolabor_female'

    # Students
    if o == 11:
        return 'student1'  # primary/junior
    if o == 12 and a < 25:
        return 'student2'  # high school / college
    if o == 12:
        # Older "student" outliers → treat as nolabor by age
        if a >= 65:
            return 'nolabor_male_senior' if g == 1 else 'nolabor_female_senior'
        return 'nolabor_male' if g == 1 else 'nolabor_female'

    # Labor: codes 1-10
    if 1 <= o <= 10:
        return 'labor_male' if g == 1 else 'labor_female'

    # Non-labor: codes 13, 14, and anything else
    if o == 14 or a >= 65:
        return 'nolabor_male_senior' if g == 1 else 'nolabor_female_senior'
    return 'nolabor_male' if g == 1 else 'nolabor_female'


def build_schedule(trips):
    """Build a 96-bin schedule from a list of trip records for one person.

    Each trip contributes bins from dep_time to arr_time, labeled with the
    destination purpose (mapped via ACTIVITY_MAPPING).

    Returns: list of 96 state strings.
    """
    # Per-bin: accumulate purpose → duration (minutes)
    bin_purposes = [defaultdict(float) for _ in range(96)]

    for trip in trips:
        dh = parse_int(trip['dep_hour'])
        dm = parse_int(trip['dep_minute'])
        ah = parse_int(trip['arr_hour'])
        am = parse_int(trip['arr_minute'])
        purpose_code = trip['edit_purpose'] if trip['edit_purpose'] else trip['raw_purpose']

        if dh is None or dm is None or ah is None or am is None:
            continue
        if dh == 99 or ah == 99:
            continue
        # Next-day arrival (hour >= 24 means next day)
        if ah >= 24:
            ah = 23
            am = 45
        # Normalize dep after 23:45
        if dh >= 24:
            continue

        start_minutes = dh * 60 + dm
        end_minutes = ah * 60 + am
        if end_minutes <= start_minutes:
            continue
        if start_minutes < 0 or end_minutes > 24 * 60:
            continue

        # Iterate 15-min bins that overlap [start, end]
        t = (start_minutes // 15) * 15
        while t < end_minutes and t < 24 * 60:
            bin_end = t + 15
            overlap = max(0, min(bin_end, end_minutes) - max(t, start_minutes))
            if overlap > 0:
                bin_purposes[t // 15][purpose_code] += overlap
            t += 15

    # Resolve dominant purpose per bin
    schedule = ['000'] * 96
    for i, acts in enumerate(bin_purposes):
        if not acts:
            continue
        # Pick max-duration purpose (ignore '0' if any other exists)
        best = max(acts.items(), key=lambda kv: (kv[1], kv[0] != '0'))
        schedule[i] = ACTIVITY_MAPPING.get(best[0], '000')

    # Force bin 0 to HOME if still empty (matches notebook convention)
    if schedule[0] == '000':
        schedule[0] = '001'

    # Morning fill: all pre-first-trip bins → HOME.
    # This captures the morning departure signal (HOME→first-trip-destination)
    # that the runtime generator needs. Without this, the HOME state in
    # morning bins is near-empty (since raw data only labels trip bins) and
    # HOME→OFFICE per-bin rates collapse to the Laplace smoothing floor.
    first_trip_bin = 96
    for i in range(1, 96):
        if schedule[i] != '000':
            first_trip_bin = i
            break
    for i in range(1, first_trip_bin):
        schedule[i] = '001'

    return schedule


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--raw', default='/mnt/large/data/00_PTData/03_KyushuH17/rawdata/H17MSTR-070405.txt')
    ap.add_argument('--output-dir', default='/tmp/markov_v2')
    ap.add_argument('--sample', type=int, default=None,
                    help='Process only N records (for testing)')
    args = ap.parse_args()

    # Group records by person (uid)
    persons = defaultdict(list)          # uid -> list of trip records
    person_meta = {}                     # uid -> (gender, age, occupation)

    print(f"Reading {args.raw}")
    with open(args.raw, encoding='ascii', errors='replace') as f:
        for i, line in enumerate(f):
            if args.sample and i >= args.sample:
                break
            if len(line.rstrip()) < 316:
                continue
            rec = parse_record(line)
            uid = rec['uid'] or (rec['addr_code'] + rec['household'] + rec['person_num'])
            persons[uid].append(rec)
            if uid not in person_meta:
                person_meta[uid] = (rec['gender'], rec['age'], rec['occupation'])

    print(f"Parsed {len(persons)} persons from {sum(len(v) for v in persons.values())} trip records")

    # Count transitions per demographic category
    # category -> [bin_idx] -> {from_state: {to_state: count}}
    cat_counts = {}
    cat_person_counts = Counter()

    for uid, trips in persons.items():
        g, a, o = person_meta[uid]
        cat = person_category(g, a, o)
        cat_person_counts[cat] += 1

        schedule = build_schedule(trips)

        if cat not in cat_counts:
            cat_counts[cat] = [
                {s: defaultdict(int) for s in STATES} for _ in range(96)
            ]
        counts = cat_counts[cat]
        for i in range(95):  # 0..94
            from_s = schedule[i]
            to_s = schedule[i + 1]
            counts[i][from_s][to_s] += 1

    print(f"\nPersons per category:")
    for cat, n in cat_person_counts.most_common():
        print(f"  {cat}: {n}")

    # Compute probabilities and write
    os.makedirs(args.output_dir, exist_ok=True)
    for cat, counts in cat_counts.items():
        probs = compute_probabilities(counts)
        out_path = Path(args.output_dir) / f"kyushu2005_trip_{cat}_prob.csv"
        write_markov_csv(probs, out_path)
        print(f"  wrote: {out_path}")


def compute_probabilities(counts, alpha=0.1, late_home_bin=88):
    """Same logic as postprocess_markov.py but applied to fresh counts.

    - Laplace smoothing with small alpha for observed rows.
    - Absorbing fallback for zero-count rows:
      bin >= late_home_bin → HOME; else self-loop.
    """
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
    """Write in the format MkChainAccessor expects."""
    with open(output_path, 'w') as f:
        for bin_idx in range(96):
            parts = [f"{bin_idx:03d}"]
            for s1 in STATES[1:]:  # skip '000' as source
                tokens = [s1]
                for s2 in STATES:
                    tokens.append(s2)
                    tokens.append(str(probs[bin_idx][s1][s2]))
                parts.append(' '.join(tokens) + ' ')
            f.write(','.join(parts) + '\n')


if __name__ == '__main__':
    main()
