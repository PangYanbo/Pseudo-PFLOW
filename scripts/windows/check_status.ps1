#Requires -Version 5.1
<#
.SYNOPSIS
    Check the status of a running or completed pipeline run.
.DESCRIPTION
    Inspects the output directory for a prefecture and reports:
    - File counts (activity, trip, trajectory)
    - Log tail (last errors/warnings)
    - Mode share from trip files
    - Validation summary if available
.PARAMETER PrefCode
    Prefecture code (1-47).
.PARAMETER OutputRoot
    Output root directory. Default: C:\pflow_staging\pref_<N>
.EXAMPLE
    .\check_status.ps1 22
    .\check_status.ps1 22 -OutputRoot D:\output\pref_22
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateRange(1, 47)]
    [int]$PrefCode,

    [Parameter(Position=1)]
    [string]$OutputRoot
)

if (-not $OutputRoot) {
    $OutputRoot = "C:\pflow_staging\pref_$PrefCode"
}

if (-not (Test-Path $OutputRoot)) {
    Write-Host "Output directory not found: $OutputRoot" -ForegroundColor Red
    exit 1
}

Write-Host "=== Status: prefecture $PrefCode ===" -ForegroundColor Cyan
Write-Host "Output root: $OutputRoot"
Write-Host ""

# File counts
$Dirs = @{
    "activity"   = Join-Path $OutputRoot "activity\$PrefCode"
    "trip"       = Join-Path $OutputRoot "trip\$PrefCode"
    "trajectory" = Join-Path $OutputRoot "trajectory\$PrefCode"
}

Write-Host "--- File counts ---" -ForegroundColor Yellow
foreach ($Stage in @("activity", "trip", "trajectory")) {
    $Dir = $Dirs[$Stage]
    if (Test-Path $Dir) {
        $Count = (Get-ChildItem -Path $Dir -Filter "*.csv" | Measure-Object).Count
        $SizeMB = [math]::Round((Get-ChildItem -Path $Dir -Filter "*.csv" | Measure-Object -Property Length -Sum).Sum / 1MB, 1)
        Write-Host "  ${Stage}: $Count files ($SizeMB MB)"
    } else {
        Write-Host "  ${Stage}: (not started)" -ForegroundColor DarkGray
    }
}
Write-Host ""

# Log inspection
$LogDir = Join-Path $OutputRoot "logs"
if (Test-Path $LogDir) {
    Write-Host "--- Recent log entries ---" -ForegroundColor Yellow
    foreach ($LogFile in @("activity.log", "trip.log")) {
        $LogPath = Join-Path $LogDir $LogFile
        if (Test-Path $LogPath) {
            Write-Host "  [$LogFile] last 5 lines:" -ForegroundColor DarkGray
            Get-Content $LogPath -Tail 5 | ForEach-Object { Write-Host "    $_" }

            # Check for errors
            $Errors = Select-String -Path $LogPath -Pattern "ERROR|Exception|FAILURE|FATAL" -SimpleMatch | Select-Object -First 3
            if ($Errors) {
                Write-Host "    ERRORS FOUND:" -ForegroundColor Red
                $Errors | ForEach-Object { Write-Host "    $($_.Line)" -ForegroundColor Red }
            }
            Write-Host ""
        }
    }
}

# Mode share from trip files
$TripDir = $Dirs["trip"]
if ((Test-Path $TripDir) -and (Get-ChildItem -Path $TripDir -Filter "*.csv" | Measure-Object).Count -gt 0) {
    Write-Host "--- Mode share (trip files) ---" -ForegroundColor Yellow

    # Transport mode mapping
    $ModeNames = @{
        0 = "NOT_DEFINED"
        1 = "TRAIN"
        2 = "BUS"
        3 = "CAR"
        4 = "BICYCLE"
        5 = "WALK"
        6 = "TAXI"
    }

    $ModeCounts = @{}
    $TotalTrips = 0

    Get-ChildItem -Path $TripDir -Filter "*.csv" | ForEach-Object {
        Import-Csv -Path $_.FullName -Header @("pid","dep","olon","olat","dlon","dlat","mode","purpose","labor") |
        ForEach-Object {
            $Mode = [int]$_.mode
            if (-not $ModeCounts.ContainsKey($Mode)) { $ModeCounts[$Mode] = 0 }
            $ModeCounts[$Mode]++
            $TotalTrips++
        }
    }

    if ($TotalTrips -gt 0) {
        Write-Host "  Total trips: $TotalTrips"
        $ModeCounts.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
            $Pct = [math]::Round($_.Value / $TotalTrips * 100, 1)
            $Name = if ($ModeNames.ContainsKey($_.Key)) { $ModeNames[$_.Key] } else { "MODE_$($_.Key)" }
            Write-Host ("  {0,-15} {1,8:N0}  ({2,5:N1}%)" -f $Name, $_.Value, $Pct)
        }
    }
    Write-Host ""
}

# Validation summary
$SummaryPath = Join-Path $OutputRoot "validation\$PrefCode\summary.md"
if (Test-Path $SummaryPath) {
    Write-Host "--- Validation summary ---" -ForegroundColor Yellow
    Get-Content $SummaryPath
} else {
    Write-Host "--- Validation: not yet run ---" -ForegroundColor DarkGray
}
