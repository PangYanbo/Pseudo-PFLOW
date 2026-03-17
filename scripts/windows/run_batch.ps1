#Requires -Version 5.1
<#
.SYNOPSIS
    Run the pipeline for multiple prefectures sequentially.
.DESCRIPTION
    Runs run_pref.ps1 for each specified prefecture. Continues on failure,
    reports batch summary at the end.
.PARAMETER PrefCodes
    One or more prefecture codes (1-47). Default: 22, 13, 26.
.PARAMETER MFactor
    Sampling factor. Default: 200.
.PARAMETER OutputBase
    Base directory for all output. Each prefecture gets a subdirectory.
    Default: C:\pflow_staging
.EXAMPLE
    .\run_batch.ps1
    .\run_batch.ps1 -PrefCodes 22, 13
    .\run_batch.ps1 -PrefCodes 1, 2, 3, 4, 5 -MFactor 100
    .\run_batch.ps1 -PrefCodes 22 -OutputBase D:\pflow_output
#>
param(
    [Parameter(Position=0)]
    [int[]]$PrefCodes = @(22, 13, 26),

    [int]$MFactor = 200,

    [string]$OutputBase = "C:\pflow_staging"
)

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Check API credentials upfront
if (-not $env:PFLOW_API_USER -or -not $env:PFLOW_API_PASS) {
    Write-Error @"
PFLOW_API_USER and PFLOW_API_PASS environment variables must be set.

Set them for the current session:
  `$env:PFLOW_API_USER = 'your_user'
  `$env:PFLOW_API_PASS = 'your_pass'
"@
    exit 1
}

$SamplePct = [math]::Round(100 / $MFactor, 1)
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Batch run: prefectures $($PrefCodes -join ', ')" -ForegroundColor Cyan
Write-Host " mfactor=$MFactor ($SamplePct% sample)" -ForegroundColor Cyan
Write-Host " Output base: $OutputBase" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

$Passed = @()
$Failed = @()
$BatchStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

foreach ($Pref in $PrefCodes) {
    Write-Host ">>> Prefecture $Pref <<<" -ForegroundColor White
    $PrefOutput = Join-Path $OutputBase "pref_$Pref"

    try {
        & "$ScriptDir\run_pref.ps1" $Pref -MFactor $MFactor -OutputRoot $PrefOutput
        if ($LASTEXITCODE -eq 0) {
            $Passed += $Pref
        } else {
            $Failed += $Pref
            Write-Host "*** Prefecture $Pref FAILED (exit code $LASTEXITCODE) -- continuing ***" -ForegroundColor Red
        }
    } catch {
        $Failed += $Pref
        Write-Host "*** Prefecture $Pref FAILED: $($_.Exception.Message) -- continuing ***" -ForegroundColor Red
    }
    Write-Host ""
}

$BatchStopwatch.Stop()

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Batch summary" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Passed: $(if ($Passed.Count -gt 0) { $Passed -join ', ' } else { 'none' })" -ForegroundColor Green
Write-Host "Failed: $(if ($Failed.Count -gt 0) { $Failed -join ', ' } else { 'none' })" -ForegroundColor $(if ($Failed.Count -gt 0) { 'Red' } else { 'Green' })
Write-Host "Total time: $([int]$BatchStopwatch.Elapsed.TotalMinutes)m $($BatchStopwatch.Elapsed.Seconds)s"

if ($Failed.Count -gt 0) {
    Write-Host ""
    Write-Host "To rerun failed prefectures:" -ForegroundColor Yellow
    Write-Host "  .\run_batch.ps1 -PrefCodes $($Failed -join ', ') -MFactor $MFactor -OutputBase $OutputBase"
    exit 1
}
