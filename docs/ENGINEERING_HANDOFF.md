# Engineering Handoff Guide

Deployment and operation instructions for the Pseudo-PFLOW pipeline on Windows servers.

**Handoff version**: `v0.5-windows-handoff`
**Date**: 2026-03-17

## What this version includes

| Feature | Status |
|---------|--------|
| Activity generation (commuter + non-commuter + student) | Validated pref 22 |
| WebAPI trip + trajectory generation | Validated pref 22 |
| Session auto-refresh (15 min proactive, retry on expiry) | Validated 52-min run |
| Transit stop precheck (reduces unnecessary API calls) | Enabled by default |
| GetMixedRoute response cache | Enabled (automatic) |
| Fail-fast on API unavailability | RuntimeException + clear log |
| Validation suite (activity, trip, trajectory) | Python scripts |
| PowerShell scripts for Windows operation | New |

## Prerequisites

### Java 11+

Download and install [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21&os=windows).

Verify:
```powershell
java -version
# Expected: openjdk version "21.x.x" or similar 11+ version
```

Set `JAVA_HOME` if Maven can't find Java:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
```

### Maven 3.6.3

**Must be exactly version 3.6.3.** Versions 3.8+ are incompatible with this project's POM.

Download: https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip

1. Extract to `C:\apache-maven-3.6.3`
2. Add `C:\apache-maven-3.6.3\bin` to your `PATH`
3. Verify:
```powershell
mvn --version
# Expected: Apache Maven 3.6.3
```

### Python 3.8+

Required only for validation scripts. Download from https://www.python.org/downloads/

Verify:
```powershell
python --version
# Expected: Python 3.8+ (no additional packages needed)
```

### Dataset

Download the processing dataset from `S3://pseudo-pflow/processing` and extract to `C:\Pseudo-PFLOW\data\`.

Expected structure:
```
C:\Pseudo-PFLOW\data\
  processing\
    bus_stop\           P11-22_XX_GML.zip (bus stop data per prefecture)
    railway\            base_station.csv
    facility\           various CSV files
    person\             person CSVs per prefecture
    markov\             calibration datasets
    ...
  agent\                agent data files
```

## Setup

### 1. Clone the repository

```powershell
git clone <repo-url>
cd Pseudo-PFLOW
git checkout v0.5-windows-handoff
```

### 2. Set API credentials

Set these as **Windows SYSTEM environment variables** (persists across sessions and reboots):

1. Open **System Properties** > **Advanced** > **Environment Variables**
2. Under **System variables** (not User variables), click **New** and add:
   - Variable: `PFLOW_API_USER`, Value: your API username
   - Variable: `PFLOW_API_PASS`, Value: your API password
3. Click **OK** to save, then **restart all PowerShell windows**

To verify they are set:
```powershell
echo $env:PFLOW_API_USER
# Should print your username
```

The pipeline fails fast with a clear error if these are missing or blank.

### 3. Configure local paths

```powershell
Copy-Item src\main\resources\config.local.properties.windows.example `
          src\main\resources\config.local.properties
```

Edit `config.local.properties` with your local paths. The default template already points to:
```properties
root=C:/Pseudo-PFLOW/data/
inputDir=C:/Pseudo-PFLOW/data/processing/
outputDir=C:/pflow_output/
```

Adjust if your data is in a different location.

**Important**: Use forward slashes (`/`) in paths, not backslashes.

### 4. Build

```powershell
mvn clean package -DskipTests
```

Expected output: `BUILD SUCCESS` and `target\DSPFlow-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

If the build fails, check:
- Maven version is exactly 3.6.3
- Java version is 11+
- Network access to Maven Central and OSGeo repositories

## Tuning vs generation (conditional workflow)

The pipeline has two separate phases:

1. **Tuning** — produces per-target-city parameter files in `config/tuning/param_groups/`. Uses LHS sampling and WebAPI scoring against PT survey targets.
2. **Generation** — reads `data/tuning/city_code_to_param_group.csv` plus the tuned `.properties` files and produces trip + trajectory output.

**On a production VM**, generation must NOT run tuning on the fly. Use the conditional orchestrator:

```powershell
.\scripts\windows\run_pref_with_tuning.ps1 <pref_code>
```

Logic:
- If all required param groups for the prefecture exist → skip tuning, generate directly
- If LOCAL param groups are missing (this prefecture has tunable target cities) → run tuning, then generate
- If only EXTERNAL param groups are missing (inherited from another VM) → halt with clear message instructing the engineer to copy the missing files
- If any city is unmapped in `city_code_to_param_group.csv` → halt before any work

### Files required to skip tuning

For prefecture N (zero-padded as `NN`), generation can run directly when all of these exist:

| File | Required for |
|------|--------------|
| `data/tuning/city_code_to_param_group.csv` | Always |
| `config/tuning/param_groups/<group>.properties` | Every unique `param_group` referenced by cities in pref N |

To check readiness without running anything:
```powershell
.\scripts\windows\check_param_groups.ps1 <pref_code>
```

Exit codes: `0` = complete, `1` = some `.properties` missing, `2` = unmapped cities, `3` = config error.

## Running the pipeline

### Quick start: single prefecture

```powershell
# Conditional run (recommended for production VMs)
.\scripts\windows\run_pref_with_tuning.ps1 22

# Direct generation only (assumes all params already in place)
.\scripts\windows\run_pref.ps1 22
```

Both run activity generation, trip+trajectory generation (WebAPI), and validation. Output goes to `C:\Pseudo-PFLOW\output\pref_22\`.

### Step-by-step execution

```powershell
# Step 1: Activity generation
.\scripts\windows\run_activity.ps1 22

# Step 2: Trip + trajectory (WebAPI)
.\scripts\windows\run_trip_webapi.ps1 22

# Step 3: Validation
.\scripts\windows\run_validate.ps1 22 C:\pflow_output
```

### Batch run: multiple prefectures

```powershell
# Default prefectures (22, 13, 26) at 0.5% sample
.\scripts\windows\run_batch.ps1

# Custom prefectures and sample rate
.\scripts\windows\run_batch.ps1 -PrefCodes 22, 13, 26, 14 -MFactor 100
```

### Check progress

While a run is in progress (in another PowerShell window):
```powershell
.\scripts\windows\check_status.ps1 22
```

Reports file counts, log tail, mode share, and validation status.

### Sampling rates

| MFactor | Sample | Persons (pref 22) | Approx. time |
|---------|--------|--------------------|--------------|
| 200 | 0.5% | ~18,000 | 10-20 min |
| 100 | 1% | ~36,000 | 20-40 min |
| 50 | 2% | ~72,000 | 40-80 min |
| 1 | 100% | ~3.6M | Hours |

Times depend on API response speed (~30-50s per transit query) and number of transit-eligible trips.

## Configuration reference

See [CONFIGURATION.md](CONFIGURATION.md) for the full property reference including:
- Path configuration
- API parameters (transportCode, maxRadius, maxRoutes, appDate)
- Model parameters (walk distance, fare model, fatigue factors)
- 4-layer config precedence system

### Key API parameters

| Property | Default | Notes |
|----------|---------|-------|
| `api.transportCode` | `3` | 3=bus-oriented, 1=train-oriented |
| `api.maxRadius` | `1000` | Station search radius (meters) |
| `api.maxRoutes` | `6` | Max route alternatives |
| `api.sessionRefreshMinutes` | `15` | Session refresh interval (API expires at ~19 min) |
| `api.precheckEnabled` | `true` | Transit stop reachability precheck |
| `trajectory.baseYear` | `2020` | Calendar year for trajectory output timestamps |

## Output structure

After a run for prefecture 22:
```
C:\Pseudo-PFLOW\output\pref_22\
  activity\22\         activity_XXXXX.csv (one per city)
  trip\22\             trip_XXXXX.csv (one per city)
  trajectory\22\       trajectory_XXXXX.csv (one per city)
  validation\22\       activity.json, trip.json, trajectory.json, summary.md
  logs\                activity.log, trip.log
```

## Validation

See [VALIDATION_GUIDE.md](VALIDATION_GUIDE.md) for detailed check rules and thresholds.

Quick validation after a run:
```powershell
.\scripts\windows\run_validate.ps1 22 C:\Pseudo-PFLOW\output\pref_22
```

### Expected results

**Activity**: All files should PASS. Purpose mix: HOME ~55%, FREE ~15%, OFFICE ~11%.

**Trip**: WARN is normal (placeholder NOT_DEFINED for stay-at-home persons, ~5–12%). Zero unexpected NOT_DEFINED is good.

**IMPORTANT — two mode share metrics**: The validation summary reports `[PRIMARY]` trip-level mode share (via `rep_mode`) and `[SECONDARY]` subtrip-level distribution. **Use the `[PRIMARY]` table when comparing to tuning targets or judging realism** — it is the only metric directly comparable to `transport_share_targets.csv`. The `[SECONDARY]` table inflates WALK/transit share because multi-leg transit trips are counted as several rows. See [VALIDATION_GUIDE.md](VALIDATION_GUIDE.md#mode-share-metrics-primary-vs-secondary) for the full explanation.

Expected **trip-level** mode share for pref 22: CAR ~57%, WALK ~23%, BICYCLE ~14%, BUS <1%, TRAIN <1% (under current tuned parameters).

**Trajectory**: Trajectory timestamps should show year 2020 (configured by `trajectory.baseYear`).

### Trajectory validation acceptance rules

| Check | Classification | Action |
|-------|---------------|--------|
| Placeholder NOT_DEFINED (zero-distance) | Non-blocking | Expected artifact (~12%) |
| Timestamp monotonicity: boundary effects (<0.5%) | Non-blocking | Minor cosmetic |
| Timestamp monotonicity: same-mode within-trip (>1%) | Warning | Investigate if >5% |
| Spatial jumps >50km (>1% of files) | **Blocking** | Routing failure |
| Speed >500 km/h (>1% of files) | **Blocking** | Routing failure |
| Duplicate rows (>5%) | **Blocking** | Generation bug |
| Year in trajectory != 2020 | **Blocking** | Config fix needed |

**Engineer rule**: If the validation summary shows 0 FAIL files, the run is PASS. WARN files from placeholder NOT_DEFINED are expected and non-blocking. Any FAIL file requires checking the specific error type against the table above.

## Troubleshooting

### "PFLOW_API_USER and PFLOW_API_PASS must be set"
Set the environment variables as described in Setup step 2. Restart PowerShell after setting system variables.

### Build fails with "No plugin found for prefix 'exec'"
Maven version mismatch. Must be exactly 3.6.3.

### "API FAILURE: GetMixedRoute returned non-200 even after session refresh"
The API is unavailable or credentials are invalid. Check:
1. API credentials are correct
2. Network can reach `pflow-api.csis.u-tokyo.ac.jp`
3. No other process is consuming the API session (one user = one session at a time)

### "Empty body" or "all lines failed JSON parse"
API returned an error page instead of route data. Usually a temporary issue. Retry the run.

### Very slow trip generation
Normal for transit-heavy prefectures. Each transit API query takes 30-50 seconds. The pipeline logs progress per city file. Use `check_status.ps1` to monitor.

### Activity files have very few rows (~399 lines)
Markov chain data parsing issue. Should not occur in this version (fixed in Phase B). If it happens, check that `inputDir` points to the correct dataset with `markov/` subdirectories.

### Path errors: "The system cannot find the path specified"
Use forward slashes in `config.local.properties`. Java Properties does not expand Windows backslashes correctly.

## Multi-machine deployment

To split prefectures across multiple Windows servers:

1. Clone the repo and complete setup on each machine
2. Each machine creates its own `config.local.properties` with local paths
3. Assign prefecture ranges:

```powershell
# Machine A: prefectures 1-15
.\scripts\windows\run_batch.ps1 -PrefCodes (1..15) -MFactor 200

# Machine B: prefectures 16-30
.\scripts\windows\run_batch.ps1 -PrefCodes (16..30) -MFactor 200

# Machine C: prefectures 31-47
.\scripts\windows\run_batch.ps1 -PrefCodes (31..47) -MFactor 200
```

4. Validate on each machine:
```powershell
foreach ($p in 1..15) {
    .\scripts\windows\run_validate.ps1 $p C:\Pseudo-PFLOW\output\pref_$p
}
```

5. Collect outputs from all machines for aggregation.

## First-run checklist

1. [ ] Java 11+ installed, `java -version` works
2. [ ] Maven 3.6.3 installed, `mvn --version` shows 3.6.3
3. [ ] Python 3.8+ installed (for validation)
4. [ ] Dataset extracted to `C:\Pseudo-PFLOW\data\`
5. [ ] Repository cloned and checked out to `v0.5-windows-handoff`
6. [ ] `PFLOW_API_USER` and `PFLOW_API_PASS` set as **Windows SYSTEM** environment variables
7. [ ] `config.local.properties` created with correct paths (forward slashes)
8. [ ] `mvn clean package -DskipTests` succeeds

A small-sample smoke test (pref 22, mfactor=200) will be run by the project lead to verify the deployment before full-scale runs are assigned.
