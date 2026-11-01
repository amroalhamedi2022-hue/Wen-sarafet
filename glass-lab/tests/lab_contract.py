#!/usr/bin/env python3
"""No activation, app data, background work or privacy-invasive permissions in lab."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = root / "showcase/src/main/AndroidManifest.xml"
ET.parse(manifest)
app = (root / "showcase/build.gradle").read_text(encoding="utf-8")
screen = (root / "showcase/src/main/java/app/wensarafet/glasslab/MainActivity.java").read_text(encoding="utf-8")
engine_files = list((root / "engine/src/main/java").rglob("*.java"))
engine = "\n".join(p.read_text(encoding="utf-8") for p in engine_files)
assert "applicationId 'app.wensarafet.glasslab'" in app
assert "implementation project(':engine')" in app
assert len(engine_files) >= 4
for forbidden in [
    "LicenseManager", "Activation", "FinanceDb", "sqlite", "SQLite",
    "BroadcastReceiver", "TextWatcher", "READ_SMS", "INTERNET",
    "android.permission", "BIND_ACCESSIBILITY_SERVICE", "foregroundService"
]:
    assert forbidden not in screen + engine + manifest.read_text(encoding="utf-8"), forbidden
for forbidden in ["postDelayed", "while (true)", "Choreographer", "TimerTask",
                  "Handler", "requestAnimationFrame", "setRepeatCount", "Thread.sleep"]:
    assert forbidden not in engine + screen, forbidden
assert "GlassPalette.at(themeIndex)" in screen
assert "GlassSurfaces.backdrop(this, theme, eco)" in screen
assert "if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;" in screen
assert "case" not in manifest.read_text(encoding="utf-8").lower()  # no background service
assert len(re.findall(r'new GlassPalette\(', (root/"engine/src/main/java/app/wensarafet/glass/GlassPalette.java").read_text())) == 4
print("PASS: standalone glass lab has four themes, no protected-app data, no background work")
