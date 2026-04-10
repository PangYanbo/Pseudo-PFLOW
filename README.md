# Pseudo-PFLOW

Pseudo-PFLOW generates synthetic population flow data for Japan — a full pipeline that simulates daily mobility of synthetic agents derived from census household data, producing per-person activity sequences, trip mode choices, trajectories, and aggregated spatial statistics.

## Prerequisites

- **Java**: 11+ (tested with Eclipse Temurin 21)
- **Maven**: 3.6.3 exactly (versions 3.8+ are incompatible)
- **Python**: 3.8+ (for validation scripts only; no extra packages needed)
- **Dataset**: Download from `S3://pseudo-pflow/processing`

### pflowlib sources

The `jp.ac.ut.csis.pflow.*` routing, geometry, and mesh library (previously distributed as `lib/pflowlib.jar`) is compiled from source at `src/jp/ac/ut/csis/pflow/`. No manual JAR installation is needed — `mvn compile` picks up the sources automatically.

## Build

```bash
mvn clean package -DskipTests
```

Produces `target/DSPFlow-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

## Configuration

### Layered config system

Configuration is loaded with 4-layer precedence (later wins):

| Priority | File | Committed |
|----------|------|-----------|
| 1 (base) | `src/main/resources/config.properties` | Yes |
| 2 (pref) | `src/main/resources/config.pref.<N>.properties` | Yes |
| 3 (local) | `src/main/resources/config.local.properties` | No (gitignored) |
| 4 (external) | any path via `-Dconfig.file=` | No |

Create a machine-local override:

```bash
# Linux
cp src/main/resources/config.local.properties.example src/main/resources/config.local.properties

# Windows
Copy-Item src\main\resources\config.local.properties.windows.example src\main\resources\config.local.properties
```

Edit with your local paths (use forward slashes on Windows):

```properties
root=C:/Pseudo-PFLOW/data/
inputDir=C:/Pseudo-PFLOW/data/processing/
outputDir=C:/pflow_output/
```

See [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for the full property reference.

### API credentials

The WebAPI pipeline requires CSIS routing API credentials. Set as environment variables (never in config files):

```bash
# Linux
export PFLOW_API_USER=your_user
export PFLOW_API_PASS=your_pass
```

```powershell
# Windows (set as System environment variables via System Properties > Environment Variables)
$env:PFLOW_API_USER = "your_user"
$env:PFLOW_API_PASS = "your_pass"
```

The pipeline fails fast with a clear error if these are missing.

## Pipeline

### Mainline pipeline (WebAPI)

The current production pipeline uses the CSIS WebAPI for transit and road routing:

| Step | Entry point | Description |
|------|-------------|-------------|
| 1 | `pseudo.pre.PersonGenerator` | Household CSV → Person CSV |
| 2 | `pseudo.gen.ActivityGenerator` | Census + Markov + MNL → Activity CSV (all labor types) |
| 3 | `pseudo.gen.TripGenerator_WebAPI_refactor` | Activity → Trip + Trajectory CSV (WebAPI routing) |
| 4 | `pseudo.gen.FileJoinner` | Trajectory CSV → ZIP per city |
| 5 | `pseudo.aggr.MeshVolumeCalculator` | Trajectory → 500m mesh population per 10 min |
| 6 | `pseudo.aggr.LinkVolumeCalculator` | Trajectory → Link volume per hour |

Step 3 creates a WebAPI session per prefecture, routes each trip through the CSIS road/transit network, and produces both trip and trajectory data in a single pass. Features include:
- Automatic session refresh (configurable interval, default 15 min)
- Transit stop reachability precheck (reduces unnecessary API calls)
- GetMixedRoute response cache
- Fail-fast on API unavailability with clear error messages

### Legacy pipeline (offline)

The original offline pipeline is retained for environments without WebAPI access:

| Step | Entry point | Description |
|------|-------------|-------------|
| 3a | `pseudo.gen.TripGenerator` | Activity → Trip CSV (local mode choice) |
| 3b | `pseudo.gen.TrajectoryGenerator` | Trip CSV + DRM road network → Trajectory CSV |

Steps 1-2 and 4-6 are shared with the mainline pipeline.

### Running

```bash
# Single prefecture (mainline)
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22"
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="22"

# With sampling (mfactor=200 → 0.5% sample)
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22 200"
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="22 200"
```

See [docs/RUN_GUIDE.md](docs/RUN_GUIDE.md) for step-by-step instructions and troubleshooting.

## Output structure

After a pipeline run for prefecture 22:

```
<outputDir>/
  activity/22/         activity_XXXXX.csv  (one per city)
  trip/22/             trip_XXXXX.csv
  trajectory/22/       trajectory_XXXXX.csv
  validation/22/       activity.json, trip.json, trajectory.json, summary.md
  logs/                activity.log, trip.log
  manifest.json        run metadata (prefecture, mfactor, file counts, timestamp)
```

## Validation

The validation framework checks activity, trip, and trajectory outputs for correctness:

```bash
# Linux
scripts/validate/run_validation.sh 22 /path/to/outputDir

# Windows
.\scripts\windows\run_validate.ps1 22 C:\Pseudo-PFLOW\output\pref_22
```

Produces per-file JSON reports and an aggregate Markdown summary. See [docs/VALIDATION_GUIDE.md](docs/VALIDATION_GUIDE.md) for check rules, thresholds, and baseline interpretation.

## Windows deployment

PowerShell scripts are provided under `scripts/windows/` for operating the pipeline on Windows servers:

| Script | Purpose |
|--------|---------|
| `run_pref_with_tuning.ps1` | Conditional orchestrator: tune missing param groups (if any), then generate |
| `check_param_groups.ps1` | Precheck whether param groups for a prefecture are ready |
| `run_pref.ps1` | Full generation pipeline for one prefecture (assumes params ready) |
| `run_batch.ps1` | Multi-prefecture sequential batch |
| `run_activity.ps1` | Activity generation only |
| `run_trip_webapi.ps1` | WebAPI trip + trajectory only |
| `run_validate.ps1` | Validation suite |
| `check_status.ps1` | Monitor running/completed runs |

See [docs/ENGINEERING_HANDOFF.md](docs/ENGINEERING_HANDOFF.md) for the complete Windows deployment guide.

## Transport tuning

A tuning framework for calibrating transport mode choice parameters against PT survey targets is available under `scripts/tuning/`. Uses Latin Hypercube Sampling over 7 model parameters, scored against prefecture-specific mode share targets.

See [docs/TUNING_GUIDE.md](docs/TUNING_GUIDE.md) for details.

## Project status

- Both legacy and mainline (WebAPI) pipelines have been smoke-tested on Linux (pref 22)
- API session management, fail-fast guards, and transit stop precheck are in place
- Validation framework operational for activity, trip, and trajectory outputs
- Windows deployment scripts and engineering handoff documentation prepared
- Stage 1 transport tuning completed for pref 22 (best loss 214.39 vs baseline 320.77)

## Documentation

| Document | Contents |
|----------|----------|
| [RUN_GUIDE.md](docs/RUN_GUIDE.md) | Step-by-step pipeline execution |
| [CONFIGURATION.md](docs/CONFIGURATION.md) | Layered config system and property reference |
| [VALIDATION_GUIDE.md](docs/VALIDATION_GUIDE.md) | Validator rules, thresholds, baselines |
| [ENGINEERING_HANDOFF.md](docs/ENGINEERING_HANDOFF.md) | Windows server deployment guide |
| [TUNING_GUIDE.md](docs/TUNING_GUIDE.md) | Transport mode calibration framework |
