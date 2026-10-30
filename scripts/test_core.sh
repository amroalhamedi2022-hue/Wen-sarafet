#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/build/core-test"
ECJ_JAR="${ECJ_JAR:-}"
mkdir -p "$OUT_DIR"

compile() {
  if [[ -n "$ECJ_JAR" && -f "$ECJ_JAR" ]]; then
    java -jar "$ECJ_JAR" -encoding UTF-8 -source 17 -target 17 -d "$OUT_DIR" "$@"
  elif java com.sun.tools.javac.Main --release 17 -version >/dev/null 2>&1; then
    java com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$OUT_DIR" "$@"
  else
    # Some slim JDK images omit ct.sym, so --release is unavailable even though
    # the Java 17 language level is supported. Keep local/CI source tests usable.
    java com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -d "$OUT_DIR" "$@"
  fi
}

compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/Money.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FinanceRules.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/DebtLedgerRules.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/UssdCodes.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/TransactionParser.java" \
  "$PROJECT_DIR/tests/FinanceCoreTest.java"

java -cp "$OUT_DIR" FinanceCoreTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/CategorySuggestionRules.java" \
  "$PROJECT_DIR/tests/CategorySuggestionRulesTest.java"
java -cp "$OUT_DIR" CategorySuggestionRulesTest
compile "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/MonthlySavingsRules.java" \
  "$PROJECT_DIR/tests/MonthlySavingsRulesTest.java"
java -cp "$OUT_DIR" MonthlySavingsRulesTest
compile "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/MonthlyBudgetHistory.java" \
  "$PROJECT_DIR/tests/MonthlyBudgetHistoryTest.java"
java -cp "$OUT_DIR" MonthlyBudgetHistoryTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FxTransferMath.java" \
  "$PROJECT_DIR/tests/FxTransferMathTest.java"
java -cp "$OUT_DIR" FxTransferMathTest
compile "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BopIndicativeRate.java" \
  "$PROJECT_DIR/tests/BopIndicativeRateTest.java"
java -cp "$OUT_DIR" BopIndicativeRateTest
compile "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FxRateRefreshPolicy.java" \
  "$PROJECT_DIR/tests/FxRateRefreshPolicyTest.java"
java -cp "$OUT_DIR" FxRateRefreshPolicyTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FinanceRules.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/DebtLedgerRules.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/Models.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/SplitDebtRules.java" \
  "$PROJECT_DIR/tests/SplitDebtRulesTest.java"
java -cp "$OUT_DIR" SplitDebtRulesTest

compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/LicenseProtocol.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupCrypto.java" \
  "$PROJECT_DIR/tests/LicenseProtocolTest.java" \
  "$PROJECT_DIR/tests/BackupCryptoTest.java"

java -cp "$OUT_DIR" app.wensarafet.tracker.LicenseProtocolTest
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupCryptoTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupSettingsCodec.java" \
  "$PROJECT_DIR/tests/BackupSettingsCodecTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupSettingsCodecTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/DriveBackupPublication.java" \
  "$PROJECT_DIR/tests/DriveBackupPublicationTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.DriveBackupPublicationTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupCrypto.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupArchiveVerifier.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupArchiveLimits.java" \
  "$PROJECT_DIR/tests/BackupArchiveVerifierTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupArchiveVerifierTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupArchiveLimits.java" \
  "$PROJECT_DIR/tests/BackupArchiveLimitsTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupArchiveLimitsTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupRestoreStaging.java" \
  "$PROJECT_DIR/tests/BackupRestoreStagingTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupRestoreStagingTest
compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupCrypto.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/BackupRestoreAuthentication.java" \
  "$PROJECT_DIR/tests/BackupRestoreAuthenticationTest.java"
java -cp "$OUT_DIR" app.wensarafet.tracker.BackupRestoreAuthenticationTest
python3 "$PROJECT_DIR/tests/debt_migration_audit.py"
python3 "$PROJECT_DIR/tests/fx_migration_audit.py"
python3 "$PROJECT_DIR/tests/category_audit_migration_v10.py"
python3 "$PROJECT_DIR/tests/fx_form_source_contract.py"

python3 "$PROJECT_DIR/tests/split_debt_schema_v11.py"

compile \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/FinanceRules.java" \
  "$PROJECT_DIR/app/src/main/java/app/wensarafet/tracker/SimpleUndoRules.java" \
  "$PROJECT_DIR/tests/SimpleUndoRulesTest.java"
java -cp "$OUT_DIR" SimpleUndoRulesTest
python3 "$PROJECT_DIR/tests/simple_delete_undo_v12.py"

python3 "$PROJECT_DIR/tests/finance_delete_cursor_and_category_undo_contract.py"
python3 "$PROJECT_DIR/tests/financial_analytics_contract.py"

python3 "$PROJECT_DIR/tests/motion_accessibility_contract.py"
python3 "$PROJECT_DIR/tests/income_drilldown_contract.py"
python3 "$PROJECT_DIR/tests/reminder_snooze_contract.py"
python3 "$PROJECT_DIR/tests/chart_capture_quality_contract.py"

python3 "$PROJECT_DIR/tests/backup_restore_preflight_contract.py"
python3 "$PROJECT_DIR/tests/drive_backup_publication_contract.py"
