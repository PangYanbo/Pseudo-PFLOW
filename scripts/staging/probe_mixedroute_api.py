#!/usr/bin/env python3
"""Probe the CSIS GetMixedRoute API to diagnose transit response behavior.

Sends a small number of representative queries with different AppDate values
and dumps raw responses for forensic analysis.

Usage:
    PFLOW_API_USER=xxx PFLOW_API_PASS=xxx python3 scripts/staging/probe_mixedroute_api.py

    # Override base URL (e.g. localhost relay):
    PFLOW_API_USER=xxx PFLOW_API_PASS=xxx python3 scripts/staging/probe_mixedroute_api.py \\
        --base-url https://localhost
"""

import argparse
import json
import os
import sys
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Default API base URL (overridable via --base-url)
DEFAULT_BASE_URL = "https://pflow-api.csis.u-tokyo.ac.jp"

# Representative OD pairs in Shizuoka (pref 22) — different distance ranges
OD_PAIRS = [
    # Short urban: Hamamatsu station area → Hamamatsu city center (~3km)
    {"name": "Hamamatsu_short", "olon": 137.7350, "olat": 34.7040, "dlon": 137.7600, "dlat": 34.7200},
    # Medium: Hamamatsu → Iwata (~20km)
    {"name": "Hamamatsu_Iwata", "olon": 137.7350, "olat": 34.7040, "dlon": 137.8510, "dlat": 34.7180},
    # Long: Hamamatsu → Shizuoka city (~70km, should have rail)
    {"name": "Hamamatsu_Shizuoka", "olon": 137.7350, "olat": 34.7040, "dlon": 138.3830, "dlat": 34.9750},
    # Tokyo area (pref 13, known transit-heavy): Shinjuku → Tokyo station (~5km)
    {"name": "Shinjuku_Tokyo", "olon": 139.7005, "olat": 35.6895, "dlon": 139.7671, "dlat": 35.6812},
]

# AppDate values to test
APP_DATES = [
    "20240401",   # Current hardcoded value
    "20250401",   # One year later
    "20260313",   # Today
    "20231001",   # Past date
]

APP_TIME = "0800"  # 8:00 AM


def create_session(user, password, base_url):
    url = f"{base_url}/webapi/CreateSession"
    print(f"CreateSession URL: {url}")
    resp = requests.post(url, data={
        "UserID": user, "Password": password
    }, verify=False)
    print(f"CreateSession: HTTP {resp.status_code}")
    body = resp.text.strip()
    print(f"  Raw response: {body!r}")
    parts = body.split(",")
    session_id = (parts[1] if len(parts) > 1 else parts[0]).strip()
    print(f"  Session ID: {session_id}")
    return session_id


def query_mixed_route(session_id, od, app_date, base_url, app_time="0800"):
    url = f"{base_url}/webapi/GetMixedRoute"
    params = {
        "UnitTypeCode": "2",
        "StartLongitude": str(od["olon"]),
        "StartLatitude": str(od["olat"]),
        "GoalLongitude": str(od["dlon"]),
        "GoalLatitude": str(od["dlat"]),
        "TransportCode": "1",
        "AppDate": app_date,
        "AppTime": app_time,
        "MaxRadius": "1000",
        "MaxRoutes": "9",
    }
    cookies = {"WebApiSessionID": session_id}
    resp = requests.post(url, data=params, cookies=cookies, verify=False)
    return resp


def query_road_route(session_id, od, base_url):
    url = f"{base_url}/webapi/GetRoadRoute"
    params = {
        "UnitTypeCode": "2",
        "StartLongitude": str(od["olon"]),
        "StartLatitude": str(od["olat"]),
        "GoalLongitude": str(od["dlon"]),
        "GoalLatitude": str(od["dlat"]),
    }
    cookies = {"WebApiSessionID": session_id}
    resp = requests.post(url, data=params, cookies=cookies, verify=False)
    return resp


def analyze_response(resp, label):
    print(f"\n  [{label}] HTTP {resp.status_code}")
    body = resp.text
    # Truncate if very long
    if len(body) > 2000:
        print(f"  Raw body ({len(body)} chars, truncated):")
        print(f"  {body[:1500]}")
        print(f"  ... ({len(body) - 1500} more chars)")
    else:
        print(f"  Raw body ({len(body)} chars):")
        print(f"  {body}")

    # Try to parse as JSON
    try:
        data = json.loads(body)
    except json.JSONDecodeError as e:
        print(f"  JSON parse error: {e}")
        return

    # Check all top-level keys
    if isinstance(data, dict):
        print(f"  Top-level keys: {list(data.keys())}")
        # Check for error/status fields
        for key in ["status", "error", "message", "result", "Status", "Error", "Message", "Result", "ErrorCode", "error_code"]:
            if key in data:
                print(f"  >>> {key}: {data[key]}")
        # Check the fields TripGenerator uses
        for key in ["num_station", "fare", "total_time", "total_distance", "features"]:
            val = data.get(key)
            if val is not None:
                if key == "features" and isinstance(val, list):
                    print(f"  {key}: list with {len(val)} elements")
                    if val:
                        print(f"    first feature keys: {list(val[0].keys()) if isinstance(val[0], dict) else type(val[0])}")
                else:
                    print(f"  {key}: {val}")
            else:
                print(f"  {key}: NOT PRESENT")
    elif isinstance(data, list):
        print(f"  Response is a list with {len(data)} elements")
        if data:
            print(f"  First element keys: {list(data[0].keys()) if isinstance(data[0], dict) else data[0]}")

    return data


def main():
    parser = argparse.ArgumentParser(description="Probe CSIS WebAPI endpoints")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL,
                        help=f"API base URL (default: {DEFAULT_BASE_URL})")
    args = parser.parse_args()
    base_url = args.base_url.rstrip("/")

    user = os.environ.get("PFLOW_API_USER")
    password = os.environ.get("PFLOW_API_PASS")
    if not user or not password:
        print("ERROR: Set PFLOW_API_USER and PFLOW_API_PASS", file=sys.stderr)
        sys.exit(1)

    print("=" * 70)
    print("CSIS GetMixedRoute API Probe")
    print(f"Base URL: {base_url}")
    print("=" * 70)

    session_id = create_session(user, password, base_url)
    print()

    # First: one road route query as baseline (confirm API is working)
    print("--- Road route baseline (Hamamatsu→Shizuoka) ---")
    resp = query_road_route(session_id, OD_PAIRS[2], base_url)
    analyze_response(resp, "RoadRoute")

    # Main probe: mixed route queries with different AppDates
    for od in OD_PAIRS:
        print()
        print("=" * 70)
        print(f"OD pair: {od['name']}")
        print(f"  Origin:  ({od['olon']}, {od['olat']})")
        print(f"  Dest:    ({od['dlon']}, {od['dlat']})")
        print("=" * 70)

        for app_date in APP_DATES:
            resp = query_mixed_route(session_id, od, app_date, base_url, APP_TIME)
            analyze_response(resp, f"AppDate={app_date}")

    print()
    print("=" * 70)
    print("Probe complete.")


if __name__ == "__main__":
    main()
