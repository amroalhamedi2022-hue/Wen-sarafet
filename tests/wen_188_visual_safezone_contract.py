#!/usr/bin/env python3
"""Hard fail on ANY functional alteration while porting the visual lens onto signed 1.8.8 source."""
from pathlib import Path
import subprocess
import re

ROOT = Path(__file__).resolve().parents[1]
BASE = "a78ce29603daf3f64efb52f15aca17e17c38a8b9"
MAIN = "app/src/main/java/app/wensarafet/tracker/MainActivity.java"
APP_GRADLE = "app/build.gradle"
SETTINGS = "settings.gradle"
ALLOWED = {
    MAIN, APP_GRADLE, SETTINGS, "gradle.properties", "proguard-rules.pro",
    "app/src/main/java/app/wensarafet/tracker/Ui.java",
    "app/src/main/java/app/wensarafet/tracker/ThemeCatalogue.java",
    "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java",
    "app/src/androidTest/java/app/wensarafet/tracker/WenVisualMotionInstrumentedTest.java",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java",
    "app/src/main/java/app/wensarafet/tracker/WenPremiumTabIconDrawable.java",
    "app/src/main/java/app/wensarafet/tracker/WenPremiumLayout.java",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java",
    "app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderUi.java",
    "app/src/main/java/app/wensarafet/tracker/SplitRemainderRules.java",
    "tests/SplitRemainderRulesTest.java",
    "app/src/androidTest/java/app/wensarafet/tracker/SplitRemainderInstrumentedTest.java",
    ".github/workflows/wen-188-split-ui-emulator.yml",
    "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenLiquidRuntimeOptics.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenGlassDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabOpticalCrownDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenThemeArtDirection.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumCaustics.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumSunlight.java",
    "app/src/androidTest/java/app/wensarafet/tracker/RealMainScreenCaptureTest.java",
    ".github/workflows/wen-real-main-screenshots.yml",
    "scripts/capture_real_main_screens.sh",
    "app/src/debug/java/app/wensarafet/tracker/OwnerThemeCaptureReceiver.java",
    "scripts/capture_main_no_instrumentation.sh",
    ".github/workflows/wen-main-physical-screens.yml",
    "docs/design/WEN_190_FIVE_PREMIUM_LIQUID_HANDOFF_20261105.md",
    "docs/releases/WEN_200_PREMIUM_INTEGRATION_HANDOFF_20261105.md",
    "docs/WEN_SARAFET_REPAIR_AND_DEVELOPMENT_PLAN_2026-10-23.md",
    "app/src/androidTest/java/app/wensarafet/tracker/ReferenceThemesInstrumentedTest.java",
    "docs/licenses/QWEA0_LiquidGlass_MIT.txt",
    "docs/design/WEN_188_VISUAL_REVIEW_EXECUTION_20261104.md",
    ".github/workflows/wen-188-visual-safe-review.yml",
    "tests/wen_188_visual_safezone_contract.py",
    ".github/workflows/unsigned-release-candidate.yml",
    "scripts/verify_upgrade_apk.sh",
    "tests/original_180_upgrade_contract.py",
    "tests/upgrade_gate_contract.py",
    "docs/design/WEN_190_REFERENCE_THEMES_20261105.md",
    "docs/design/WEN_20261106_OWNER_VISUAL_PARITY_PLAN.md",
    "app/src/androidTest/java/app/wensarafet/tracker/ThemeCatalogueInstrumentedTest.java",
    ".github/workflows/wen-visual-screenshots.yml",
    "app/src/debug/AndroidManifest.xml",
    "app/src/debug/java/app/wensarafet/tracker/VisualPreviewActivity.java",
    "scripts/capture_visual_preview.sh",
    "tests/glass_energy_contract.py",
    "tests/wen_190_visual_motion_contract.py",
    "tests/batch22_category_budget_icon_contract.py",
    "scripts/build_apk.sh",
    "scripts/sign_verified_upgrade.sh",
    "scripts/audit_recovered_source.sh",
}

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)

def baseline(path):
    return git("show", f"{BASE}:{path}")

changed = set(git("diff", "--name-only", BASE, "HEAD").splitlines())
unexpected = changed - ALLOWED
assert not unexpected, f"Safe Zone: unexpected changes: {sorted(unexpected)}"
# The main-only historical plan predates both visual branches; strictly freeze
# its contents to official main instead of treating this already-approved doc
# as an unauthorised source modification.
MAIN_ONLY_PLAN = "docs/WEN_SARAFET_REPAIR_AND_DEVELOPMENT_PLAN_2026-10-23.md"
assert (ROOT / MAIN_ONLY_PLAN).read_text(encoding="utf-8") == git(
    "show", f"283112f4bedaf2e07dd7370e581f84e14959710e:{MAIN_ONLY_PLAN}"
), "SAFE ZONE: existing official development plan unexpectedly changed"
# Original account identity art is SHA-1 pinned at the 1.8.8 Sandbox:
# do not allow a preview mock or theme refactor to replace PNG assets.
ORIGINAL_LOGOS = {
    "app/src/main/res/drawable-nodpi/icon_bop_mark.png":
        "d71006e0eba4cedcd1d04b2217eeef4911b83808",
    "app/src/main/res/drawable-nodpi/icon_jawwal_pay_mark.png":
        "0516bd77381b48b87c404e613c908405812d7b88",
    "app/src/main/res/drawable-nodpi/icon_palpay_mark.png":
        "d09932adbc59a39b4ba5b8d52a7350f7aa4ba1a8",
}
for logo_path, vetted_blob in ORIGINAL_LOGOS.items():
    assert git("hash-object",logo_path).strip() == vetted_blob, (
        "SAFE ZONE: original account icon PNG unexpectedly changed: "+logo_path
    )

required = {MAIN, APP_GRADLE, SETTINGS}
assert required <= changed, f"Visual adapter incomplete: {sorted(required - changed)}"

# Verify no financial, debt, activation, notification, contact, backup, etc.
# behavior changed even within the very large MainActivity.java file.
current_main = (ROOT / MAIN).read_text(encoding="utf-8")
# Exact source pin: THIS MainActivity was manually compared line-by-line with
# 1.9 main. Its only 40 new lines are visual host lifecycle, original-tab
# delegation, screen reveal/screen swipe hooks, and non-semantic View tags.
# The original protected callbacks/finance methods are not edited. Any future
# change to MainActivity, including new feature work, must re-audit this SHA.
APPROVED_VISUAL_HOST_BLOB = "dec2d386f2c7dbada90649e568f74d81e35bf493"
assert git("hash-object", MAIN).strip() == APPROVED_VISUAL_HOST_BLOB, (
    "SAFE ZONE: MainActivity differs from the manually audited visual-only host patch"
)
# Compare the whole 300K Java host byte-for-byte with the verified 2.0
# release candidate after reversing ONLY three strictly visual modifications.
# No finance, auth, routing or original callback can slip through this gate.
PRE_THEME_CATALOGUE = "caf2f6eeac3a5a34fc688b21544902d7ccd13e71"
previous_host = git("show", f"{PRE_THEME_CATALOGUE}:{MAIN}")
sanitized_host = current_main

def revert_visual_section(current, old, from_current, from_old, until):
    assert current.count(from_current) == old.count(from_old) == 1
    assert current.count(until) == old.count(until) == 1
    fresh = current[current.index(from_current):current.index(until)]
    historical = old[old.index(from_old):old.index(until)]
    return current.replace(fresh, historical, 1)

# Strictly presentational floating-navigation inset and radius:
# original tab click handlers, activity routes and selection state are unchanged.
gold_nav_margin = """        if (Ui.isLiquidTheme(this)) {
            // Owner reference: truly floating 16dp inset, preserving
            // the original four tab callbacks and scroll routing.
            int sideInset = WenPremiumLayout.navigationSideInsetDp(this);
            navParams.setMargins(Ui.dp(this,sideInset),0,
                    Ui.dp(this,sideInset),
                    Ui.dp(this,WenPremiumLayout.navigationBottomInsetDp(this)));
        }"""
legacy_nav_margin = """        if (Ui.isLiquidTheme(this)) navParams.setMargins(Ui.dp(this,10),0,Ui.dp(this,10),Ui.dp(this,8));"""
gold_nav_radius = """nav.setBackground(Ui.glassNavigationBackground(this,
                WenGoldGlassSurface.enabled(this) ? 28 : 26));"""
legacy_nav_radius = "nav.setBackground(Ui.glassNavigationBackground(this, 26));"
for optical, historic in ((gold_nav_margin,legacy_nav_margin),
                          (gold_nav_radius,legacy_nav_radius)):
    assert sanitized_host.count(optical) == 1, "Unreviewed bottom bar appearance"
    sanitized_host = sanitized_host.replace(optical,historic,1)
# Preserve tab routes byte-for-byte; only layout and glass are in scope.
for route in (
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
):
    assert current_main.count(route) == 1, "Navigation route changed"

# New gold-only screen background is a passive view placed before the
# original shell; no finance callbacks, layout actions, or input routing.
# Revert exactly this one visual layer before the full MainActivity diff.
original_scene = """        root.setBackground(Ui.glassScreenBackground(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout shell = Ui.column(this);"""
gold_scene = """        root.setBackground(Ui.glassScreenBackground(this));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (WenGoldGlassSurface.enabled(this)) {
            // A single real scene View behind every overlay, shared by
            // header, balance and chart optics. No duplicate background
            // snapshots or fabricated gradients in the glass children.
            View goldBackdrop = new View(this);
            goldBackdrop.setTag(WenGoldGlassSurface.BACKDROP_TAG);
            android.graphics.drawable.Drawable existingScene = Ui.glassScreenBackground(this);
            goldBackdrop.setBackground(existingScene);
            root.setBackground(null);
            root.addView(goldBackdrop, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        LinearLayout shell = Ui.column(this);"""
assert sanitized_host.count(gold_scene) == 1
sanitized_host = sanitized_host.replace(gold_scene, original_scene, 1)

# Only the top-bar VIEW method is ported from the owner's 1.8.8 Sandbox.
# Every action callback remains the original handler, and the full Java file
# remains protected by APPROVED_VISUAL_HOST_BLOB above.
for callback in (
    "addAccount.setOnClickListener(v -> showAccountManagement());",
    "settings.setOnClickListener(v -> showSettings());",
):
    assert current_main.count(callback) == 1, "Protected header action changed"
for title in ("كل العمليات", "الديون", "التحليلات", "الرئيسية"):
    assert title in current_main, "Missing Sandbox per-screen heading: " + title
sanitized_host = revert_visual_section(
    sanitized_host, previous_host,
    "    private View mainTopBar()", "    private View mainTopBar()",
    "    private LinearLayout bottomNavigation("
)
# Original Sandbox subpage chrome keeps its original back-action delegate,
# but now supports champagne contrast and a finite press response.
assert current_main.count("back.setOnClickListener(v->backAction.run());") == 1
sanitized_host = revert_visual_section(
    sanitized_host, previous_host,
    "    private View pageHeader(", "    private View pageHeader(",
    "    private void showSettings()"
)
# FAB bubble rendering and clipping ONLY: its existing transaction callback
# and screen placement are byte-for-byte restored in this comparison.
assert current_main.count("showAddTransaction(); // exact same original action, still immediate") == 1
assert 'fab.setStateListAnimator(null);' in current_main
assert 'fab.setClipToOutline(true);' in current_main
sanitized_host = revert_visual_section(
    sanitized_host, previous_host,
    "        // A hard circular outline (from Sandbox 1.8.8)",
    "        fab.setElevation(Ui.dp(this, 8));",
    '        fab.setTag("wen_intro_fab");'
)

# RTL position correction affects only the premium FAB alignment, and
# preserves the owner's stored side preference and original click action.
old_fab_position = "        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(fabSize, fabSize, Gravity.BOTTOM | (Ui.homeFabOnRight(this) ? Gravity.END : Gravity.START));"
premium_fab_position = """        // Absolute LEFT/RIGHT is essential on an RTL root: START maps to the
        // right side, contrary to the user's saved home_fab_right preference.
        // Five approved premium scenes restore the reference FAB on the LEFT
        // by default; an explicit user choice of right stays honoured.
        int fabGravity = app.wensarafet.glass.WenPremiumLiquidDrawable
                .supports(Ui.themeIndex(this))
                ? (Ui.homeFabOnRight(this) ? Gravity.RIGHT : Gravity.LEFT)
                : (Ui.homeFabOnRight(this) ? Gravity.END : Gravity.START);
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                fabSize, fabSize, Gravity.BOTTOM | fabGravity);"""
assert previous_host.count(old_fab_position) == 1
assert sanitized_host.count(premium_fab_position) == 1
sanitized_host = sanitized_host.replace(premium_fab_position, old_fab_position, 1)

# One purely visual setting hides the thick scrollbar on the five
# premium materials; it does not alter scroll listeners or event dispatch.
old_scrollbar = """        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = showTransactions ? transactionsContent(1000) : homeContent();"""
premium_scrollbar = """        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        // Decorative five-scene materials should never be obscured by the
        // thick transient MIUI/Android scroll indicator. Scrolling still works.
        if (app.wensarafet.glass.WenPremiumLiquidDrawable.supports(
                Ui.themeIndex(this))) scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = showTransactions ? transactionsContent(1000) : homeContent();"""
assert sanitized_host.count(premium_scrollbar) == 1
sanitized_host = sanitized_host.replace(premium_scrollbar, old_scrollbar, 1)

# Real reference parity: three currency cards must remain visible together.
# This transformation touches only width, padding and TextView appearance.
# No currency, money computation, database lookup or click routing may change.
balance_start = "    private View balanceCards() {"
balance_end = "    private View spendingDashboard() {"
premium_balance = sanitized_host[sanitized_host.index(balance_start):sanitized_host.index(balance_end)]
legacy_balance = previous_host[previous_host.index(balance_start):previous_host.index(balance_end)]
for required_balance in (
    'Map<String, Long> totals = db.getTotals();',
    'Arrays.asList("ILS", "USD", "JOD")',
    'Money.format(totals.get(currency), currency)',
    'Ui.glassBalanceBackground(this, 22)',
    'if (premium) return row;',
    'new LinearLayout.LayoutParams(0, WenPremiumLayout.dp(this,90,58), 1f)',
    'WenGoldGlassSurface.wrap(card,22)',
):
    assert premium_balance.count(required_balance) == 1, "Unreviewed balance content/layout: " + required_balance
for blocked_balance in ("db.update", "db.delete", "db.insert", "showAddTransaction(",
                        "LicenseManager", "setOnClickListener", "Sqlite"):
    assert blocked_balance not in premium_balance, "Safe Zone violated by balance layout: " + blocked_balance
assert premium_balance != legacy_balance
sanitized_host = sanitized_host.replace(premium_balance, legacy_balance, 1)

# Nav/FAB vertical offsets are position-only; preserve the original click,
# side preference, screen routing and accessibility hitboxes.
premium_nav_frame = """FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, WenPremiumLayout.navigationHeight(this), Gravity.BOTTOM);"""
old_nav_frame = """FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72), Gravity.BOTTOM);"""
premium_fab_margin = """fabParams.setMargins(Ui.dp(this, 18), 0, Ui.dp(this, 18),
                WenPremiumLayout.fabBottomOffset(this));"""
old_fab_margin = """fabParams.setMargins(Ui.dp(this, 18), 0, Ui.dp(this, 18), Ui.dp(this, 88));"""
for visual, old in ((premium_nav_frame,old_nav_frame),(premium_fab_margin,old_fab_margin)):
    assert sanitized_host.count(visual) == 1
    sanitized_host = sanitized_host.replace(visual,old,1)

# Extra-short reference viewport metrics: solely View dimension changes,
# not data queries, debt logic, callbacks or installed app preferences.
old_header_height = """shell.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 112)));"""
new_header_height = """shell.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                WenPremiumLayout.dp(this, 112, 74)));"""
assert sanitized_host.count(new_header_height) == 1
sanitized_host = sanitized_host.replace(new_header_height, old_header_height, 1)

# Section heading is entirely presentational.
sanitized_host = revert_visual_section(
    sanitized_host, previous_host,
    "    private View sectionTitle(", "    private View sectionTitle(",
    "    private View balanceCards("
)
# The 7-day chart / three expense cards preserve every expense value and
# analytics click; only their heights/visual margins differ.
report_start = "    private View spendingDashboard() {"
report_end = "    private View statCard("
report_view = sanitized_host[sanitized_host.index(report_start):sanitized_host.index(report_end)]
report_base = previous_host[previous_host.index(report_start):previous_host.index(report_end)]
for call in ('getExpenseTotal(', 'getTransactions(', 'showAnalyticsScreen()',
             'statCard("مصاريف اليوم"', 'statCard("آخر 7 أيام"',
             'statCard("هذا الشهر"'):
    assert report_view.count(call) == report_base.count(call), (
        "Protected analytics call or expenditure data changed: " + call)
# Finance values are read from original TextViews BEFORE adding overlays.
assert 'todayCard = WenGoldGlassSurface.wrap(todayCard,20);' in report_view
assert 'weekCard = WenGoldGlassSurface.wrap(weekCard,20);' in report_view
assert 'monthCard = WenGoldGlassSurface.wrap(monthCard,20);' in report_view
assert 'box.addView(WenGoldGlassSurface.wrap(chartCard,22), chartParams);' in report_view
assert report_view[report_view.index("        Runnable updateOverview = () -> {"):] == report_base[
    report_base.index("        Runnable updateOverview = () -> {"):], (
    "Protected chart data update callbacks changed"
)
sanitized_host = sanitized_host.replace(report_view, report_base, 1)

# Account rows keep EXACT balance expression, original account icon mapper,
# click handler and detail screen. Only padding/height are altered.
account_start = "    private View accountCard(Models.Account account) {"
account_end = "    private void showAccountDetails("
account_view = sanitized_host[sanitized_host.index(account_start):sanitized_host.index(account_end)]
account_base = previous_host[previous_host.index(account_start):previous_host.index(account_end)]
for call in ('Money.format(account.balanceMinor, account.currency)',
             'View icon = accountIcon(account);',
             'card.setOnClickListener(v -> showAccountDetails(account));',
             'Ui.pressAnimation(card);'):
    assert account_view.count(call) == account_base.count(call) == 1, (
        "Protected account content or handler changed: " + call)
sanitized_host = sanitized_host.replace(account_view, account_base, 1)

# The existing statistic's readable ink correction remains tracked below.
# Its two short-viewport metric declarations are reversed byte-for-byte.
card_padding_current = """card.setPadding(Ui.dp(this,12), WenPremiumLayout.dp(this,8,4),
                Ui.dp(this,12), WenPremiumLayout.dp(this,7,3));"""
card_padding_original = """card.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 7));"""
metric_current = """LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                WenPremiumLayout.dp(this,78,51), 1);"""
metric_original = """LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 78), 1);"""
for current, legacy in ((card_padding_current, card_padding_original),
                         (metric_current, metric_original)):
    assert sanitized_host.count(current) == 1
    sanitized_host = sanitized_host.replace(current, legacy, 1)

# Stats remain the original view/click handlers and amounts, only ink changes.
sanitized_host = revert_visual_section(
    sanitized_host, previous_host,
    "        // Contrast is material-specific, not dictated by a universal white label.",
    "        card.addView(Ui.text(this, label, 12, Color.rgb(239, 244, 255), false));",
    "        amountView.setAutoSizeTextTypeUniformWithConfiguration("
)

theme_start = '        content.addView(sectionTitle("مظهر التطبيق",'
theme_end = '        content.addView(sectionTitle("تخصيص الواجهة",'
assert previous_host.count(theme_start) == sanitized_host.count(theme_start) == 1
assert previous_host.count(theme_end) == sanitized_host.count(theme_end) == 1
new_picker = sanitized_host[sanitized_host.index(theme_start):sanitized_host.index(theme_end)]
old_picker = previous_host[previous_host.index(theme_start):previous_host.index(theme_end)]
for approved in ("ThemeCatalogue.visibleCount(this)", "ThemeCatalogue.displayName(i)",
                 "ThemeCatalogue.isVisible(this, i)", "ThemeCatalogue.hide(this, index)",
                 "ThemeCatalogue.restoreUserHidden(this)", "Ui.saveTheme(this, index)",
                 'setPositiveButton("حذف"', "index != currentTheme"):
    assert approved in new_picker, "Missing owner-approved theme action: " + approved
for forbidden in ("FinanceDbHelper", "SQLiteDatabase", "deleteTransaction(",
                  "showAddTransaction(", "getBalance(", "showDebtsScreen(",
                  "insertTransaction(", "SmsCapture"):
    assert forbidden not in new_picker, "Unsafe theme UI addition: " + forbidden
sanitized_host = sanitized_host.replace(new_picker, old_picker, 1)
# EXACT reversible gold-only layout changes, shared across every
# primary page. The real backdrop is a passive View behind the same original
# debt/analytics content and callbacks; these replacements MUST recover the
# entire 2.0 MainActivity source byte-for-byte.
assert current_main.count("WenGoldGlassSurface.attachGoldBackdrop(primaryRoot);") == 2
assert current_main.count("WenPremiumLayout.navigationHeight(this)") == 4
gold_root = """primaryRoot.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        WenGoldGlassSurface.attachGoldBackdrop(primaryRoot);
        LinearLayout page=Ui.column(this);"""
legacy_root = """primaryRoot.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout page=Ui.column(this);"""
assert sanitized_host.count(gold_root) == 2, "Gold backdrop may only wrap debt/analytics"
sanitized_host = sanitized_host.replace(gold_root, legacy_root)
gold_debt_header = """page.addView(debtTopBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                WenPremiumLayout.dp(this, 112, 74)));"""
old_debt_header = """page.addView(debtTopBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 112)));"""
gold_analytics_header = """page.addView(analyticsTopBar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                WenPremiumLayout.dp(this, 112, 74)));"""
old_analytics_header = """page.addView(analyticsTopBar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,112)));"""
for gold, old in ((gold_debt_header, old_debt_header),
                  (gold_analytics_header, old_analytics_header)):
    assert sanitized_host.count(gold) == 1, "Unreviewed gold primary screen header size"
    sanitized_host = sanitized_host.replace(gold, old, 1)
for page in ("debt", "analytics"):
    variable = page + "NavParams"
    gold_navigation = ("""FrameLayout.LayoutParams {variable} = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,WenPremiumLayout.navigationHeight(this),Gravity.BOTTOM);
        if (Ui.isLiquidTheme(this)) {variable}.setMargins(
                Ui.dp(this,WenPremiumLayout.navigationSideInsetDp(this)),0,
                Ui.dp(this,WenPremiumLayout.navigationSideInsetDp(this)),
                Ui.dp(this,WenPremiumLayout.navigationBottomInsetDp(this)));""").replace("{variable}", variable)
    old_navigation = ("""FrameLayout.LayoutParams {variable} = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,72),Gravity.BOTTOM);
        if (Ui.isLiquidTheme(this)) {variable}.setMargins(Ui.dp(this,10),0,Ui.dp(this,10),Ui.dp(this,8));""").replace("{variable}", variable)
    assert sanitized_host.count(gold_navigation) == 1, (
        "Unreviewed " + page + " original navigation view layout"
    )
    sanitized_host = sanitized_host.replace(gold_navigation, old_navigation, 1)
gold_insights_navigation = "new LinearLayout.LayoutParams(-1,WenPremiumLayout.navigationHeight(this)));"
old_insights_navigation = "new LinearLayout.LayoutParams(-1,Ui.dp(this,72)));"
assert sanitized_host.count(gold_insights_navigation) == 1
sanitized_host = sanitized_host.replace(gold_insights_navigation, old_insights_navigation, 1)

assert sanitized_host == previous_host, (
    "SAFE ZONE: MainActivity differs beyond approved theme picker, header and metric text"
)

SOURCE_190 = "283112f4bedaf2e07dd7370e581f84e14959710e"
main_pre_visual_host = git("show", f"{SOURCE_190}:{MAIN}")
assert main_pre_visual_host != current_main, "Missing staged optical host transfer"
for original_callback in (
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
):
    assert original_callback in current_main
assert "showAddTransaction(); // exact same original action, still immediate" in current_main

visual_hook = (
    "        // Visual-only; keep every original 1.8.8 tab callback untouched.\n"
    "        if (Ui.isLiquidTheme(this))\n"
    "            NavigationVisualDecorator.install(nav,\n"
    "                    new TextView[]{home, tx, debts, analytics}, selectedTab);\n"
)
assert current_main.count(visual_hook) == 1, "Expected one visual-only integration hook"
# Exactly ONE approved functionality exception: calculate the final split UI
# amount. Restore all five additive UI hooks before byte-comparing MainActivity
# against the original 1.8.8; no DB, Save, ledger, or callback edits allowed.
split_ui_hooks = [
    (
        "        // Auto-calculate ONLY the last split entry; original save logic is unchanged.\n"
        "        SplitRemainderUi.bind(rowsBox, () -> tx.amountMinor, null);\n"
    ),
    (
        "        SplitRemainderUi.bind(splitRowsBox, () -> {\n"
        "            Long total = Money.parseMinor(amount.getText().toString());\n"
        "            return total == null ? -1L : total;\n"
        "        }, amount);\n"
    ),
    "        amount.setTag(SplitRemainderUi.AMOUNT_FIELD_TAG);\n",
    "            SplitRemainderUi.refresh(container);\n",
    "        SplitRemainderUi.refresh(container);\n",
]
# Approved split EDIT display fix: cents are never passed through binary double.
# Restore this one presentation-only line before comparing MainActivity to
# signed 1.8.8. Persisted values, debt ledger, DB and save handlers stay frozen.
exact_display = "            row.amount.setText(SplitRemainderUi.formatMinor(split.amountMinor));\n"
previous_display = '            row.amount.setText(String.format(Locale.ROOT, "%.2f", split.amountMinor / 100.0));\n'
assert current_main.count(exact_display) == 1, "SAFE ZONE: split edit should display exact stored minor units"
# The sole production MainActivity presentation change for reference themes:
# same original FAB click handler, position and dimensions, new drawable only.
old_fab = """        fab.setBackground(Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)
                : Ui.gradient(Ui.BRAND, Ui.VIOLET, 24, this));"""
new_fab = """        // Two new reference themes render a circular optical material here.
        // The original add callback, size, position and hold feedback are unchanged.
        fab.setBackground(Ui.isReferenceGlassTheme(this)
                ? Ui.glassFloatingAddBackground(this)
                : (Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)
                : Ui.gradient(Ui.BRAND, Ui.VIOLET, 24, this)));"""
assert current_main.count(new_fab) == 1, "SAFE ZONE: only add-button appearance is approved"
cleaned_main = main_pre_visual_host.replace(new_fab, old_fab).replace(visual_hook, "")
for hook in split_ui_hooks:
    assert cleaned_main.count(hook) == 1, "Unauthorized/missing split hook: " + repr(hook)
    cleaned_main = cleaned_main.replace(hook, "", 1)
assert cleaned_main.count(exact_display) == 1
cleaned_main = cleaned_main.replace(exact_display, previous_display, 1)
assert cleaned_main == baseline(MAIN), (
    "SAFE ZONE: MainActivity changed outside nav presentation + the five approved split UI hooks"
)
split_ui = (ROOT / "app/src/main/java/app/wensarafet/tracker/SplitRemainderUi.java").read_text(encoding="utf-8")
split_calculation = (ROOT / "app/src/main/java/app/wensarafet/tracker/SplitRemainderRules.java").read_text(encoding="utf-8")
for forbidden in ["FinanceDbHelper", "SQLiteDatabase", "db.", "insert", "deleteTransaction",
                  "reclassifyTransaction", "setOnClickListener", "SmsCapture", "AppLockManager"]:
    assert forbidden not in split_ui + split_calculation, (
        "SAFE ZONE: split auto-remainder must not mutate financial or security logic: " + forbidden
    )
assert "Money.parseMinor" in split_ui
assert "SplitRemainderRules.lastPart" in split_ui
assert "Math.addExact" in split_calculation and "Math.subtractExact" in split_calculation
assert "derived.setKeyListener(null)" in split_ui, "Last split amount must be read-only"
assert "BigDecimal.valueOf(minorUnits, 2).toPlainString()" in split_ui, "Exact edit display must avoid doubles"
assert "entry.getKey().removeTextChangedListener(entry.getValue());" in split_ui, "Removed split fields must detach stale observers"
assert "if (becameEarlierPart) derived.setText(\"\");" in split_ui, "Former automatic remainder must become a blank editable entry"
assert "derived.setContentDescription(\"مبلغ الجزء\");" in split_ui, "Accessibility label must be restored after adding a part"
assert "SplitRemainderUi.refresh(container);" in current_main, "Add/remove must recalculate final amount"


# The source-only overlay REMOVES (never grants) AndroidX's automatic
# signature compatibility permission. Reversing this exact removal must
# recover the 1.8.8 manifest byte-for-byte; the built APK is also audited.
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = (ROOT / manifest_path).read_text(encoding="utf-8")
plain_header = '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    package="app.wensarafet.tracker">'
guarded_header = """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="app.wensarafet.tracker">
    <!-- Visual dependency adds a compatibility-only internal permission.
         minSdk 33 uses platform receiver flags. Remove that merged permission
         so the effective 1.8.8 APK permission set remains IDENTICAL. -->
    <permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />
    <uses-permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />"""
assert manifest.count(guarded_header) == 1, "Manifest overlay must only remove the added renderer permission"
assert manifest.replace(guarded_header, plain_header) == baseline(manifest_path), (
    "SAFE ZONE: production AndroidManifest changed beyond permission removal"
)

current_gradle = (ROOT / APP_GRADLE).read_text(encoding="utf-8")
dependency = (
    '    // MIT-licensed native navigation optics; UI rendering only (no permissions).\n'
    '    implementation "com.github.QWEA0:liquidglass:v2.0.11"\n'
)
assert current_gradle.count(dependency) == 1
test_runner = (
    '        // Test APK only; the release applicationId, permissions and signing stay identical.\n'
    '        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"\n'
)
test_deps = (
    '    // Android tests only: none of these dependencies enter the production APK.\n'
    '    androidTestImplementation "androidx.test:core:1.6.1"\n'
    '    androidTestImplementation "androidx.test:runner:1.6.2"\n'
    '    androidTestImplementation "androidx.test.ext:junit:1.2.1"\n'
)
assert current_gradle.count(test_runner) == 1 and current_gradle.count(test_deps) == 1, (
    "Instrumentation changes must be test APK only"
)
# Packaging-only release bump: remove exact approved version deltas before
# comparing the entire Gradle signing/package/dependency configuration.
restored_gradle = current_gradle.replace(test_runner, "").replace(test_deps, "").replace(dependency, "")
assert restored_gradle.count('versionCode 28') == 1
assert restored_gradle.count('versionName "2.0"') == 1
restored_gradle = restored_gradle.replace("versionCode 28", "versionCode 24").replace('versionName "2.0"', 'versionName "1.8.8"')
assert restored_gradle == baseline(APP_GRADLE), (
    "SAFE ZONE: unexpected build, package, version, dependency or signing mutation"
)
settings = (ROOT / SETTINGS).read_text(encoding="utf-8")
repo_line = "repositories { google(); mavenCentral(); maven { url 'https://jitpack.io' } }"
assert settings.count(repo_line) == 1
assert settings.replace(repo_line, "repositories { google(); mavenCentral() }") == baseline(SETTINGS)

properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
assert properties.count("android.useAndroidX=true") == 1
assert properties.replace("android.useAndroidX=true", "android.useAndroidX=false") == baseline("gradle.properties"), (
    "SAFE ZONE: Gradle properties altered beyond the renderer's AndroidX prerequisite"
)

for callback in [
    "home.setOnClickListener(v -> showMain(false));",
    "tx.setOnClickListener(v -> showMain(true));",
    "debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));",
    "analytics.setOnClickListener(v -> showAnalyticsScreen());",
]:
    assert callback in current_main, f"SAFE ZONE: changed original callback: {callback}"
assert 'versionName "2.0"' in current_gradle
rules = (ROOT / "proguard-rules.pro").read_text(encoding="utf-8")
optional_platform_warnings = (
    "\n# Optional API 36 optical classes in the MIT lens library; guarded at runtime.\n"
    "# Narrowly suppress R8's unresolved-platform warning (no finance/security keep rule).\n"
    "-dontwarn android.graphics.RuntimeColorFilter\n"
    "-dontwarn android.graphics.RuntimeXfermode\n"
)
assert rules.count(optional_platform_warnings) == 1
assert rules.replace(optional_platform_warnings, "") == baseline("proguard-rules.pro"), (
    "SAFE ZONE: ProGuard rules changed beyond the two optional visual APIs"
)
assert "versionCode 28" in current_gradle
assert 'applicationId "app.wensarafet.tracker"' in current_gradle

# Release workflow identity must advance in lockstep with the package.
release_workflow = (ROOT / ".github/workflows/unsigned-release-candidate.yml").read_text(encoding="utf-8")
expected_release = baseline(".github/workflows/unsigned-release-candidate.yml")
expected_release = expected_release.replace("Wen-Sarafet-1.8.8-aligned-UNSIGNED.apk", "Wen-Sarafet-2.0-aligned-UNSIGNED.apk")
expected_release = expected_release.replace("versionCode='24' versionName='1.8.8'", "versionCode='28' versionName='2.0'")
expected_release = expected_release.replace("wen-1-8-8-original-package-unsigned", "wen-2-0-original-package-unsigned")
assert release_workflow == expected_release, "SAFE ZONE: unexpected production release workflow mutation"
review_workflow = (ROOT / ".github/workflows/wen-188-visual-safe-review.yml").read_text(encoding="utf-8")
assert review_workflow.count("versionCode='28' versionName='2.0'") == 1

# No change to authentication/signing data; only permit predecessor 1.8.8
# into the existing static signed-APK upgrade checker.
upgrade = (ROOT / "scripts/verify_upgrade_apk.sh").read_text(encoding="utf-8")
expected_upgrade = git("show", f"{SOURCE_190}:scripts/verify_upgrade_apk.sh")
expected_upgrade = expected_upgrade.replace(
    '"$old_version" -ne 24 )',
    '"$old_version" -ne 24 && "$old_version" -ne 25 && "$old_version" -ne 26 && "$old_version" -ne 27 )')
expected_upgrade = expected_upgrade.replace(
    "or 1.8.8 (24), and update must have a higher versionCode",
    "or 1.8.8 (24), 1.8.9 (25), 1.8.10 (26), 1.9 (27), and update must have a higher versionCode")
expected_upgrade = expected_upgrade.replace(
    "INSTALLED_SIGNED_1.8.x.apk", "INSTALLED_SIGNED_1.8.x_OR_1.9.apk")
assert upgrade == expected_upgrade, "SAFE ZONE: changed signing upgrade checks beyond allowed predecessor"

# Permit only the exact test-fixture updates needed for a code26 original
# signer upgrade from code24; the actual signature and package checker is fixed.
expected_tests = git("show", f"{SOURCE_190}:tests/upgrade_gate_contract.py")
expected_tests = expected_tests.replace('assert parsed.stdout == "27|1.9", parsed', 'assert parsed.stdout == "28|2.0", parsed')
expected_tests = expected_tests.replace("      *current-181*) code=17 ;;", "      *current-1810*) code=26 ;;\n      *current-181*) code=17 ;;")
expected_tests = expected_tests.replace("      *current-188*) code=24 ;;", "      *current-188*) code=24 ;;\n      *current-189*) code=25 ;;\n      *current-190*) code=27 ;;")
expected_tests = expected_tests.replace("      *) code=27 ;;", "      *) code=28 ;;")
expected_tests = expected_tests.replace("versionName='1.9'", "versionName='2.0'")
expected_tests = expected_tests.replace('"24 -> 27"', '"24 -> 28"')
expected_tests = expected_tests.replace('    stale = home / "same-as-current.apk"',
    "    # Validate every newer original-signed predecessor without replacing the signer gate.\n    for old_name, old_code in ((\"current-189.apk\", 25),\n                               (\"current-1810.apk\", 26),\n                               (\"current-190.apk\", 27)):\n        prior = home / old_name\n        prior.write_bytes(b\"fake previously signed owner APK\")\n        verified = subprocess.run(\n            [\"bash\", str(checker), str(prior), str(home / \"candidate.apk\")],\n            env=env, capture_output=True, text=True,\n        )\n        assert verified.returncode == 0 and f\"{old_code} -> 28\" in verified.stdout, (\n            old_name, verified.stdout, verified.stderr\n        )\n" + '    stale = home / "same-as-current.apk"')
assert (ROOT / "tests/upgrade_gate_contract.py").read_text(encoding="utf-8") == expected_tests, "SAFE ZONE: release upgrade test changed beyond original-signer and version coverage"

# The trusted upgrade contract may only advance the expected version; it
# must still validate the ORIGINAL app signer, license and install identity.
old_guard = git("show", f"{SOURCE_190}:tests/original_180_upgrade_contract.py")
expected_guard = old_guard.replace(r'versionCode\s+27\b', r'versionCode\s+28\b')
expected_guard = expected_guard.replace('versionName "1.9"', 'versionName "2.0"')
expected_guard = expected_guard.replace("audited version 1.9", "audited version 2.0")
assert (ROOT / "tests/original_180_upgrade_contract.py").read_text(encoding="utf-8") == expected_guard, "SAFE ZONE: unapproved original signing contract change"

# Protect legacy themes and every persisted preference index:
# only two brand-new palette rows may follow the original 22 names/rows.
original_ui = baseline("app/src/main/java/app/wensarafet/tracker/Ui.java")
new_ui = (ROOT / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text(encoding="utf-8")
def palette_block(text, label):
    start = text.index(label)
    return text[start:].split("};", 1)[0]
def palette_rows(text, label):
    return re.findall(r"\{([^{}]+)\}", palette_block(text, label))
old_names = re.findall(r'"([^"]+)"', palette_block(original_ui, "public static final String[] THEME_NAMES"))
new_names = re.findall(r'"([^"]+)"', palette_block(new_ui, "public static final String[] THEME_NAMES"))
assert len(old_names) == 22, "Unexpected source baseline for themes"
assert len(new_names) == 30 and new_names[:22] == old_names, "SAFE ZONE: legacy theme names or indices changed"
for name in ("private static final int[][] THEMES", "private static final int[][] THEME_LAYERS"):
    old_rows, new_rows = palette_rows(original_ui, name), palette_rows(new_ui, name)
    assert len(old_rows) == 22 and len(new_rows) == 30
    assert new_rows[:22] == old_rows, "SAFE ZONE: original theme palette changed: " + name
assert new_names[22:25] == [
    "Prism Glow • زجاج نيوني",
    "Golden Bloom • زجاج شمسي",
    "Royal Gold • الذهب الملكي",
], "SAFE ZONE: established reference theme IDs changed"
assert new_names[25:] == [
    "Royal Dark Gold • ذهبي ملكي داكن",
    "Imperial Dark Gold • ذهبي إمبراطوري",
    "Champagne Gold • شامبانيا ملكي فاتح",
    "Soft Orange-Green Glass • زجاج برتقالي أخضر",
    "Purple-Pink Liquid Glass • زجاج بنفسجي وردي",
], "SAFE ZONE: only the five owner-approved themes may be appended"
# Entire existing 25-theme selection + both palettes are frozen relative
# to the exact, already-promoted v1.9 main. Only five tail entries may differ.
PREMIUM_BASE = "283112f4bedaf2e07dd7370e581f84e14959710e"
promoted_ui = git("show", f"{PREMIUM_BASE}:app/src/main/java/app/wensarafet/tracker/Ui.java")
promoted_names = re.findall(r'"([^"]+)"', palette_block(promoted_ui, "public static final String[] THEME_NAMES"))
assert new_names[:25] == promoted_names, "SAFE ZONE: a promoted theme name/index changed"
for name in ("private static final int[][] THEMES", "private static final int[][] THEME_LAYERS"):
    assert palette_rows(new_ui, name)[:25] == palette_rows(promoted_ui, name), (
        "SAFE ZONE: an established 1.9 palette changed: " + name
    )
# Keep Prism/Bloom pixel palettes frozen exactly when appending Royal Gold.
original_reference_rows = {
    "private static final int[][] THEMES": [
        "0xFFAD3CDB,0xFF62257F,0xFFF4E3FC,0xFFFAF0FC,0xFFFFFAFF,0xFF291638,0xFF795D87,0xFFC94376,0xFF238D86,0xFF8244DA,0xFFF471B9,0xFF8B88EE",
        "0xFFCE873E,0xFF87502D,0xFFFFE9C8,0xFFFFF7EA,0xFFFFFDF5,0xFF3D3028,0xFF857367,0xFFCA5060,0xFF54866D,0xFFB993BC,0xFFF8AA67,0xFF95BDA6",
    ],
    "private static final int[][] THEME_LAYERS": [
        "0xFF782A9E,0xFFC435B0,0xFFE64CC2,0xFF7956EE",
        "0xFFB86E39,0xFFF2AC65,0xFFFBD6A0,0xFF99B9A1",
    ],
}
for key, frozen in original_reference_rows.items():
    assert [row.strip() for row in palette_rows(new_ui, key)[22:24]] == frozen, (
        "SAFE ZONE: Prism/Bloom palette changed while adding Royal Gold: " + key
    )
assert 'getSharedPreferences("appearance", Context.MODE_PRIVATE)' in new_ui
assert "isReferenceGlassTheme(context)" in new_ui
premium_draw_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java").read_text(encoding="utf-8")
draw_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java").read_text(encoding="utf-8")
assert "ROYAL_DARK = 25" in premium_draw_source and "PURPLE_PINK = 29" in premium_draw_source
assert "WenPremiumLiquidDrawable.supports(selected)" in new_ui
assert "premium ? 0x99FFFFFF" in (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java").read_text(encoding="utf-8")
assert "PRISM = 22" in draw_source and "BLOOM = 23" in draw_source and "ROYAL = 24" in draw_source
assert "PowerManager" in draw_source and "isLowRamDevice" in draw_source
for forbidden in ("FinanceDbHelper", "AppLockManager", "SQLiteDatabase", "OnClickListener", "LicenseManager", "SharedPreferences", "Handler(", "Choreographer.getInstance("):
    assert forbidden not in draw_source + premium_draw_source, "Theme renderer enters protected logic: " + forbidden

# The legacy recovery audit may accept v1.9 as a valid next-minor
# release without disabling any XML, banking, credential or package checks.
old_recovery = git("show", f"{SOURCE_190}:scripts/audit_recovered_source.sh")
expected_recovery = old_recovery.replace(r'^1[.](8[.][0-9]+|9)$', r'^(1[.](8[.][0-9]+|9)|2[.]0)$')
assert expected_recovery != old_recovery, "2.0 recovery gate must be explicit"
assert (ROOT / "scripts/audit_recovered_source.sh").read_text(encoding="utf-8") == expected_recovery, (
    "SAFE ZONE: recovery audit changed beyond v1.9 acceptance"
)

# The owner-only signer must also keep the exact keystore, certificate,
# original security hashes and payload checks. Only its version regex expands.
old_signer = git("show", f"{SOURCE_190}:scripts/sign_verified_upgrade.sh")
expected_signer = old_signer.replace(r'^1\.(8\.[0-9]+|9)$', r'^(1\.(8\.[0-9]+|9)|2\.0)$')
assert expected_signer != old_signer, "2.0 original signer gate missing"
assert (ROOT / "scripts/sign_verified_upgrade.sh").read_text(encoding="utf-8") == expected_signer, (
    "SAFE ZONE: signing helper changed beyond allowing v1.9"
)

# The owner's build script must retain its signing, hash, debug and
# identity checks; only accept the requested next-minor version 1.9.
old_builder = git("show", f"{SOURCE_190}:scripts/build_apk.sh")
expected_builder = old_builder.replace(r'^1\.(8\.[0-9]+|9)$', r'^(1\.(8\.[0-9]+|9)|2\.0)$')
assert expected_builder != old_builder, "2.0 owner build gate missing"
assert (ROOT / "scripts/build_apk.sh").read_text(encoding="utf-8") == expected_builder, (
    "SAFE ZONE: signing/build script changed beyond v1.9 metadata allowance"
)

# The archived Batch22 suite must still check ALL 22 old IDs in place;
# only its expected array tail and total length are extended to 25.
old_batch22 = baseline("tests/batch22_category_budget_icon_contract.py")
expected_batch22 = old_batch22.replace(
    'assert theme_names[18:] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names',
    'assert theme_names[18:22] == ["زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد"], theme_names\n'
    'assert theme_names[22:25] == ["Prism Glow • زجاج نيوني", "Golden Bloom • زجاج شمسي", "Royal Gold • الذهب الملكي"], theme_names\n'
    'assert theme_names[25:] == ["Royal Dark Gold • ذهبي ملكي داكن", "Imperial Dark Gold • ذهبي إمبراطوري", "Champagne Gold • شامبانيا ملكي فاتح", "Soft Orange-Green Glass • زجاج برتقالي أخضر", "Purple-Pink Liquid Glass • زجاج بنفسجي وردي"], theme_names')
expected_batch22 = expected_batch22.replace(
    'assert len(rows) == len(theme_names) == 22, (name, len(rows))',
    'assert len(rows) == len(theme_names) == 30, (name, len(rows))')
expected_batch22 = expected_batch22.replace(
    '18 preserved IDs plus four new glass themes passed',
    '22 preserved IDs plus eight opt-in reference/premium themes passed')
assert (ROOT / "tests/batch22_category_budget_icon_contract.py").read_text(encoding="utf-8") == expected_batch22, (
    "SAFE ZONE: original category and layout contracts changed beyond two new theme indices"
)

# Preserve the legacy passive-glass assertions while updating ONLY the
# original FAB opt-in predicate to the new two-theme visual adapter.
old_energy = baseline("tests/glass_energy_contract.py")
expected_energy = old_energy.replace(
    'assert "fab.setBackground(Ui.isLiquidTheme(this) ?" in main',
    '''assert "fab.setBackground(Ui.isReferenceGlassTheme(this)" in main
assert "? Ui.glassFloatingAddBackground(this)" in main
assert ": (Ui.isLiquidTheme(this) ? Ui.glassAccentBackground(this, 24)" in main''')
# Only the previously frozen visual source assertions have changed:
# orbiting card halo removed, sandbox bevel and pressure optics restored.
expected_energy = expected_energy.replace(
    'assert "showLens = strong && w > h * 3.1f && !lowPower;" in engine',
    '''assert "showLens = false;" in engine, "Sandbox cards must not render unrelated floating orbs"
assert "prismRim.setShader(new LinearGradient" in engine, "Restore fine optical rim"
assert "crown.setShader(new LinearGradient" in engine, "Restore pearly crown"
assert "final int bodyAlpha = strong ? 202 : 221;" in engine, "Keep actual background visible through glass"''')
expected_energy = expected_energy.replace(
    'assert "new android.view.animation.OvershootInterpolator(1.35f)" in ui',
    '''assert "GlassMotion.bindPress(view, SKY)" in ui, "Restore sandbox finite pressure bubble"
assert "WenFabBubbleDrawable(context, SKY, DARK)" in ui, "Restore circular original-Liquid FAB"
assert "liquid ? 2 : 10" in ui and "liquid ? 125 : 300" in ui, "Fast staged Liquid entrances"
assert "setDuration(115)" in ui, "Gentle sandbox-style screen reveal"''')
# Preserve the historical legacy 26dp nav while enforcing the 28dp
# gold-only owner reference. No callback or navigation destination changes.
expected_energy = expected_energy.replace(
    'assert "glassNavigationBackground(this, 26)" in main',
    '''assert "glassNavigationBackground(this," in main
assert "WenGoldGlassSurface.enabled(this) ? 28 : 26" in main''')
expected_energy = expected_energy.replace(
    'assert "if (isLiquidTheme(v.getContext()))" in ui',
    'assert "if (isLiquidTheme(view.getContext()))" in ui')
expected_energy = expected_energy.replace(
    'print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")',
    '''fab_bubble = (root / "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java").read_text(encoding="utf-8")
assert "public float getPress()" in fab_bubble and "public void setPress(float fraction)" in fab_bubble
assert "new RadialGradient" in fab_bubble, "Real pearly FAB depth must be restored"
assert "Timer" not in fab_bubble and "postDelayed" not in fab_bubble
assert "return false; // Never steal existing FAB click/hold actions." in ui

print("PASS: component-wide Liquid glass surfaces are passive; other themes retain fallbacks")''')
assert (ROOT / "tests/glass_energy_contract.py").read_text(encoding="utf-8") == expected_energy, (
    "SAFE ZONE: original glass energy regression changed beyond new FAB opt-in"
)

# Sandbox visual port is STRICTLY confined to graphics and four Ui helper
# methods. Compare the fully reconstructed 1.9 file with the pre-port main
# commit, not just fragile line grep patterns.
SANDBOX_SOURCE = "b57dcd3c71df7273acf5cd316687472f60c11d80"
ui_path = "app/src/main/java/app/wensarafet/tracker/Ui.java"
# Compare against independently approved 30-theme code, not the old palette.
# Only four precisely enumerated sandbox visual methods can vary.
PREMIUM_SOURCE = "ea61bb7a266652751fe9c08f321e0721350df1ea"
original_190_ui = git("show", f"{PREMIUM_SOURCE}:{ui_path}")
visual_only_methods = (
    "glassAccentBackground", "glassFloatingAddBackground",
    "animateScreen", "animateChildren", "pressAnimation",
)

def omit_visual_method_bodies(java):
    for name in visual_only_methods:
        regex = re.compile(
            r"    public static [^\n]*\b" + re.escape(name)
            + r"\([^)]*\)\s*\{"
        )
        hits = list(regex.finditer(java))
        assert len(hits) == 1, "SAFE ZONE: expected one UI helper: " + name
        start = hits[0].start()
        pos = hits[0].end()
        depth = 1
        while pos < len(java) and depth:
            if java[pos] == "{":
                depth += 1
            elif java[pos] == "}":
                depth -= 1
            pos += 1
        assert depth == 0, "SAFE ZONE: unterminated visual UI method: " + name
        java = java[:start] + "    /* preserved visual helper: " + name + " */" + java[pos:]
    return java

# One precisely approved appearance-only toggle allows the two vivid
# owner reference palettes to display legible WHITE text on dark-glass scenes.
# Remove it before the complete class comparison; every legacy 0-24 theme,
# preference, financial helper and palette remains byte-identical.
old_contrast = "DARK = theme == 20 || theme == 24 || theme == 25 || theme == 26"
new_contrast = old_contrast + chr(10) + "                || theme == 28 || theme == 29"
assert new_ui.count(new_contrast) == 1, (
    "SAFE ZONE: unexpected changes to premium dark/glass scene condition"
)
normalized_ui = new_ui.replace(new_contrast, old_contrast, 1)
# Safely retire only the two non-gold visual IDs. These exact three replacements
# are the ONLY changes allowed to legacy theme handling; palettes 0-27 and
# preferences unrelated to "theme" remain byte-identical.
retirement_changes = [
    (
        """        // Load through the visual-only compatibility resolver: retained old
        // preset 28/29 never paints again, even on an update install.
        int theme = themeIndex(context);
        if (theme < 0 || theme >= THEMES.length) theme = 25;""",
        """        int theme = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("theme", 0);
        if (theme < 0 || theme >= THEMES.length) theme = 0;"""
    ),
    (
        """    public static int themeIndex(Context context) {
        int persisted = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getInt("theme", 0);
        // Retired colourful presets are VISUAL-ONLY aliases of Royal Dark.
        // Do not rewrite unrelated appearance prefs or the finance database.
        // Preserves stable IDs for old saved sessions and safe update installs.
        if (persisted == 28 || persisted == 29) return 25;
        return persisted;
    }""",
        """    public static int themeIndex(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("theme", 0);
    }"""
    ),
    (
        """    public static void saveTheme(Context context, int index) {
        // Never allow selecting either retired non-gold theme via stale UI.
        if (index == 28 || index == 29) index = 25;
        if (index < 0 || index >= THEMES.length) index = 25;""",
        """    public static void saveTheme(Context context, int index) {
        if (index < 0 || index >= THEMES.length) index = 0;"""
    ),
]
for fresh, original in retirement_changes:
    assert normalized_ui.count(fresh) == 1, "Unauthorised theme migration change"
    normalized_ui = normalized_ui.replace(fresh, original, 1)
assert omit_visual_method_bodies(normalized_ui) == omit_visual_method_bodies(original_190_ui), (
    "SAFE ZONE: non-visual Ui class or legacy palettes changed during appearance work"
)
for marker in (
    "WenFabBubbleDrawable(context, SKY, DARK)",
    "GlassMotion.bindPress(view, SKY)",
    "liquid ? 2 : 10",
    "liquid ? 125 : 300",
    "setDuration(115)",
    "return false; // Never steal existing FAB click/hold actions.",
):
    assert marker in new_ui, "Missing sandbox visual-only transfer: " + marker

# The original Sandbox generic glass renderer MUST remain exactly intact.
graphic = "WenGlassDrawable.java"
location = "glass-engine/src/main/java/app/wensarafet/glass/" + graphic
assert (ROOT / location).read_text(encoding="utf-8") == git(
    "show", f"{SANDBOX_SOURCE}:{location}"
), "SAFE ZONE: original Sandbox glass renderer changed"

# Premiums 25–29 need correct GOLD/GREEN/VIOLET materials instead of the
# Sandbox BLUE button. Pin the single audited drawing-only blob, and prove the
# original hit/hold animation, draw and opacity methods are byte-identical.
location = "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java"
bubble = (ROOT / location).read_text(encoding="utf-8")
vetted_bubble_blob = "d26043c8e787937776d1fca546919e2646605238"
assert git("hash-object", location).strip() == vetted_bubble_blob, (
    "SAFE ZONE: unreviewed original FAB drawable modifications"
)
original_bubble = git("show", f"{SANDBOX_SOURCE}:{location}")
legacy_methods = "    public float getPress() { return press; }"
assert bubble[bubble.index(legacy_methods):] == original_bubble[
    original_bubble.index(legacy_methods):], (
    "SAFE ZONE: original FAB press/hold/draw contract altered"
)
for value in ("case 25:", "case 26:", "case 27:", "case 28:",
              "default: // Pink-Violet Glass"):
    assert value in bubble, "Approved five-theme FAB material absent: " + value
for protected in ("FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
                  "LicenseManager", "SmsCaptureReceiver", "SandboxMode",
                  "setOnClickListener(", "setOnLongClickListener("):
    assert protected not in bubble, "SAFE ZONE: decorative FAB source references protected behaviour"

# Strictly isolate new on-screen transitions from all protected business methods.
screen_motion = (ROOT / "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java").read_text(encoding="utf-8")
assert git("hash-object",
           "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java").strip() == (
    "860c1a2187d7c3474b00f86d42c8500bcc189c67"
), "SAFE ZONE: host motion changed after reviewed transfer"
for forbidden in (
    "FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
    "BudgetAlertManager", "DebtLedgerRules", "LicenseManager",
    "SandboxMode", "SmsCaptureReceiver", "setOnClickListener(",
    "setOnLongClickListener(", "getSharedPreferences(",
    "showAddTransaction(", "showMain(", "showAnalyticsScreen(",
    "showDebtsScreen(", "deleteTransaction(", "insert(",
):
    assert forbidden not in screen_motion, "SAFE ZONE: screen motion crosses business logic: " + forbidden
assert "destination.performClick();" in screen_motion
assert "protectedHorizontalTarget" in screen_motion
assert "ValueAnimator.areAnimatorsEnabled()" in screen_motion
assert "power.isPowerSaveMode()" in screen_motion
assert "ViewAnimationUtils.createCircularReveal" in screen_motion
assert "private void revealTopChrome(View page)" in screen_motion
assert "introIcons(next);" in screen_motion
assert "setOnTouchListener(" not in screen_motion
assert "postDelayed(" not in screen_motion and "while (true)" not in screen_motion
crown_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenFabOpticalCrownDrawable.java").read_text(encoding="utf-8")
for forbidden in ("OnTouchListener", "OnClickListener", "OnLongClickListener",
                  "FinanceDbHelper", "SQLiteDatabase", "AppLockManager",
                  "Handler(", "postDelayed(", "Choreographer"):
    assert forbidden not in crown_source, "FAB crown must remain an inert visual asset"
assert "view.getOverlay().add(crown);" in new_ui
assert "v.getOverlay().remove(crown);" in new_ui
assert "crown.setPress(t);" in new_ui

adapter = (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java").read_text(encoding="utf-8")
native = (ROOT / "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java").read_text(encoding="utf-8")
motion = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java").read_text(encoding="utf-8")
for forbidden in [
    "FinanceDbHelper", "DebtLedgerRules", "ContentResolver", "AppLockManager",
    "LicenseManager", "SmsCaptureReceiver", "SandboxMode", "attachFab(", "setOnClickListener(",
    "SQLiteDatabase", "WEN_SARAFET_KEYSTORE",
]:
    assert forbidden not in adapter + native, f"Visual layer crosses Safe Zone: {forbidden}"
assert "return false;" in adapter, "The optical layer must stay touch-through"
assert "GlassMotion.bindNavigationTab" in adapter
assert "tabs[hovered].performClick();" in motion, "Must reuse original navigation callbacks"
assert "if (tapEligible && hovered == originIndex)" in motion, "Selected tab tap must preserve 1.8.8 refresh action"
assert "tapEligible=false;" in motion, "Dragging away and back cannot trigger accidental refresh"
assert "tabs[hovered].performClick();" in motion.split("if (hovered == selected)", 1)[1].split("} else {",1)[0], "Selected tap no longer invokes original callback"
assert "NavigationLiquidMaterial.attach(lens, track)" in adapter
# The premium lens now samples the genuine scrolling underlay and uses
# physically visible edge refraction instead of inactive 0-value optics.
# Older themes retain the original restrained settings via ternary fallback.
assert "glass.setEnableDynamicBackground(premiumLiquid && !powerSave);" in native
assert "glass.setEnableBackdropBlur(premiumLiquid && !powerSave);" in native
assert "glass.setEnableChromaticAberration(premiumLiquid && !powerSave);" in native
assert "glass.setDispersionStrength(premiumLiquid && !powerSave ? .065f : 0f);" in native
assert "glass.setRefractionHeight((premiumLiquid ? 30f : 6f) * d);" in native
assert "View scene = findUnderlay(container);" in native
assert "glass.setBackdropSource(scene != null ? scene : nav);" in native
assert "if (premium) tabs.bringToFront();" in adapter
assert "glass.setEnablePressEffect(false);" in native, "Original tab must remain gesture owner"
assert "LIQUID_FOLLOW = .60f" in motion
assert "LIQUID_RELEASE_MS = 265L" in motion
assert "NAV_LIQUID_STRETCH_ACTIVE" in native

# Android 13 emulator revealed a fatal LinearGradient constructor error:
# a five-colour array with only four positions crashes before the first frame.
# Statistically audit ALL twenty real gradient stop arrays before shipping.
import re
direction_src = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenThemeArtDirection.java").read_text(encoding="utf-8")
stop_pairs = list(re.finditer(
    r"new int\[\]\{([^}]+)\}\s*,\s*new float\[\]\{([^}]+)\}",
    direction_src
))
assert len(stop_pairs) == 20, "Prematurely removed one of the 20 themed shader stop arrays"
for pair in stop_pairs:
    shades = [value.strip() for value in pair.group(1).split(",") if value.strip()]
    positions = [float(value.strip().rstrip("fF"))
                 for value in pair.group(2).split(",") if value.strip()]
    assert len(shades) == len(positions), (
        "ANDROID RENDER CRASH: gradient shades/positions length mismatch"
    )
    assert positions[0] == 0.0 and positions[-1] == 1.0, (
        "Android shader positions must span 0..1"
    )
    assert all(a < b for a,b in zip(positions, positions[1:])), (
        "Android shader positions must be strictly increasing"
    )
print("PASS: all 20 premium gradient stop arrays valid (5 independent scenes)")

# Audit caustic renderer's UI-only API surface; no event handlers, timers,
# draw-loop allocations, network or links to the application's Safe Zone.
optical_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumCaustics.java").read_text(encoding="utf-8")
for unsafe in ("FinanceDbHelper", "SQLiteDatabase", "getAccounts(", "OnClickListener",
               "OnTouchListener", "onDraw(", "Handler(", "postDelayed(", "new Thread(",
               "requestLayout(", "BitmapFactory", "HttpURLConnection", "AssetManager"):
    assert unsafe not in optical_source, "Optics incorrectly modified business logic: "+unsafe
for contract in ("void configure(", "void draw(Canvas canvas)", "case 25:", "case 26:",
                 "case 27:", "case 28:", "default:", "drawRays = false"):
    assert contract in optical_source, (
        "Premium optics owner-specific contract missing: "+contract
    )

# Premium tab glyphs are inert vector drawables, never replacement actions.
tab_glyph = (ROOT / "app/src/main/java/app/wensarafet/tracker/WenPremiumTabIconDrawable.java").read_text(encoding="utf-8")
assert "extends Drawable" in tab_glyph
for shape in ("case 0:", "case 1:", "case 2:", "default:"):
    assert shape in tab_glyph, "Premium icon shape missing: "+shape
for illegal in ("FinanceDbHelper", "SQLiteDatabase", "setOnClickListener",
                "setOnTouchListener", "setOnLongClickListener", "HttpURLConnection"):
    assert illegal not in tab_glyph, "Premium glyph enters Safe Zone: "+illegal
assert "new WenPremiumTabIconDrawable" in adapter, "Premium nav doesn't use original vector icon mapping"
assert "int glyphSize = Ui.dp(context, 29);" in adapter
assert "item.setMinHeight(Ui.dp(context, 48));" in adapter
assert "item.setAutoSizeTextTypeUniformWithConfiguration(" in adapter
assert "dim=Ui.dp(context, 29);" in tab_glyph
assert "WenGoldGlassSurface.navigationInk(theme, selected)" in tab_glyph
assert "glass.setGlassTint(opticalTint, premiumLiquid ? .018f : .045f);" in native

# A viewport size selector may ONLY inspect device configuration and the
# current visual index, never account state, persistence or navigation.
layout_src = (ROOT / "app/src/main/java/app/wensarafet/tracker/WenPremiumLayout.java").read_text(encoding="utf-8")
assert "config.screenHeightDp < 800" in layout_src
assert "dp(context, 86, 76)" in layout_src
assert "WenPremiumLayout.navigationHeight(this)" in current_main
assert "WenPremiumLayout.fabBottomOffset(this)" in current_main
for forbidden in ("FinanceDbHelper", "SQLiteDatabase", "getAccounts(", "setOnClickListener",
                  "getSharedPreferences(", "saveTheme(", "new Thread("):
    assert forbidden not in layout_src, "Premium size adapter entered protected logic"

# Android 13+ optional GPU solar-field shader: only the BACKDROP can
# instantiate it. Pre-33/low-power devices continue using the Canvas fallback.
solar_source = (ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumSunlight.java").read_text(encoding="utf-8")
for ray_contract in ('Build.VERSION.SDK_INT >= 33', 'new RuntimeShader(FRAGMENT)',
                     'void configure(', 'void draw(Canvas canvas, RectF bounds)',
                     'canvas.isHardwareAccelerated()', 'shader.setFloatUniform'):
    assert ray_contract in solar_source, "Optional optical shader contract missing: "+ray_contract
for prohibited in ('FinanceDbHelper', 'SQLiteDatabase', 'OnClickListener', 'Handler(',
                   'postDelayed(', 'HttpURLConnection', 'new Thread(', 'getSharedPreferences('):
    assert prohibited not in solar_source, "Solar shader touches protected area: "+prohibited
# Android AGSL requires *premultiplied* alpha. Without RGB*alpha a
# nominal 10% sunlight beam renders at near-full RGB, causing neon washout.
assert 'half3(uColor*opacity),half(opacity)' in solar_source, (
    "AGSL premultiplied-alpha regression: bright full-screen overexposure"
)
assert 'half3(uColor),half(clamp(glow' not in solar_source
premium_shader_host=(ROOT / "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java").read_text(encoding="utf-8")
assert 'kind == WenReferenceGlassDrawable.Surface.BACKDROP' in premium_shader_host
assert 'sunField = ' in premium_shader_host

# A single inert gold optical material contains no financial, storage,
# navigation, activation, network, or event-handler business logic. Confirm
# fast-path paints and card content layering remain passive.
gold_surface = (ROOT / "app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java").read_text(encoding="utf-8")
for required in ('class WenGoldGlassSurface', 'static void attachGoldBackdrop(',
                 'static int navigationInk(', 'static int headerTitleInk(',
                 'LiquidGlassView glass',
                 'setBackdropSource(target)', 'glass.setEnableBackdropBlur(!economical)',
                 'glass.setEnableDynamicBackground(!economical)',
                 'host.addView(content', 'void enhanceHeader(',
                 'radiusDp*density', 'bounds.height()*.15f'):
    assert required in gold_surface, "Gold material contract missing: "+required
for forbidden in ('FinanceDbHelper', 'SQLiteDatabase', 'AccountRepository',
                  'SmsCaptureReceiver', 'AppLockManager', 'LicenseManager',
                  'setOnClickListener(', 'setOnTouchListener(', 'postDelayed(',
                  'HttpURLConnection', 'getSharedPreferences('):
    assert forbidden not in gold_surface, "Gold material crossed Safe Zone: "+forbidden
for method in ('void draw(Canvas canvas)',):
    assert method in gold_surface

# Only three approved GOLD scenes remain visible. Legacy selection state
# remains stable until manually changed; obsolete 28/29 cannot be restored.
catalogue = (ROOT / "app/src/main/java/app/wensarafet/tracker/ThemeCatalogue.java").read_text(encoding="utf-8")
assert 'if (themeIndex >= 28) return false;' in catalogue
assert 'if (themeIndex < 25) return Ui.themeIndex(context) == themeIndex;' in catalogue
for gold_name in ('"Royal Dark"', '"Imperial Dark"', '"Champagne Light"'):
    assert gold_name in catalogue
assert '"Orange Green"' not in catalogue and '"Pink Violet"' not in catalogue

# Checksums of every remaining tracked file, including money, SQLite,
# SMS notifications, protected signing/biometric and all resource files,
# are guaranteed by the exact allowlist + git diff above.
print("PASS: 1.8.8 source immutable except visual hook + five exact split remainder UI hooks")
print("PASS: original package, schema and signing unchanged; audited version 2.0/code28")
print("PASS: protected finance, debt, SMS, backup, auth and notification trees untouched")
