# Staging Forensics: Prefecture 22 (mfactor=200)

Date: 2026-03-13. Mainline: `TripGenerator_WebAPI_refactor`.

---

## 1. Missing public transit — BLOCKER (API-side)

**Diagnosis**: Zero TRAIN/BUS/MIX trips in all output.

### Root cause: GetMixedRoute API returns error code `11024`

Raw API probe (2026-03-13) confirmed the `GetMixedRoute` endpoint returns the bare string `11024` (not JSON) for **every request**, regardless of:
- AppDate (tested: 20231001, 20240401, 20250401, 20260313)
- OD pair (tested: Hamamatsu local, Hamamatsu→Shizuoka 70km, Shinjuku→Tokyo)
- TransportCode (tested: 1=train, 3=bus, omitted)
- UnitTypeCode (tested: 1, 2, omitted)
- Session cookie method (cookie dict, header, POST body, JSESSIONID)
- AppDate/AppTime presence (tested: with and without)

Meanwhile, `GetRoadRoute` works correctly with the same session, returning valid GeoJSON with `status: 1`.

**`11024` is an API error code**, not valid route data. The Java code silently accepts it because:
1. `getMixedRoute()` passes the response to `mapper.readTree("11024")` — Jackson parses this as an integer JsonNode (valid JSON)
2. `path("num_station").asInt()` on an integer node returns 0 (no such field)
3. `path("fare").asInt()` also returns 0
4. The guard `num_station > 0 && fare > 0` evaluates to `false`
5. Transit is silently skipped — no error logged

**Conclusion**: The blocker is **API-side** — either:
- The account lacks GetMixedRoute permissions (different service tier)
- The GetMixedRoute service is currently disabled/deprecated
- Error 11024 means a specific authorization or configuration failure

This is **not** an AppDate issue, parameter issue, or parser bug. The code is correctly calling the API; the API is refusing the request. Cost model calibration (Cause B from initial diagnosis) remains a secondary concern but is moot until the API returns actual data.

**Next step**: Contact the CSIS WebAPI administrator to clarify error code 11024 and verify account permissions for GetMixedRoute. The diagnostic probe script is at `scripts/staging/probe_mixedroute_api.py`.

### Secondary: Cost model (deferred)

Even if the API returned valid transit data, the cost model makes CAR unrealistically cheap. This should be calibrated after API access is restored, but is not actionable until then.

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

| Issue | Severity | Category | Fix effort |
|-------|----------|----------|------------|
| 1. Missing transit | **BLOCKER** | Generator logic | Medium (AppDate config + cost calibration) |
| 2. Student 0-persons | Non-blocker | Validator | Small (move MIN_ROW_COUNT check) |
| 3. Person coverage gap | Non-blocker* | Pipeline integration | Medium (input/output chaining) |
| 4. Trajectory monotonicity | Non-blocker | 80% cosmetic, 17% calibration | Deferred |
| 5. File naming truncation | Non-blocker | Output format | Small (fix substring logic) |

*Must be resolved before production.

## Top 2 highest-impact fixes

1. **Transit diagnosis + AppDate fix** (Issue 1): Make `AppDate` configurable via config.properties, add diagnostic logging when `publicTransit==false` after `getMixedRoute`. This unblocks transit mode and enables calibration of cost parameters.

2. **Pipeline input chaining** (Issue 3): Make `TripGenerator_WebAPI_refactor` read activity input from `outputDir/activity/` so both stages use the same sampled population. This ensures end-to-end consistency and fixes the person coverage mismatch.
