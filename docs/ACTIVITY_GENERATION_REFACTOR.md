# Activity Generation Refactor — Design

## Naming Principles

1. The **mainline** uses the simplest, most natural name — no qualifiers.
2. Specialized/debug/tuning variants carry explicit qualifiers.
3. Output directories follow the same rule: mainline output is the standard name; specialized outputs are labeled.

---

## Current State

### Files

| Current Name | Role | Output Dir | Output Suffix |
|---|---|---|---|
| `ActGenerator.java` | Abstract base class | — | — |
| `Commuter.java` | Worker activity generation (standalone `main()`) | `activity/{pref}/` | `*_labor.csv` |
| `NonCommuter.java` | Non-worker activity generation (standalone `main()`) | `activity/{pref}/` | `*_nolabor.csv` |
| `Student.java` | Student activity generation (standalone `main()`) | `activity/{pref}/` | `*_student.csv` |

### Current Pipeline Flow

```
Commuter.main()        → activity/{pref}/person_{city}_labor.csv
NonCommuter.main()     → activity/{pref}/person_{city}_nolabor.csv      ← run in parallel
Student.main()         → activity/{pref}/person_{city}_student.csv

Merge_ActivityData.ipynb (Python)  → activity_merged/{pref}/person_{city}.csv

TripGenerator.main()       reads ← activity_merged/
TripGenerator_WebAPI_refactor.main() reads ← activity/   (reads all files, no suffix filter)
```

### Problems

1. **Mainline output has the ugly intermediate name** (`activity_merged/`), while specialized outputs occupy the clean name (`activity/`). This is backwards.
2. **Three separate `main()` methods** with near-identical boilerplate (config loading, data loading, prefecture loop). ~100 lines of copy-paste in each.
3. **No single Java entry point** to run all three labor types — requires external orchestration (3 JVM launches + a Python merge script).
4. **The merge step is a Python notebook**, not part of the Java pipeline. It breaks the Maven-only execution model.

---

## Proposed State

### File Renames

| Current | Proposed | Role |
|---|---|---|
| `ActGenerator.java` | `ActivityGenerator.java` | Abstract base + **new unified `main()`** |
| `Commuter.java` | `CommuterActivityGenerator.java` | Specialized standalone for worker activities |
| `NonCommuter.java` | `NonCommuterActivityGenerator.java` | Specialized standalone for non-worker activities |
| `Student.java` | `StudentActivityGenerator.java` | Specialized standalone for student activities |

### Class Hierarchy (unchanged structurally)

```
ActivityGenerator (abstract)
  ├── CommuterActivityGenerator      (inner ActivityTask)
  ├── NonCommuterActivityGenerator   (inner ActivityTask)
  └── StudentActivityGenerator       (inner ActivityTask)
```

### Output Directories

| Generator | Output Dir | File Pattern |
|---|---|---|
| `ActivityGenerator.main()` (unified) | `activity/{pref}/` | `person_{city}.csv` |
| `CommuterActivityGenerator.main()` | `activity_commuter/{pref}/` | `person_{city}.csv` |
| `NonCommuterActivityGenerator.main()` | `activity_noncommuter/{pref}/` | `person_{city}.csv` |
| `StudentActivityGenerator.main()` | `activity_student/{pref}/` | `person_{city}.csv` |

Key decisions:
- The **mainline unified output** gets the clean `activity/` name — this is what TripGenerator consumes.
- Specialized standalone outputs get qualified directory names (`activity_commuter/`, etc.).
- File suffixes (`_labor`, `_nolabor`, `_student`) are **dropped** — the directory already identifies the labor type.
- The Python merge notebook becomes unnecessary — `ActivityGenerator.main()` writes combined output directly.

### Pipeline Flow (after refactor)

**Mainline (normal production use):**
```
ActivityGenerator.main()
  → internally runs Commuter + NonCommuter + Student per city
  → merges in-memory (or writes sequentially to same file)
  → activity/{pref}/person_{city}.csv

TripGenerator.main()              reads ← activity/
TripGenerator_WebAPI_refactor     reads ← activity/
```

**Specialized (debugging/tuning a single labor type):**
```
CommuterActivityGenerator.main()        → activity_commuter/{pref}/person_{city}.csv
NonCommuterActivityGenerator.main()     → activity_noncommuter/{pref}/person_{city}.csv
StudentActivityGenerator.main()         → activity_student/{pref}/person_{city}.csv
```

---

## Implementation Plan

### Step 1 — Rename classes (pure rename, no logic changes)

Mechanical renames with `git mv` + find-and-replace of class references:

| Action | Detail |
|---|---|
| `git mv ActGenerator.java ActivityGenerator.java` | Rename abstract base |
| `git mv Commuter.java CommuterActivityGenerator.java` | Rename worker generator |
| `git mv NonCommuter.java NonCommuterActivityGenerator.java` | Rename non-worker generator |
| `git mv Student.java StudentActivityGenerator.java` | Rename student generator |
| Update `class` declarations | Match new filenames |
| Update all internal references | `Commuter.createActivity` → `CommuterActivityGenerator.createActivity`, etc. |
| Update all external references | `TripGenerator*.java`, `CLAUDE.md`, docs, notebooks |

Files that reference these classes (must be updated):
- `TripGenerator.java` (line 270: `Commuter.class.getClassLoader()`)
- `TripGenerator_WebAPI_refactor.java` (same pattern)
- `NonCommuter.java` (line 160: `Commuter.class.getClassLoader()` — copy-paste bug, should be own class)
- `Student.java` (line 283: `Commuter.class.getClassLoader()` — same copy-paste bug)
- `CLAUDE.md`, `docs/TRIP_GENERATION_ARCHITECTURE.md`

**One commit:** `refactor: rename activity generators (ActGenerator → ActivityGenerator, Commuter → CommuterActivityGenerator, etc.)`

### Step 2 — Update specialized output directories

Change each specialized generator's `main()` to write to its qualified directory:

| Generator | Old outputDir | New outputDir |
|---|---|---|
| `CommuterActivityGenerator` | `{output}/activity/` | `{output}/activity_commuter/` |
| `NonCommuterActivityGenerator` | `{output}/activity/` | `{output}/activity_noncommuter/` |
| `StudentActivityGenerator` | `{output}/activity/` | `{output}/activity_student/` |

Also drop the `_labor` / `_nolabor` / `_student` file suffixes — the directory already conveys the type. New pattern: `person_{city}.csv` (no suffix).

**One commit:** `refactor: use qualified output dirs for specialized activity generators`

### Step 3 — Extract shared data-loading boilerplate

All three specialized generators repeat ~80 lines of identical config/data loading in `main()`:
- config.properties loading
- station, city, hospital, restaurant, retail, economic census, tatemono loading
- prefecture loop scaffolding

Extract into a shared static helper in `ActivityGenerator`:

```java
public abstract class ActivityGenerator {
    // ... existing abstract base ...

    /** Loads all shared reference data needed by activity generators. */
    protected static Country loadCountryData(Properties prop) throws Exception {
        Country country = new Country();
        String inputDir = prop.getProperty("inputDir");
        // station, city, hospital, restaurant, retail, ecensus, tatemono
        // ... identical loading logic from all three main() methods ...
        return country;
    }

    protected static Properties loadConfig() throws IOException {
        InputStream is = ActivityGenerator.class.getClassLoader()
            .getResourceAsStream("config.properties");
        if (is == null) throw new FileNotFoundException("config.properties not found");
        Properties prop = new Properties();
        prop.load(is);
        return prop;
    }
}
```

Each specialized `main()` then reduces to ~30 lines (config load, markov setup, generator instantiation, per-city loop).

**One commit:** `refactor: extract shared data-loading into ActivityGenerator base`

### Step 4 — Add unified `ActivityGenerator.main()`

New `main()` method in `ActivityGenerator` that:
1. Loads config and shared data (once)
2. For each prefecture, for each city:
   a. Runs `CommuterActivityGenerator.assign()` on WORKER households
   b. Runs `NonCommuterActivityGenerator.assign()` on NO_LABOR households
   c. Runs `StudentActivityGenerator.assign()` on student-labor households
   d. Writes all activities to a single `activity/{pref}/person_{city}.csv`

This eliminates:
- Three separate JVM launches
- The Python merge notebook
- The `activity_merged/` intermediate directory

The in-memory merge is straightforward: after all three generators process their respective `HouseHold` subsets for a city, collect all `HouseHold` lists and pass them to a single `PersonAccessor.writeActivities()` call.

**One commit:** `feat: unified ActivityGenerator.main() — replaces 3-launch + Python merge`

### Step 5 — Update TripGenerator input path

Change `TripGenerator.java` to read from `activity/` instead of `activity_merged/`:

```java
// Before:
String inputDir = String.format("%s/activity_merged/", dir);
// After:
String inputDir = String.format("%s/activity/", dir);
```

`TripGenerator_WebAPI_refactor.java` already reads from `activity/` — no change needed.

**One commit:** `fix: TripGenerator reads from activity/ (was activity_merged/)`

### Step 6 — Update docs and pipeline table

Update `CLAUDE.md`, `docs/TRIP_GENERATION_ARCHITECTURE.md`:
- Pipeline table: steps 2–4 become a single step `ActivityGenerator` (with note that specialized generators exist)
- Entry point list: `pseudo.gen.ActivityGenerator` replaces `Commuter` / `Student` / `NonCommuter`

**One commit:** `docs: update pipeline docs for ActivityGenerator refactor`

---

## What Does NOT Change

- **Activity generation logic** — the `createActivities()` / `ActivityTask` inner classes remain identical.
- **Markov chain selection** — each labor type still uses its own markov files and `EMarkov` key.
- **Concurrency model** — `ExecutorService` + `invokeAll` via `assign()` inherited from base class.
- **MNL parameters** — each labor type loads its own MNL file.
- **Output CSV format** — same columns, same order.
- **TripGenerator / TrajectoryGenerator** — consume the same `activity/{pref}/person_{city}.csv` format.

---

## Risk Assessment

| Risk | Mitigation |
|---|---|
| Rename breaks other classes | Grep all `.java` files for old names; fix before committing |
| Unified main() changes output ordering | Activity CSV has no ordering requirement — rows are per-person, consumed independently |
| Student's `assignSchool()` per-household step has different lifecycle | Handle in unified main() by running Student's assign with school data pre-loaded per prefecture |
| `SchoolRefAccessor` loads per-prefecture school data mid-loop | Unified main() must replicate this per-pref-per-city loading pattern from Student.main() |
| NonCommuter/Student copy-paste `Commuter.class.getClassLoader()` | Already a latent bug — fix during rename (use own class) |

---

## Verification Plan

After each step, run:
```bash
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -q -DskipTests clean compile
```

After Step 4 (unified main), smoke test:
```bash
# Unified: should produce activity/{pref}/person_{city}.csv with all labor types
mvn exec:java -Dexec.mainClass="pseudo.gen.ActivityGenerator" -Dexec.args="22 1000"

# Verify combined output has labor + nolabor + student rows
wc -l /tmp/pflow_smoke/activity/22/person_22101.csv
# Expected: sum of previous per-type counts (~400K + ~300K + ~130K ≈ ~830K)
```

After Step 5, run TripGenerator on the unified output:
```bash
mvn exec:java -Dexec.mainClass="pseudo.gen.TripGenerator" -Dexec.args="22"
# Verify trip files generated from unified activity input
```
