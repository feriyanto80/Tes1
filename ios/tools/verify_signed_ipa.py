#!/usr/bin/env python3
"""Validate PBS Balap signed .ipa for Ad Hoc OTA distribution.

Runs on macOS (codesign, security). Does NOT sign files.
Fails closed if no valid signed payload / Ad Hoc profile is present.
No credentials or registered UDIDs are printed.
"""
import argparse
import datetime as dt
import json
import os
import pathlib
import plistlib
import subprocess
import sys
import tempfile
import zipfile

EXPECTED_BUNDLE_ID = "id.pbsgroup.pbsbalap"

def fail(msg: str) -> None:
    raise SystemExit("FAILED IPA DISTRIBUTION CHECK: " + msg)

def run(*args: str) -> bytes:
    p = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    if p.returncode:
        err = p.stderr.decode(errors="replace").strip()
        fail(f"{args[0]} exited {p.returncode}: {err[-700:]}")
    return p.stdout

def verify(ipa: pathlib.Path, expected_team: str = "") -> dict:
    if not ipa.is_file() or ipa.stat().st_size < 10000:
        fail("IPA missing or too small")
    with tempfile.TemporaryDirectory(prefix="pbs-balap-ipa-") as tmp:
        root = pathlib.Path(tmp)
        with zipfile.ZipFile(ipa) as z:
            apps = sorted({x.split("/")[1] for x in z.namelist() if x.startswith("Payload/") and x.count("/") >= 2 and x.split("/")[1].endswith(".app")})
            if len(apps) != 1:
                fail(f"Expected exactly one app in Payload, found {len(apps)}")
            prefix = "Payload/" + apps[0] + "/"
            members = set(z.namelist())
            for required in ["Info.plist", "embedded.mobileprovision", "_CodeSignature/CodeResources"]:
                if prefix + required not in members:
                    fail("Missing " + required + "; this IPA is not valid for OTA installation")
            info = plistlib.loads(z.read(prefix + "Info.plist"))
            bundle_id = info.get("CFBundleIdentifier")
            if bundle_id != EXPECTED_BUNDLE_ID:
                fail(f"Unexpected bundle ID {bundle_id!r}, expected {EXPECTED_BUNDLE_ID}")
            executable = info.get("CFBundleExecutable", "")
            if not executable or prefix + executable not in members:
                fail("Missing main executable")
            z.extractall(root)
        app = root / "Payload" / apps[0]
        run("/usr/bin/codesign", "--verify", "--strict", "--verbose=2", str(app))
        signature_info = run("/usr/bin/codesign", "-d", "--entitlements", ":-", str(app))
        # codesign emits non-sensitive entitlements to stdout or stderr depending on macOS version.
        profile_bytes = run("/usr/bin/security", "cms", "-D", "-i", str(app / "embedded.mobileprovision"))
        profile = plistlib.loads(profile_bytes)
        team_ids = profile.get("TeamIdentifier") or []
        if expected_team and expected_team not in team_ids:
            fail("Provisioning TeamIdentifier mismatches selected Apple Team")
        prov_ent = profile.get("Entitlements") or {}
        application_id = prov_ent.get("application-identifier") or ""
        if not application_id.endswith("." + EXPECTED_BUNDLE_ID):
            fail("Provisioning profile App ID does not match PBS Balap bundle ID")
        expires = profile.get("ExpirationDate")
        now = dt.datetime.now(dt.timezone.utc)
        if not isinstance(expires, dt.datetime):
            fail("Profile has no expiration date")
        if expires.tzinfo is None:
            expires = expires.replace(tzinfo=dt.timezone.utc)
        if expires <= now:
            fail("Provisioning profile has expired")
        udids = profile.get("ProvisionedDevices") or []
        if profile.get("ProvisionsAllDevices", False):
            fail("This workflow is specifically for Ad Hoc, not Enterprise, distribution")
        if not udids:
            fail("No devices in Ad Hoc provisioning profile; register iPhone UDIDs first")
        # Do not expose UDIDs or provisioning contents in public CI logs/artifacts.
        return {
            "result": "PASSED",
            "name": info.get("CFBundleDisplayName") or info.get("CFBundleName"),
            "bundle_id": bundle_id,
            "version": info.get("CFBundleShortVersionString"),
            "build": info.get("CFBundleVersion"),
            "signing_verified_by_macos_codesign": True,
            "ad_hoc_profile_present": True,
            "registered_device_count": len(udids),
            "profile_expires_utc": expires.isoformat(),
            "ready_for_betadrop_upload": True,
            "note": "Installation restricted to registered iPhone UDIDs in the profile. Not tested on a physical device."
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("ipa", type=pathlib.Path)
    parser.add_argument("--team", default=os.environ.get("IOS_TEAM_ID", ""))
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        report = verify(args.ipa, args.team)
        data = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
        print(data)
        if args.report:
            args.report.write_text(data, encoding="utf-8")
    except (zipfile.BadZipFile, plistlib.InvalidFileException) as error:
        fail(str(error))
