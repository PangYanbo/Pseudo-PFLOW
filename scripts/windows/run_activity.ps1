#Requires -Version 5.1
<#
.SYNOPSIS
    Run activity generation for a single prefecture.
.DESCRIPTION
    Generates activity CSVs (commuter + non-commuter + student) for the given prefecture
    at the specified sampling rate.
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER MFactor
    Sampling factor. 200 = 0.5% sample, 100 = 1%, 1 = full population. Default: 200.
.PARAMETER ConfigFile
    Optional external config file path (highest priority override).
.EXAMPLE
    .\run_activity.ps1 22
    .\run_activity.ps1 22 -MFactor 100
    .\run_activity.ps1 22 -ConfigFile C:\config\pref22.properties
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

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectDir = (Resolve-Path "$ScriptDir\..\..").Path

# Build Maven options
$MavenOpts = @()
if ($ConfigFile) {
    if (-not (Test-Path $ConfigFile)) {
        Write-Error "Config file not found: $ConfigFile"
        exit 1
    }
    $MavenOpts += "-Dconfig.file=$ConfigFile"
}

$SamplePct = [math]::Round(100 / $MFactor, 1)
Write-Host "=== Activity generation: prefecture $PrefCode, mfactor=$MFactor ($SamplePct% sample) ===" -ForegroundColor Cyan

$Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

Push-Location $ProjectDir
try {
    $Args = @(
        "-q", "exec:java",
        "-Dexec.mainClass=pseudo.gen.ActivityGenerator",
        "-Dexec.args=$PrefCode $MFactor"
    ) + $MavenOpts

    & mvn @Args
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Activity generation failed (exit code $LASTEXITCODE)"
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

$Stopwatch.Stop()
Write-Host "[${PrefCode}] Activity generation complete ($([int]$Stopwatch.Elapsed.TotalSeconds)s)" -ForegroundColor Green
