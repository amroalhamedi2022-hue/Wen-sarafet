#!/usr/bin/env python3
"""SQL-level acceptance test for safe manual amount/note edits (no Android runtime)."""
import json
import re
import sqlite3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
db_source=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
ui_source=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text()

part=db_source.split("private static final String SAFE_SIMPLE_MANUAL_EDIT =",1)[1].split(";",1)[0]
terms=re.findall(r'"(?:\\.|[^"\\])*"',part)
assert terms
filter_sql="".join(json.loads(token) for token in terms)
assert "t.source='MANUAL'" in filter_sql
assert "t.needs_review=0" in filter_sql
for required in ("t.destination_account_id IS NULL","t.special_kind IS NULL",
                 "t.destination_amount_minor IS NULL", "t.exchange_rate IS NULL",
                 "transaction_splits", "debt_movements", "capture_inbox",
                 "t.category_id IS NOT NULL", "c.kind=t.type"):
    assert required in filter_sql,required
assert db_source.count("+SAFE_SIMPLE_MANUAL_EDIT")>=1
method=db_source.split("public void editSimpleManualTransaction(",1)[1].split(
    "public void deleteTransaction(",1)[0]
for required in ("writable.beginTransaction();",
                 "oldAmount!=expectedAmountMinor", "oldNote.equals",
                 "SimpleTransactionEditRules.balanceDelta", "adjustBalance(writable,accountId,delta)",
                 "writable.update(\"transactions\"", "writable.setTransactionSuccessful();",
                 "writable.endTransaction();", "BalanceWidgetProvider.updateAll(appContext);"):
    assert required in method,required
# Owner explicitly removed this action from the edit menu: no UI call path is allowed.
menu=ui_source.split("private void showTransactionEditMenu(Models.Transaction tx)",1)[1].split(
    "private void showSimpleManualTransactionEditDialog(",1)[0]
assert "showSimpleManualTransactionEditDialog(tx)" not in menu
assert "تعديل المبلغ والملاحظة" not in menu
# The protected edit rules are kept in Safe Zone; no blind code deletion.
assert "db.editSimpleManualTransaction(tx.id,tx.amountMinor,tx.note,value," in ui_source
assert "db.editSimpleManualTransaction(tx.id,tx.amountMinor,tx.note,value," in ui_source
assert "java.math.BigDecimal.valueOf(tx.amountMinor,2).toPlainString()" in ui_source

db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY,kind TEXT);
CREATE TABLE transactions(
 id INTEGER PRIMARY KEY,source TEXT,needs_review INTEGER,type TEXT,category_id INTEGER,
 amount_minor INTEGER,destination_account_id INTEGER,special_kind TEXT,debt_person_id INTEGER,
 debt_direction TEXT,destination_amount_minor INTEGER,exchange_rate TEXT,
 exchange_rate_source TEXT,exchange_rate_checked_at INTEGER,account_id INTEGER,
 affects_balance INTEGER,note TEXT
);
CREATE TABLE transaction_splits(transaction_id INTEGER);
CREATE TABLE debt_movements(transaction_id INTEGER);
CREATE TABLE capture_inbox(transaction_id INTEGER);
INSERT INTO categories VALUES(1,'EXPENSE'),(2,'INCOME'),(3,'TRANSFER');
INSERT INTO transactions(id,source,needs_review,type,category_id,amount_minor,account_id,affects_balance,note)
VALUES (1,'MANUAL',0,'EXPENSE',1,100,1,1,'bread'),
       (2,'MANUAL',0,'INCOME',2,100,1,1,'salary'),
       (3,'MANUAL',0,'EXPENSE',1,100,1,0,'record only'),
       (4,'SMS',0,'EXPENSE',1,100,1,1,'imported'),
       (5,'MANUAL',0,'EXPENSE',1,100,1,1,'split'),
       (6,'MANUAL',0,'EXPENSE',1,100,1,1,'debt'),
       (7,'MANUAL',0,'EXPENSE',1,100,1,1,'fx'),
       (8,'MANUAL',0,'EXPENSE',NULL,100,1,1,'uncategorized'),
       (9,'MANUAL',0,'EXPENSE',1,100,1,1,'captured'),
       (10,'MANUAL',0,'EXPENSE',1,100,1,1,'linked debt'),
       (11,'MANUAL',0,'EXPENSE',2,100,1,1,'wrong category'),
       (12,'MANUAL',1,'EXPENSE',1,100,1,1,'review'),
       (13,'MANUAL',0,'TRANSFER',NULL,100,1,1,'transfer');
UPDATE transactions SET special_kind='DEBT' WHERE id=6;
UPDATE transactions SET destination_amount_minor=250 WHERE id=7;
INSERT INTO transaction_splits VALUES(5);
INSERT INTO capture_inbox VALUES(9);
INSERT INTO debt_movements VALUES(10);
""")
selected=[row[0] for row in db.execute(
    "SELECT t.id FROM transactions t WHERE "+filter_sql+" ORDER BY t.id")]
assert selected==[1,2,3],selected
# A referenced manual transaction remains protected even if a stale screen still points to it.
db.execute("INSERT INTO transaction_splits VALUES(1)")
assert db.execute("SELECT COUNT(*) FROM transactions t WHERE t.id=1 AND "+filter_sql).fetchone()[0]==0
db.execute("DELETE FROM transaction_splits WHERE transaction_id=1")
assert db.execute("SELECT COUNT(*) FROM transactions t WHERE t.id=1 AND "+filter_sql).fetchone()[0]==1
print("SimpleManualEditContract PASS: only safe manual rows, atomic balance guard, stale edit check, exact amount UI")
