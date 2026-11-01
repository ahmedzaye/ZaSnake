#!/usr/bin/env python3
"""Static release readiness check, designed to run locally in Android Studio's Terminal.
Never treats a static check as proof that a paid purchase or cloud service actually works.
"""
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
failures = []
checks = []


def check(label, ok, why=""):
    checks.append((label, ok, why))
    if not ok:
        failures.append(label)


def read(path):
    file = root / path
    if not file.is_file():
        return ""
    return file.read_text(encoding="utf-8")


gradle = read("app/build.gradle.kts")
project = read("build.gradle.kts")
resources = read("app/src/main/res/values/strings.xml")
manifest = read("app/src/main/AndroidManifest.xml")
release = read("app/src/release/res/values/admob_ids.xml")
firebase_file = root / "app/google-services.json"

check("Firebase google-services.json exists", firebase_file.exists())
if firebase_file.exists():
    try:
        obj = json.loads(firebase_file.read_text(encoding="utf-8"))
        packages = {c.get("client_info", {}).get("android_client_info", {}).get("package_name")
                    for c in obj.get("client", [])}
        check("Firebase Android app package matches", "com.zayedart.zasnake" in packages)
        check("Firebase project name matches", obj.get("project_info", {}).get("project_id") == "za-snake")
    except (ValueError, OSError) as e:
        check("Firebase JSON parses", False, str(e))

check("Google services plugin in root and app", 'com.google.gms.google-services' in project and
      'id("com.google.gms.google-services")' in gradle)
check("Production package", 'applicationId = "com.zayedart.zasnake"' in gradle)
code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
check("Version code ready for first release (verify in Play Console)", bool(code and int(code.group(1)) >= 3))
check("Version name 1.0.0", 'versionName = "1.0.0"' in gradle)
check("Google Play Games project ID set", '>914975720790<' in resources)
check("All 11 Play Games achievements configured", resources.count('REPLACE_WITH_PLAY_CONSOLE_ACHIEVEMENT_ID') == 0)
check("Release AdMob app ID present", 'ca-app-pub-6495262421269253~4884698925' in release)
check("Rewarded revive production unit present", 'ca-app-pub-6495262421269253/5814637214' in release)
check("Rewarded double production unit present", 'ca-app-pub-6495262421269253/4581446596' in release)
check("AdMob manifest configured", 'com.google.android.gms.ads.APPLICATION_ID' in manifest)
check("UMP dependency configured", 'com.google.android.ump:user-messaging-platform' in gradle)
check("Google Play Billing dependency configured", 'com.android.billingclient:billing:' in gradle)
checkout = read("app/src/main/java/com/ahmedzayed/zasnake/ui/ZaSnakeApp.kt")
check("Real-money checkout implemented & verified", 'onClick = {}, enabled = false' not in checkout
      and 'launchBillingFlow' in (checkout + read('app/src/main/java/com/ahmedzayed/zasnake/billing/CoinPriceCatalog.kt')),
      'A working checkout still requires server verification and real-device testing')

for name, ok, why in checks:
    status = 'PASS' if ok else 'BLOCKED'
    print(f'[{status}] {name}' + (f' - {why}' if why else ''))

print(f'\nStatic checks: {len(checks) - len(failures)}/{len(checks)} passed')
print('Separate mandatory manual checks: Android Studio builds/tests, signed AAB, AdMob test-device + UMP,')
print('Play Games OAuth/SHA1/Saved Games, deployed purchase verifier, license-tester purchases, Google Play policies.')
sys.exit(1 if failures else 0)
