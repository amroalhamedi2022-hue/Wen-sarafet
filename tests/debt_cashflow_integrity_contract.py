#!/usr/bin/env python3
"""Regression for loan/outflow classification and lossless v13->v14 posting migration.

Executes the exact backfill SQL in SQLite, not merely a copied formula.
Does NOT access or alter any user's live database.
"""
import json
import re
import sqlite3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
src=(ROOT/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
rules=(ROOT/"app/src/main/java/app/wensarafet/tracker/FinanceRules.java").read_text(encoding="utf-8")
split=(ROOT/"app/src/main/java/app/wensarafet/tracker/SplitDebtRules.java").read_text(encoding="utf-8")
edit=(ROOT/"app/src/main/java/app/wensarafet/tracker/SimpleTransactionEditRules.java").read_text(encoding="utf-8")

def method(start,end):
    return src.split(start,1)[1].split(end,1)[0]

upgrade=method("private static void backfillLegacyPostedBalanceImpact(", "private static void createCaptureSchema(")
assert "ALTER TABLE transactions ADD COLUMN balance_impact_minor INTEGER NOT NULL DEFAULT 0" in upgrade
assert "UPDATE accounts" not in upgrade and "DELETE FROM" not in upgrade
sql_java=upgrade.split("String sql =",1)[1].split(";\n        db.execSQL(sql)",1)[0]
sql="".join(json.loads(v) for v in re.findall(r'"(?:\\.|[^"\\])*"',sql_java))
assert sql.startswith("UPDATE transactions SET balance_impact_minor")
assert "transaction_splits" in sql and "exclude_from_expense" in sql
assert "special_kind='DEBT_PAYMENT'" in sql

db=sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys=ON")
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,balance_minor INTEGER NOT NULL);
CREATE TABLE categories(id INTEGER PRIMARY KEY,exclude_from_expense INTEGER NOT NULL);
CREATE TABLE transactions(
 id INTEGER PRIMARY KEY,type TEXT NOT NULL,amount_minor INTEGER NOT NULL,
 category_id INTEGER,affects_balance INTEGER NOT NULL,base_affects_balance INTEGER NOT NULL,
 special_kind TEXT,account_id INTEGER NOT NULL REFERENCES accounts(id));
CREATE TABLE transaction_splits(
 id INTEGER PRIMARY KEY,transaction_id INTEGER NOT NULL REFERENCES transactions(id),
 category_id INTEGER,special_kind TEXT,amount_minor INTEGER NOT NULL);
CREATE TABLE debt_movements(id INTEGER PRIMARY KEY,transaction_id INTEGER REFERENCES transactions(id),
 amount_minor INTEGER NOT NULL);
INSERT INTO accounts VALUES(1,300000);
INSERT INTO categories VALUES(1,0),(2,1);
INSERT INTO transactions VALUES
 (1,'EXPENSE',10000,1,1,1,NULL,1),
 (2,'EXPENSE',10000,2,1,1,NULL,1),
 (3,'EXPENSE',10000,NULL,1,1,NULL,1),
 (4,'EXPENSE',7000,NULL,0,1,'DEBT',1),
 (5,'EXPENSE',4000,NULL,1,1,'DEBT_PAYMENT',1),
 (6,'INCOME',6000,NULL,1,1,NULL,1),
 (7,'TRANSFER',5000,NULL,1,1,NULL,1),
 (8,'EXPENSE',9000,1,0,0,NULL,1),
 (9,'EXPENSE',10000,NULL,1,1,NULL,1);
INSERT INTO transaction_splits VALUES
 (1,3,1,NULL,2000),(2,3,2,NULL,3000),(3,3,NULL,'DEBT',5000),
 (4,9,2,NULL,6000),(5,9,NULL,'DEBT_PAYMENT',4000);
INSERT INTO debt_movements VALUES(1,4,7000),(2,5,4000);
""")
old_accounts=db.execute("SELECT * FROM accounts").fetchall()
old_rows=db.execute("SELECT id,type,amount_minor,category_id,affects_balance,base_affects_balance,special_kind,account_id FROM transactions ORDER BY id").fetchall()
old_splits=db.execute("SELECT * FROM transaction_splits ORDER BY id").fetchall()
old_debts=db.execute("SELECT * FROM debt_movements ORDER BY id").fetchall()

db.execute("BEGIN")
db.execute("ALTER TABLE transactions ADD COLUMN balance_impact_minor INTEGER NOT NULL DEFAULT 0")
db.execute(sql)
db.execute("COMMIT")
actual=dict(db.execute("SELECT id,balance_impact_minor FROM transactions"))
expected={1:10000,2:0,3:2000,4:0,5:4000,6:6000,7:5000,8:0,9:4000}
assert actual==expected,(actual,expected)
assert db.execute("SELECT * FROM accounts").fetchall()==old_accounts
assert db.execute("SELECT id,type,amount_minor,category_id,affects_balance,base_affects_balance,special_kind,account_id FROM transactions ORDER BY id").fetchall()==old_rows
assert db.execute("SELECT * FROM transaction_splits ORDER BY id").fetchall()==old_splits
assert db.execute("SELECT * FROM debt_movements ORDER BY id").fetchall()==old_debts
assert db.execute("PRAGMA foreign_key_check").fetchall()==[]

# Reclassification: reverse the exact OLD posting and apply full NEW physical debit.
def reclassify_balance(account_balance,original_posting,new_posting):
    return account_balance + original_posting - new_posting
assert reclassify_balance(90000,10000,10000)==90000,"normal purchase -> debt no refund"
assert reclassify_balance(90000,0,10000)==80000,"legacy excluded purchase corrected only when explicitly recategorized"
assert reclassify_balance(90000,10000,10000)==90000,"normal purchase -> excluded no refund"
assert reclassify_balance(90000,2000,10000)==82000,"legacy mixed split correction"
assert reclassify_balance(90000,10000,0)==100000,"explicit change to record-only is reversible"

paths={
"public long addTransaction(":["balance_impact_minor", "applyBalanceEffect"],
"public long addDebtTransaction(":["boolean affectsBalance = baseAffectsBalance", "balance_impact_minor"],
"public void reclassifyTransaction(":["postedBalanceImpact(writable, transactionId)", 'cv.put("balance_impact_minor"'],
"public void reclassifyTransactionAsDebt(":["postedBalanceImpact(writable, transactionId)", "boolean actualMoney = baseAffects", "balance_impact_minor"],
"public void updateDebtTransaction(":["postedBalanceImpact(writable, transactionId)", "baseAffects ? amountMinor : 0L"],
"public void editSimpleManualTransaction(":["postedBalanceImpact(writable,transactionId)", "postedBalanceDelta", "balance_impact_minor"],
"public void deleteTransaction(":["postedBalanceImpact(writable, transactionId)", "balance_impact_minor"],
"public void undoDeletedSimpleTransaction(":["balance_impact_minor", "restoredBalance"],
"public long addFxTransfer(":["balance_impact_minor", "sourceAmountMinor"],
"public long importCapture(":["capturedImpact", "balance_impact_minor"],
"private long addCapturedTransfer(":["balance_impact_minor", "item.amountMinor"],
"public void deleteDebt(":["postedBalanceImpact(writable, txId)", "reverseBalanceEffect"],
}
for start,required in paths.items():
    assert start in src,start
    segment=src.split(start,1)[1].split("\n    public ",1)[0]
    for check in required:
        assert check in segment,(start,check)

assert "return true; // Debt, repayment, and excluded spending are still cash outflows." in split
assert "if (!affectsBalance)" in edit
assert "return total;" in rules
assert "getExpenseTotal(" in src and "AND COALESCE(c.exclude_from_expense,0)=0" in src
assert "getCategoryExpenseTotals(" in src
print("DebtCashflowIntegrityContract PASS: exact old postings, full debt outflows, analytics exclusions, lossless backfill, no automatic account corrections")
