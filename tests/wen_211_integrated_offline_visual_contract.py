#!/usr/bin/env python3
"""Cumulative offline + neutral-glass release contract.

Supersedes the visual-ONLY SHA assumptions for this integration branch.
It does not waive them: reconstruct the *entire* 290 KB MainActivity from
two independently reviewed, pinned predecessors. Exact comparison rejects
even a one-character undeclared change to finance, DB or authorization logic.
Run with Git full history (actions/checkout fetch-depth: 0).
"""
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
assert expected == present(M), (
    "SAFE ZONE: cumulative MainActivity deviates from the fully reconstructed "
    "visual HEAD + reviewed strict-offline security overlay"
)

# Pin the complete graphic edit and retain EXACT pixel-only neutral
# presentation from the approved adapter, not just indicative string checks.
UI = P + "Ui.java"
BUBBLE = "glass-engine/src/main/java/app/wensarafet/glass/WenFabBubbleDrawable.java"
assert present(UI) == at(NEUTRAL_UI, UI), "Unexpected visual/UI mutation"
bubble = present(BUBBLE)
assert bubble == at(NEUTRAL_BUBBLE, BUBBLE), "Unreviewed global FAB appearance mutation"
original_bubble = at(VISUAL_BASE, BUBBLE)
marker = "    public float getPress() { return press; }"
assert bubble[bubble.index(marker):] == original_bubble[original_bubble.index(marker):], (
    "FAB original press, click and draw contract was altered"
)
assert "fab.setBackground(Ui.glassFloatingAddBackground(this));" in expected
assert "WenFabBubbleDrawable(" in present(UI)
assert "new int[]{0x4CFFFFFF,0x1FFFFFFF,0x38FFFFFF}" in bubble
assert "new int[]{0x94FFFFFF,0x38FFFFFF,0x75FFFFFF}" in bubble
assert "return false; // Never steal existing FAB click/hold actions." in present(UI)

# All business and owner protection files are exact-source identical to the
# independently passed visual baseline. No migrations, balance adjustments,
# signing, activation or data-rewrite code changed in this merge.
protected = (
    P + "FinanceDbHelper.java", P + "Money.java",
    P + "FinanceRules.java", P + "DebtLedgerRules.java",
    P + "AppIntegrity.java", P + "LicenseManager.java",
    P + "AppLockManager.java", P + "SecurePinStore.java",
    P + "BackupCrypto.java", P + "BackupArchiveVerifier.java",
    P + "SmsCaptureReceiver.java", P + "FinanceNotificationListener.java",
    "app/build.gradle", "settings.gradle"
)
for name in protected:
    assert present(name) == at(VISUAL_BASE, name), (
        "SAFE ZONE: protected code changed: " + name
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
    "app/src/main/AndroidManifest.xml", P + "TransactionParser.java",
    "app/src/main/res/xml/backup_rules.xml",
    "app/src/main/res/xml/data_extraction_rules.xml",
    "tests/FinanceCoreTest.java", "tests/fx_form_source_contract.py",
    "tests/source_repository_guard.py",
    "scripts/audit_recovered_source.sh", "scripts/audit_release.sh",
    "docs/security/WEN_200_STRICT_OFFLINE_OVERLAY_2026-11-06.md",
)
for path in exact_security:
    assert present(path) == at(SECURITY_BASE, path), (
        "Reviewed strict-offline source unexpectedly diverged: " + path
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
assert "WenPremiumLayout.navigationClearance(this)" in present(M)
assert "WenGoldGlassSurface.attachGoldBackdrop(primaryRoot)" in present(M)

# All existing non-security code must be unchanged from latest visual branch.
# Paths covered above are independently pinned; no other changes allowed.
permitted = set(exact_security) | set(removed_network) | {
    M, UI, BUBBLE, "scripts/test_core.sh",
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
}
modified = set(git("diff", "--name-only", VISUAL_BASE, "HEAD").splitlines())
assert modified <= permitted, "Unexpected cross-branch file changes: " + repr(sorted(modified - permitted))

print("PASS: cumulative MainActivity byte-exact; finance/auth/SQLite/logo unchanged; "
      "neutral glass across themes; strict offline source and manifest verified.")
