# Transport Tuning Guide

## Quick Start

```bash
cd scripts/tuning

# 1. Generate configs (dry run — no Maven, no WebAPI)
python transport_tune.py 22 --generate-only

# 2. Run Stage 1 (1 baseline + 9 LHS configs, representative city only)
python transport_tune.py 22 --stage 1

# 3. Score existing output without rerunning (if runs already completed)
python transport_tune.py 22 --stage 1 --score-only

# 4. Generate report
python transport_report.py 22
```

## Prerequisites

- Java 11, Maven 3.6.3
- Environment variables: `PFLOW_API_USER`, `PFLOW_API_PASS`
- Activity source data under the configured `root` path
- WebAPI server accessible (CSIS routing API)

## How It Works

### Stage 1: Coarse Screening

1. **ActivityGenerator** runs once with `config_000` (baseline) to produce shared activity data in `output/tuning/<pref>/activity/`
2. **10 configs** are generated: `config_000` (baseline defaults) + 9 Latin Hypercube samples across 7 parameters
3. **TripGenerator_WebAPI_refactor** runs 10 times, each with a different config overlay via `-Dconfig.file=`
4. Trip output is scored against PT survey targets for **all** target cities in the prefecture
5. Candidates are ranked by population-weighted average loss

### Scoring

- Uses **trip-level representative mode** (`rep_mode` column) — each `(person_id, trip_id)` counts once
- **Placeholder NOT_DEFINED** (zero-distance trips) excluded from denominator
- Loss = weighted sum of squared errors: CAR(3.0), TRAIN(2.0), BUS(2.0), BICYCLE(1.0), WALK(1.0)
- Loss is population-weighted across all target cities in the prefecture

### Stage 2: Validation (not yet implemented)

Top 3-4 candidates from Stage 1, expanded to all target cities at 2x sample size.

## Parameters

| Parameter | Config key | Range | Default |
|-----------|-----------|-------|---------|
| Time fare | `fare.per.hour` | 500–2000 | 1000 |
| Initial fare | `fare.init` | 100–500 | 200 |
| Distance fare | `fare.per.kilometer` | 5–30 | 10 |
| Walk fatigue | `fatigue.walk` | 1.0–3.0 | 1.5 |
| Bicycle fatigue | `fatigue.bicycle` | 1.0–2.0 | 1.2 |
| Transfer penalty | `api.transit.transferPenalty` | 0–300 | 0 |
| Min transit dist | `min.transit.distance` | 500–1500 | 1000 |

Search space defined in `config/tuning/transport_search_space.yaml`.

## Output Layout

```
output/tuning/22/
  configs/                     # Generated .properties files
    config_000.properties      # Baseline
    config_001.properties      # LHS sample 1
    ...
  activity/22/                 # Shared activity output
  stage1/
    000/trip/22/               # Baseline trip output
    001/trip/22/               # Candidate 001 trip output
    ...
  scores/
    score_000_s1.json          # Per-candidate score
    ranking_s1.json            # Sorted ranking
  logs/
    activity.log
    trip_000.log
    ...
  report.md                    # Summary report
```

## Individual Scripts

| Script | Purpose |
|--------|---------|
| `city_align.py` | Resolve target city codes to generated file paths |
| `transport_score.py` | Score one run's trips against targets |
| `lhs.py` | Generate LHS parameter configs |
| `transport_tune.py` | Main orchestrator |
| `transport_report.py` | Generate summary report |

### Standalone scoring

```bash
# Score any trip directory against targets
python transport_score.py ../../data/tuning/transport_share_targets.csv \
    /tmp/pflow_staging_tripid/trip/22 22

# JSON output
python transport_score.py ... --json
```

### Standalone LHS generation

```bash
python lhs.py ../../config/tuning/transport_search_space.yaml \
    ../../output/tuning/22/configs --n 9 --seed 42
```

## Baseline Reference

Pref-22 staging baseline (weighted avg loss: **497.6**):

| Mode | 22100 Target | 22100 Gen | 22211 Target | 22211 Gen |
|------|-------------|-----------|-------------|-----------|
| CAR | 56.1% | 66.0% | 74.6% | 57.7% |
| TRAIN | 6.4% | 2.7% | 2.7% | 0.0% |
| BUS | 2.6% | 2.8% | 0.1% | 0.0% |
| BICYCLE | 14.2% | 14.1% | 6.9% | 20.1% |
| WALK | 20.7% | 14.4% | 15.8% | 22.1% |
