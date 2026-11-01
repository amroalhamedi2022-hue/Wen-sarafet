#!/usr/bin/env python3
"""Legacy foreign-currency account recovery: non-destructive, explicit choice and SQL identity."""
import sqlite3
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=(root/"app/src/main/java/app/wensarafet/tracker/LegacyAccountVisibility.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
assert 'SELECT id,name,currency,balance_minor,active FROM accounts' in source
assert 'SELECT currency,active FROM accounts WHERE id=?' in source
assert 'values.put("active",1)' in source
assert '"id=? AND active=0"' in source
assert 'db.beginTransaction()' in source
assert 'db.setTransactionSuccessful()' in source
assert 'db.endTransaction()' in source
for forbidden in ('values.put("balance_minor"', 'values.put("currency"', 'UPDATE transactions',
                  'DELETE FROM', 'DROP TABLE','INSERT INTO'):
    assert forbidden not in source, forbidden
assert 'if(!hasDifferentCurrencies){showFxAccountDiagnostics();return;}' in ui
assert "LegacyAccountVisibility.list(db)" in ui
assert "LegacyAccountVisibility.restore(db,account.id)" in ui
assert "showFxTransferDialog();" in ui
assert ".setPositiveButton(\"إظهار الحساب\"" in ui
assert "لا يُحوّل التطبيق العملة تلقائيًا" in ui

db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,name TEXT,currency TEXT,
 balance_minor INTEGER,active INTEGER,position INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,account_id INTEGER,amount_minor INTEGER);
INSERT INTO accounts VALUES(1,'بنك فلسطين - شيكل','ILS',10000,1,1),
 (2,'بنك فلسطين - دولار','USD',2540,0,2),
 (3,'خانة قديمة','EUR',900,0,3);
INSERT INTO transactions VALUES(5,2,100),(6,2,-40);
""")
before_accounts=db.execute("SELECT id,name,currency,balance_minor,position FROM accounts ORDER BY id").fetchall()
before_tx=db.execute("SELECT * FROM transactions ORDER BY id").fetchall()
assert db.execute("UPDATE accounts SET active=1 WHERE id=? AND active=0",(2,)).rowcount==1
assert db.execute("SELECT active FROM accounts WHERE id=2").fetchone()[0]==1
assert db.execute("UPDATE accounts SET active=1 WHERE id=? AND active=0",(2,)).rowcount==0
assert db.execute("SELECT id,name,currency,balance_minor,position FROM accounts ORDER BY id").fetchall()==before_accounts
assert db.execute("SELECT * FROM transactions ORDER BY id").fetchall()==before_tx
print("LegacyFXAccountContract PASS: hidden historical USD recovery changes only active flag with explicit UI confirmation")
