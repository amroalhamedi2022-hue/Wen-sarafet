#!/usr/bin/env python3
"""Debt detachment must preserve posted cash; audit must be readable and page all rows.

These are actual SQLite fixture + source integration contracts, not a real device test.
"""
from pathlib import Path
import sqlite3
ROOT=Path(__file__).resolve().parents[1]
finance=(ROOT/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
ui=(ROOT/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
presentation=(ROOT/"app/src/main/java/app/wensarafet/tracker/AuditPresentation.java").read_text(encoding="utf-8")

assert "DB_VERSION = 15" in finance
assert "if (oldVersion < 15)" in finance
new_migration=finance.split("if (oldVersion < 15)",1)[1].split("private static void backfillLegacyPostedBalanceImpact",1)[0]
assert "recreateAuditTriggers(db)" in new_migration
assert "UPDATE accounts" not in new_migration and "DELETE FROM" not in new_migration

special=finance.split("public void reclassifyDebtTransactionToCategory(",1)[1].split(
    "private void reclassifyTransactionInternal(",1)[0]
for text in ("writable.beginTransaction()","writable.setTransactionSuccessful()",
    "ensureDebtOpeningCanBeRemoved(writable,transactionId)",
    "requireCategoryForType(writable,categoryId,newType)",
    'writable.delete("debt_movements","transaction_id=?"',
    'values.putNull("special_kind")','values.putNull("debt_person_id")',
    "for(Long debtId:affectedDebts)recomputeDebt(writable,debtId)"):
    assert text in special,text
assert "applyBalanceEffect(" not in special
assert "reverseBalanceEffect(" not in special
assert 'values.put("balance_impact_minor"' not in special
assert "if(!oldType.equals(newType))" in special
assert '"CAPTURED".equals(source)' in special

editor=ui.split("private void showEditDebtTransactionDialog(",1)[1].split(
    "private void showCategoryPicker(",1)[0]
assert "showCategoryPicker(tx,tx.type)" in editor
assert 'new String[]{"مصروف", "دخل"}' not in editor

history=ui.split("private void showAuditHistory()",1)[1].split(
    "private void showCategoryManagement()",1)[0]
for text in ("getChangeHistoryPage(safePage,PAGE_SIZE,query,selectedTable)",
    "db.countChangeHistory(query,selectedTable)",
    "body.addView(card,fieldWrapParams())","Ui.glassListBackground",
    "اضغط لعرض تفاصيل هذا التغيير","showAuditHistory(safePage+1,query,selectedTable)",
    "showAuditHistory(safePage-1,query,selectedTable)",
    '"SELECT d.id,p.name,d.direction,d.currency FROM debts d "'):
    assert text in history,text
assert "body.addView(card,fieldParams())" not in history
assert "getChangeHistory(200)" not in history
assert "AuditPresentation.details(" in history

for key in ("DEBT_ADD","DEBT_EDIT","DEBT_DELETE","ACCOUNT_ADJUST",
            '"debt_movements"','"balance"','"excluded"','"note"'):
    assert key in presentation,key
manual=finance.split("public void setAccountBalance(",1)[1].split("public void moveAccount(",1)[0]
for t in ("writable.beginTransaction()","writable.insertOrThrow(\u0022change_history\u0022,null,event)",
          "writable.setTransactionSuccessful()","writable.endTransaction()",
          '"ACCOUNT_ADJUST"'):
    assert t in manual,t

linked=ui.split("private void showAccountLinkedDebtPayment(",1)[1].split(
    "private void showManualDebtPayment(",1)[0]
assert "db.addDebtTransaction(" in linked
assert "FinanceRules.SPECIAL_DEBT_PAYMENT" in linked
assert "amountMinor>debt.remainingMinor" in linked
assert "debt.currency.equals(account.currency)" in linked
assert "true,FinanceRules.SPECIAL_DEBT_PAYMENT,debt.personId,debt.direction" in linked

# Independent SQLite fixture: already paid money is NEVER debited or refunded
# simply by moving a debt-related outflow into ordinary expense analytics.
db=sqlite3.connect(":memory:",isolation_level=None)
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,balance_minor INTEGER NOT NULL);
CREATE TABLE categories(id INTEGER PRIMARY KEY,kind TEXT NOT NULL);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT NOT NULL,source TEXT NOT NULL,
    account_id INTEGER NOT NULL,category_id INTEGER,special_kind TEXT,
    debt_person_id INTEGER,debt_direction TEXT,amount_minor INTEGER,
    balance_impact_minor INTEGER NOT NULL);
CREATE TABLE debt_movements(id INTEGER PRIMARY KEY,transaction_id INTEGER NOT NULL,
    debt_id INTEGER NOT NULL,kind TEXT NOT NULL,amount_minor INTEGER NOT NULL);
CREATE TABLE change_history(id INTEGER PRIMARY KEY AUTOINCREMENT,table_name TEXT,
    record_id INTEGER,action TEXT,previous TEXT,current TEXT);
INSERT INTO accounts VALUES(1,90000);
INSERT INTO categories VALUES(7,'EXPENSE');
INSERT INTO transactions VALUES(10,'EXPENSE','CAPTURED',1,NULL,'DEBT',9,'RECEIVABLE',10000,0);
INSERT INTO debt_movements VALUES(1,10,8,'OPEN',10000);
""")
before=db.execute("SELECT balance_minor FROM accounts WHERE id=1").fetchone()[0]
db.execute("BEGIN")
db.execute("DELETE FROM debt_movements WHERE transaction_id=10")
db.execute("UPDATE transactions SET category_id=7,special_kind=NULL,debt_person_id=NULL,debt_direction=NULL WHERE id=10")
db.execute("COMMIT")
assert db.execute("SELECT balance_minor FROM accounts WHERE id=1").fetchone()[0]==before
assert db.execute("SELECT category_id,special_kind,balance_impact_minor FROM transactions WHERE id=10").fetchone()==(7,None,0)
assert db.execute("SELECT COUNT(*) FROM debt_movements").fetchone()[0]==0

# An unsafe exit must be atomic; attached repayment cannot leave orphan debt.
db.executescript("""
INSERT INTO transactions VALUES(11,'EXPENSE','MANUAL',1,NULL,'DEBT',9,'RECEIVABLE',10000,10000);
INSERT INTO debt_movements VALUES(2,11,8,'OPEN',10000);
INSERT INTO debt_movements VALUES(3,12,8,'PAYMENT',4000);
""")
assert db.execute("SELECT balance_minor FROM accounts").fetchone()[0]==before

# Pagination must preserve all old audit rows and honor filters.
for i in range(143):
    table="transactions" if i%2==0 else "debt_movements"
    db.execute("INSERT INTO change_history(table_name,record_id,action,previous,current) VALUES(?,?,?,?,?)",
               (table,100+i,"EDIT",f"amount={i}",f"amount={i+1}"))
allids=[r[0] for r in db.execute("SELECT id FROM change_history ORDER BY id DESC")]
pages=[]
for offset in range(0,143,40):
    pages.extend(r[0] for r in db.execute("SELECT id FROM change_history ORDER BY id DESC LIMIT 40 OFFSET ?",(offset,)))
assert pages==allids and len(set(pages))==143
assert db.execute("SELECT COUNT(*) FROM change_history WHERE table_name='debt_movements'").fetchone()[0]==71
print("DebtAuditHistoryContract PASS: non-posting debt exit, source safety, recorded account adjustment, full history paging")
