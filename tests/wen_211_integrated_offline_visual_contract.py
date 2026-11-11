#!/usr/bin/env python3
"""Cumulative offline + neutral-glass release contract.

Supersedes the visual-ONLY SHA assumptions for this integration branch.
It does not waive them: reconstruct the *entire* 290 KB MainActivity from
two independently reviewed, pinned predecessors. Exact comparison rejects
even a one-character undeclared change to finance, DB or authorization logic.
Run with Git full history (actions/checkout fetch-depth: 0).
"""
import hashlib
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
VISUAL_BASE = "74a92381bbc98b1129fe729f3794820bc392a96a"
NEUTRAL_MAIN = "44f8548bf83f56897acac2377a2d31b1c522398a"
NEUTRAL_UI = "37c32e0761b802519ea7290a894c19a7b78e037c"
NEUTRAL_BUBBLE = "cd3731f2d50b4fd5a1436ded3cbcdc4c94776840"
SECURITY_BASE = "3ce3542df5eb6079646e1eee3bdc929cc6730133"
P = "app/src/main/java/app/wensarafet/tracker/"
M = P + "MainActivity.java"
RELEASE_BASE = "19bc7621bf2621d4d89b18d2d91e29a4f558ff8c"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True)


def at(ref, path):
    return git("show", ref + ":" + path)


def present(path):
    return (ROOT / path).read_text(encoding="utf-8")


def exactly_once(source, part):
    assert source.count(part) == 1, ("Not a unique security patch anchor: " + repr(part[:90]))


def replace(source, old, new):
    exactly_once(source, old)
    return source.replace(old, new, 1)


def section(source, start, end, inclusive=False):
    a, b = source.index(start), source.index(end, source.index(start) + len(start))
    return source[a:b + (len(end) if inclusive else 0)]


# Fully reproduce EXACTLY the approved security-only modifications atop the
# newest neutral-FAB MainActivity; no code from the older security MainActivity
# replaces later Liquid Glass, screen hosting, navigation or account UI.
expected = at(NEUTRAL_MAIN, M)
security = at(SECURITY_BASE, M)
expected = replace(expected, "    private char[] pendingDrivePassword;\n", "")
expected = replace(expected, "    private static final int OPEN_DRIVE_FOLDER = 7304;\n", "")


def transplant(start, end, inclusive=False):
    global expected
    old = section(expected, start, end, inclusive)
    new = section(security, start, end, inclusive)
    expected = replace(expected, old, new)


transplant("    @Override protected void onStop() {",
           "    @Override protected void onNewIntent")
transplant("    @Override protected void onActivityResult",
           "    private void clearPendingBackupPassword()")
expected = replace(expected, section(expected,
           "    private void showDriveLinkDialog() {",
           "    private void showRestoreBackupDialog("), "")
transplant("    private void showFxTransferDialog() {",
           "    private void showTransactionTransferDialog")
transplant('        content.addView(sectionTitle("النسخ الاحتياطي"',
           "        content.addView(backupCard);", True)
# VISUAL ONLY owner-reviewed 2026-11-11: reconstruct five exact sandbox-parity changes to the already pinned strict-offline MainActivity.
# Every patch anchor must occur exactly once, without modifying money/security/navigation handlers.
expected = replace(expected, "        // Gold floating dock must NEVER obscure a readable account/amount.\n        // Reserve only viewport space; DB queries and scroll data stay exact.\n        if (WenGoldGlassSurface.enabled(this))\n            shell.setPadding(0,0,0,WenPremiumLayout.navigationClearance(this));", "        // Keep the scroll viewport full-height behind the floating navigation.\n        // The content itself receives a safe bottom spacer below; this allows\n        // real on-screen cards to show THROUGH the dock while scrolling.")
expected = replace(expected, "        LinearLayout content = showTransactions ? transactionsContent(1000) : homeContent();\n        scroll.addView(content", "        LinearLayout content = showTransactions ? transactionsContent(1000) : homeContent();\n        if (WenGoldGlassSurface.enabled(this)) {\n            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),\n                    content.getPaddingRight(), Math.max(content.getPaddingBottom(),\n                            WenPremiumLayout.navigationClearance(this)));\n        }\n        scroll.addView(content")
expected = replace(expected, "        final View topBarBackground = topBar.findViewWithTag(\"main_header_background\");\n        scroll.setOnScrollChangeListener", "        final View topBarBackground = topBar.findViewWithTag(\"main_header_background\");\n        final View topBarOptics = topBar.findViewWithTag(WenGoldGlassSurface.HEADER_GLASS_TAG);\n        final View topBarSpecular = topBar.findViewWithTag(WenGoldGlassSurface.HEADER_SPECULAR_TAG);\n        scroll.setOnScrollChangeListener")
expected = replace(expected, "                topBarBackground.setAlpha(1f - progress * .84f);\n                topBarBackground.setElevation(progress > .02f ? 0f : Ui.dp(this, 7));", "                // The native LiquidGlassView and the specular overlay ALSO\n                // fade. Previously only the tiny underlying backing faded.\n                float initialBacking = WenGoldGlassSurface.enabled(this)\n                        ? (Ui.themeIndex(this) == 27 ? .15f : .12f) : 1f;\n                topBarBackground.setAlpha(initialBacking * (1f - progress * .94f));\n                if (topBarOptics != null) topBarOptics.setAlpha(1f - progress * .92f);\n                if (topBarSpecular != null) topBarSpecular.setAlpha(1f - progress * .92f);\n                topBarBackground.setElevation(progress > .02f ? 0f : Ui.dp(this, 7));")
expected = replace(expected, "        nav.setElevation(Ui.dp(this, 12));\n\n        TextView home = navButton", "        nav.setElevation(Ui.dp(this, WenGoldGlassSurface.enabled(this) ? 5 : 12));\n\n        TextView home = navButton")


# 2.1 optical parity changes are limited to these six exact markup-only
# edits. Financial and navigation click handlers remain the original lines.
expected = replace(expected, "        if (Ui.isLiquidTheme(this)) {\n            // Owner reference: truly floating 16dp inset, preserving", "        if (Ui.isUnifiedGlassTheme(this)) {\n            // Owner reference: truly floating glass dock, preserving")
expected = replace(expected, "        if (Ui.isLiquidTheme(this))\n            NavigationVisualDecorator.install(nav,", "        if (Ui.isUnifiedGlassTheme(this))\n            NavigationVisualDecorator.install(nav,")
expected = replace(expected, "        if (Ui.isLiquidTheme(this)) debtNavParams.setMargins(", "        if (Ui.isUnifiedGlassTheme(this)) debtNavParams.setMargins(")
expected = replace(expected, "        if (Ui.isLiquidTheme(this)) analyticsNavParams.setMargins(", "        if (Ui.isUnifiedGlassTheme(this)) analyticsNavParams.setMargins(")
expected = replace(expected, "        final View background=topBar.findViewWithTag(\"main_header_background\");\n        scroll.setOnScrollChangeListener", "        final View background=topBar.findViewWithTag(\"main_header_background\");\n        final View goldOptic=topBar.findViewWithTag(WenGoldGlassSurface.HEADER_GLASS_TAG);\n        final View goldSpecular=topBar.findViewWithTag(WenGoldGlassSurface.HEADER_SPECULAR_TAG);\n        scroll.setOnScrollChangeListener")
expected = replace(expected, "                background.setAlpha(1f-fade*.84f);\n                background.setElevation(fade>.02f?0f:Ui.dp(this,7));", "                float baseAlpha=WenGoldGlassSurface.enabled(this)\n                        ? (Ui.themeIndex(this)==27?.15f:.12f):1f;\n                background.setAlpha(baseAlpha*(1f-fade*.94f));\n                if(goldOptic!=null)goldOptic.setAlpha(1f-fade*.92f);\n                if(goldSpecular!=null)goldSpecular.setAlpha(1f-fade*.92f);\n                background.setElevation(fade>.02f?0f:Ui.dp(this,7));")

assert expected == at(RELEASE_BASE, M), (
    "SAFE ZONE: cumulative MainActivity deviates from the fully reconstructed "
    "visual HEAD + reviewed strict-offline security overlay"
)

# Pin the complete graphic edit and retain EXACT pixel-only neutral
# presentation from the approved adapter, not just indicative string checks.
UI = P + "Ui.java"
BUBBLE = "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java"
FINAL_VISUAL_AUDIT = "f5211774e58b50addcce054f85427643d0acaf3e"
assert at(RELEASE_BASE, UI) == at(FINAL_VISUAL_AUDIT, UI), (
    "UI must match exactly the reviewed 2.1 visual-only surface changes"
)
bubble = at(RELEASE_BASE, BUBBLE)
reviewed_bubble = at(NEUTRAL_BUBBLE, BUBBLE)
# Only four white-glow paint alpha factors change; geometry and press logic frozen.
reviewed_bubble = replace(reviewed_bubble, "        opticalDepth.setAlpha((int)(alpha*(.84f+.16f*press)));", "        opticalDepth.setAlpha((int)(alpha*(.70f+.20f*press)));")
reviewed_bubble = replace(reviewed_bubble, "        highlight.setAlpha((int)(alpha*(.66f+.15f*press)));", "        highlight.setAlpha((int)(alpha*(.45f+.20f*press)));")
reviewed_bubble = replace(reviewed_bubble, "        caustic.setAlpha((int)(alpha*(.70f+.30f*press)));", "        caustic.setAlpha((int)(alpha*(.32f+.26f*press)));")
reviewed_bubble = replace(reviewed_bubble, "        rainbow.setAlpha((int)(alpha*(.72f+.28f*press)));", "        rainbow.setAlpha((int)(alpha*(.46f+.34f*press)));")
assert bubble == reviewed_bubble, "Unreviewed FAB source mutation outside approved optical-tone patch"
original_bubble = at(VISUAL_BASE, BUBBLE)
marker = "    public float getPress() { return press; }"
end_press = "    @Override public void draw(Canvas c)"
assert section(bubble, marker, end_press) == section(original_bubble, marker, end_press), (
    "FAB input/press behavior must match the originally reviewed sandbox"
)
assert "fab.setBackground(Ui.glassFloatingAddBackground(this));" in expected
assert "WenFabBubbleDrawable(" in present(UI)
assert "new int[]{0x4CFFFFFF,0x1FFFFFFF,0x38FFFFFF}" in bubble
assert "new int[]{0x94FFFFFF,0x38FFFFFF,0x75FFFFFF}" in bubble
assert "return false; // Never steal existing FAB click/hold actions." in present(UI)

# Other appearance-only files are frozen byte-for-byte against the reviewed
# visual update commit, independent from the older security/financial baseline.
VISUAL_REFRESH_AUDIT = FINAL_VISUAL_AUDIT
VISUAL_REFRESH_PATHS = (
    P + "NavigationVisualDecorator.java",
    P + "ThemeCatalogue.java",
    P + "WenGoldGlassSurface.java",
    P + "WenPremiumLayout.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenPremiumLiquidDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenGlassDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/WenReferenceGlassDrawable.java",
    "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java",
    P + "WenScreenVisualMotion.java",
    "tests/glass_energy_contract.py",
    "tests/original_180_upgrade_contract.py",
    "tests/upgrade_gate_contract.py",
    "tests/wen_200_strict_offline_contract.py",
    ".github/workflows/unsigned-release-candidate.yml",
    "scripts/build_apk.sh",
    "scripts/verify_upgrade_apk.sh",
    "scripts/sign_verified_upgrade.sh",
    "scripts/audit_recovered_source.sh",
    "app/src/androidTest/java/app/wensarafet/tracker/ReferenceThemesInstrumentedTest.java",
)
for path in VISUAL_REFRESH_PATHS:
    assert at(RELEASE_BASE, path) == at(VISUAL_REFRESH_AUDIT, path), (
        "Unreviewed visual delta outside eight-file 2026-11-11 review: " + path
    )

# The previous release remains fully reproducible above. Pin this visual
# update by exact file bytes while all other visual files stay on that release.
REFINEMENT_SHA256 = {
    "app/src/androidTest/java/app/wensarafet/tracker/ReferenceThemesInstrumentedTest.java":
        "c1398e1c8d642ef04ad1283089299c6ba1f4a0c08d490895395847258833565e",
    "app/src/main/java/app/wensarafet/tracker/MainActivity.java":
        "4b313ee0058d01da0bce3a04b22a4ad10a3d0453afbb1c4fe277f1b98119f12a",
    "app/src/main/java/app/wensarafet/tracker/NavigationLiquidMaterial.java":
        "330135f1cda3fc570b593a77c623a3242ac7a01e09f713ff379beb5eabf23032",
    "app/src/main/java/app/wensarafet/tracker/NavigationVisualDecorator.java":
        "e379339cb13ad9fad10e54f14adfa13c026dafef01e9d0576cefff7ba9b9870c",
    "app/src/main/java/app/wensarafet/tracker/Ui.java":
        "dfdefa0db2944c094cdf9a1f12ba79a616dad1c8e489a3803d5727d1c34b32c7",
    "app/src/main/java/app/wensarafet/tracker/WenGoldGlassSurface.java":
        "22f2f80f34d0c51f06d0d659b70123e4bdacff3078bec10e5dc7bb6298196e4b",
    "app/src/main/java/app/wensarafet/tracker/WenPremiumLayout.java":
        "f260ed3372b95ff9ce811afcc211f30054752db761217ecf20f3eaa6afc4e0f8",
    "app/src/main/java/app/wensarafet/tracker/WenScreenVisualMotion.java":
        "ec34cbafc3afcc940e2987b0a39bd9b29d59950029b2d38c211b17cab8b1cb63",
    "glass-engine/src/main/java/app/wensarafet/glass/GlassMotion.java":
        "299b98c3647f29cc4d39c4bb1979b50365d23a0cf5128225db021225cdb956ba",
    "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java":
        "38a709218671b6a79d98a377540cc420d6243d3bf57c9d813387d5115041d078",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationLensDrawable.java":
        "ea13ce9781675ba864000156b1cf2406c629398d0348788e2eb7e4ec174e473f",
    "glass-engine/src/main/java/app/wensarafet/glass/WenNavigationTrack.java":
        "057e8a4c19686ff67ac36c2aa2a7fd6e0d4fef5c34c38592d82c7078727ea087",
    "glass-engine/src/main/java/app/wensarafet/glass/WenOpticalNavigationOverlay.java":
        "40d1de4de7ba806af6a5cb2a97f034b44f7aca198d07a4d2b44d20578f6f0f26",
    "scripts/capture_main_no_instrumentation.sh":
        "76e488173ed7f4790888db2c8df3ba8ce71093cc5057f35ae50b9528a4c80abd",
    "scripts/capture_owner_android_device.ps1":
        "177e76d2a164ba692c2e6e37f8a1cbbe17491a2375d9df16fdc9335afaaf8c44",
    "tests/glass_energy_contract.py":
        "af977d5e19a85bc42a7a162bedf67b28b605f25cf95a54614deb443361b2d0c1",
    "tests/ui_polish_185_contract.py":
        "611394c7981b51bc2adb52f45a3d1ff9e52389915769a98470796167473dacd6",
}
for path, expected_sha in REFINEMENT_SHA256.items():
    actual_sha = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
    assert actual_sha == expected_sha, "Unreviewed visual file bytes: " + path
for path in VISUAL_REFRESH_PATHS:
    if path not in REFINEMENT_SHA256:
        assert present(path) == at(RELEASE_BASE, path), "Unreviewed change: " + path

# All business and owner protection files are exact-source identical to the
# independently passed visual baseline. No migrations, balance adjustments,
# signing, activation or data-rewrite code changed in this merge.
protected = (
    P + "FinanceDbHelper.java", P + "Money.java",
    P + "FinanceRules.java", P + "DebtLedgerRules.java",
    P + "AppIntegrity.java", P + "LicenseManager.java",
    P + "AppLockManager.java", P + "SecurePinStore.java",
    P + "BackupCrypto.java", P + "BackupArchiveVerifier.java",
    P + "SmsCaptureReceiver.java",
    "settings.gradle"
)
for name in protected:
    assert present(name) == at(VISUAL_BASE, name), (
        "SAFE ZONE: protected code changed: " + name
    )

# Only the release version increments; application ID, signing, protections,
# min/target SDK, dependency graph and Gradle signing configuration are frozen.
original_gradle = at(VISUAL_BASE, "app/build.gradle")
release_gradle = replace(original_gradle, "        versionCode 28\n        versionName \"2.0\"",
                         "        versionCode 29\n        versionName \"2.1\"")
assert present("app/build.gradle") == release_gradle, (
    "SAFE ZONE: release Gradle changed beyond 2.1/code29 increment"
)
logos = {
    "app/src/main/res/drawable-nodpi/icon_bop_mark.png":
        "d71006e0eba4cedcd1d04b2217eeef4911b83808",
    "app/src/main/res/drawable-nodpi/icon_jawwal_pay_mark.png":
        "0516bd77381b48b87c404e613c908405812d7b88",
    "app/src/main/res/drawable-nodpi/icon_palpay_mark.png":
        "d09932adbc59a39b4ba5b8d52a7350f7aa4ba1a8",
}
for path, sha in logos.items():
    assert git("hash-object", path).strip() == sha, "Original bank account logo changed: " + path

# Import only the reviewed original SECURITY file contents; no older visual
# or financial source can be smuggled in through these shared paths.
exact_security = (
    "app/src/main/AndroidManifest.xml",
    "app/src/main/res/xml/backup_rules.xml",
    "app/src/main/res/xml/data_extraction_rules.xml",
    "tests/fx_form_source_contract.py",
    "tests/source_repository_guard.py",
    "scripts/audit_release.sh",
    "docs/security/WEN_200_STRICT_OFFLINE_OVERLAY_2026-11-06.md",
)
# The recovered-source audit changes ONLY the whitelist for 2.1 and a
# version-only comment. All strict offline/secret assertions remain intact.
recovered_audit = "scripts/audit_recovered_source.sh"
expected_audit = at(SECURITY_BASE, recovered_audit)
expected_audit = replace(expected_audit,
    '[[ "$version_name" =~ ^(1[.](8[.][0-9]+|9)|2[.]0)$ ]]',
    '[[ "$version_name" =~ ^(1[.](8[.][0-9]+|9)|2[.](0|1))$ ]]')
expected_audit = expected_audit.replace(
    'Prevent any runtime Internet permission in the full-featured 2.0 app.',
    'Prevent any runtime Internet permission in the full-featured 2.1 app.')
assert present(recovered_audit) == expected_audit, (
    "SAFE ZONE: recovered source audit changed beyond 2.1 release metadata")

for path in exact_security:
    assert present(path) == at(SECURITY_BASE, path), (
        "Reviewed strict-offline source unexpectedly diverged: " + path
    )

# A *single* reviewed security enhancement expands "Code:" OTP rejection
# beyond the old numeric 4–8 digit heuristic. Pin both entire source files
# after adding positive and negative regression fixtures; other finance logic
# remains byte-identical to the previously approved security snapshot.
OTP_AUDIT = "a88abf5042ef9b7e9cf6c0c1a2020a78f1570b97"
for path in (P + "TransactionParser.java", "tests/FinanceCoreTest.java"):
    assert present(path) == at(OTP_AUDIT, path), (
        "SAFE ZONE: OTP parser or finance regression tests changed beyond the reviewed patch: " + path
    )
assert 'CODE_PREFIX.matcher(normalized).find()' in present(P + "TransactionParser.java")
assert 'CODE： ABC123' in present("tests/FinanceCoreTest.java")
assert 'Code: short' in present("tests/FinanceCoreTest.java")
assert 'CaptureEngine.handle' in present(P + "SmsCaptureReceiver.java")
assert 'CaptureEngine.handle' in present(P + "FinanceNotificationListener.java")
assert 'TransactionParser.isVerificationMessage(clean)' in present(P + "CaptureEngine.java")

# The ONLY Android notification change is a second, early OTP gate before
# prefixing title. Freeze the entire class at the audited privacy commit.
OTP_NOTIFICATION_AUDIT = "d662a41c75522ba56b3c2393a086951e83a0ccbb"
notifier = P + "FinanceNotificationListener.java"
assert present(notifier) == at(OTP_NOTIFICATION_AUDIT, notifier), (
    "SAFE ZONE: notification capture changed beyond reviewed OTP-only fix"
)
assert present(notifier).index("isVerificationMessage(body)") < (
    present(notifier).index('body = title + " — " + body;')
)



removed_network = (
    P + "BopRateFetcher.java", P + "BopIndicativeRate.java",
    P + "FxRateRefreshPolicy.java", P + "DriveBackupManager.java",
    P + "DriveBackupPublication.java"
)
assert all(not (ROOT / p).exists() for p in removed_network), (
    "Network rate or cloud backup runtime classes must remain deleted"
)
for needle in (
    "DriveBackupManager", "DriveBackupPublication", "BopRateFetcher",
    "fx_online_rates", "OPEN_DRIVE_FOLDER", "pendingDrivePassword",
    "showDriveLinkDialog", "تحديث أونلاين"
):
    assert needle not in present(M), "Network reference reintroduced: " + needle
for needle in (
    "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"
):
    assert needle not in present("app/src/main/AndroidManifest.xml")
assert 'android:allowBackup="false"' in present("app/src/main/AndroidManifest.xml")
assert 'isLocalDocumentUri(uri)' in present(M)
assert '"MANUAL",System.currentTimeMillis()' in present(M)
assert 'Code: 12345 For p2m' in present("tests/FinanceCoreTest.java")
assert "R.drawable.icon_bop_mark" in present(M)
assert "visualMotion.captureOriginalTabs" in present(M)
assert "primaryDockParams()" in present(M)
assert "WenGoldGlassSurface.attachGoldBackdrop(primaryRoot)" in present(M)

# All existing non-security code must be unchanged from latest visual branch.
# Paths covered above are independently pinned; no other changes allowed.
permitted = set(exact_security) | set(removed_network) | {
    M, UI, BUBBLE, "app/build.gradle", "scripts/test_core.sh",
    "tests/BopIndicativeRateTest.java", "tests/FxRateRefreshPolicyTest.java",
    "tests/DriveBackupPublicationTest.java",
    "tests/drive_backup_publication_contract.py",
    "tests/glass_energy_contract.py",
    "tests/wen_211_integrated_offline_visual_contract.py",
    ".github/workflows/android-compile.yml",
    ".github/workflows/unsigned-release-candidate.yml",
    ".github/workflows/wen-188-visual-safe-review.yml",
    ".github/workflows/core-rules.yml",
    ".github/workflows/wen-main-physical-screens.yml",
    ".github/workflows/wen-real-main-screenshots.yml",
    "tests/wen_200_strict_offline_contract.py",
    # Fully pinned in OTP_AUDIT above; no other finance/parser changes.
    P + "TransactionParser.java",
    "tests/FinanceCoreTest.java",
    P + "FinanceNotificationListener.java",
    "tests/otp_capture_contract.py",
}
permitted.update(VISUAL_REFRESH_PATHS)
permitted.update(REFINEMENT_SHA256)
modified = set(git("diff", "--name-only", VISUAL_BASE, "HEAD").splitlines())
assert modified <= permitted, "Unexpected cross-branch file changes: " + repr(sorted(modified - permitted))

print("PASS: cumulative MainActivity and glass updates pinned; finance/auth/SQLite/logo unchanged; "
      "neutral glass across themes; strict offline source and manifest verified.")
