#Requires -Version 5.1
<#
.SYNOPSIS
    Check parameter group readiness for a prefecture before generation.
.DESCRIPTION
    Reads city_code_to_param_group.csv and determines which param_group files
    are referenced by cities in the given prefecture. Verifies:
      1. Every city in the prefecture's activity directory has a mapping
      2. Every referenced param_group has a corresponding .properties file
    Distinguishes LOCAL (tuned on this prefecture) vs EXTERNAL (inherited)
    param groups so the orchestrator knows whether tuning is possible.
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER ActivityDir
    Optional: directory of activity CSVs to determine which cities will be
    processed. Defaults to <inputDir>/activity/<pref> if available.
    If not provided, derives city list from city_code_to_param_group.csv.
.OUTPUTS
    Exit 0 if all referenced param_groups exist AND all cities mapped.
    Exit 1 if any param_group .properties file is missing.
    Exit 2 if any city is missing from the mapping CSV.
.EXAMPLE
    .\check_param_groups.ps1 14
    .\check_param_groups.ps1 14 -ActivityDir C:\Pseudo-PFLOW\data\activity\14
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateRange(1, 47)]
    [int]$PrefCode,

    [string]$ActivityDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = (Resolve-Path "$ScriptDir\..\..").Path

$MappingCsv = Join-Path $ProjectDir "data\tuning\city_code_to_param_group.csv"
$ParamGroupDir = Join-Path $ProjectDir "config\tuning\param_groups"

if (-not (Test-Path $MappingCsv)) {
    Write-Error "Mapping CSV not found: $MappingCsv"
    exit 3
}
if (-not (Test-Path $ParamGroupDir)) {
    Write-Error "Param group directory not found: $ParamGroupDir"
    exit 3
}

# Normalize prefecture code to 2-digit zero-padded string
$PrefStr = "{0:D2}" -f $PrefCode

Write-Host "=== Param group precheck: prefecture $PrefStr ===" -ForegroundColor Cyan
Write-Host "Mapping CSV:    $MappingCsv"
Write-Host "Param group dir: $ParamGroupDir"
Write-Host ""

# Load mapping CSV: city_code (5-digit zero-padded) -> param_group
$mapping = @{}
$reader = [System.IO.StreamReader]::new($MappingCsv)
$headerLine = $reader.ReadLine()  # skip header
while (-not $reader.EndOfStream) {
    $line = $reader.ReadLine().Trim()
    if (-not $line) { continue }
    $parts = $line -split ','
    if ($parts.Count -lt 2) { continue }
    $code = $parts[0].Trim().TrimStart([char]0xFEFF)  # strip BOM
    # Normalize to 5-digit zero-padded string
    if ($code -match '^\d+$') {
        $code = "{0:D5}" -f [int]$code
    }
    $group = $parts[1].Trim()
    if ($code -and $group) {
        $mapping[$code] = $group
    }
}
$reader.Close()

# Find cities in the prefecture (mapping rows where city_code starts with prefStr)
$prefCities = @{}
foreach ($entry in $mapping.GetEnumerator()) {
    if ($entry.Key.StartsWith($PrefStr)) {
        $prefCities[$entry.Key] = $entry.Value
    }
}

if ($prefCities.Count -eq 0) {
    Write-Host "ERROR: No cities for prefecture $PrefStr in mapping CSV" -ForegroundColor Red
    exit 3
}

# Determine cities that will actually be processed
# Use activity dir if provided/found; otherwise use mapping-derived city list
$citiesToProcess = @{}
if (-not $ActivityDir) {
    # Try default location: read inputDir from config (skip — too complex). Use mapping.
}

if ($ActivityDir -and (Test-Path $ActivityDir)) {
    Write-Host "Activity dir: $ActivityDir (using actual file list)"
    Get-ChildItem -Path $ActivityDir -Filter "person_*.csv" -ErrorAction SilentlyContinue | ForEach-Object {
        $base = $_.BaseName  # e.g. "person_14101" or "person_14101_labor"
        $rest = $base.Substring("person_".Length)
        $usPos = $rest.IndexOf('_')
        if ($usPos -gt 0) {
            $rest = $rest.Substring(0, $usPos)
        }
        # Normalize to 5-digit
        if ($rest -match '^\d+$') {
            $rest = "{0:D5}" -f [int]$rest
        }
        $citiesToProcess[$rest] = $true
    }
    Write-Host "  Found $($citiesToProcess.Count) city files in activity dir"
} else {
    Write-Host "Activity dir: (not provided — using mapping-derived city list)"
    foreach ($code in $prefCities.Keys) {
        $citiesToProcess[$code] = $true
    }
    Write-Host "  $($citiesToProcess.Count) cities derived from mapping"
}

# Check 1: every city to process has a mapping
$unmappedCities = @()
foreach ($code in $citiesToProcess.Keys) {
    if (-not $mapping.ContainsKey($code)) {
        $unmappedCities += $code
    }
}

# Collect referenced param_groups (only for cities we'll actually process)
$referencedGroups = @{}
foreach ($code in $citiesToProcess.Keys) {
    if ($mapping.ContainsKey($code)) {
        $referencedGroups[$mapping[$code]] = $true
    }
}

# Check 2: each referenced group has a .properties file; classify LOCAL vs EXTERNAL
$missingLocal = @()
$missingExternal = @()
$presentLocal = @()
$presentExternal = @()

foreach ($group in ($referencedGroups.Keys | Sort-Object)) {
    $propFile = Join-Path $ParamGroupDir "$group.properties"
    $isLocal = $group.StartsWith("pref$PrefStr" + "_")
    $exists = Test-Path $propFile

    if ($exists) {
        if ($isLocal) { $presentLocal += $group } else { $presentExternal += $group }
    } else {
        if ($isLocal) { $missingLocal += $group } else { $missingExternal += $group }
    }
}

# Report
Write-Host "--- Referenced param groups ---" -ForegroundColor Yellow
Write-Host "  LOCAL  present: $($presentLocal.Count)" -ForegroundColor Green
foreach ($g in $presentLocal) { Write-Host "    OK  $g" }
Write-Host "  EXTERNAL present: $($presentExternal.Count)" -ForegroundColor Green
foreach ($g in $presentExternal) { Write-Host "    OK  $g (inherited)" }

if ($missingLocal.Count -gt 0) {
    Write-Host "  LOCAL  MISSING: $($missingLocal.Count)" -ForegroundColor Red
    foreach ($g in $missingLocal) { Write-Host "    MISSING  $g" -ForegroundColor Red }
}
if ($missingExternal.Count -gt 0) {
    Write-Host "  EXTERNAL MISSING: $($missingExternal.Count)" -ForegroundColor Red
    foreach ($g in $missingExternal) { Write-Host "    MISSING  $g (inherited — must be copied from another VM)" -ForegroundColor Red }
}
Write-Host ""

if ($unmappedCities.Count -gt 0) {
    Write-Host "--- Unmapped cities (not in city_code_to_param_group.csv) ---" -ForegroundColor Red
    foreach ($c in ($unmappedCities | Sort-Object)) { Write-Host "    UNMAPPED  $c" -ForegroundColor Red }
    Write-Host ""
    Write-Host "ERROR: $($unmappedCities.Count) cities have no param group mapping. Update city_code_to_param_group.csv before running." -ForegroundColor Red
    exit 2
}

# Final status
$totalMissing = $missingLocal.Count + $missingExternal.Count
if ($totalMissing -eq 0) {
    Write-Host "STATUS: COMPLETE — pref $PrefStr can run generation directly" -ForegroundColor Green
    exit 0
} else {
    Write-Host "STATUS: INCOMPLETE — $totalMissing param group file(s) missing" -ForegroundColor Yellow
    if ($missingLocal.Count -gt 0) {
        Write-Host "  -> Local groups missing: tuning can produce them on this VM" -ForegroundColor Yellow
    }
    if ($missingExternal.Count -gt 0) {
        Write-Host "  -> External groups missing: must be copied from the VM that owns those prefectures" -ForegroundColor Red
    }
    exit 1
}
