#!/usr/bin/env python3
"""Fail-closed source-level widget/FAB/gold budget contract for sandbox 2.2.1.
The original financial and navigation behavior is not changed by this test.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def get(file):
    return (root / file).read_text(encoding="utf-8")
fab=get("glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java")
assert "new RadialGradient" not in fab and "new LinearGradient" not in fab
assert "highlight" not in fab.lower() or "NO internal radial highlight" in fab
assert fab.count("canvas.drawCircle(") == 2, "FAB must paint only body + outer rim"
assert "getPress()" in fab and "setPress(" in fab, "Original pressure contract"
gold=get("app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java")
wrapped=gold.split("static View wrap(",1)[1].split("/** Paint only top",1)[0]
assert "new LiquidGlassView" not in wrapped and "buildGlass" not in wrapped
assert "setBackdropSource" not in wrapped and "bindBackdrop" not in wrapped
assert "host.addView(content" in wrapped and "host.setBackground(original.mutate())" in wrapped
assert "host.addView(specular" in wrapped, "Cheap edge material must survive"
premium=get("glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java")
assert "!(theme >= ROYAL_DARK && theme <= CHAMPAGNE)" in premium
ui=get("app/src/main/java/app/wensarafet/tracker/Ui.java")
assert "new app.wensarafet.glass.WenFabBubbleDrawable" in ui
quick=get("app/src/main/res/layout/widget_quick_action.xml")
assert 'android:scaleType="centerCrop"' in quick
assert 'android:id="@+id/widget_choose_contact"' in quick
for name, id in (
    ("widget_balances.xml", "widget_balance_root"),
    ("widget_balances_triple.xml", "widget_balance_triple_root"),
):
    layout=get("app/src/main/res/layout/"+name)
    assert ('android:id="@+id/'+id+'"') in layout
    assert layout.count('android:scaleX="1.15"') == (2 if id=="widget_balance_root" else 3)
for name in ("jawwal","palpay","bop"):
    glass=get("app/src/main/res/drawable/widget_crystal_quick_"+name+".xml")
    assert "vector" in glass and "android:strokeWidth" in glass
gradle=get("app/build.gradle")
assert 'applicationIdSuffix ".sandbox"' in gradle
assert 'versionName "2.2.2"' in gradle
print("PASS: widget zoom/rim; one-tone FAB; golden cached optics; original widget ids; 2.2.1 sandbox")

# Regression gate: both balance layouts must allocate almost all available
# vertical space to artwork, not fix it at 54dp / 47dp.
for name in ("widget_balances.xml", "widget_balances_triple.xml"):
    layout=get("app/src/main/res/layout/"+name)
    assert layout.count('android:layout_weight="1"') >= (4 if name=="widget_balances.xml" else 6), (
        "Each balance logo should adapt to the host height")
    assert layout.count('android:scaleX="1.15"') == (2 if name=="widget_balances.xml" else 3)
activity=get("app/src/main/java/app/wensarafet/tracker/MainActivity.java")
assert "fab.setElevation(0f)" in activity
assert "fabBlurController.refreshIfDue()" in activity
assert "RenderEffect.createBlurEffect" in activity
frost=get("app/src/main/java/app/wensarafet/tracker/WenFabBackdropBlur.java")
assert "scroll.draw(c)" in frost and "Bitmap.createBitmap(w,h" in frost
assert "FileOutputStream" not in frost and "getExternalFilesDir" not in frost
assert "lastFrame < 200L" in frost
print("PASS: balance logos flexible; 4dp real FAB backdrop blur; no FAB outer shadow")
