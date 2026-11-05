#!/usr/bin/env python3
"""Merchant suggestions only in captured notifications, never auto-post or key-by-key UI."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
capture=(root/"app/src/main/java/app/wensarafet/tracker/CaptureEngine.java").read_text(encoding="utf-8")
policy=(root/"app/src/main/java/app/wensarafet/tracker/CaptureSuggestionPolicy.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
form=ui.split("private void showAddTransaction()",1)[1].split("private void ",1)[0]
assert "db.suggestCategory(" not in form
assert "refreshSuggestion.run()" not in form
assert "CaptureSuggestionPolicy.suggest(context,db,transactionId" in capture
assert "اقتراح حسب المحل:" in capture
assert "هذا اقتراح فقط، ولم يُغيّر التطبيق تصنيف العملية تلقائيًا" in capture
assert "notifySaved(context, id, parsed, db, txId, clean)" in capture
assert "db.importCapture(id, parsed.type)" in capture
assert capture.index("db.importCapture(id, parsed.type)") < capture.index(
    "notifySaved(context, id, parsed, db, txId, clean)")
assert "db.reclassifyTransaction(" not in capture
assert "db.updateDebtTransaction(" not in capture
assert "6L*60*60*1000" in policy
assert '"merchant_receipt_suggestion_cache"' in policy
assert "MessageDigest.getInstance(\"SHA-256\")" in policy
assert '"بحسب المحل أو الوصف السابق".equals(suggestion.reason)' in policy
assert "db.suggestCategory(" in policy
assert "db.getCategories(parsed.type)" in policy
assert "db.getTransactionById(transactionId)" in policy
assert "cached" not in form.lower()
print("CapturedMerchantSuggestionContract PASS: once per captured merchant, cached and suggestion-only, no manual-input scans")
