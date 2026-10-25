#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/build/core-test"
ECJ_JAR="${ECJ_JAR:-}"
mkdir -p "$OUT_DIR"

compile() {
  if [[ -n "$ECJ_JAR" && -f "$ECJ_JAR" ]]; then
    java -jar "$ECJ_JAR" -encoding UTF-8 -source 17 -target 17 -d "$OUT_DIR" "$@"
  else
    java com.sun.tools.javac.Main -encoding UTF-8 --release 17 -d "$OUT_DIR" "$@"
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
