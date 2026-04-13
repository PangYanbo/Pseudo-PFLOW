#Requires -Version 5.1
<#
.SYNOPSIS
    Run validation suite for a prefecture's output.
.DESCRIPTION
    Validates activity, trip, and trajectory CSVs and produces a Markdown summary.
    Requires Python 3.8+.
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER OutputRoot
    Output root directory containing activity/, trip/, trajectory/ subdirectories.
.PARAMETER ReportDir
    Optional report output directory. Default: <OutputRoot>\validation\<PrefCode>\
.EXAMPLE
    .\run_validate.ps1 22 C:\Data\output
    .\run_validate.ps1 22 C:\Data\output C:\Reports\22
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateRange(1, 47)]
    [int]$PrefCode,

    [Parameter(Mandatory=$true, Position=1)]
    [string]$OutputRoot,

    [Parameter(Position=2)]
    [string]$ReportDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = (Resolve-Path "$ScriptDir\..\..").Path
$ValidateDir = Join-Path $ProjectDir "scripts\validate"

if (-not $ReportDir) {
    $ReportDir = Join-Path $OutputRoot "validation\$PrefCode"
}

# Resolve Python interpreter command (prefer 'python', fall back to 'python3').
# On Windows, 'python' is the standard install; 'python3' typically does NOT
# exist as a command unless installed via WSL/Cygwin/Chocolatey. On Linux/macOS,
# both usually exist.
$PythonCmd = $null
foreach ($candidate in @("python", "python3")) {
    $resolved = Get-Command $candidate -ErrorAction SilentlyContinue
    if ($resolved) {
        $verOutput = & $candidate --version 2>&1
        if ($LASTEXITCODE -eq 0 -and $verOutput -match "Python 3") {
            $PythonCmd = $candidate
            break
        }
    }
}
if (-not $PythonCmd) {
    Write-Error "Python 3.8+ is required. Install from https://www.python.org/downloads/ and ensure 'python' is on PATH."
    exit 1
}

Write-Host "=== Validation: prefecture $PrefCode ===" -ForegroundColor Cyan
Write-Host "Output root: $OutputRoot"
Write-Host "Report dir:  $ReportDir"

New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null

$ActivityDir = Join-Path $OutputRoot "activity\$PrefCode"
$TripDir = Join-Path $OutputRoot "trip\$PrefCode"
$TrajectoryDir = Join-Path $OutputRoot "trajectory\$PrefCode"

# Activity validation
if (Test-Path $ActivityDir) {
    Write-Host "`n--- Activity validation ---" -ForegroundColor Yellow
    & $PythonCmd (Join-Path $ValidateDir "validate_activity.py") $ActivityDir (Join-Path $ReportDir "activity.json")
} else {
    Write-Host "SKIP: activity directory not found: $ActivityDir" -ForegroundColor DarkYellow
}

# Trip validation
if (Test-Path $TripDir) {
    Write-Host "`n--- Trip validation ---" -ForegroundColor Yellow
    $TripArgs = @((Join-Path $ValidateDir "validate_trip.py"), $TripDir, (Join-Path $ReportDir "trip.json"))
    if (Test-Path $ActivityDir) {
        $TripArgs += $ActivityDir
    }
    & $PythonCmd @TripArgs
} else {
    Write-Host "SKIP: trip directory not found: $TripDir" -ForegroundColor DarkYellow
}

# Trajectory validation
if (Test-Path $TrajectoryDir) {
    Write-Host "`n--- Trajectory validation ---" -ForegroundColor Yellow
    & $PythonCmd (Join-Path $ValidateDir "validate_trajectory.py") $TrajectoryDir (Join-Path $ReportDir "trajectory.json")
} else {
    Write-Host "SKIP: trajectory directory not found: $TrajectoryDir" -ForegroundColor DarkYellow
}

# Summary
Write-Host "`n--- Summary ---" -ForegroundColor Yellow
& $PythonCmd (Join-Path $ValidateDir "validate_summary.py") $ReportDir $PrefCode (Join-Path $ReportDir "summary.md")

$SummaryPath = Join-Path $ReportDir "summary.md"
if (Test-Path $SummaryPath) {
    Write-Host "`nSummary written to: $SummaryPath" -ForegroundColor Green
    Write-Host ""
    Get-Content $SummaryPath
}
