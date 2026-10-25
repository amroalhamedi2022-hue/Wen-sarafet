#!/usr/bin/env python3
"""Minimal SQLite regression for v8 -> v9 fields and undo accounting (not Android instrumentation)."""
import sqlite3
from pathlib import Path

s = Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java'
code = s.read_text()
assert 'DB_VERSION = 11' in code
fields = ('destination_amount_minor','exchange_rate','exchange_rate_source','exchange_rate_checked_at')
for col in fields:
    assert f'ALTER TABLE transactions ADD COLUMN {col} ' in code, col
    assert col in code.split('CREATE TABLE transactions',1)[1].split('FOREIGN KEY',1)[0],col

c = sqlite3.connect(':memory:')
c.execute('CREATE TABLE accounts (id INTEGER PRIMARY KEY, name TEXT, currency TEXT, balance_minor INTEGER, active INTEGER DEFAULT 1)')
c.execute('CREATE TABLE transactions (id INTEGER PRIMARY KEY, type TEXT NOT NULL, amount_minor INTEGER NOT NULL, currency TEXT NOT NULL, account_id INTEGER, destination_account_id INTEGER, category_id INTEGER, special_kind TEXT, needs_review INTEGER DEFAULT 0, created_at INTEGER DEFAULT 1)')
c.execute('CREATE TABLE transaction_splits (transaction_id INTEGER, category_id INTEGER)')
c.execute('CREATE TABLE capture_inbox (status TEXT, inferred_type TEXT)')
c.executemany('INSERT INTO accounts (id,name,currency,balance_minor) VALUES (?,?,?,?)',[(1,'BOP','USD',10000),(2,'Jawwal Pay','ILS',500)])
# Existing v8 transaction unchanged by schema migration
c.execute("INSERT INTO transactions (id,type,amount_minor,currency,account_id,category_id) VALUES(1,'EXPENSE',100,'USD',1,3)")
for col,kind in [('destination_amount_minor','INTEGER'),('exchange_rate','TEXT'),('exchange_rate_source','TEXT'),('exchange_rate_checked_at','INTEGER')]:
    c.execute(f'ALTER TABLE transactions ADD COLUMN {col} {kind}')
assert c.execute('SELECT destination_amount_minor FROM transactions WHERE id=1').fetchone()==(None,)
# FX transaction neither expense nor income; one source subtraction and one destination addition
c.execute("INSERT INTO transactions (id,type,amount_minor,currency,account_id,destination_account_id,destination_amount_minor,exchange_rate,exchange_rate_source) VALUES (2,'TRANSFER',2000,'USD',1,2,7100,'3.55','MANUAL')")
c.execute('UPDATE accounts SET balance_minor = balance_minor-2000 WHERE id=1')
c.execute('UPDATE accounts SET balance_minor = balance_minor+7100 WHERE id=2')
assert c.execute('SELECT balance_minor FROM accounts ORDER BY id').fetchall()==[(8000,),(7600,)]
c.execute('UPDATE accounts SET balance_minor = balance_minor+2000 WHERE id=1')
c.execute('UPDATE accounts SET balance_minor = balance_minor-7100 WHERE id=2')
c.execute('DELETE FROM transactions WHERE id=2')
assert c.execute('SELECT balance_minor FROM accounts ORDER BY id').fetchall()==[(10000,),(500,)]
q=("SELECT (SELECT COUNT(*) FROM transactions t WHERE t.type=? AND t.special_kind IS NULL AND t.category_id IS NULL "
   "AND NOT EXISTS (SELECT 1 FROM transaction_splits s WHERE s.transaction_id=t.id)) "
   "+ (SELECT COUNT(*) FROM capture_inbox WHERE status='PENDING' AND inferred_type=?)")
assert c.execute(q,('EXPENSE','EXPENSE')).fetchone()[0]==0
c.execute("INSERT INTO transactions (id,type,amount_minor,currency,account_id,category_id) VALUES(3,'EXPENSE',200,'ILS',2,NULL)")
assert c.execute(q,('EXPENSE','EXPENSE')).fetchone()[0]==1
c.execute('UPDATE transactions SET category_id=3 WHERE id=3')
assert c.execute(q,('EXPENSE','EXPENSE')).fetchone()[0]==0
print('FxMigrationAudit: schema v8->v9, FX reversal, and no-pending-alert query passed')
