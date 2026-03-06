# Codebase Audit Report — 2026-03-07

> **⚠ Disclaimer — scope of this report**
> This audit was performed in **read-only mode** on the `master` branch of this machine only.
> Additional unsynced branches may exist on other servers (see CLAUDE.md → "Future branch intake SOP").
> All **delete / move / deprecate** recommendations are **provisional** until those branches are pulled,
> compared with `git range-diff`, and confirmed to contain no unique work in the affected modules.
> Do not execute any destructive action until that cross-machine check is complete.

---

## Date / Branch / HEAD

| Item | Value |
|------|-------|
| Date | 2026-03-07 (JST) |
| Branch | `master` |
| HEAD | `2e4e4d8` — `build: bind maven-assembly-plugin to package phase` |
| Audit mode | READ-ONLY — no files were modified |

---

## Finding 1 — routing2 / routing3 / routing4: Three parallel routing engines

**Paths:**
```
src/jp/ac/ut/csis/pflow/routing2/   (loader, logic, matching, res, example — ~60 files)
src/jp/ac/ut/csis/pflow/routing3/   (loader, logic, mapmatching, res, sample — ~55 files)
src/jp/ac/ut/csis/pflow/routing4/   (loader, logic, mapmatching, res, sample — ~70 files)
```

**Evidence:**
- All 11+ main pipeline classes (`TrajectoryGenerator`, `TripGenerator_WebAPI*`, `Country`, `DataAccessor`, `Student`, etc.) import **exclusively `routing4`**.
- `routing3` has **zero imports from outside its own package**.
- `routing2` has **exactly one external import**: `gtfs/GeoUtils.java:3: import jp.ac.ut.csis.pflow.routing2.res.Network` — a utility class in the GTFS module, not a main pipeline step.
- Each routing version replicates a parallel class hierarchy: `Network`, `Node`, `Link`, `Route`, `AStar`, `Dijkstra`, `PgSeiDrmLoader`, `SparseMapMatching`, `RailwayLinkCost`, etc.
- 30 example/sample files exist across `routing2/example/`, `routing3/sample/`, `routing4/sample/` — none imported by production code.

**In main pipeline (Steps 1–9)?** `routing4` — yes (Steps 5–6). `routing2` / `routing3` — no.

**Recommendation:**

| Package | Action |
|---------|--------|
| `routing4` | **Keep** — sole active routing engine |
| `routing3` | **Delete** (provisional) — zero external callers; fully superseded |
| `routing2` | **Delete** (provisional) — fix `gtfs/GeoUtils.java` import to routing4 first (one line) |
| `routing2/example/`, `routing3/sample/`, `routing4/sample/` | **Delete** — standalone demos, never referenced |

**Merge risk: HIGH** — Other machines likely have active WebAPI/GTFS work that may import routing4 sample classes or extend routing logic. Do not delete until all server branches are fetched and diffed.

---

## Finding 2 — clustering / clustering2: Superseded parallel library

**Paths:**
```
src/jp/ac/ut/csis/pflow/clustering/    (4 files: Kmeans, MeanShift, CanopyKmeans, package-info)
src/jp/ac/ut/csis/pflow/clustering2/   (9 files: adds DBSCAN, Cluster, 3 test harnesses)
```

**Evidence:**
- `clustering` (v1): zero imports from anywhere — not used by anything inside or outside its package.
- `clustering2` (v2): self-referencing only (`KmeansTest`, `DBSCANTest01` import `clustering2.*`). No production pipeline code imports either version.
- `clustering2` adds `DBSCAN`, `Cluster` (a proper result type), and test harnesses — it is clearly the successor.

**In main pipeline (Steps 1–9)?** No — both packages are offline research/analysis utilities.

**Recommendation:**

| Package | Action |
|---------|--------|
| `clustering2` | **Move** to `src/tools/clustering/` to signal non-pipeline status |
| `clustering` | **Delete** (provisional) — fully superseded, zero callers |

**Merge risk: LOW** — no production code references either version.

---

## Finding 3 — geom / geom2: Two geometry utility layers (50:1 usage ratio)

**Paths:**
```
src/jp/ac/ut/csis/pflow/geom/    (12 files: LonLat, STPoint, Mesh, DistanceUtils, TrajectoryUtils, ...)
src/jp/ac/ut/csis/pflow/geom2/   (18 files: superset — adds ILonLat, ITime, ITimeSpan, MeshUtils, DateTimeUtils, ...)
```

**Evidence:**
- Pipeline classes import `geom2` **50 times** vs `geom` **1 time** (that single import is internal to pflowlib itself).
- `geom2` is a superset: adds interface layer (`ILonLat`, `ITime`, `ITimeSpan`), `MeshUtils`, `DateTimeUtils`.
- Overlapping class names in both packages: `LonLat`, `Mesh`, `DistanceUtils`, `Geohash`, `GeometryUtils`, `TrajectoryUtils` — different implementations.
- `geom.STPoint` is used within pflowlib routing/interpolation internals (not in the main `pseudo.*` pipeline).

**In main pipeline (Steps 1–9)?** Indirectly — `geom2` underpins `routing4` data structures used by Steps 5–6.

**Recommendation:**

| Package | Action |
|---------|--------|
| `geom2` | **Keep** — primary geometry library |
| `geom` | **Do not delete yet** — `geom.STPoint` is used inside pflowlib routing internals; audit intra-pflowlib callers before acting |

**Merge risk: MEDIUM** — geom/geom2 are pflowlib internals; server branches extending routing logic may depend on specific geom classes.

---

## Finding 4 — WebAPI variants: 5 overlapping TripGenerator entry points (~4,136 LOC)

**Paths:**
```
src/pseudo/gen/TripGenerator.java                              (canonical pipeline Step 5)
src/pseudo/gen/TripGenerator_WebAPI.java                       (684 lines)
src/pseudo/gen/TripGenerator_WebAPI_refactor.java              (790 lines)
src/pseudo/gen/TripGenerator_WebAPI_refactor2.java             (661 lines)
src/pseudo/gen/TripGenerator_WebAPI_GTFS.java                  (1240 lines)
src/pseudo/gen/TripGenerator_WebAPI_GTFS_OpenTripPlanner.java  (761 lines)
```

**Evidence:**
- All 5 variants have `public static void main()` — all are standalone runnable entry points.
- All import `routing4` consistently.
- `TripGenerator_WebAPI_GTFS.java` (1240 lines) is the largest and imports `gtfs.*` — the active GTFS-capable variant.
- `TripGenerator_WebAPI_GTFS_OpenTripPlanner.java` calls `OTPTripPlanner.planTripWithWalking()` — requires the `otp:1.4.0` provided-scope dependency.
- `_refactor.java` and `_refactor2.java` naming indicates in-progress incremental rewrites, likely not final.
- `TripGenerator_WebAPI.java` appears to be the first iteration, predating GTFS support.

**In main pipeline (Steps 1–9)?** Only `TripGenerator.java` is canonical Step 5. WebAPI variants are **alternative run modes** for API-routed trips, not the standard Dijkstra/A* batch pipeline.

**Recommendation:**

| File | Action |
|------|--------|
| `TripGenerator.java` | **Keep** — canonical Step 5 |
| `TripGenerator_WebAPI_GTFS.java` | **Keep** — most complete WebAPI+GTFS variant |
| `TripGenerator_WebAPI_GTFS_OpenTripPlanner.java` | **Keep (guard as experimental)** — active OTP caller |
| `TripGenerator_WebAPI.java` | **Deprecate** — superseded by GTFS variants |
| `TripGenerator_WebAPI_refactor.java` | **Deprecate** — WIP; confirm with team before removing |
| `TripGenerator_WebAPI_refactor2.java` | **Deprecate** — WIP; confirm with team before removing |

**Merge risk: VERY HIGH** — These files are almost certainly being actively developed on other machines. Do not touch until all server branches are pulled and compared.

---

## Finding 5 — dcity package: Completely isolated (16 files, zero pipeline callers)

**Paths:**
```
src/dcity/aggr/      (BoundaryVolume, LinkVolume, DataLoader, Person, ShpLoader, Boundaries — 6 files)
src/dcity/gtfs/otp/  (Trip, OptRoute, OptRouting, Seminar6 — 4 files)
src/dcity/gtfs/view/ (DataLoader, Trip, Stop, StopTime, Generator, Result — 6 files)
```

**Evidence:**
- `grep -rn "import dcity\." src/pseudo/` returned **zero results**.
- `dcity/gtfs/otp/Seminar6.java` contains hardcoded file paths — a one-off research script.
- `dcity/aggr/Person.java` duplicates the concept of `pseudo/res/Person.java` (different schema, different purpose).
- `dcity/gtfs/view/` duplicates GTFS model classes (`Trip`, `Stop`, `StopTime`) already in `gtfs/`.

**In main pipeline (Steps 1–9)?** No.

**Recommendation:** **Delete** `dcity/gtfs/view/`, `dcity/gtfs/otp/`, and `dcity/aggr/` (provisional) — confirmed zero callers. `Seminar6.java` (hardcoded paths) is safe to delete independently.

**Merge risk: LOW** — isolated from production code. Medium risk only if a server branch is actively developing `dcity/gtfs/view/` for a new feature.

---

## Finding 6 — drm/binary package: Confirmed dead code (12 files)

**Paths:**
```
src/jp/ac/ut/csis/pflow/drm/binary/
  AParseDRM.java, ParseDRM.java, ParseDRM21/22/23/31/32.java
  EBCDIC.java, AllRoadConversionMain.java, BaseRoadConversionMain.java
```

**Evidence:**
- `grep -rn "import jp.ac.ut.csis.pflow.drm"` outside `drm/` returned **zero results**.
- DRM binary parsing is superseded by `routing4/loader/PgSeiDrmLoader.java` via PostgreSQL.
- `AllRoadConversionMain.java` and `BaseRoadConversionMain.java` have `main()` methods — one-time data conversion scripts, not pipeline steps.

**In main pipeline (Steps 1–9)?** No.

**Recommendation:** **Delete** (provisional) — fully superseded by database-backed DRM loading in routing4.

**Merge risk: LOW** — no production code depends on it.

---

## Finding 7 — reallocation package: Confirmed dead code (13 files)

**Paths:**
```
src/jp/ac/ut/csis/pflow/reallocation/
  AReallocator, PgReallocator, PgTelepointReallocator, PgTelepointReallocatorTokyo2008
  PgZmapReallocator, PgZmapReallocatorTokyo2008, CsvReallocator
  POI, TelepointPOI, ZmapPOI, IReallocatator, IPTReallocator, package-info
```

**Evidence:**
- `grep -rn "import jp.ac.ut.csis.pflow.reallocation"` returned **zero results** from outside the package.
- Classes reference Telepoint and Zmap POI databases (legacy Japanese POI datasets, circa 2008).
- `PgTelepointReallocatorTokyo2008` suffix confirms Tokyo 2008 vintage — predates current pipeline architecture.
- POI selection in the current pipeline is handled directly in `DataAccessor` + `ActGenerator.choiceFreeDestination()`.

**In main pipeline (Steps 1–9)?** No.

**Recommendation:** **Delete** (provisional) — legacy POI reallocator, zero callers.

**Merge risk: LOW** — zero external callers.

---

## Finding 8 — sim4: Active module, isolated from pipeline (13 files)

**Paths:**
```
src/sim/sim4/
  ctr/ (Controller, Simulator), res/ (Agent, Trip, ETrip)
  filter/ (IFilter, TrajectoryWriter), io/ (IAgentLoader, INetworkLoader, TestTripCreater)
  trip/ (Routing, RoutingTask, TrafficConfig)
```

**Evidence:**
- All imports in `sim4` are self-referencing — no external package imports `sim.sim4.*`.
- `sim4/io/TestTripCreater.java` is a test harness, not production.
- `sim4/ctr/Simulator.java` implements an agent-based simulation loop architecturally separate from the `pseudo.gen` pipeline.

**In main pipeline (Steps 1–9)?** No — sim4 is a standalone **traffic simulation** module.

**Recommendation:** **Keep but isolate** — move to `src/tools/sim4/` to signal non-pipeline status. Do not delete; likely under active development on other machines.

**Merge risk: HIGH** — sim4 is complex and self-contained; server branches likely have extensions. Treat as an independent sub-project.

---

## Finding 9 — pt package: Research/calibration tools (4 files)

**Paths:**
```
src/pt/
  MarkovAnalyzer.java, MotifAnalyzer.java, OutingAnalyzer.java, TripGenerator.java
```

**Evidence:**
- `pseudo/gen/ActGenerator.java:29: import pt.MotifAnalyzer` — the only external caller.
- `pt/TripGenerator.java` **collides by name** with `pseudo/gen/TripGenerator.java` (different package, different class — confusing for navigation and future merges).
- These are PT (Person Trip) survey analysis tools used for calibrating Markov chain parameters, upstream of the pipeline.

**In main pipeline (Steps 1–9)?** `MotifAnalyzer` — indirectly yes (used by `ActGenerator`, parent of Steps 2–4). Others — no.

**Recommendation:**
- **Keep** `MotifAnalyzer.java` (actively used).
- **Rename** `pt/TripGenerator.java` → `pt/PTSurveyTripGenerator.java` to eliminate naming collision.
- **Add comments** to `MarkovAnalyzer` and `OutingAnalyzer` marking them as calibration tools, not pipeline steps.

**Merge risk: MEDIUM** — `ActGenerator` is core to Steps 2–4; changes to `pt/` import surface touch active pipeline code.

---

## Finding 10 — gtfs vs dcity/gtfs: Duplicate GTFS model classes

**Paths:**
```
src/gtfs/            Trip, Stop, StopTime, Route, Fare, FareRule, GTFSParser, GTFSRouter, OTPTripPlanner, ...
src/dcity/gtfs/view/ Trip, Stop, StopTime, Generator, DataLoader, Result  (duplicate models)
src/dcity/gtfs/otp/  Trip, OptRoute, OptRouting, Seminar6
```

**Evidence:**
- `Trip`, `Stop`, `StopTime` exist in both `gtfs/` and `dcity/gtfs/view/` — separate class hierarchies, different schemas.
- `gtfs/` is **live**: imported by `pseudo/res/Country.java` (`import gtfs.*`) and by `TripGenerator_WebAPI_GTFS`.
- `dcity/gtfs/` is **isolated**: zero imports from `pseudo/`.
- `gtfs/GeoUtils.java:3: import jp.ac.ut.csis.pflow.routing2.res.Network` — the only routing2 external caller; a one-line fix (change to routing4) would eliminate the last routing2 dependency.
- `gtfs/OTPTripPlanner.java` is **live**: called by `TripGenerator_WebAPI_GTFS_OpenTripPlanner.java`.

**In main pipeline (Steps 1–9)?** `gtfs/` — yes (WebAPI+GTFS alternative Step 5). `dcity/gtfs/` — no.

**Recommendation:**

| | Action |
|--|--------|
| `gtfs/` | **Keep** — active GTFS library for WebAPI variants |
| `gtfs/GeoUtils.java` | **Fix** — replace `routing2.res.Network` import with `routing4.res.Network` (1 line) |
| `dcity/gtfs/view/` | **Delete** (provisional) — duplicate model classes, zero callers |
| `dcity/gtfs/otp/` | **Delete** (provisional) — Seminar6 has hardcoded paths; research script |

**Merge risk: MEDIUM** — `gtfs/` is used by WebAPI variants likely under active development. `dcity/gtfs/` is safe to delete.

---

## Summary Table

| # | Module / Area | Status | In Pipeline (1–9)? | Recommendation | Merge Risk |
|---|--------------|--------|--------------------|----------------|------------|
| 1 | `routing4` | Active | Yes (Steps 5–6) | **Keep** | — |
| 1 | `routing3` | Dead | No | **Delete** (provisional) | HIGH |
| 1 | `routing2` | Dead (1 minor caller) | No | **Delete** after fixing GeoUtils | HIGH |
| 1 | `routing*/example,sample` | Dead demos | No | **Delete** | LOW |
| 2 | `clustering2` | Research | No | **Move to tools/** | LOW |
| 2 | `clustering` | Dead | No | **Delete** (provisional) | LOW |
| 3 | `geom2` | Active | Indirect | **Keep** | — |
| 3 | `geom` | Partial (pflowlib internal) | Indirect | **Audit intra-pflowlib before acting** | MEDIUM |
| 4 | `TripGenerator.java` | Active | Yes (Step 5) | **Keep** | — |
| 4 | `TripGenerator_WebAPI_GTFS.java` | Active | Alt. Step 5 | **Keep** | VERY HIGH |
| 4 | `TripGenerator_WebAPI_GTFS_OTP.java` | Active | Alt. Step 5 | **Keep (guard)** | VERY HIGH |
| 4 | `TripGenerator_WebAPI.java` | Superseded | No | **Deprecate** | HIGH |
| 4 | `TripGenerator_WebAPI_refactor*.java` | WIP | No | **Deprecate** | HIGH |
| 5 | `dcity/` | Dead | No | **Delete** (provisional) | LOW |
| 6 | `drm/binary` | Dead | No | **Delete** (provisional) | LOW |
| 7 | `reallocation/` | Dead | No | **Delete** (provisional) | LOW |
| 8 | `sim4/` | Isolated | No | **Move to tools/** | HIGH |
| 9 | `pt/MotifAnalyzer` | Active | Yes (Steps 2–4) | **Keep** | MEDIUM |
| 9 | `pt/TripGenerator` | Name collision | No | **Rename** | MEDIUM |
| 10 | `gtfs/` | Active | Alt. Step 5 | **Keep; fix GeoUtils** | MEDIUM |
| 10 | `dcity/gtfs/` | Dead | No | **Delete** (provisional) | LOW |

---

## Proposed Staged Cleanup Plan

All stages require manual approval per commit. Stages with HIGH/VERY HIGH merge risk must wait until `git fetch origin` confirms no server branches reference the affected modules.

### Stage 1 — Zero-risk deletes (confirmed dead, no server-branch risk)
- Delete `clustering/` (superseded by clustering2, zero callers)
- Delete `drm/binary/` (zero callers, legacy conversion tool)
- Delete `reallocation/` (zero callers, 2008-era legacy)
- Delete `dcity/gtfs/otp/Seminar6.java` (hardcoded paths, one-off research script)
- Delete `dcity/gtfs/view/` (duplicate GTFS models, zero callers)
- Delete `dcity/aggr/` (zero callers)
- Delete `routing2/example/`, `routing3/sample/`, `routing4/sample/` (standalone demos)

### Stage 2 — Low-risk moves / renames
- Move `clustering2/` → `src/tools/clustering/`
- Move `sim4/` → `src/tools/sim4/`
- Rename `pt/TripGenerator.java` → `pt/PTSurveyTripGenerator.java`

### Stage 3 — Requires one code fix first (deferred until server branches checked)
1. Fix `gtfs/GeoUtils.java`: replace `routing2.res.Network` → `routing4.res.Network` (1 line)
2. Delete `routing2/` entirely
3. Delete `routing3/` entirely

### Stage 4 — Deferred until all server branches merged
- Decide fate of `TripGenerator_WebAPI.java`, `_refactor.java`, `_refactor2.java` after confirming no active development
- Complete `geom` vs `geom2` intra-pflowlib usage audit before touching `geom/`
