#!/usr/bin/env python3
"""Debt Safe Zone: actual join SQL + no-overdraft and atomic-deletion source guards.

These are SQLite/source contract tests, not a device/UI integration test.
"""
import json
import re
import sqlite3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
src=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
model=(root/"app/src/main/java/app/wensarafet/tracker/Models.java").read_text(encoding="utf-8")

def literal_sql(text):
    return "".join(json.loads(v) for v in re.findall(r'"(?:\\.|[^"\\])*"', text))

ledger=src.split("public List<Models.DebtMovement> getDebtMovements(long debtId)",1)[1].split(
    "public int countDebtLinkedFinancialTransactions(",1)[0]
sql=literal_sql(re.search(r'String sql=(.*?);\s*try', ledger, re.S).group(1))
assert "LEFT JOIN transactions t ON t.id=m.transaction_id" in sql
assert "transaction_splits s WHERE s.transaction_id=m.transaction_id" in sql
assert "ORDER BY m.created_at DESC,m.id DESC" in sql
assert "item.fromSplitTransaction=item.transactionId!=null && item.linkedSplitPartCount>0" in ledger
assert "item.linkedTransactionNote" in ledger
assert "item.linkedTransactionAmountMinor" in ledger

lookup=src.split("public Models.Transaction getTransactionById(long transactionId)",1)[1].split(
    "public List<Models.Transaction> getExpenseTransactions(",1)[0]
transaction_sql=literal_sql(re.search(r'String sql=(.*?);\s*try',lookup,re.S).group(1))
assert transaction_sql.endswith("WHERE t.id=?")
assert "EXISTS(SELECT 1 FROM transaction_splits" in transaction_sql
assert "populateSpecial(tx)" in lookup

deletion=src.split("public void deleteDebt(long debtId) {",1)[1].split(
    "private void ensureDebtTotalsValid(",1)[0]
assert deletion.index("writable.beginTransaction();") < deletion.index(
    "SELECT DISTINCT transaction_id FROM debt_movements")
assert "getReadableDatabase().rawQuery(" not in deletion
assert 'SELECT 1 FROM debts WHERE id=?' in deletion
assert 'SELECT 1 FROM transaction_splits WHERE transaction_id=? LIMIT 1' in deletion
assert 'SELECT COUNT(DISTINCT debt_id) FROM debt_movements WHERE transaction_id=?' in deletion
assert 'if(writable.delete("transactions", "id=?"' in deletion
assert 'if(writable.delete("debts", "id=?"' in deletion
assert 'writable.setTransactionSuccessful();' in deletion
manual_delete=src.split("public void deleteManualDebtMovement(",1)[1].split(
    "public void deleteDebt(",1)[0]
assert 'if (!c.isNull(1)) throw new IllegalArgumentException(' in manual_delete
assert 'id=? AND transaction_id IS NULL' in manual_delete
assert 'ensureDebtTotalsValid(writable, debtId); recomputeDebt(writable, debtId);' in manual_delete

for text in (
    "NonNegativeBalanceRules.afterDelta(current, delta)",
    "NonNegativeBalanceRules.requireNonNegative(newBalances[0])",
    "NonNegativeBalanceRules.requireNonNegative(nextBalance)",
    "NonNegativeBalanceRules.requireNonNegative(balances[0])",
    "NonNegativeBalanceRules.requireNonNegative(restoredBalance)",
    "NonNegativeBalanceRules.afterDelta(destinationBalance,-fxDestinationMinor)",
    "NonNegativeBalanceRules.requireNonNegative(balanceMinor)",
):
    assert text in src, text
assert "Math.addExact(current, delta)" not in src.split(
    "private void adjustBalance(",1)[1], "central overdraft guard absent"
for text in ("fromSplitTransaction","linkedSplitPartCount",
             "linkedTransactionAmountMinor","linkedTransactionNote"):
    assert text in model, text
for text in ("جزء من عملية مالية مقسمة","db.getTransactionById(movement.transactionId)",
             "تم حذف الحركة وتحديث رصيد الدين فورًا","countDebtLinkedFinancialTransactions(debt.id)",
             "تأكيد حذف سجل الدين نهائيًا", "showDebtPersonDetails(debt.personId,debt.personName,debt.direction)"):
    assert text in ui, text

db=sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys=ON")
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,name TEXT);
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,emoji TEXT);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,currency TEXT,
 account_id INTEGER,destination_account_id INTEGER,category_id INTEGER,note TEXT,raw_text TEXT,
 needs_review INTEGER,created_at INTEGER,affects_balance INTEGER,
 FOREIGN KEY(account_id) REFERENCES accounts(id));
CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY,transaction_id INTEGER,amount_minor INTEGER,
 FOREIGN KEY(transaction_id) REFERENCES transactions(id));
CREATE TABLE debts(id INTEGER PRIMARY KEY,person_id INTEGER);
CREATE TABLE debt_movements(id INTEGER PRIMARY KEY,debt_id INTEGER,transaction_id INTEGER,
 kind TEXT,amount_minor INTEGER,created_at INTEGER,note TEXT,
 FOREIGN KEY(debt_id) REFERENCES debts(id),
 FOREIGN KEY(transaction_id) REFERENCES transactions(id));
INSERT INTO accounts VALUES (1,'نقدي');
INSERT INTO categories VALUES (7,'طعام','🍽️');
INSERT INTO debts VALUES (50,8);
INSERT INTO transactions VALUES
 (100,'EXPENSE',2000,'ILS',1,NULL,7,'مطعم وتقسيم','',0,1000,1),
 (101,'INCOME',400,'ILS',1,NULL,NULL,'سداد من الحساب','',0,2000,1);
INSERT INTO transaction_splits VALUES (1,100,600),(2,100,1400);
INSERT INTO debt_movements VALUES
 (1,50,100,'OPEN',600,1010,'جزء من عملية مقسمة'),
 (2,50,101,'PAYMENT',400,2000,'سداد'),
 (3,50,NULL,'MANUAL_PAYMENT',100,3000,'دفعة يدوية');
""")
rows=db.execute(sql,(50,)).fetchall()
assert len(rows)==3,rows
m={r[0]:r for r in rows}
assert m[1][2]==100 and m[1][7]==2 and m[1][8]==2000 and m[1][9]=="مطعم وتقسيم",m[1]
assert m[2][7]==0 and m[2][8]==400
assert m[3][2] is None and m[3][7]==0 and m[3][8] is None
original=db.execute(transaction_sql,(100,)).fetchone()
assert original[0]==100 and original[2]==2000 and original[14]==1
assert db.execute(transaction_sql,(9999,)).fetchone() is None

# Verify a linked financial movement cannot be independently deleted without
# transaction_id IS NULL; manual row can. No original transaction is touched.
assert db.execute("DELETE FROM debt_movements WHERE id=1 AND transaction_id IS NULL").rowcount==0
assert db.execute("DELETE FROM debt_movements WHERE id=3 AND transaction_id IS NULL").rowcount==1
assert db.execute("SELECT COUNT(*) FROM transactions").fetchone()[0]==2
assert db.execute("PRAGMA foreign_key_check").fetchall()==[]
print("DebtSafeZoneContract PASS: split provenance, manual-only deletion, atomic-preflight and all balance guards")
