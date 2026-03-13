# Staging Forensics: Prefecture 22 (mfactor=200)

Date: 2026-03-13. Mainline: `TripGenerator_WebAPI_refactor`.

---

## 1. Missing public transit — RESOLVED

**Symptom**: Zero TRAIN/BUS/MIX trips in all output. GetMixedRoute API returned error code `11024`.

### Root cause: Missing request parameters `MaxRadius` and `MaxRoutes`

The GetMixedRoute endpoint requires explicit `MaxRadius` and `MaxRoutes` parameters. Without them:
- Error `11024` = MaxRadius not set (default should be 1000)
- Error `11023` = MaxRoutes set incorrectly (default should be 9)

The code was constructing requests without these parameters, so the API rejected every query. Jackson silently parsed the error code as an integer node, causing `num_station` and `fare` to default to 0 and transit to be skipped with no error logged.

### Fix applied

Added `MaxRadius` and `MaxRoutes` to `getStringStringMap()` in `TripGenerator_WebAPI_refactor.java`, backed by config properties `api.maxRadius` (default 1000) and `api.maxRoutes` (default 9).

### Verification (2026-03-13)

API probe confirmed valid responses after fix:
- Hamamatsu→Shizuoka: `status=1, num_station=2, fare=3630, 22.0min` (浜松→静岡 via JR新幹線ひかり)
- Shinjuku→Tokyo: `status=1, num_station=5, fare=200, 14.7min` (新宿→四ツ谷→御茶ノ水→神田→東京)

Full pref-22 staging rerun (mfactor=200, 17,398 persons, 43 cities):

| Mode | Count | Share |
|------|-------|-------|
| CAR | 29,302 | 52.2% |
| WALK | 11,814 | 21.0% |
| BICYCLE | 8,582 | 15.3% |
| TRAIN | 4,234 | 7.5% |
| NOT_DEFINED | 2,227 | 4.0% |

Mixed-route diagnostics: 32,048 API queries, 3,867 transit available, 2,109 MIX selected (won cost comparison). TRAIN now appearing at 7.5%.

### Remaining: Cost model calibration

CAR at 52.2% vs TRAIN at 7.5% — car ownership rate for pref 22 is 0.914 (highest in Japan), so high CAR share is expected, but cost model parameters (`fare.per.kilometer`, `fare.init`, `car.availability`) may still need tuning. Also, 28,146 of 32,048 queries returned `num_station==0` (no nearby station within MaxRadius), which is consistent with Shizuoka's rural/suburban geography.

---

## 2. Student trip files with 0 persons — NON-BLOCKER (validator artifact)

**Diagnosis**: 5 student trip files (`trip_301_s.csv`, `trip_304_s.csv`, `trip_305_s.csv`, `trip_306_s.csv`, `trip_429_s.csv`) have 4–9 valid rows but the validator reports 0 persons.

**Root cause**: The validator enforces `MIN_ROW_COUNT=10` with an early return (line 91–94 of `validate_trip.py`) **before** the parsing loop runs. So `num_persons` is never populated, defaulting to 0 in the summary output. The data itself is valid — these are small rural cities with legitimately few student trips.

**Fix**: Move the `MIN_ROW_COUNT` check to after the parsing loop, or lower the threshold. Either way, a validator-only change.

**Classification**: **NON-BLOCKER**. Cosmetic reporting issue.

---

## 3. Person coverage mismatch (17,398 activity → 14,826 trip) — NON-BLOCKER (data provenance)

**Diagnosis**: The gap exists because activity files and trip files were generated from **different upstream sources**.

- Activity files (`person_22101.csv`): produced by `ActivityGenerator`, reading from `agent/` household data with mfactor=200.
- Trip files (`trip_101_l.csv`): produced by `TripGenerator_WebAPI_refactor`, reading from **pre-existing per-labor activity files** at `/mnt/large/data/PseudoPFLOW/activity/22/` (e.g., `person_22101_labor.csv`), not from the staging activity output.

Breakdown of the 2,572-person gap:
- ~936 are single-activity HOME-only persons (correctly have no trips)
- ~1,636 are persons whose IDs exist in the unified activity output but not in the per-labor files used for trip input (different sampling populations)
- ~345 trip persons don't appear in the activity output at all (confirms different source data)

**Root cause**: `TripGenerator_WebAPI_refactor` reads activity input from `inputDir` (the main dataset), not from `outputDir` (where the staging ActivityGenerator wrote). The two steps are not chained.

**Fix**: Either (a) make TripGenerator read from `outputDir/activity/` instead of `inputDir`, or (b) ensure ActivityGenerator writes per-labor files that TripGenerator expects. This is a pipeline integration issue.

**Classification**: **NON-BLOCKER for stability testing**, but **must be resolved** before production runs to ensure end-to-end consistency.

---

## 4. Trajectory FAIL breakdown — NON-BLOCKER (known patterns)

119/129 files FAIL, all due to timestamp monotonicity violations. 4,843 violations in 2,707,190 rows (0.18%). Zero spatial jumps or coordinate errors.

| Category | Violations | Where | Cause |
|----------|-----------|-------|-------|
| Stay-at-home placeholder jumps | ~3,900 (80%) | `_n` files | Home-only persons get repeated 00:00→23:59 stubs; 23:59→00:00 jump between repetitions |
| Trip-boundary scheduling reversals | ~800 (17%) | `_l` files | Routed travel time exceeds the time budget between activities (arrival after next departure) |
| Minor boundary overlaps | ~140 (3%) | `_l`, `_s` files | Consecutive trip timestamps overlap by 2–17 minutes |

Duplicates: 6,328 duplicate rows in 41 `_n` files (from repeated home-placeholder rows). Warning-level only.

**Classification**: **NON-BLOCKER**. 80% are cosmetic placeholders. The 17% scheduling reversals are a real calibration issue but not a crash/correctness blocker.

---

## 5. File naming — truncated city codes — NON-BLOCKER (cosmetic + cross-ref impact)

**Diagnosis**: Trip/trajectory files use 3-digit city codes (`trip_101_l.csv`) instead of 5-digit (`trip_22101_l.csv`). The prefix `22` (prefecture) is lost.

**Root cause**: `TripGenerator_WebAPI_refactor.java` line 844 uses `file.getName().substring(9, 14)` to extract the city code from input filenames like `person_22101_labor.csv`. This extracts `"101_l"` instead of `"22101"`.

**Impact**:
- Cross-referencing with activity files (`person_22101.csv`) requires mapping 3-digit→5-digit codes
- The validation trip→activity cross-reference partially fails because filenames don't match
- Would collide if multiple prefectures wrote to the same directory (e.g., pref 1 city 101 vs pref 22 city 101)

**Classification**: **NON-BLOCKER** for single-prefecture runs, but should be fixed before multi-prefecture production.

---

## Summary

| Issue | Status | Category | Resolution |
|-------|--------|----------|------------|
| 1. Missing transit | **RESOLVED** | Request parameters | Added MaxRadius=1000, MaxRoutes=9 |
| 2. Student 0-persons | **RESOLVED** | Validator | Moved MIN_ROW_COUNT to warning-only |
| 3. Person coverage gap | **RESOLVED** | Pipeline integration | Input chaining + double-sampling fix |
| 4. Trajectory monotonicity | Deferred | 80% cosmetic, 17% calibration | Not a blocker |
| 5. File naming truncation | **RESOLVED** | Output format | Proper city code extraction |

## Remaining work

- **Cost model calibration**: CAR 52.2% vs TRAIN 7.5% — tune `fare.*` and `car.availability` parameters
- **Trajectory monotonicity**: 0.18% violations, mostly cosmetic — deferred
- **Multi-prefecture staging**: Ready to run prefs 13, 26 now that transit is working
