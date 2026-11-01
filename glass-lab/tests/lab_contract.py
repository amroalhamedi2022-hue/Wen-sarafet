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
assert len(engine_files) >= 5
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
assert 'canvas.drawText(days[i]' in screen
assert "glassCard(false, 23, 8)" in screen
assert "new LinearLayout.LayoutParams(-1, d(130))" in screen
assert "new LinearLayout.LayoutParams(-1, d(61))" in screen
assert 'currentPage == i ? (theme.dark ? Color.WHITE : theme.deep)' in screen
assert 'getIntent().getIntExtra("preview_page", 0)' in screen
assert 'getIntent().getIntExtra("preview_theme", 0)' in screen
assert "GlassSurfaces.navigation(this, theme, 27, eco)" in screen
assert "if (!controlsVisible) return;" in screen
assert 'android.view.WindowInsets.Type.systemBars()' in screen
assert 'd(187)' in screen
assert "GlassSurfaces.headerButton(this, theme)" in screen
assert "private void showSettings(LinearLayout body)" in screen
assert "private void addSettingsRow(" in screen
assert "GlassSurfaces.chart(this, theme, 23, eco)" in screen
assert "إجمالي أرصدتك" in screen
assert "ميزانية الشهر — ILS" in screen
assert "تجربة تمرير بدون حسابات فعلية" not in screen
assert "currentPage = 4; render();" in screen
assert "GlassSurfaces.selectedTab(this, theme, 19)" in screen
assert "GlassSurfaces.opticalHero(this, theme, 26, eco, optics)" in screen
assert "WenOpticalDrawable" in engine
assert "new RuntimeShader(AGSL)" in engine
assert "canvas.isHardwareAccelerated()" in engine
assert 'runtime.setInputShader("scene"' in engine
assert "GlassSurfaces.backdrop(this, theme, eco)" in screen
assert "if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;" in screen
assert "case" not in manifest.read_text(encoding="utf-8").lower()  # no background service
assert len(re.findall(r'new GlassPalette\(', (root/"engine/src/main/java/app/wensarafet/glass/GlassPalette.java").read_text())) == 4
print("PASS: standalone glass lab has four themes, no protected-app data, no background work")
