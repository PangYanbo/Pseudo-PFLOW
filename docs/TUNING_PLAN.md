# Tuning Plan

## Overview

Two-part calibration workflow, kept separate from the mainline generation pipeline.

- **Part A**: Transport mode share tuning (parameter search + validation)
- **Part B**: Activity behavior comparison (evaluation only, no parameter tuning yet)

Target data: `data/tuning/transport_share_targets.csv` and `data/tuning/activity_behavior_targets.csv` (70 cities across 42 prefectures, from PT survey data).

---

## Part A — Transport Mode Share Tuning

### A1. Tuning Objective

Minimize the weighted sum of squared errors between generated trip-level mode shares and PT survey targets:

```
loss(city) = Σ_m  w_m × (generated_m - target_m)²
```

where mode shares are computed on **real trips only** (placeholder NOT_DEFINED excluded from the denominator — see A3).

Weights by mode priority:
| Mode | Weight | Rationale |
|------|--------|-----------|
| CAR | 3.0 | Largest share, most sensitive to parameter changes |
| TRAIN | 2.0 | Key transit mode, directly affected by cost model |
| BUS | 2.0 | Key transit mode, directly affected by cost model |
| BICYCLE | 1.0 | Secondary mode |
| WALK | 1.0 | Residual category |

Per-prefecture loss is the population-weighted average across target cities in that prefecture. When Stage 1 uses only 1 city, the loss is that city's loss directly.

### A2. Mode Mapping: Generated → Target

| Generated (rep_mode) | Target column | Notes |
|----------------------|---------------|-------|
| CAR (3) | `car_share` | Target = `car_driver_share` + `car_passenger_share` + `motorcycle_share` |
| TRAIN (4) | `rail_share` | Direct match |
| BUS (5) | `bus_share` | Direct match |
| BICYCLE (2) | `bicycle_share` | Direct match |
| WALK (1) | `walk_other_share` | See NOT_DEFINED handling below |

**Motorcycle rule**: The target data includes `motorcycle_share` (range 0.1%–8.1%, mean 2.0%). Pseudo-PFLOW does not generate motorcycle as a separate mode. For comparison:
- **Default (v1)**: Merge motorcycle into CAR. Adjusted target = `car_driver_share + car_passenger_share + motorcycle_share`. Rationale: motorcycle is motorized road travel; merging into CAR keeps the transit/non-motorized boundary clean.
- **Alternative** (not used in v1): Merge into BICYCLE (two-wheeled). Or exclude and renormalize.
- The chosen rule is applied when loading targets in the tuning script. Changing it later requires only editing the target-loading function.

### A3. Generated Metric and NOT_DEFINED Handling

**Trip-level representative mode share** (from `rep_mode` column, index 11 in 12-column trip CSV).

Each unique `(person_id, trip_id)` counts once. The mode is `rep_mode`, which resolves MIX trips to their highest-priority segment (TRAIN > BUS > CAR > BICYCLE > WALK).

Do NOT use subtrip-level mode share — it inflates BUS and WALK due to MIX subtrip decomposition (verified: BUS/TRAIN drops from 2.36 to 1.39 at trip level).

**Placeholder NOT_DEFINED handling**:

NOT_DEFINED trips fall into two categories (as classified by the validator):
- **Placeholder** (zero distance, origin == destination): Stay-at-home stubs, single-activity persons. These are data artifacts, not real trips.
- **Unexpected** (nonzero distance): Real movement where mode choice failed. These are rare (<1%).

For the tuning loss function:
1. **Exclude placeholder NOT_DEFINED** from the denominator. Mode shares are computed as: `share_m = count_m / (total_trips - placeholder_ND_trips)`.
2. **Include unexpected NOT_DEFINED** in the denominator (counted as real trips with unknown mode).
3. Report the following diagnostics alongside the loss, but do NOT include them in the loss function:
   - `placeholder_nd_rate` = placeholder ND / total trips
   - `unexpected_nd_rate` = unexpected ND / total trips
   - `real_trip_count` = total - placeholder ND

This means generated mode shares sum to ~100% on real trips, and can be directly compared to target shares (which also sum to ~100% from the PT survey).

**How this affects the loss function**: The target shares from the PT survey already exclude non-outing persons. By removing placeholder NOT_DEFINED from the generated denominator, both sides measure the same thing: mode distribution of actual movements. Without this exclusion, placeholder NOT_DEFINED would dilute all generated shares downward, creating a systematic bias.

### A4. City-Code Alignment Strategy

Generated activity files use 5-digit city codes (`person_XXXXX.csv`). Target files use codes that may differ in two ways:

#### Case 1: Direct match (43 of 70 target cities)
Target code matches a generated file directly. Example: target `22211` (磐田市) → `person_22211.csv`.

#### Case 2: Ward aggregation (15 of 70 target cities)
Designated cities (政令指定都市) appear as a single code in the target (e.g., `22100` for 静岡市) but are split into ward-level files in the generated output (e.g., `person_22101.csv`, `person_22102.csv`, `person_22103.csv`).

Pattern: target codes ending in `00` or `30` (e.g., `13100`, `14130`, `27140`) aggregate all generated files whose first 4 digits match.

Known aggregation cases:
| Target code | City | Ward files |
|-------------|------|-----------|
| 11100 | さいたま市 | 9 |
| 12100 | 千葉市 | 6 |
| 13100 | 東京23区 | 9 |
| 14100 | 横浜市 | 9 |
| 14130 | 川崎市 | 28 |
| 22100 | 静岡市 | 3 |
| 23100 | 名古屋市 | 9 |
| 26100 | 京都市 | 9 |
| 27100 | 大阪市 | 7 |
| 27140 | 堺市 | 31 |
| 28100 | 神戸市 | 7 |
| 34100 | 広島市 | 8 |
| 40100 | 北九州市 | 7 |
| 40130 | 福岡市 | 14 |
| 43100 | 熊本市 | 5 |

#### Case 3: Leading-zero padding (12 of 70 target cities)
Prefectures 1–9 use zero-padded codes in generated files (`01203`) but non-padded in the target (`1203`). The alignment function zero-pads the target code to 5 digits before matching.

#### Alignment implementation

The scoring script resolves target codes to generated file(s) using this function:

```python
def resolve_city_files(target_code, pref_code, generated_dir):
    """Return list of generated file paths for a target city code."""
    # Step 1: zero-pad target code to 5 digits
    padded = target_code.zfill(5)

    # Step 2: try direct match
    direct = glob(f"{generated_dir}/person_{padded}.csv")  # or trip_{padded}.csv
    if direct:
        return direct

    # Step 3: try ward aggregation (first 4 digits)
    prefix = padded[:4]
    ward_files = sorted(glob(f"{generated_dir}/person_{prefix}*.csv"))
    if ward_files:
        return ward_files

    return []  # no match
```

When multiple files are returned (aggregation), the scoring script **concatenates all rows** before computing mode shares. This treats the designated city as a single population pool, matching the PT survey scope.

For activity comparison (Part B), the same aggregation logic applies: outing rate, trip count, and purpose distribution are computed across all ward files combined.

### A5. Sampling Strategy and the 1000-ID Rule

**Problem**: At mfactor=200 (0.5% sampling), small cities may produce fewer than 1000 persons. Mode share from <1000 persons is noisy (e.g., city 22429 gives only 36 persons at mfactor=200).

**Rule**: Each target city must have at least 1000 person IDs in the tuning run. The mfactor is adapted per city:

```
full_population = count of unique person IDs in activity source files
mfactor = max(1, floor(full_population / 1000))
```

For pref-22 target cities (using unified activity files from ActivityGenerator at mfactor=1):

| City | Full pop (approx) | mfactor for 1000 IDs | Actual IDs |
|------|-------------------|---------------------|------------|
| 22100 (静岡市, 3 wards) | ~237K | 237 | ~1000 |
| 22211 (磐田市) | ~132K | 132 | ~1000 |

At these rates, each city produces ~1000 persons — enough for stable mode shares while keeping WebAPI query count manageable (~2000–3000 queries per city).

**Implementation**: The tuning script computes mfactor per city by counting lines in the activity source files (summing across wards for aggregated cities), then passes it to `PersonAccessor.loadActivity()`.

### A6. Two-Stage Search Strategy

**Stage 1: Coarse screening** (10 configurations, 1 representative city per prefecture, ~1000 persons)

Pick the **representative city** for each prefecture: prefer the center city (largest, most transit), since it has the widest mode diversity and is most sensitive to parameter changes. For pref 22: use 22100 (Shizuoka, center city) as the Stage 1 target; skip 22211 (Iwata, peripheral/car-dominated) until Stage 2.

Generate 10 parameter sets using Latin Hypercube Sampling (LHS) across the 7-parameter space:

| Parameter | Config key | Range | Default |
|-----------|-----------|-------|---------|
| fare.per.hour | `fare.per.hour` | 500–2000 | 1000 |
| fare.init | `fare.init` | 100–500 | 200 |
| fare.per.kilometer | `fare.per.kilometer` | 5–30 | 10 |
| fatigue.walk | `fatigue.walk` | 1.0–3.0 | 1.5 |
| fatigue.bicycle | `fatigue.bicycle` | 1.0–2.0 | 1.2 |
| api.transit.transferPenalty | `api.transit.transferPenalty` | 0–300 | 0 |
| min.transit.distance | `min.transit.distance` | 500–1500 | 1000 |

Always include the current defaults as config_000 (baseline). The remaining 9–11 are LHS-sampled.

ActivityGenerator output is shared across all configurations (activity generation does not depend on transport parameters). Run ActivityGenerator once, then run TripGenerator 10 times with different config overrides.

**Budget**: 10 configs × 1 city × ~1000 persons × ~3 queries/person = ~30K WebAPI queries. At ~0.5s/query ≈ 4h sequential, ~1h with parallelism.

Score each configuration. Rank by loss.

**Stage 2: Validation** (top 3–4 candidates, expanded to all target cities, ~2000 persons/city)

Re-run top 3–4 candidates (plus baseline) on **all target cities** in the prefecture at 2× sample size (~2000 persons/city). For pref 22: run on both 22100 and 22211.

This confirms:
- Stage 1 ranking is stable with more data
- Parameters generalize across center and peripheral cities
- No overfitting to one city's characteristics

If Stage 2 ranking differs significantly from Stage 1, flag for manual review.

**Budget**: 4 configs × 2 cities × ~2000 persons × ~3 queries = ~48K queries ≈ 2–3h.

**Output**: 1 best parameter set per prefecture, plus 2–3 runner-up candidates with their scores.

### A7. Tuning Scripts and Directory Layout

```
scripts/tuning/
  transport_tune.py          # Main orchestrator: LHS generation, run management, scoring
  transport_score.py         # Score a single run's trip output against targets
  transport_report.py        # Generate summary report from all scored runs
  lhs.py                     # Latin Hypercube Sampling for parameter generation
  city_align.py              # City-code alignment: resolve target codes to generated files
  activity_compare.py        # Activity behavior comparison (Part B)

data/tuning/
  transport_share_targets.csv    # PT survey targets (existing)
  activity_behavior_targets.csv  # PT survey targets (existing)

output/tuning/<pref>/
  configs/                   # Generated config.properties files (one per candidate)
    config_000.properties    # Baseline (current defaults)
    config_001.properties
    ...
  stage1/                    # Stage 1 trip output (1 city)
    001/trip_<city_code>.csv
    002/trip_<city_code>.csv
    ...
  stage2/                    # Stage 2 trip output (all target cities)
    001/trip_<city_code>.csv
    ...
  scores/                    # Per-candidate score JSONs
    score_001_s1.json        # Stage 1 score
    score_001_s2.json        # Stage 2 score
    ...
  report.md                  # Final summary
  activity_comparison.md     # Activity behavior comparison (Part B)
```

### A8. Separation from Mainline

1. Tuning scripts live under `scripts/tuning/`, not `scripts/validate/` or `src/`.
2. Tuning output goes to `output/tuning/<pref>/`, never to `outputDir` used by the mainline.
3. Tuning runs use `-Dconfig.file=` to inject candidate parameters, overriding `config.properties` without modifying it.
4. ActivityGenerator output is written to `output/tuning/<pref>/activity/` (shared across candidates).
5. The mainline pipeline is never invoked by tuning scripts — tuning calls `mvn exec:java` with explicit `-Dconfig.file=` pointing to tuning-specific configs.

### A9. Config Override Mechanism

Each candidate config file contains only the 7 tunable parameters plus outputDir:

```properties
# Candidate 001
outputDir=/path/to/output/tuning/22/stage1/001/
fare.per.hour=1200
fare.init=300
fare.per.kilometer=15
fatigue.walk=2.0
fatigue.bicycle=1.4
api.transit.transferPenalty=100
min.transit.distance=800
```

Passed via `-Dconfig.file=output/tuning/22/configs/config_001.properties`. This overlays on top of base `config.properties` + `config.pref.22.properties` via ConfigLoader's 4-layer precedence.

### A10. Per-Prefecture Output Format

Final report (`output/tuning/<pref>/report.md`) contains:

```markdown
# Transport Tuning Report: Prefecture 22

## Best parameter set
| Parameter | Value |
|-----------|-------|
| fare.per.hour | 1200 |
| ... | ... |

## Loss: 12.3 (stage 2)

## Mode share comparison (real trips only, placeholder NOT_DEFINED excluded)
| Mode | Target (22100) | Generated (22100) | Target (22211) | Generated (22211) |
|------|---------------|-------------------|---------------|-------------------|
| CAR  | 56.1%         | 58.2%             | 74.6%         | 71.3%             |
| TRAIN | 6.4%         | 5.1%              | 2.7%          | 2.3%              |
| BUS  | 2.6%          | 3.0%              | 0.1%          | 0.2%              |
| BICYCLE | 14.2%      | 13.8%             | 6.9%          | 7.1%              |
| WALK | 20.7%         | 19.9%             | 15.8%         | 17.1%             |

## Diagnostics
| City | Placeholder ND rate | Unexpected ND rate | Real trips |
|------|--------------------|--------------------|------------|
| 22100 | 3.8%              | 0.1%               | 4521       |
| 22211 | 4.2%              | 0.0%               | 2876       |

## Runner-up candidates
| Rank | Loss  | fare.per.hour | fare.init | ... |
|------|-------|---------------|-----------|-----|
| 2    | 14.1  | 1100          | 250       | ... |
| 3    | 15.8  | 1400          | 350       | ... |

## Stage 1 → Stage 2 ranking stability
| Config | Stage 1 rank | Stage 2 rank | Stage 1 loss | Stage 2 loss |
|--------|-------------|-------------|-------------|-------------|
| 003    | 1           | 1           | 10.2        | 12.3        |
| 007    | 2           | 3           | 11.8        | 15.8        |
| 001    | 3           | 2           | 12.5        | 14.1        |
```

---

## Part B — Activity Behavior Comparison

### B1. Comparison Objective (no parameter tuning)

Measure how well ActivityGenerator output matches PT survey activity behavior targets. This is evaluation-only — no parameter search in Phase 1.

### B2. Target Metrics and Generated Metric Mapping

#### B2.1 Outing Rate

**Target**: `outing_rate` (% of persons who make at least one trip per day, range 66–80%).

**Generated metric**: From ActivityGenerator output, count persons with >1 activity (i.e., not home-only). `outing_rate = persons_with_trips / total_persons × 100`.

Concretely: in the activity CSV, a person with only one row (HOME activity) is a non-outing person. A person with ≥2 rows made at least one outing.

#### B2.2 Trip Count

**Target**: `trip_count_gross` (average trips per person including non-outing persons, range 1.67–2.18) and `trip_count_net` (average trips per outing person only, range 2.47–2.83).

**Generated metric**:
- `trip_count_gross` = total trips / total persons. A "trip" = one activity-to-activity transition = `(number of activities - 1)` per person. Persons with 1 activity contribute 0 trips.
- `trip_count_net` = total trips / outing persons (persons with ≥2 activities).

Note: The generated trip count from ActivityGenerator counts activity transitions, not transport-level trips. This matches the PT survey definition of "trip" (one origin-destination movement). MIX sub-trips are irrelevant here — we count at the activity level.

#### B2.3 Purpose Distribution

**Target**: 12 purpose share columns (commute, school, business, return_home, shopping, eating, walk, tourism, hospital, pickup, accompany, other).

**Generated metric mapping** (EPurpose → target categories):

| EPurpose | ID | Target category | Notes |
|----------|-----|----------------|-------|
| OFFICE | 2 | `purpose_commute_share` | Direct match |
| SCHOOL | 3 | `purpose_school_share` | Direct match |
| BUSINESS | 500 | `purpose_business_share` | Direct match |
| HOME | 1 | `purpose_return_home_share` | Return-home trips |
| SHOPPING | 100 | `purpose_shopping_share` | Direct match |
| EATING | 200 | `purpose_eating_share` | Direct match |
| HOSPITAL | 300 | `purpose_hospital_share` | Direct match |
| FREE | 400 | `purpose_other_share` | FREE is a catch-all; maps to walk + tourism + pickup + accompany + other combined |
| RETURN_OFFICE | 4 | `purpose_commute_share` | Return to office ≈ commute |

**Unmapped target categories**: `purpose_walk_share`, `purpose_tourism_share`, `purpose_pickup_share`, `purpose_accompany_share` have no direct EPurpose equivalent. All are subsumed under FREE(400) in the generator. The comparison report should show:
- Individual generated purpose shares for OFFICE, SCHOOL, BUSINESS, HOME, SHOPPING, EATING, HOSPITAL
- A combined "FREE/other" share compared against the sum of `walk + tourism + pickup + accompany + other` from the target

City-code alignment follows the same rules as Part A (see A4).

### B3. Activity Comparison Reports

**Script**: `scripts/tuning/activity_compare.py`

**Input**: Activity CSV directory + target CSV.

**Output**: `output/tuning/<pref>/activity_comparison.md`

```markdown
# Activity Comparison: Prefecture 22

## Outing Rate
| City | Target | Generated | Delta |
|------|--------|-----------|-------|
| 22100 | 79.2%  | 82.1%     | +2.9% |
| 22211 | 78.8%  | 80.5%     | +1.7% |

## Trip Count
| City | Target (gross) | Gen (gross) | Target (net) | Gen (net) |
|------|---------------|-------------|-------------|-----------|
| 22100 | 2.18          | 2.31        | 2.76        | 2.82      |
| 22211 | 2.14          | 2.25        | 2.72        | 2.79      |

## Purpose Distribution (city 22100)
| Purpose | Target | Generated | Delta |
|---------|--------|-----------|-------|
| Commute | 17.8%  | 19.2%     | +1.4% |
| School  | 5.2%   | 6.1%     | +0.9% |
| Business | 6.1%  | 5.8%     | -0.3% |
| Return home | 42.5% | 41.8%  | -0.7% |
| Shopping | 10.0% | 9.5%     | -0.5% |
| Eating  | 2.0%   | 1.8%     | -0.2% |
| Hospital | 2.9%  | 3.1%     | +0.2% |
| Free/other | 13.5% | 12.7%  | -0.8% |
```

The activity comparison is produced alongside transport tuning but reported separately. No parameters are tuned based on activity comparison in Phase 1.

### B4. When to Run Activity Comparison

- Run once per prefecture, using the same ActivityGenerator output that feeds the transport tuning runs.
- Since activity output is shared across all transport parameter candidates, the activity comparison needs to run only once (not per candidate).
- Activity comparison should be included in the final tuning report as a separate section.

---

## Execution Summary

### For one prefecture (e.g., pref 22):

1. **Prepare**: Identify target cities (22100, 22211). Select representative city for Stage 1 (22100). Compute mfactor per city for 1000-ID minimum. Resolve city-code alignment (22100 → 22101+22102+22103).
2. **Activity generation**: Run ActivityGenerator once for all target cities. Store in `output/tuning/22/activity/`.
3. **Activity comparison**: Run `activity_compare.py` against targets. Store report.
4. **LHS generation**: Generate 10 parameter configs (1 baseline + 9 LHS). Store in `output/tuning/22/configs/`.
5. **Stage 1 (coarse)**: Run TripGenerator_WebAPI_refactor 10 times on representative city only (~1000 persons). Score each.
6. **Stage 2 (validation)**: Re-run top 3–4 candidates on all target cities at ~2000 persons/city. Re-score.
7. **Report**: Produce `output/tuning/22/report.md` with best params + runner-ups.

### Budget estimate per prefecture:

| Step | WebAPI queries | Wall time (est.) |
|------|---------------|-----------------|
| Stage 1 (10 configs × 1 city × 1000 persons) | ~30K | ~1–2h |
| Stage 2 (4 configs × 2 cities × 2000 persons) | ~48K | ~2–3h |
| **Total** | **~78K** | **~3–5h** |

### Scripts to create:

| Script | Purpose | Priority |
|--------|---------|----------|
| `scripts/tuning/lhs.py` | Generate LHS parameter configs | P0 |
| `scripts/tuning/transport_tune.py` | Orchestrate tuning runs | P0 |
| `scripts/tuning/transport_score.py` | Score trip output vs targets | P0 |
| `scripts/tuning/city_align.py` | Resolve target codes to generated files | P0 |
| `scripts/tuning/transport_report.py` | Generate summary report | P1 |
| `scripts/tuning/activity_compare.py` | Activity behavior comparison | P1 |
