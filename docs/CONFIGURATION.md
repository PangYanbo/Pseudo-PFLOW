# Configuration Guide

## Layered config system

Configuration is loaded by `utils.ConfigLoader` with 4 layers (later wins):

| Priority | File | Location | Committed |
|----------|------|----------|-----------|
| 1 (base) | `config.properties` | `src/main/resources/` | Yes |
| 2 (pref) | `config.pref.<N>.properties` | `src/main/resources/` | Yes |
| 3 (local) | `config.local.properties` | `src/main/resources/` | No (gitignored) |
| 4 (external) | any path via `-Dconfig.file=` | anywhere | No |

Each layer overlays the previous. Only keys present in the later file are overridden; absent keys retain their earlier value.

### Layer 1: Base defaults (`config.properties`)

Committed to the repo. Contains all path roots, markov dataset mappings (pref.1–47), car/bike ownership rates, API endpoints, and model parameter defaults.

### Layer 2: Prefecture overrides (`config.pref.<N>.properties`)

Optional. Create `src/main/resources/config.pref.22.properties` to override any key when running prefecture 22. Loaded only when the generator receives a prefecture argument.

Example — increase walk distance for rural prefecture:

```properties
max.walk.distance=5000
max.destination.search.distance=30000
```

### Layer 3: Machine-local overrides (`config.local.properties`)

Gitignored. Use this for paths and credentials that differ per machine. Copy from the example:

```bash
cp src/main/resources/config.local.properties.example src/main/resources/config.local.properties
```

Then edit:

```properties
root=/mnt/data/PseudoPFLOW/
inputDir=/mnt/data/PseudoPFLOW/processing/
outputDir=/tmp/pflow_output/
```

### Layer 4: External file (`-Dconfig.file=`)

Highest priority. Pass via Maven or JVM argument:

```bash
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" \
    -Dexec.args="22" \
    -Dconfig.file=/tmp/my_override.properties
```

Useful for CI, batch scripts, or one-off test runs without modifying any committed file.

## Key properties reference

### Paths

| Key | Description | Example |
|-----|-------------|---------|
| `root` | Base data directory | `/mnt/large/data/PseudoPFLOW/` |
| `inputDir` | Input data (processing dataset) | `${root}processing/` |
| `outputDir` | Pipeline output directory | `/tmp/pflow_smoke/` |

### Markov chain datasets

```properties
pref.<N>=markov/<dataset_name>
```

Maps each prefecture (1–47) to a Markov chain calibration dataset. These control activity sequence generation. Current mappings:

| Prefectures | Dataset |
|-------------|---------|
| 1–7 | `markov/asahikawa2002` |
| 8–16 | `markov/tokyo2018` |
| 17–24 | `markov/chukyo2011` |
| 25–47 | `markov/kinki2010` |

### Vehicle ownership rates

```properties
car.<N>=<rate>    # car ownership rate for prefecture N (0.0–1.0)
bike.<N>=<rate>   # bicycle ownership rate for prefecture N (0.0–1.0)
```

Used by `TripGenerator_WebAPI_refactor` for mode choice probability weighting. Read with null guards — missing keys default to 0.

### API configuration

```properties
api.createSessionURL=https://pflow-api.csis.u-tokyo.ac.jp/webapi/CreateSession
api.getRoadRouteURL=https://pflow-api.csis.u-tokyo.ac.jp/webapi/GetRoadRoute
api.getMixedRouteURL=https://pflow-api.csis.u-tokyo.ac.jp/webapi/GetMixedRoute
api.appDate=20240401
api.maxRadius=1000
api.maxRoutes=9
api.transportCode=3
```

- `api.appDate`: timetable date for transit routing (YYYYMMDD)
- `api.maxRadius`: station search radius in meters (required by API; default 1000)
- `api.maxRoutes`: max route alternatives returned (required by API; default 9)
- `api.transportCode`: `3` = bus-oriented routing (default), `1` = train-oriented routing. The API returns routes matching the requested transport type; bus routes include walk+bus segments, train routes include walk+rail segments. Fails fast if set to any other value.

Credentials are **not** in config files. Set as environment variables:

```bash
export PFLOW_API_USER=your_user
export PFLOW_API_PASS=your_pass
```

The generator fails fast with `IllegalStateException` if these are missing or blank.

### Model parameters

| Key | Default | Unit | Used by |
|-----|---------|------|---------|
| `max.walk.distance` | 3000 | meters | TripGenerator |
| `max.station.search.distance` | 5000 | meters | TripGenerator |
| `min.transit.distance` | 1000 | meters | TripGenerator_WebAPI_refactor |
| `max.destination.search.distance` | 20000 | meters | AbstractActivityGenerator |
| `school.max.distance` | 5000 | meters | StudentActivityGenerator |
| `fare.per.kilometer` | 10 | yen/km | TripGenerator_WebAPI_refactor |
| `fare.per.hour` | 1000 | yen/hr | TripGenerator_WebAPI_refactor |
| `fare.init` | 200 | yen | TripGenerator_WebAPI_refactor |
| `fatigue.walk` | 1.5 | multiplier | TripGenerator_WebAPI_refactor |
| `fatigue.bicycle` | 1.2 | multiplier | TripGenerator_WebAPI_refactor |
| `car.availability` | 0.4 | ratio | TripGenerator_WebAPI_refactor |
| `train.service.start` | 18000 | seconds | AbstractActivityGenerator |
| `activity.time.interval` | 900 | seconds | AbstractActivityGenerator |
| `senior.age.threshold` | 65 | years | NonCommuterActivityGenerator |

All have hardcoded fallback defaults. Override per prefecture via `config.pref.<N>.properties` or globally via `config.local.properties`.

## Multi-machine setup

To run different prefectures on different machines:

1. Clone the repo on each machine
2. Each machine creates its own `config.local.properties` with local paths:

   ```properties
   # Machine A (has data on /data/)
   root=/data/PseudoPFLOW/
   inputDir=/data/PseudoPFLOW/processing/
   outputDir=/data/output/
   ```

   ```properties
   # Machine B (has data on /mnt/large/)
   root=/mnt/large/data/PseudoPFLOW/
   inputDir=/mnt/large/data/PseudoPFLOW/processing/
   outputDir=/mnt/large/output/
   ```

3. Run different prefecture ranges on each machine:

   ```bash
   # Machine A: prefectures 1-23
   for p in $(seq 1 23); do
     mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="$p"
     mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="$p"
   done

   # Machine B: prefectures 24-47
   for p in $(seq 24 47); do
     mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="$p"
     mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator_WebAPI_refactor" -Dexec.args="$p"
   done
   ```

4. Validate on each machine:

   ```bash
   for p in $(seq 1 23); do
     scripts/validate/run_validation.sh $p /data/output/
   done
   ```

Since `config.local.properties` is gitignored, each machine's paths never conflict with the committed config.

## Precedence example

Given these files:

```
# config.properties (committed)
max.walk.distance=3000
outputDir=/default/output/

# config.pref.22.properties (committed)
max.walk.distance=4000

# config.local.properties (local, gitignored)
outputDir=/tmp/my_output/
```

Running with `args="22"`:
- `max.walk.distance` = **4000** (pref override wins over base)
- `outputDir` = **`/tmp/my_output/`** (local wins over base; pref didn't set it)
