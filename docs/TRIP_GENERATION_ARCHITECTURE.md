# Trip Generation Architecture

## Overview

Step 5 of the Pseudo-PFLOW pipeline converts per-person activity sequences into trips (and optionally trajectories). There are two pipeline paths and several historical variants. This document records the current status of each file in `src/pseudo/gen/`.

---

## Active Pipelines

### Mainline: `TripGenerator_WebAPI_refactor.java`

**Role**: Canonical trip + trajectory generation in a single pass. Uses the CSIS WebAPI for real-time routing.

**Unique features**:
- **Cost-minimization mode choice**: selects transport mode (CAR, WALK, BICYCLE, MIX) based on economic model — fare + time × FARE_PER_HOUR + fatigue indices (no statistical ModeAccessor table)
- **MIX (transit) routing via WebAPI**: calls `routeByWebAPI()` for mixed-mode trips; falls back to Dijkstra road routing for CAR
- **"1.2 algorithm"**: for MIX mode, interpolates intermediate rail trajectory points between nearest rail-network stations using a local railway Dijkstra (fills the gap where the WebAPI returns only origin/destination)
- **Integrated trajectory output**: writes trajectory CSV files directly alongside trip assignment; combines steps 5 + 6 of the pipeline into one execution
- **Required env vars**: `PFLOW_API_USER` / `PFLOW_API_PASS` (fail-fast at startup if missing)
- **Hardcoded outputDir** at line ~737: `"C:/large/PseudoPFLOW/"` — must be updated for each deployment (not yet changed; requires live API + DRM data to verify)

**Config dependencies**: `root`, `inputDir`, `api.url`, `car.N`, `bike.N` properties; network files `base_drm_road.csv`, `base_station.csv`, `base_rail.csv`

---

### Legacy Retained: `TripGenerator.java` + `TrajectoryGenerator.java`

These form the **offline/fallback pipeline** (no WebAPI dependency).

#### `TripGenerator.java`
- **Mode choice**: roulette-wheel on `ModeAccessor` probability tables (statistical, not cost-based)
- **Train decomposition**: proper 3-leg structure (access mode → train → egress mode) using nearest-station Dijkstra lookups; access/egress modes re-chosen independently
- **Output**: trip CSV only (trajectory done separately by `TrajectoryGenerator`)
- **Limitations**: no trajectory output; no real-time cost model; no bicycle or MIX mode
- **Suitable for**: offline batch runs, environments without WebAPI access

#### `TrajectoryGenerator.java`
- **Input**: trip CSV from `TripGenerator`
- **Features**: DRM Dijkstra road routing; chunked output with configurable zip; retry rerouting on failure (up to 3 attempts); **resume capability** (skips already-processed output files — critical for long prefecture-scale runs)
- **Unique value**: resume logic is not present in the mainline `TripGenerator_WebAPI_refactor.java`

---

## Dev/Archive Candidates

These files are no longer on the active development path. They should be moved to an archive directory outside `src/` before the next release. **Do not delete until all server-local branches have been pushed and compared** (unsynced variants may exist on other machines).

### `TripGenerator_WebAPI_refactor2.java`
- **Status**: incomplete refactor of the mainline
- **Issues**: incomplete `process()` body for road-mode trips; walk time formula bug (`roadtime * 600 * 10` — off by factor ~600); no trajectory output for road modes; otherwise very similar to `TripGenerator_WebAPI_refactor.java`
- **Archive rationale**: not compilable in isolation; no functionality not present in mainline
- **Merge risk**: LOW — diverged only recently from mainline

### `TripGenerator_WebAPI.java`
- **Status**: earlier WebAPI integration attempt using `ModeAccessor` statistical probabilities (not cost-based)
- **Unique value**: `createSubtrips()` method — cleaner algorithm for decomposing multi-leg trips into access/egress segments; this pattern could be ported to the mainline
- **Issues**: Apache Axis static import (`import static ...AxisProperties`) — latent compile risk; substantially different mode-choice logic than mainline; potentially active on MDX server branch
- **Archive rationale**: mainline supersedes; `ModeAccessor` approach replaced by cost model
- **Merge risk**: HIGH — may have active variant on MDX/other-server branch; do not archive until confirmed no unsynced work

### `TripGenerator_WebAPI_GTFS_OpenTripPlanner.java`
- **Status**: OTP (OpenTripPlanner) transit router integration; prefecture-28 specific
- **Issues**: hardcoded OTP endpoint `http://localhost:8080/otp/routers/default/plan`; not generalized for multi-prefecture use; depends on `otp` artifact (scoped `provided` in current pom.xml)
- **Archive rationale**: narrow geographic scope; OTP dependency adds operational complexity; GTFS routing approach not in current roadmap
- **Merge risk**: LOW — clearly scoped experiment

---

## Research Reference (Retain in `src/`)

### `TripGenerator_WebAPI_GTFS.java`
- **Status**: research/calibration reference; not an active pipeline entry point
- **Unique value**:
  - **COMMUNITY mode**: handles community bus / demand-responsive transit (not in any other variant)
  - **Occupation/purpose cost coefficients**: differentiated fare weights by trip purpose and occupational type — more economically realistic than flat FARE_PER_HOUR
  - **`ConcurrentHashMap<String, StationUsage>` station analytics**: tracks per-station boarding/alighting counts during generation — useful for calibration validation
  - **Kobe-specific GTFS data**: contains geographic validation for the Kobe metropolitan area
- **Retain because**: contains calibrated cost coefficients and the COMMUNITY mode approach that may be needed when extending mainline to cover rural/suburban transit
- **Do not archive**: these coefficients should be ported to mainline before this file is removed

---

## Ideas Worth Porting to Mainline

| Idea | Source File | Description |
|------|------------|-------------|
| `createSubtrips()` refactor | `TripGenerator_WebAPI.java` | Cleaner separation of multi-leg trip decomposition into a named method; makes the mainline `process()` easier to read |
| Occupation/purpose cost coefficients | `TripGenerator_WebAPI_GTFS.java` | Per-purpose fare weight differentiation improves mode-choice realism |
| COMMUNITY mode | `TripGenerator_WebAPI_GTFS.java` | Rural/suburban demand-responsive transit; needed for non-metropolitan prefectures |
| Station usage analytics | `TripGenerator_WebAPI_GTFS.java` | `ConcurrentHashMap<String, StationUsage>` — useful for calibration output |
| Resume capability | `TrajectoryGenerator.java` | Skip already-processed output files; important for long multi-prefecture batch runs |
| `outputDir` config property | All generator `main()` methods | Override output directory without changing `root`; standardize across all generators |

---

## Proposed Archive Plan

When ready to execute (after all server-local branches are confirmed pushed and compared):

**Target directory**: `archive/pseudo/gen/` at the project root (outside `src/` — not indexed by Maven, not in classpath)

```
archive/
  pseudo/
    gen/
      TripGenerator_WebAPI_refactor2.java
      TripGenerator_WebAPI.java                  ← hold until MDX branch confirmed merged
      TripGenerator_WebAPI_GTFS_OpenTripPlanner.java
```

`TripGenerator_WebAPI_GTFS.java` stays in `src/pseudo/gen/` until COMMUNITY mode and cost coefficients are ported to mainline.

**Preconditions before executing**:
1. Confirm `TripGenerator_WebAPI.java` has no unsynced active variant on MDX or other server branches
2. Port COMMUNITY mode + cost coefficients from `TripGenerator_WebAPI_GTFS.java` to mainline
3. Confirm `mvn compile` still passes after any moves (moved files are not referenced by other classes)
4. One commit per file moved (for clean git history and easy revert)

---

## File Status Summary

| File | Status | Action |
|------|--------|--------|
| `TripGenerator_WebAPI_refactor.java` | **Mainline** | Active; fix outputDir line ~737 when API env available |
| `TripGenerator.java` | **Legacy retained** | Offline fallback; keep |
| `TrajectoryGenerator.java` | **Legacy retained** | Offline fallback; keep (resume logic unique) |
| `TripGenerator_WebAPI_GTFS.java` | **Research reference** | Keep until COMMUNITY mode ported |
| `TripGenerator_WebAPI_refactor2.java` | Archive candidate | Move to `archive/` when ready |
| `TripGenerator_WebAPI.java` | Archive candidate | Move to `archive/` after MDX branch confirmed |
| `TripGenerator_WebAPI_GTFS_OpenTripPlanner.java` | Archive candidate | Move to `archive/` when ready |
