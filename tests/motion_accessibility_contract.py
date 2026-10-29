#!/usr/bin/env python3
"""Source contract for C9: no invisible Activity or navigation with animations disabled.

Full interaction still requires an Android device with animator duration scale = 0.
"""
from pathlib import Path

main = (Path(__file__).resolve().parents[1]
        / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
screen = main.split("@Override public void setContentView(View next) {", 1)[1].split(
    "@Override protected void onCreate(", 1)[0]
initial = screen.split("if (screenHost == null) {", 1)[1].split("if (activeScreen == next)", 1)[0]
assert initial.index("if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;") < initial.index(
    "next.setAlpha(0f);")
subsequent = screen.split("screenHost.addView(next, match);", 2)[-1]
assert subsequent.index("if (!android.animation.ValueAnimator.areAnimatorsEnabled())") < subsequent.index(
    "next.setAlpha(0f);")
immediate = subsequent.split("if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {", 1)[1].split(
    "return;", 1)[0]
for expected in ("next.setAlpha(1f);", "next.setTranslationX(0f);", "screenHost.removeView(previous);"):
    assert expected in immediate, expected

main_scroll = main.split("scroll.setOnScrollChangeListener(", 1)[1].split(
    "setContentView(root);", 1)[0]
assert main_scroll.count("if (!android.animation.ValueAnimator.areAnimatorsEnabled())") >= 2
assert "bottomNav.setVisibility(View.INVISIBLE);" in main_scroll
assert "bottomNav.setAlpha(1f);" in main_scroll
assert "bottomNav.setTranslationY(0f);" in main_scroll
print("MotionAccessibilityContract passed (source only; device NOT TESTED)")
