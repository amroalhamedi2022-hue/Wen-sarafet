#!/usr/bin/env python3
"""UI-only controls: no live per-keystroke suggestion and no stale edit menu."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
manual=ui.split("private void showAddTransaction() {",1)[1].split("private void ",1)[0]
assert "db.suggestCategory(" not in manual
assert "refreshSuggestion.run()" not in manual
assert "amount.addTextChangedListener(learningInputs)" not in manual
assert "note.addTextChangedListener(learningInputs)" not in manual
assert "category.setAdapter(adapter(labels))" in manual
assert "db.addTransaction(" in manual
menu=ui.split("private void showTransactionEditMenu(Models.Transaction tx)",1)[1].split(
    "private void showSimpleManualTransactionEditDialog(",1)[0]
assert "showSimpleManualTransactionEditDialog(tx)" not in menu
assert "تعديل المبلغ والملاحظة" not in menu
assert "showCategoryPicker(tx, FinanceRules.EXPENSE)" in menu
history=ui.split("private void showAuditHistory()",1)[1].split("private void showCategoryManagement()",1)[0]
assert "AuditPresentation.title(" in history
assert "AuditPresentation.details(" in history
assert "SELECT id,name FROM categories" in history
assert "SELECT id,name,currency FROM accounts" in history
assert "اضغط لعرض تفاصيل هذا التغيير" in history
assert "if(entry.undoable &&" in history
assert "db.undoDeletedSimpleTransaction(entry.id)" in history
assert "db.undoLatestSimpleCategoryChange(entry.id)" in history
assert "entry.action+\" · #" not in history
print("AuditAndManualEntryContract PASS: clear audit, protected Undo, no manual suggestion scans/amount editor")
