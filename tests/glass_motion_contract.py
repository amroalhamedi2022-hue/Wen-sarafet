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
assert "scaleX(1.06f).scaleY(1.09f)" not in engine, "No separate enlarged touch bubble"
assert "optic.bindPhysicalPill(lens);" in engine
assert "MotionEvent.ACTION_CANCEL" in engine
assert "lens.animate().translationX(fingerLensX(view.getLeft()+event.getX()))" in engine
assert "private float fingerLensX(float fingerX)" in engine
assert "lens.setTranslationX(destination)" in engine
assert "boolean fromNavGesture = pendingGlassNavigationSwitch;" in main
assert "if (fromNavGesture || paletteSwitch)" in main
assert "boolean paletteSwitch = pendingPaletteSwitch;" in main
assert "pendingPaletteSwitch = false;" in main
assert "lastGlassNavigationTab = -1;" in main
assert "next.setAlpha(1f);" in main
assert "screenHost.removeView(previous);" in main
assert "setDuration(115)" in main
assert "liquid ? 2 : 10" in ui
assert "liquid ? 125 : 300" in ui
for label in ["الرئيسية", "العمليات", "الديون", "التحليل"]:
    assert 'setContentDescription("تبويب ' + label + '")' in main
assert "FrameLayout track = new FrameLayout(this);" in main
assert "view.setSelected(selected);" in main
assert "tabs[hovered].performClick();" in engine
assert "tracking=true; cancelled=false;" in engine
assert "lens.setTranslationX(destination)" in engine
assert "new NavigationGesture(" in engine
assert "boolean fromNavGesture = pendingGlassNavigationSwitch;" in main
assert "lastGlassNavigationTab = selectedTab;" in main
assert "GlassMotion.slideLens(" in main
assert "int lensStart = pendingGlassNavigationSwitch ? selectedTab : lastGlassNavigationTab;" in main
assert "track, lens, items, lensStart, selectedTab" in main
lensSource = (p/"glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java").read_text(encoding="utf-8")
assert "GlassMotion.bindNavigationTab(" in main
assert "glassSlidingNavigationLens(this)" in main
assert "new app.wensarafet.glass.WenNavigationLensDrawable(" in ui
assert "new LinearGradient" in lensSource
assert "new " not in lensSource.split("@Override public void draw(Canvas canvas)",1)[1].split(
        "private void applyAlpha()",1)[0]
assert "postDelayed" not in lensSource and "ValueAnimator" not in lensSource
assert "finishNavigationLensHandoff(track, lens)" in main
assert "public static void finishNavigationLensHandoff(FrameLayout track, View lens)" in engine
assert "morphTo(1f, 190);" in engine, "Hold must create a deeper optical droplet"
assert "morphTo(0f, 190);" in engine, "Release/cancel must restore the resting pill"
assert "lens.setTranslationX(tabs[hovered].getLeft());" in engine, "Pin at destination before click"
assert "lens.setTranslationX(destination)" in engine, "Dragged lens follows fingertip"
assert "WenNavigationLensDrawable glass = opticalLens();" in engine
assert "ValueAnimator.ofFloat(glass.getMorph(), destination)" in engine
assert "glass.setMorph(1f);" in engine
assert "glass.setMorph(0f);" in engine
assert "private final RectF fluid = new RectF();" in lensSource
assert "public void setMorph(float fraction)" in lensSource
assert "fluid.set(surface);" in lensSource, "Pressed outline must preserve resting pill dimensions"
assert "canvas.drawRoundRect(fluid, r, r, halo);" not in lensSource
assert "if (morphAnimator != null)" in engine
move = engine.split("class NavigationGesture", 1)[1].split(
    "case MotionEvent.ACTION_MOVE:", 1)[1].split("cancelled=false;", 1)[0]
assert "returnToSelected();" not in move, "Do not snap home mid-Hold when leaving vertically"
assert "Only the final UP/CANCEL decides" in engine
comparison = main.split("private View comparisonCard(", 1)[1].split(
    "private void showIncomePeriodScreen(", 1)[0]
assert "SandboxMode.isSandbox(this) && Ui.isLiquidTheme(this)" in comparison
assert "glassBalanceBackground(this, 20)" in comparison
assert "setAutoSizeTextTypeUniformWithConfiguration(13, 18, 1" in comparison
assert "trialGlass ? 136 : 116" in comparison
assert "lens.setElevation(" not in main, "Glass lens must not cover its selected icon/text"
assert "tabs[previousIndex].getLeft()" in engine and "selected.getLeft()" in engine
assert "previousIndex != selectedIndex" in engine
assert "if (selected && !Ui.isLiquidTheme(this))" in main
# The original click destinations are preserved through the shared tab router.
assert "home.setOnClickListener(v -> navigatePrimaryTab(TAB_HOME));" in main
assert "tx.setOnClickListener(v -> navigatePrimaryTab(TAB_TRANSACTIONS));" in main
assert "debts.setOnClickListener(v -> navigatePrimaryTab(TAB_DEBTS));" in main
assert "analytics.setOnClickListener(v -> navigatePrimaryTab(TAB_ANALYTICS));" in main
for target in ["showMain(false);","showMain(true);",
        "showDebtsScreen(FinanceRules.DEBT_RECEIVABLE);","showAnalyticsScreen();"]:
    assert target in main.split("private void navigatePrimaryTab(int target)",1)[1].split(
            "private boolean touchingHorizontalScroller",1)[0]
assert "private boolean touchingHorizontalScroller(View root, float x, float y)" in main
assert "!touchingHorizontalScroller(screenHost,e.getRawX(),e.getRawY())" in main
assert "MotionEvent.ACTION_CANCEL" in main
assert "primarySwipeActive=true;" in main
assert "Math.abs(dx)>Math.abs(dy)*1.65f" in main
assert "navigatePrimaryTab(target);" in main
assert 'wrapper.setTag("wen_top_chrome");' in main
assert "animateGlassTopBar(next);" in main
assert "Ui.pressAnimation(back);" in main
assert "canvas.drawLine(" not in lensSource, "The old straight highlight must not return"
assert "canvas.drawOval(inner, opticalCore);" not in lensSource
# The original icon-scale stand-in is no longer allowed: optical sampling
# must visibly magnify ORIGINAL tab pixels inside the lens.
assert "focusLensTab(" not in engine
assert "WenOpticalNavigationOverlay optic;" in engine
assert "optic.begin((View) view.getParent()" in engine
assert "optic.move(x);" in engine
assert "optic.progress(progress);" in engine
optical = (p/"glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java").read_text(encoding="utf-8")
assert "new BitmapShader(source, Shader.TileMode.CLAMP" in optical
assert "tabStrip.draw(capture);" in optical
assert "sampleMatrix.setScale(zoom, zoom, cx, cy);" in optical
assert "canvas.drawRoundRect(droplet, radius, radius, magnified);" in optical
assert "setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);" in optical
assert "setVisibility(INVISIBLE);" in optical
assert "float zoom = 1f + .26f * opacity;" in optical, "Subtle optical magnification, not second bubble"
assert "physicalPill.getX()" in optical and "physicalPill.getWidth()" in optical
assert "canvas.drawRoundRect(droplet, radius, radius, magnified);" in optical
assert "canvas.drawRoundRect(droplet, ry, ry, body);" not in optical
assert "canvas.drawRoundRect(droplet, ry, ry, rim);" not in optical
progress = optical.split("public void progress(float next)",1)[1].split("public void discard()",1)[0]
assert "discard();" not in progress, "Initial animator tick at zero must never destroy the captured bitmap"
assert "opacity <= .005f ? INVISIBLE : VISIBLE" in progress
assert "source.recycle()" not in optical, "Do not recycle GPU-shared texture during a drawing frame"
assert "postDelayed(" not in optical and "PixelCopy.request(" not in optical
assert "new " not in optical.split("@Override protected void onDraw(Canvas canvas)",1)[1]
assert "new app.wensarafet.glass.WenNavigationTrack(this, Ui.DARK)" in main
assert "track.addView(opticalOverlay" not in main
trackSource = (p/"glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java").read_text(encoding="utf-8")
assert "setForeground(opticalForeground);" in trackSource, "Native foreground must own lens draw"
assert "opticalForeground.invalidateSelf();" in trackSource, "Changes must invalidate actual foreground"
assert "optic.renderOnto(canvas);" in trackSource
assert "optic = track instanceof WenNavigationTrack" in engine
assert "hostTrack.invalidateOptic();" in optical
assert "setLayerType(View.LAYER_TYPE_SOFTWARE" not in optical
assert "startupIconAnimationPending = true" in main
assert "private void animateStartupIcons(View page)" in main
assert "startupIconAnimationPending = false;" in main
assert "setStartDelay(i*65L)" in main
assert "animateStartupIcons(next);" in main
assert "page.getViewTreeObserver().addOnPreDrawListener(" in main
assert "page.postOnAnimation(() -> {" in main
assert "private void bindGlassFab(TextView fab)" in main
assert "ValueAnimator.ofFloat(bubble.getPress(),target)" in main
assert "bubble.setPress((Float)anim.getAnimatedValue())" in main
assert "bubble.setPress(0f);" in main
assert "bindGlassFab(fab);" in main
fab=(p/"glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java").read_text(encoding="utf-8")
assert "public void setPress(float fraction)" in fab
assert "c.drawCircle(mx,my,radius,fill);" in fab
assert "android.view.ViewAnimationUtils.createCircularReveal(" in main
assert "pendingFabReveal=true;" in main
assert 'headerTitle="كل العمليات"' in main
assert 'headerTitle="الديون"' in main
assert 'headerTitle="التحليلات"' in main
assert 'headerTitle="الرئيسية"' in main
assert "Ui.isLiquidTheme(this) ? .82f : .84f" in main
assert "physicalPill.getHeight()" in optical
assert "returnFromAddWithReverse(false);" in main
assert "returnFromAddWithReverse(transactionsTab);" in main
assert "final boolean reverseFab = pendingFabReverse" in main
assert "screenHost.addView(next, 0, match);" in main
assert "createCircularReveal(" in main
assert "outgoing,cx,cy,startRadius,Math.max(1f,fabRevealRadius)" in main
assert "outgoing.getParent()==screenHost" in main

assert 'final boolean iconOnlyLaunch = startupIconAnimationPending' in main
assert "if (iconOnlyLaunch) {" in main
assert "screenHost.postOnAnimation(() -> {" in main
assert "navigationFrameSequence" in main


assert "startupResolved && db!=null" in main, "Swipe should work on first launch"
assert "int target=currentTab+(dx>0f?1:-1);" in main, "RTL physical right must navigate forward"
assert "target > currentTab ? 1 : -1" in main, "Slide direction must follow RTL swipe"
assert "next.setTranslationX(-slideDirection * Ui.dp(this, 46));" in main
assert "screenHost.removeView(previous);" in main
assert "previous.animate().translationX(slideDirection" not in main, "No second heavy page during slide"
assert "next.animate().translationX(0f).setDuration(190)" in main
assert "if (pendingPrimarySlide != 0) return;" in main, "Header must not animate twice during sliding"
assert "if (!SandboxMode.isSandbox(this) || !Ui.isLiquidTheme(this))" in main

assert "android.animation.ValueAnimator.areAnimatorsEnabled()" in main
print("PASS: RTL-safe horizontal primary swipes, continuous lens, moving top chrome; original destinations retained")
