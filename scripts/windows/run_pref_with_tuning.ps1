#Requires -Version 5.1
<#
.SYNOPSIS
    Conditional pipeline orchestrator: tune missing param groups (if needed),
    then run generation for a prefecture.
.DESCRIPTION
    Workflow:
      1. Run check_param_groups.ps1 to determine readiness.
      2. If COMPLETE -> skip tuning, go straight to generation.
      3. If INCOMPLETE with LOCAL groups missing -> run tuning for the prefecture,
         re-check, then generate.
      4. If INCOMPLETE with only EXTERNAL groups missing -> halt with clear error.
         (External groups must be copied from another VM that owns those prefectures.)
      5. If cities are unmapped in city_code_to_param_group.csv -> halt with clear error.

    Tuning and generation remain separate phases — no on-the-fly tuning.
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER MFactor
    Sampling factor for generation (default: 200).
.PARAMETER OutputRoot
    Generation output directory. Default: C:\Pseudo-PFLOW\output\pref_<N>
.EXAMPLE
    .\run_pref_with_tuning.ps1 14
    .\run_pref_with_tuning.ps1 22 -MFactor 100
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateRange(1, 47)]
    [int]$PrefCode,

    [Parameter(Position=1)]
    [int]$MFactor = 200,

    [string]$OutputRoot
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = (Resolve-Path "$ScriptDir\..\..").Path

if (-not $OutputRoot) {
    $OutputRoot = "C:\Pseudo-PFLOW\output\pref_$PrefCode"
}

# API credential check
if (-not $env:PFLOW_API_USER -or -not $env:PFLOW_API_PASS) {
    Write-Error "PFLOW_API_USER and PFLOW_API_PASS must be set as System environment variables."
    exit 1
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Conditional pipeline: prefecture $PrefCode" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Precheck
Write-Host "[1/3] Checking parameter group readiness..." -ForegroundColor Yellow
& "$ScriptDir\check_param_groups.ps1" $PrefCode
$precheckExit = $LASTEXITCODE
Write-Host ""

if ($precheckExit -eq 0) {
    Write-Host "[2/3] All param groups present — skipping tuning, going directly to generation" -ForegroundColor Green
    Write-Host ""
    & "$ScriptDir\run_pref.ps1" $PrefCode -MFactor $MFactor -OutputRoot $OutputRoot
    exit $LASTEXITCODE
}

if ($precheckExit -eq 2) {
    Write-Host "HALT: Unmapped cities detected. Fix city_code_to_param_group.csv before retrying." -ForegroundColor Red
    exit 2
}

if ($precheckExit -ne 1) {
    Write-Error "Precheck failed with unexpected exit code $precheckExit"
    exit $precheckExit
}

# precheckExit == 1: incomplete. Determine LOCAL vs EXTERNAL missing.
# Re-parse the precheck output via a second invocation that captures only the relevant lines.
Write-Host "[2/3] Param groups incomplete — determining whether tuning can be run locally..." -ForegroundColor Yellow

# Re-read mapping to classify missing
$PrefStr = "{0:D2}" -f $PrefCode
$MappingCsv = Join-Path $ProjectDir "data\tuning\city_code_to_param_group.csv"
$ParamGroupDir = Join-Path $ProjectDir "config\tuning\param_groups"

$mapping = @{}
$reader = [System.IO.StreamReader]::new($MappingCsv)
$null = $reader.ReadLine()  # header
while (-not $reader.EndOfStream) {
    $line = $reader.ReadLine().Trim()
    if (-not $line) { continue }
    $parts = $line -split ','
    if ($parts.Count -lt 2) { continue }
    $code = $parts[0].Trim().TrimStart([char]0xFEFF)
    if ($code -match '^\d+$') { $code = "{0:D5}" -f [int]$code }
    $group = $parts[1].Trim()
    if ($code -and $group) { $mapping[$code] = $group }
}
$reader.Close()

# Collect referenced groups for this prefecture
$refGroups = @{}
foreach ($entry in $mapping.GetEnumerator()) {
    if ($entry.Key.StartsWith($PrefStr)) {
        $refGroups[$entry.Value] = $true
    }
}

$missingLocal = @()
$missingExternal = @()
foreach ($group in $refGroups.Keys) {
    $propFile = Join-Path $ParamGroupDir "$group.properties"
    if (-not (Test-Path $propFile)) {
        if ($group.StartsWith("pref$PrefStr" + "_")) {
            $missingLocal += $group
        } else {
            $missingExternal += $group
        }
    }
}

# Decision
if ($missingLocal.Count -eq 0 -and $missingExternal.Count -gt 0) {
    Write-Host "HALT: Only EXTERNAL param groups are missing. This VM cannot tune them." -ForegroundColor Red
    Write-Host "Missing external groups (must be copied from the owning VM):" -ForegroundColor Red
    foreach ($g in $missingExternal) {
        Write-Host "  $g.properties" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Action: copy the listed .properties files into config\tuning\param_groups\ on this VM, then rerun." -ForegroundColor Yellow
    exit 1
}

if ($missingLocal.Count -gt 0) {
    Write-Host "Local param groups missing — running tuning for prefecture $PrefCode" -ForegroundColor Yellow
    Write-Host "Missing local groups:"
    foreach ($g in $missingLocal) { Write-Host "  $g" }
    if ($missingExternal.Count -gt 0) {
        Write-Host "NOTE: External groups also missing — they will need to be copied separately:" -ForegroundColor Yellow
        foreach ($g in $missingExternal) { Write-Host "  $g.properties" -ForegroundColor Yellow }
    }
    Write-Host ""

    # Run Python tuning
    # IMPORTANT: propagate the outer -MFactor value into the Python tuning
    # script, otherwise tune_all_cities.py falls back to its default (200)
    # regardless of what the caller requested.
    Push-Location $ProjectDir
    try {
        & python -u "scripts\tuning\tune_all_cities.py" --pref $PrefCode --mfactor $MFactor
        $tuneExit = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($tuneExit -ne 0) {
        Write-Error "Tuning failed (exit $tuneExit). Cannot proceed to generation."
        exit $tuneExit
    }

    # Re-verify
    Write-Host ""
    Write-Host "Re-checking param groups after tuning..." -ForegroundColor Yellow
    & "$ScriptDir\check_param_groups.ps1" $PrefCode
    $recheckExit = $LASTEXITCODE
    if ($recheckExit -ne 0) {
        Write-Error "After tuning, param groups still incomplete (exit $recheckExit). External groups may still be missing — see precheck output above."
        exit $recheckExit
    }
}

# Step 3: Generation
Write-Host ""
Write-Host "[3/3] Running generation for pref $PrefCode" -ForegroundColor Yellow
Write-Host ""
& "$ScriptDir\run_pref.ps1" $PrefCode -MFactor $MFactor -OutputRoot $OutputRoot
exit $LASTEXITCODE
