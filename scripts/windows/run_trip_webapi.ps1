#Requires -Version 5.1
<#
.SYNOPSIS
    Run WebAPI trip + trajectory generation for a single prefecture.
.DESCRIPTION
    Creates a WebAPI session, routes each trip through the CSIS road/transit network API,
    and produces both trip and trajectory data in a single pass.
    Requires PFLOW_API_USER and PFLOW_API_PASS environment variables.
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER MFactor
    Sampling factor. Default: 200.
.PARAMETER ConfigFile
    Optional external config file path.
.EXAMPLE
    .\run_trip_webapi.ps1 22
    .\run_trip_webapi.ps1 22 -MFactor 100
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateRange(1, 47)]
    [int]$PrefCode,

    [Parameter(Position=1)]
    [int]$MFactor = 200,

    [string]$ConfigFile
)

$ErrorActionPreference = "Stop"

# Check API credentials
if (-not $env:PFLOW_API_USER -or -not $env:PFLOW_API_PASS) {
    Write-Error @"
PFLOW_API_USER and PFLOW_API_PASS environment variables must be set.

Set them for the current session:
  `$env:PFLOW_API_USER = 'your_user'
  `$env:PFLOW_API_PASS = 'your_pass'

Or set them permanently via System Properties > Environment Variables.
"@
    exit 1
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = (Resolve-Path "$ScriptDir\..\..").Path

$MavenOpts = @()
if ($ConfigFile) {
    if (-not (Test-Path $ConfigFile)) {
        Write-Error "Config file not found: $ConfigFile"
        exit 1
    }
    $MavenOpts += "-Dconfig.file=$ConfigFile"
}

$SamplePct = [math]::Round(100 / $MFactor, 1)
Write-Host "=== Trip+trajectory generation (WebAPI): prefecture $PrefCode, mfactor=$MFactor ($SamplePct% sample) ===" -ForegroundColor Cyan
Write-Host "API user: $($env:PFLOW_API_USER)" -ForegroundColor DarkGray

$Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

Push-Location $ProjectDir
try {
    $Args = @(
        "-q", "exec:java",
        "-Dexec.mainClass=pseudo.gen.TripGenerator_WebAPI_refactor",
        "-Dexec.args=$PrefCode $MFactor"
    ) + $MavenOpts

    & mvn @Args
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Trip generation failed (exit code $LASTEXITCODE)"
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

$Stopwatch.Stop()
Write-Host "[${PrefCode}] Trip+trajectory generation complete ($([int]$Stopwatch.Elapsed.TotalSeconds)s)" -ForegroundColor Green
