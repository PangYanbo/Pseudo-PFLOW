# Run Guide

How to run the Pseudo-PFLOW mainline pipeline end-to-end and validate outputs.

## Prerequisites

- **Java**: 11+ (tested with Temurin 21)
- **Maven**: 3.6.3 exactly (3.8+ incompatible)
- **Python**: 3.8+ (for validation scripts)
- **Dataset**: Download from `S3://pseudo-pflow/processing` into a local directory
- **WebAPI credentials**: Set `PFLOW_API_USER` and `PFLOW_API_PASS` environment variables (required for step 3 WebAPI variant)

## Configuration

Edit `src/main/resources/config.properties`:

```properties
root=/path/to/PseudoPFLOW/
inputDir=/path/to/PseudoPFLOW/processing/
outputDir=/path/to/output/
```

For machine-specific overrides without modifying the tracked file, create `src/main/resources/config.local.properties` (gitignored):

```properties
root=/my/local/path/
outputDir=/tmp/pflow_output/
```

See `src/main/resources/config.local.properties.example` for a template.

### Prefecture-specific overrides

Create `src/main/resources/config.pref.<N>.properties` to override parameters for a single prefecture (e.g., `config.pref.22.properties`).

### Config precedence

`config.properties` < `config.pref.<N>.properties` < `config.local.properties` < `-Dconfig.file=<path>`

## Build

```bash
mvn clean package -DskipTests
```

This produces `target/DSPFlow-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

For all `mvn exec:java` commands below, set `JAVA_HOME` if needed:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
```

## Pipeline steps

All mainline generators accept a prefecture code as the first argument. Without arguments, they process all 47 prefectures.

### Step 1: Person generation

```bash
mvn exec:java -Dexec.mainClass="pseudo.pre.PersonGenerator"
```

**Note**: PersonGenerator has not yet been updated to use ConfigLoader. Paths are currently hardcoded in the source. Edit `PersonGenerator.java` line 214 before running.

**Input**: Household CSVs, labor/holiday/enrollment rate files under `inputDir`.
**Output**: Person CSVs under `root/person/`.

### Step 2: Activity generation

```bash
# Single prefecture
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22"

# All prefectures
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator"
```

Runs commuter, non-commuter, and student activity generation internally. Writes combined output to `outputDir/activity/<pref>/`.

For debugging individual labor types:

```bash
mvn exec:java -Dexec.mainClass="pseudo.gen.CommuterActivityGenerator" -Dexec.args="22"
mvn exec:java -Dexec.mainClass="pseudo.gen.NonCommuterActivityGenerator" -Dexec.args="22"
mvn exec:java -Dexec.mainClass="pseudo.gen.StudentActivityGenerator" -Dexec.args="22"
```

These write to `outputDir/activity_commuter/`, `activity_noncommuter/`, `activity_student/` respectively.

### Step 3: Trip generation (WebAPI mainline)

```bash
export PFLOW_API_USER=your_user
export PFLOW_API_PASS=your_pass

mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="22"
```

Reads from `outputDir/activity/<pref>/`, writes trips to `outputDir/trip/<pref>/` and trajectories to `outputDir/trajectory/<pref>/`.

This step creates a WebAPI session per prefecture, routes each trip through the CSIS road/transit network API, and produces both trip and trajectory data in a single pass. Transit routing uses `api.transportCode=3` (bus-oriented) by default; set to `1` for train-oriented routing. See `docs/CONFIGURATION.md` for all API parameters.

**Legacy offline alternative** (no WebAPI needed):

```bash
# Trip generation only
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator" -Dexec.args="22"

# Then trajectory generation (requires local DRM road network files)
mvn exec:java -Dexec.mainClass="pseudo.gen.TrajectoryGenerator" -Dexec.args="22"
```

### Step 4: File joining

```bash
mvn exec:java -Dexec.mainClass="pseudo.gen.FileJoinner"
```

Produces ZIP files per city from trajectory CSVs.

### Step 5: Aggregation

```bash
# Mesh population (500m grid, 10-minute intervals)
mvn exec:java -Dexec.mainClass="pseudo.aggr.MeshVolumeCalculator"

# Link volume (per hour)
mvn exec:java -Dexec.mainClass="pseudo.aggr.LinkVolumeCalculator"
```

## Output directory structure

After a full run for prefecture 22:

```
<outputDir>/
  activity/22/       activity_XXXXX.csv (one per city)
  trip/22/           trip_XXXXX.csv
  trajectory/22/     trajectory_XXXXX.csv
```

## Validation

Run the validation suite after any pipeline step completes:

```bash
scripts/validate/run_validation.sh 22 /path/to/outputDir
```

This validates activity, trip, and trajectory outputs and produces a Markdown summary at `<outputDir>/validation/22/summary.md`.

To write reports to a different location:

```bash
scripts/validate/run_validation.sh 22 /path/to/outputDir /tmp/reports/22
```

See [VALIDATION_GUIDE.md](VALIDATION_GUIDE.md) for detailed check rules, thresholds, and baseline interpretation.

## Smoke testing

For a quick smoke test on a single prefecture:

```bash
# 1. Set up isolated output directory
mkdir -p /tmp/pflow_smoke

# 2. Create local config override
cat > src/main/resources/config.local.properties <<'EOF'
outputDir=/tmp/pflow_smoke/
EOF

# 3. Run activity + trip for one prefecture
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22"
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="22"

# 4. Validate
scripts/validate/run_validation.sh 22 /tmp/pflow_smoke
cat /tmp/pflow_smoke/validation/22/summary.md
```

## Troubleshooting

**`NoClassDefFoundError: ThreadLocalRandom`**: Fixed in current codebase. If you see this on an older branch, the Netty `ThreadLocalRandom` import needs replacing with `java.util.concurrent.ThreadLocalRandom`.

**WebAPI session failures**: Check that `PFLOW_API_USER` and `PFLOW_API_PASS` are set and that the CSIS API endpoint is reachable.

**Empty activity files (399 lines)**: Usually caused by malformed Markov chain data. The `MkChainAccessor` now fails fast on parse errors instead of silently producing empty data.

**`NullPointerException` on `listFiles()`**: Fixed in current codebase. All `File.listFiles()` calls now have null guards.
