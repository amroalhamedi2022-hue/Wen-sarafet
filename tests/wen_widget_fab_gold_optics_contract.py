#!/usr/bin/env python3
"""Fail-closed visual and performance source contract for Wen sandbox 2.2.4.
No production money, security or navigation logic is allowed to change.
"""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
def get(path):
    return (root / path).read_text(encoding="utf-8")
app=get("app/src/main/java/app/wensarafet/tracker/MainActivity.java")
fab=get("glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java")
gold=get("app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java")
premium=get("glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java")
ui=get("app/src/main/java/app/wensarafet/tracker/Ui.java")
gradle=get("app/build.gradle")
assert 'versionCode 34' in gradle and 'versionName "2.2.4"' in gradle
assert 'applicationIdSuffix ".sandbox"' in gradle
assert "new app.wensarafet.glass.WenFabBubbleDrawable" in ui
# The user's *live* background must show through the glass continuously.
assert "fab.setBackground(Ui.glassFloatingAddBackground(this));" in app
assert "fab.setElevation(0f)" in app
assert "root.addView(fab, fabParams);" in app
assert "RenderEffect.createBlurEffect" not in app
assert "fabBlur" not in app
assert "WenFabBackdropBlur" not in app
assert not (root/"app/src/main/java/app/wensarafet/tracker/WenFabBackdropBlur.java").exists()
assert "showAddTransaction(); // exact same original action" in app
assert "Ui.pressAnimation(fab);" in app
assert "getPress()" in fab and "setPress(float fraction)" in fab
assert fab.count("canvas.drawCircle(")==2
assert "new RadialGradient" not in fab and "new LinearGradient" not in fab
assert "Bitmap" not in fab and "RenderEffect" not in fab
assert re.search(r"glassOpacity=dark\?\.17f:\.15f",fab)
assert "edge.setStrokeWidth(Math.max(1f,.85f*density))" in fab
# Gold surface visual materials keep exact layer order, but no wrapper views.
wrap=gold.split("static View wrap(",1)[1].split("/** Paint only top",1)[0]
assert "new LayerDrawable(" in wrap and "new Specular(" in wrap
assert "content.setBackground(preservedOptics)" in wrap
assert "host.addView" not in wrap and "new LiquidGlassView" not in wrap
assert "new FrameLayout(context)" not in wrap
assert "attachGoldBackdrop" in gold
assert "canvas.drawRoundRect" in premium and "WenThemeArtDirection.configure" in premium
assert "goldCardCache" in premium and "drawFullMaterial" in premium
assert "surface==WenReferenceGlassDrawable.Surface.CARD" in premium
assert "(long)width*height<=550000L" in premium
assert "!(theme >= ROYAL_DARK && theme <= CHAMPAGNE)" in premium
# All 3 wallet widgets, double and triple balances retain IDs and big images.
quick=get("app/src/main/res/layout/widget_quick_action.xml")
assert 'android:scaleType="centerCrop"' in quick
assert 'android:id="@+id/widget_choose_contact"' in quick
for name,rootid,n in [("widget_balances.xml","widget_balance_root",2),
                        ("widget_balances_triple.xml","widget_balance_triple_root",3)]:
    layout=get("app/src/main/res/layout/"+name)
    assert f'android:id="@+id/{rootid}"' in layout
    assert layout.count('android:scaleX="1.15"')==n
    assert layout.count('android:layout_weight="1"')>=2*n
for suffix in ("jawwal","palpay","bop"):
    assert "<vector" in get("app/src/main/res/drawable/widget_crystal_quick_"+suffix+".xml")
print("PASS: live clear FAB/press/click; all widgets; static golden optics + cached card render; sandbox v2.2.4")
