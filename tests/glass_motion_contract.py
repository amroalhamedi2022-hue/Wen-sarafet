#!/usr/bin/env python3
"""M1–M3 visual-motion source contracts for standalone app; no business code edits."""
from pathlib import Path
p = Path(__file__).resolve().parents[1]
engine = (p/"glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java").read_text(encoding="utf-8")
ui = (p/"app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")
main = (p/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
assert "package app.wensarafet.glass;" in engine
assert "wensarafet.tracker" not in engine
assert "FinanceRules" not in engine and "FinanceDbHelper" not in engine
assert "return true;" in engine.split("class NavigationGesture", 1)[1]
assert "tabs[hovered].performClick();" in engine
assert "beforeNavigate.run();" in engine
assert "MotionEvent.ACTION_MOVE" in engine
assert "nearest(float fingerX)" in engine
assert "if (hovered == selected)" in engine
assert "insideVertical" in engine
for action in ["MotionEvent.ACTION_DOWN", "MotionEvent.ACTION_MOVE",
               "MotionEvent.ACTION_UP", "MotionEvent.ACTION_CANCEL"]:
    assert action in engine
assert "ViewConfiguration.get(view.getContext()).getScaledTouchSlop()" in engine
assert "glowTo(1f, 340)" in engine
assert "glowTo(0f, 175)" in engine
assert "new RadialGradient" in engine
assert "view.getForeground() == null" in engine, "Do not overwrite native foreground ripple"
assert "lowPower(view.getContext())" in engine
assert "ValueAnimator.areAnimatorsEnabled()" in engine
assert "onViewDetachedFromWindow" in engine and "state.reset();" in engine
assert "new " not in engine.split("@Override public void draw(Canvas canvas)",1)[1].split(
    "@Override public void setAlpha", 1)[0], "Do not allocate in glint draw hot path"
for forbidden in ["while (true)", "postDelayed", "TimerTask", "Thread.sleep",
                  "INTERNET", "android.permission"]:
    assert forbidden not in engine
assert "GlassMotion.bindPress(view, SKY)" in ui
assert "GlassMotion.bindNavigationTab(" in main
assert "bindNavigationTab(View tab, FrameLayout track, View lens" in engine
assert "scaleX(1.06f).scaleY(1.09f)" in engine
assert "MotionEvent.ACTION_CANCEL" in engine
assert "lens.animate().translationX(view.getLeft())" in engine
assert "lens.setTranslationX(destination)" in engine
assert "boolean fromNavGesture = pendingGlassNavigationSwitch;" in main
assert "if (fromNavGesture)" in main
assert "next.setAlpha(1f);" in main
assert "screenHost.removeView(previous);" in main
assert "setDuration(115)" in main
assert "liquid ? 2 : 10" in ui
assert "liquid ? 125 : 300" in ui
for label in ["الرئيسية", "العمليات", "الديون", "التحليل"]:
    assert 'setContentDescription("تبويب ' + label + '")' in main
assert "FrameLayout track = new FrameLayout(this);" in main
assert "lastGlassNavigationTab = selectedTab;" in main
assert "GlassMotion.slideLens(" in main
assert "lens.setElevation(" not in main, "Glass lens must not cover its selected icon/text"
assert "tabs[previousIndex].getLeft()" in engine and "selected.getLeft()" in engine
assert "previousIndex != selectedIndex" in engine
assert "if (selected && !Ui.isLiquidTheme(this))" in main
assert "home.setOnClickListener(v -> showMain(false));" in main
assert "tx.setOnClickListener(v -> showMain(true));" in main
assert "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));" in main
assert "analytics.setOnClickListener(v -> showAnalyticsScreen());" in main
print("PASS: event-only, RTL-aware liquid nav + tap/hold feedback; original tab actions preserved")
