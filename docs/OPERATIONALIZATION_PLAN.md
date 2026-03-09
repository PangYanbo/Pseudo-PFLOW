# Operationalization Plan

**Date**: 2026-03-09
**Scope**: Configuration redesign, validation framework, cleanup, documentation

---

## A. Configuration Redesign

### A1. Layered Config Structure

```
src/main/resources/
  config.properties          ← base defaults (committed)
  config.local.properties    ← machine-specific overrides (gitignored)
```

**Loading order** (in Java, applied by each main class):
1. Load `config.properties` from classpath (base defaults)
2. If `config.local.properties` exists on classpath, overlay it (overrides any key)
3. If system property `-Dconfig.file=/path/to/file.properties` is set, overlay that too
4. Command-line args override everything (prefecture, mfactor, etc.)

**Implementation**: One static utility method `ConfigLoader.load()` returns a merged `Properties`. All main classes call it instead of their own `loadProperties()` / inline loading. ~30 lines of code.

### A2. Parameters to Externalize

**Trip generation cost model** (currently hardcoded constants in `TripGenerator_WebAPI_refactor`):

| Constant | Current Value | Proposed Config Key | Notes |
|----------|--------------|-------------------|-------|
| `MIN_TRANSIT_DISTANCE` | 1000 | `trip.minTransitDistance` | meters |
| `FARE_PER_KILOMETER` | 10 | `trip.farePerKm` | yen |
| `FARE_PER_HOUR` | 1000 | `trip.farePerHour` | yen |
| `FATIGUE_INDEX_WALK` | 1.5 | `trip.fatigueWalk` | multiplier |
| `FATIGUE_INDEX_BICYCLE` | 1.2 | `trip.fatigueBicycle` | multiplier |
| `FARE_INIT` | 200 | `trip.fareInit` | yen |
| `CAR_AVAILABILITY` | 0.4 | `trip.carAvailability` | probability |

**Activity generation** (currently hardcoded in `AbstractActivityGenerator`):

| Constant | Current Value | Proposed Config Key |
|----------|--------------|-------------------|
| `MAX_SEARCH_DISTANDE` | 20000 | `activity.maxSearchDistance` |
| `TRAIN_SERVICE_START_TIME` | 18000 (5h) | `activity.trainServiceStart` |
| `timeInterval` | 900 (15min) | `activity.timeInterval` |

**Student-specific**:

| Constant | Current Value | Proposed Config Key |
|----------|--------------|-------------------|
| `SCHOOL_MAX_DISTANCE` | 5000 | `activity.schoolMaxDistance` |

**Trajectory**:

| Constant | Current Value | Proposed Config Key |
|----------|--------------|-------------------|
| `MAX_WALK_DISTANCE` | 3000 | `trajectory.maxWalkDistance` |
| `MAX_SEARCH_STATAION_DISTANCE` | 5000 | `trajectory.maxSearchStationDistance` |

**WebAPI date** (currently hardcoded `"20240401"`):

| Constant | Current Value | Proposed Config Key |
|----------|--------------|-------------------|
| `AppDate` | "20240401" | `api.appDate` |

**Thread count** (currently `Runtime.getRuntime().availableProcessors()`):

| Constant | Current Value | Proposed Config Key |
|----------|--------------|-------------------|
| `numThreads` | auto | `numThreads` (optional; 0 = auto) |

### A3. Command-Line Interface

Standardize across all mainline entry points:

```
java -cp ... pseudo.gen.ActivityGenerator <prefCode> [mfactor]
java -cp ... pseudo.gen.TripGenerator <prefCode>
java -cp ... pseudo.gen.TripGenerator_WebAPI_refactor <prefCode>
java -cp ... pseudo.gen.TrajectoryGenerator <prefCode>
```

Where `<prefCode>` is 1-47 (required), and `mfactor` defaults to 1.

Currently only `ActivityGenerator` accepts args. The others hardcode `start=22; end=22`. Fix: add the same `args` parsing pattern to `TripGenerator`, `TripGenerator_WebAPI_refactor`, and `TrajectoryGenerator`.

### A4. Example `config.local.properties` (gitignored)

```properties
# Machine-specific overrides — do not commit
root=/data/PseudoPFLOW/
inputDir=/data/PseudoPFLOW/processing/
outputDir=/data/PseudoPFLOW/output/

# Override cost model for calibration
trip.farePerKm=25
trip.fatigueWalk=2.0

# Override thread count
numThreads=8
```

### A5. Config Files Summary

| File | Location | Committed | Purpose |
|------|----------|-----------|---------|
| `config.properties` | `src/main/resources/` | Yes | Base defaults for all machines |
| `config.local.properties` | `src/main/resources/` | No (gitignored) | Machine-specific overrides |
| `config.local.properties.example` | `src/main/resources/` | Yes | Template showing available override keys |

---

## B. Validation Framework

### B1. Script Structure

```
scripts/
  validate.sh                     ← entry point: runs all checks for a prefecture
  validate_activity.py            ← activity file checks
  validate_trip.py                ← trip file checks
  validate_trajectory.py          ← trajectory file checks
  validate_summary.py             ← aggregates per-file results into per-prefecture report
  requirements.txt                ← python deps (pandas only)
```

**Why Python?** CSV parsing, statistical checks, and report generation are 10x faster to write in Python than Java. Pandas is the only dependency. These are offline validation scripts, not part of the pipeline.

**Invocation**:
```bash
./scripts/validate.sh /data/PseudoPFLOW 22
# or validate all:
./scripts/validate.sh /data/PseudoPFLOW all
```

### B2. Checks

#### Activity Checks (`validate_activity.py`)

| Check | Logic | Severity |
|-------|-------|----------|
| **File completeness** | Every city file in `agent/{pref}/` has a corresponding file in `activity/{pref}/` | ERROR |
| **Row count** | Each activity file has > 0 rows | ERROR |
| **Column count** | Each row has >= 10 columns | ERROR |
| **Person ID continuity** | No gaps in person IDs within a file | WARN |
| **Activity count per person** | Each person has >= 1 activity | ERROR |
| **Time ordering** | Activities per person have non-decreasing start times | ERROR |
| **Time range** | All start times in [0, 86400] seconds | WARN |
| **Purpose distribution** | HOME/OFFICE/SCHOOL/FREE proportions within expected bounds | WARN |
| **Location sanity** | Lon in [122, 154], Lat in [24, 46] (Japan bounding box) | ERROR |

#### Trip Checks (`validate_trip.py`)

| Check | Logic | Severity |
|-------|-------|----------|
| **File completeness** | Every activity file has a corresponding trip file | ERROR |
| **Row count** | Each trip file has > 0 rows | ERROR |
| **Column count** | >= 9 columns per row | ERROR |
| **Mode distribution** | Transport mode ratios: walk/bike/car/train/mix/bus | INFO |
| **Mode code validity** | Mode ID in {0,1,2,3,4,5,6,7} | ERROR |
| **Trip count vs activity count** | trips >= activities - 1 (per person) | WARN |
| **Departure time ordering** | Trips per person have non-decreasing depTime | WARN |
| **OD distance** | Origin != Destination for non-NOT_DEFINED trips | WARN |

#### Trajectory Checks (`validate_trajectory.py`)

| Check | Logic | Severity |
|-------|-------|----------|
| **File completeness** | Every trip file has a corresponding trajectory file | ERROR |
| **Row count** | > 0 rows | ERROR |
| **Column count** | >= 8 columns | ERROR |
| **Timestamp ordering** | Timestamps per person are non-decreasing | ERROR |
| **Timestamp range** | All timestamps within expected date window | WARN |
| **Spatial jump detection** | Distance between consecutive points < 50km (configurable) | WARN |
| **Speed sanity** | Implied speed between consecutive points < 300 km/h | WARN |
| **Duplicate timestamps** | Flag consecutive points with identical timestamp | WARN |
| **Coverage** | Every person in trip file appears in trajectory file | ERROR |

### B3. Report Output

Each validator produces a JSON file:

```json
{
  "prefecture": 22,
  "file": "person_22101.csv",
  "stage": "activity",
  "timestamp": "2026-03-09T10:00:00",
  "checks": [
    {"name": "row_count", "status": "PASS", "value": 145230},
    {"name": "time_ordering", "status": "FAIL", "count": 3, "details": "persons: [1002, 5431, 8820]"},
    {"name": "mode_distribution", "status": "INFO", "value": {"WALK": 0.32, "CAR": 0.41, ...}}
  ],
  "summary": {"errors": 0, "warnings": 1, "info": 1}
}
```

`validate_summary.py` reads all per-file JSONs and produces:
- `validation_report_{pref}.md` — human-readable Markdown
- `validation_report_{pref}.json` — machine-readable aggregate

---

## C. Cleanup and Archive Strategy

### C1. Move to `archive_local/` (gitignored)

| Item | Reason |
|------|--------|
| `pflowlib-src/` | Redundant backup; sources already in `src/jp/ac/ut/csis/pflow/` |
| `cfr-0.152.jar` | Decompiler tool; one-time use, not needed for builds |
| `lib/pflowlib.jar` | Backup reference JAR; superseded by source integration |
| `.idea/shelf/` | Old IntelliJ stashed changes (Oct/Dec 2025) |
| `src/scripts/Untitled*.ipynb` (4 files) | Generic unnamed notebooks |
| `src/scripts/.ipynb_checkpoints/` | Notebook autosave artifacts |
| `src/scripts/Kobe/` | Local analysis results |
| `src/scripts/feed_kobecity_*.zip` | GTFS transit data file |

### C2. Add to `.gitignore`

```
# Archive (local-only material, not pushed)
archive_local/

# Machine-specific config
src/main/resources/config.local.properties

# IntelliJ module file (already have *.iml rule, but DSPFlow.iml is committed)
DSPFlow.iml
```

Note: `DSPFlow.iml` is committed despite the `*.iml` gitignore rule. Need `git rm --cached DSPFlow.iml` to stop tracking it.

### C3. Move to `docs/`

| File | Current Location | New Location |
|------|-----------------|--------------|
| `REFRACTOR_STATUS_2026-03-06.md` | repo root | `docs/REFACTOR_STATUS_2026-03-06.md` |
| `AUDIT_REPORT_2026-03-07.md` | repo root | `docs/AUDIT_REPORT_2026-03-07.md` |
| `CHANGELOG.md` | repo root | stays (standard location) |
| `README.md` | repo root | stays (standard location) |
| `CLAUDE.md` | repo root | stays (standard location) |

### C4. Notebook Organization

Keep meaningful notebooks in `src/scripts/`, but rename/organize:

| Current | Action |
|---------|--------|
| `Evaluate_Activity.ipynb` | Keep — active evaluation tool |
| `SIP_PseudoPFLOW_Eval.ipynb` | Keep — main evaluation notebook |
| `Activity_Data_Completion.ipynb` | Keep — data completeness check |
| `AdminLevelCheck.ipynb` | Keep — geographic admin validation |
| `Merge_ActivityData.ipynb` | Keep (already deprecated) |
| `Merge_TripData.ipynb` | Keep — may still be useful |
| `Process_PTMaster.ipynb` | Keep — PT survey processing |
| `WebAPI_test.ipynb` | Keep — API testing |
| `SoftbankOD.ipynb` | Keep — OD data analysis |
| `Transfer_household.ipynb` | Keep — household data processing |
| `POI_filter.ipynb` | Keep — POI filtering |
| `Kobe.ipynb` | Keep — regional analysis |
| `Untitled*.ipynb` (4) | Archive to `archive_local/` |

---

## D. Guides

### D1. `docs/RUN_GUIDE.md`

Structure:
```
# Run Guide

## Prerequisites
- Java 11+ (tested with Temurin 21)
- Maven 3.6.3 (exact version required)
- Dataset files in {root}/processing/

## Configuration
- Base config: src/main/resources/config.properties
- Machine overrides: src/main/resources/config.local.properties
- API credentials: PFLOW_API_USER / PFLOW_API_PASS env vars

## Build
  mvn clean package -DskipTests

## Pipeline Steps (in order)

### Step 1: Person Generation
  mvn exec:java -Dexec.mainClass="pseudo.pre.PersonGenerator" -Dexec.args="22"

### Step 2: Activity Generation
  mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22"

### Step 3a: Trip Generation (offline)
  mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator" -Dexec.args="22"

### Step 3b: Trip + Trajectory Generation (WebAPI)
  mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="22"

### Step 4: Trajectory Generation (offline, if using Step 3a)
  mvn exec:java -Dexec.mainClass="pseudo.gen.TrajectoryGenerator" -Dexec.args="22"

### Step 5: File Joining
  mvn exec:java -Dexec.mainClass="pseudo.gen.FileJoinner" -Dexec.args="22"

### Step 6-7: Aggregation
  mvn exec:java -Dexec.mainClass="pseudo.aggr.MeshVolumeCalculator"
  mvn exec:java -Dexec.mainClass="pseudo.aggr.LinkVolumeCalculator"

## Running Multiple Prefectures in Parallel
  # On machine A:
  mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="1"
  # On machine B:
  mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22"

## Output Directory Structure
  {outputDir}/
    agent/{pref}/person_{city}.csv
    activity/{pref}/person_{city}.csv
    trip/{pref}/trip_{city}.csv
    trajectory/{pref}/trajectory_{city}.csv

## Troubleshooting
  (common errors and their solutions)
```

### D2. `docs/VALIDATION_GUIDE.md`

Structure:
```
# Validation Guide

## Quick Start
  ./scripts/validate.sh /data/PseudoPFLOW 22

## What Gets Checked
  - Activity: file completeness, row counts, time ordering, location sanity
  - Trip: mode distribution, trip-activity count alignment, departure ordering
  - Trajectory: timestamp ordering, spatial jumps, speed sanity, coverage

## Reading Reports
  - JSON: validation_report_{pref}.json
  - Markdown: validation_report_{pref}.md

## Check Severities
  - ERROR: data is corrupt or missing — must fix before downstream steps
  - WARN: data is suspicious — review manually
  - INFO: statistics for monitoring (mode ratios, counts)

## Expected Ranges (per prefecture)
  (table of expected activity/trip/trajectory counts from pref-22 baseline)
```

---

## E. Implementation Roadmap

### Phase 1: Config Infrastructure (1 commit)
1. Create `utils/ConfigLoader.java` (~30 lines)
2. Create `config.local.properties.example`
3. Add `config.local.properties` to `.gitignore`
4. Update all mainline `main()` methods to use `ConfigLoader.load()` instead of inline loading
5. Add `args` parsing to `TripGenerator`, `TripGenerator_WebAPI_refactor`, `TrajectoryGenerator` (same pattern as `ActivityGenerator`)
6. Compile + verify existing smoke test still works

### Phase 2: Externalize Constants (1 commit)
1. Add default values for all cost/distance constants to `config.properties`
2. Read them via `ConfigLoader` with hardcoded fallbacks in each class
3. Pattern: `double farePerKm = Double.parseDouble(prop.getProperty("trip.farePerKm", "10"));`
4. Compile + verify no behavioral change (defaults match current hardcoded values)

### Phase 3: Validation Scripts (1 commit)
1. Create `scripts/` directory at repo root
2. Write `validate_activity.py`, `validate_trip.py`, `validate_trajectory.py`
3. Write `validate_summary.py` and `validate.sh`
4. Write `requirements.txt` (pandas)
5. Run against pref-22 smoke output to verify

### Phase 4: Cleanup + Archive (1 commit)
1. Create `archive_local/`
2. Move items per C1
3. `git rm --cached DSPFlow.iml`
4. Update `.gitignore` per C2
5. Move root `.md` files to `docs/` per C3
6. Verify `git status` shows only intended changes

### Phase 5: Documentation (1 commit)
1. Write `docs/RUN_GUIDE.md`
2. Write `docs/VALIDATION_GUIDE.md`
3. Update `CLAUDE.md` to reference them

### Execution Order

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
  config     constants   validation   cleanup    docs
```

Phases 1-2 are sequential (2 depends on 1).
Phases 3-5 are independent of each other but depend on 1-2 being done.
Total: 5 commits.

---

## Proposed Target Directory Structure

```
Pseudo-PFLOW/
  .gitignore                          (updated)
  pom.xml
  README.md
  CLAUDE.md
  CHANGELOG.md
  docs/
    RUN_GUIDE.md                      (NEW)
    VALIDATION_GUIDE.md               (NEW)
    ACTIVITY_GENERATION_REFACTOR.md
    TRIP_GENERATION_ARCHITECTURE.md
    TRIPGEN_WEBAPI_REFACTOR_AUDIT.md
    OPERATIONALIZATION_PLAN.md        (this file)
    branch-policy.md
    team-dev-flow.md
    REFACTOR_STATUS_2026-03-06.md     (moved from root)
    AUDIT_REPORT_2026-03-07.md        (moved from root)
  scripts/
    validate.sh                       (NEW)
    validate_activity.py              (NEW)
    validate_trip.py                  (NEW)
    validate_trajectory.py            (NEW)
    validate_summary.py               (NEW)
    requirements.txt                  (NEW)
  src/
    main/resources/
      config.properties               (updated with new keys)
      config.local.properties.example  (NEW)
    pseudo/
      gen/                            (no structural changes)
    utils/
      ConfigLoader.java               (NEW)
  src/scripts/                        (existing notebooks, cleaned up)
  archive_local/                      (NEW, gitignored)
    pflowlib-src/
    cfr-0.152.jar
    lib/pflowlib.jar
    Untitled*.ipynb
```

---

## Files Changed/Created Summary

| Action | Path | Phase |
|--------|------|-------|
| NEW | `src/utils/ConfigLoader.java` | 1 |
| NEW | `src/main/resources/config.local.properties.example` | 1 |
| EDIT | `src/main/resources/config.properties` | 2 |
| EDIT | `.gitignore` | 1+4 |
| EDIT | `src/pseudo/gen/TripGenerator.java` (args parsing) | 1 |
| EDIT | `src/pseudo/gen/TripGenerator_WebAPI_refactor.java` (args + config) | 1+2 |
| EDIT | `src/pseudo/gen/TrajectoryGenerator.java` (args parsing) | 1 |
| EDIT | `src/pseudo/gen/ActivityGenerator.java` (ConfigLoader) | 1 |
| EDIT | `src/pseudo/gen/AbstractActivityGenerator.java` (config) | 2 |
| EDIT | `src/pseudo/gen/StudentActivityGenerator.java` (config) | 2 |
| NEW | `scripts/validate.sh` | 3 |
| NEW | `scripts/validate_activity.py` | 3 |
| NEW | `scripts/validate_trip.py` | 3 |
| NEW | `scripts/validate_trajectory.py` | 3 |
| NEW | `scripts/validate_summary.py` | 3 |
| NEW | `scripts/requirements.txt` | 3 |
| MOVE | `REFRACTOR_STATUS_2026-03-06.md` → `docs/` | 4 |
| MOVE | `AUDIT_REPORT_2026-03-07.md` → `docs/` | 4 |
| MOVE | `pflowlib-src/`, `cfr-0.152.jar`, `lib/pflowlib.jar` → `archive_local/` | 4 |
| DELETE (cached) | `DSPFlow.iml` | 4 |
| NEW | `docs/RUN_GUIDE.md` | 5 |
| NEW | `docs/VALIDATION_GUIDE.md` | 5 |
| EDIT | `CLAUDE.md` | 5 |
