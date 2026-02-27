# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pseudo-PFLOW generates synthetic population flow (pseudo-PFLOW) data for Japan — a full pipeline that simulates daily mobility of synthetic agents (persons) derived from census household data, producing trajectories and aggregated spatial statistics.

## Build & Run

**Requirement**: Maven 3.6.3 exactly (versions ≥ 3.8 are incompatible).

```bash
# Compile
mvn compile

# Package (creates fat JAR with dependencies)
mvn package -DskipTests

# Run a specific main class
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator"
```

The custom library `lib/pflowlib.jar` must be installed in the local Maven repository (or configured via IntelliJ's project libraries). It provides routing, geometry, and mesh utilities under `jp.ac.ut.csis.pflow.*`.

**Java target**: 11 (despite `java.version=1.8` property; the compiler plugin overrides to 11).

## Configuration

Runtime configuration is in `src/main/resources/config.properties`:
- `root` / `inputDir`: Root paths to the dataset (typically `C:/Data/PseudoPFLOW/` — must be updated per environment)
- `pref.N`: Markov chain dataset to use for prefecture N (1–47)
- `api.*`: Credentials and endpoints for CSIS WebAPI routing
- `car.N` / `bike.N`: Vehicle ownership rates per prefecture

Dataset files are downloaded from `S3://pseudo-pflow/processing`. Each processing step reads from subdirectories under `root`.

## Pipeline Architecture

The pipeline runs in sequential steps, each as a Java `main()` class:

| Step | Entry Point | Input → Output |
|------|-------------|----------------|
| 1 | `pseudo.pre.PersonGenerator` | Household CSV → Person CSV (assigns worker/student/no-worker roles) |
| 2 | `pseudo.gen.Commuter` | Census + markov + MNL params → Activity CSV (workers) |
| 3 | `pseudo.gen.Student` | Census + school refs → Activity CSV (students) |
| 4 | `pseudo.gen.NonCommuter` | Census + markov → Activity CSV (non-workers) |
| 5 | `pseudo.gen.TripGenerator` | Activity CSV → Trip CSV (mode choice) |
| 6 | `pseudo.gen.TrajectoryGenerator` | Trip CSV + road/rail networks → Trajectory CSV |
| 7 | `pseudo.gen.FileJoinner` | Trajectory CSV → ZIP per city |
| 8 | `pseudo.aggr.MeshVolumeCalculator` | Trajectory → 500m mesh population per 10 min |
| 9 | `pseudo.aggr.LinkVolumeCalculator` | Trajectory → Link volume per hour |

Steps 2–4 run in parallel (one per labor type). Steps 2–4 outputs are merged before step 5.

## Package Structure

- **`pseudo.res`** — Core domain objects: `Person`, `HouseHold`, `Activity`, `Trip`, `SPoint` (trajectory point), `City`, `Country` (container for all cities + station network). Enums: `ELabor`, `EGender`, `ETransport`, `EPurpose`, `ECity`, `EPTCity`.
- **`pseudo.acs`** — CSV data loaders/accessors: `DataAccessor` (city/station/facility loading), `PersonAccessor` (read/write person/activity/trip files), `MkChainAccessor` (Markov chain), `MNLParamAccessor` (MNL coefficients), `ModeAccessor` (transport mode probabilities), `MotifAccessor`.
- **`pseudo.pre`** — Preprocessing: census processing (`CensusKakou1–3`), `PersonGenerator` (household→person).
- **`pseudo.gen`** — Generation: activity generators (`Commuter`, `Student`, `NonCommuter`), `TripGenerator`, `TrajectoryGenerator`, WebAPI-based routing variants.
- **`pseudo.aggr`** — Aggregation: `MeshVolumeCalculator`, `LinkVolumeCalculator`, `Sampling`.
- **`pt`** — PT survey analysis: `MarkovAnalyzer`, `MotifAnalyzer`, `OutingAnalyzer` (used for calibration, not pipeline execution).
- **`network`** — Network loaders: `DrmLoader` (road DRM format), `RailLoader` (railway TSV).
- **`utils`** — `Roulette` (weighted random selection), `Softmax`, `DateUtils`.
- **`sim/sim3`, `sim/sim4`** — Traffic simulation modules (agent-based, queuing logic).
- **`gtfs`, `dcity/gtfs`** — GTFS transit parsing and OTP (OpenTripPlanner) integration.
- **`src/scripts`** — Jupyter notebooks for data evaluation and analysis (Python).

## Key Design Patterns

**Concurrency**: All generator classes use `ExecutorService` with a fixed thread pool (sized to `availableProcessors()`). Work is split into `numThreads * 10` `Callable` tasks, submitted via `invokeAll`.

**Mode choice**: `TripGenerator` uses roulette-wheel selection (`utils.Roulette`) on probabilities from `ModeAccessor`. For train trips, access/egress modes are chosen separately, producing 3 sub-trips.

**Location choice**: Destination selection uses MNL (Multinomial Logit) parameters loaded via `MNLParamAccessor`, applied per labor type, gender, and purpose.

**Activity sequence**: Generated using Markov chains (`MkChainAccessor`) calibrated from Person Trip (PT) survey data, varying by gender, labor status, and metropolitan area.

**`pflowlib.jar`** provides (closed-source):
- `jp.ac.ut.csis.pflow.geom2.*` — `LonLat`, `Mesh`/`MeshUtils`, `DistanceUtils`, `TrajectoryUtils`
- `jp.ac.ut.csis.pflow.routing4.*` — `Network`, `Node`, `Link`, `Route`, `Dijkstra`, `AStar`

## Branch Policy

- **`pseudo-pflow-v3-dev`** (current): experimental development, debugging, small fixes
- **`refactor-pseudo-v3`**: core class refactoring (lead developer only)
- **`master`**: stable release snapshots — never push directly
- Use `git pull --rebase` instead of merge to keep history clean
