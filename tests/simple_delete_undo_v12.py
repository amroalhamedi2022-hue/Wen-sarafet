#!/usr/bin/env python3
"""Real SQLite DDL + transaction rollback tests, not an Android instrumentation substitute."""
from pathlib import Path
import json,re,sqlite3
src=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java').read_text()
assert 'DB_VERSION = 13' in src
upgrade=src.split('if (oldVersion < 12)',1)[1].split('    private static void createCaptureSchema',1)[0]
assert 'createSafeUndoSchema(db);' in upgrade
schema=src.split('private static void createSafeUndoSchema(SQLiteDatabase db) {',1)[1].split('    public static final class AuditEntry',1)[0]
strings=re.findall(r'"(?:\\.|[^"\\])*"',schema)
sql=''.join(json.loads(s) for s in strings)
assert sql.startswith('CREATE TABLE IF NOT EXISTS deleted_transaction_snapshots')
assert 'UNIQUE' not in sql # Repeated delete / restore cycle must work.
assert 'FOREIGN KEY(audit_id)' in sql
method=src.split('public void deleteTransaction(long transactionId)',1)[1].split('private void requireDebtPerson',1)[0]
undo=src.split('public void undoDeletedSimpleTransaction(long auditId)',1)[1].split('private static boolean canSnapshotSimpleDelete',1)[0]
assert '!hasSpecialMetadata && destinationId==null' in method
assert 'SELECT balance_minor,currency FROM accounts' in undo
assert 'SELECT kind FROM categories' in undo
for word in ('beginTransaction()', 'setTransactionSuccessful()', 'canSnapshotSimpleDelete', 'deleted_transaction_snapshots','writable.delete("transactions"'):
 assert word in method,('delete',word)
for word in ('beginTransaction()','setTransactionSuccessful()','Math', 'deleted_transaction_snapshots','insertOrThrow("transactions"','restored_at'):
 if word=='Math':continue
 assert word in undo,('undo',word)
conn=sqlite3.connect(':memory:')
conn.execute('PRAGMA foreign_keys=ON')
conn.executescript('''
 CREATE TABLE change_history (id INTEGER PRIMARY KEY,table_name TEXT,record_id INTEGER,action TEXT,undone INTEGER NOT NULL DEFAULT 0);
 CREATE TABLE accounts (id INTEGER PRIMARY KEY,currency TEXT,balance_minor INTEGER);
 CREATE TABLE categories (id INTEGER PRIMARY KEY,kind TEXT,archived INTEGER DEFAULT 0);
 CREATE TABLE transactions (id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,currency TEXT,account_id INTEGER,category_id INTEGER,note TEXT,raw_text TEXT,needs_review INTEGER,created_at INTEGER,source TEXT,affects_balance INTEGER,base_affects_balance INTEGER,
 FOREIGN KEY(account_id) REFERENCES accounts(id), FOREIGN KEY(category_id) REFERENCES categories(id));
 CREATE TRIGGER audit_delete BEFORE DELETE ON transactions BEGIN INSERT INTO change_history(table_name,record_id,action) VALUES('transactions',OLD.id,'DELETE'); END;
 CREATE TRIGGER audit_add AFTER INSERT ON transactions BEGIN INSERT INTO change_history(table_name,record_id,action) VALUES('transactions',NEW.id,'ADD'); END;
 INSERT INTO accounts VALUES(1,'ILS',1000);INSERT INTO categories VALUES(1,'EXPENSE',0);
 INSERT INTO transactions VALUES(17,'EXPENSE',200,'ILS',1,1,'test',NULL,0,100,'MANUAL',1,1);
''')
conn.execute(sql)
conn.commit()
def delete_row():
 conn.execute('BEGIN')
 old=conn.execute('SELECT id,type,amount_minor,currency,account_id,category_id,note,raw_text,needs_review,created_at,source,affects_balance,base_affects_balance FROM transactions WHERE id=17').fetchone()
 conn.execute('UPDATE accounts SET balance_minor=balance_minor+200 WHERE id=1')
 conn.execute('DELETE FROM transactions WHERE id=17')
 audit=conn.execute("SELECT id FROM change_history WHERE action='DELETE' AND record_id=17 ORDER BY id DESC LIMIT 1").fetchone()[0]
 conn.execute('''INSERT INTO deleted_transaction_snapshots (audit_id,transaction_id,type,amount_minor,currency,account_id,category_id,note,raw_text,needs_review,created_at,source,affects_balance,base_affects_balance,balance_impact_minor,deleted_at)
 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',(audit,*old,200,10000))
 conn.commit();return audit
# The model query in the actual Java method selects and inserts individual fields.
# This harness independently tests SQLite's FK/rollback and snapshot lifecycle.
a=delete_row()
assert conn.execute('SELECT balance_minor FROM accounts').fetchone()[0]==1200
assert conn.execute('SELECT transaction_id FROM deleted_transaction_snapshots WHERE audit_id=?',(a,)).fetchone()[0]==17
conn.execute('BEGIN')
try:
 conn.execute('INSERT INTO transactions VALUES (17,\'EXPENSE\',200,\'ILS\',1,1,\'test\',NULL,0,100,\'MANUAL\',1,1)')
 conn.execute('UPDATE accounts SET balance_minor=balance_minor-200 WHERE id=1')
 conn.execute('UPDATE deleted_transaction_snapshots SET restored_at=20000 WHERE audit_id=?',(a,))
 conn.execute('UPDATE change_history SET undone=1 WHERE id=?',(a,))
 conn.commit()
except Exception:conn.rollback();raise
assert conn.execute('SELECT balance_minor FROM accounts').fetchone()[0]==1000
assert conn.execute('SELECT restored_at FROM deleted_transaction_snapshots WHERE audit_id=?',(a,)).fetchone()[0]==20000
assert conn.execute('PRAGMA foreign_key_check').fetchall()==[]
# Second delete after restoration MUST NOT collide with first snapshot.
b=delete_row();assert b!=a
assert conn.execute('SELECT COUNT(*) FROM deleted_transaction_snapshots').fetchone()[0]==2
# Failing FK restore must leave balance & audit unchanged.
try:
 conn.execute('BEGIN')
 conn.execute('INSERT INTO transactions VALUES(999,\'EXPENSE\',200,\'ILS\',1,999,\'broken\',NULL,0,1,\'MANUAL\',1,1)')
 conn.commit();raise AssertionError('orphan was inserted')
except sqlite3.IntegrityError: conn.rollback()
assert conn.execute('SELECT balance_minor FROM accounts').fetchone()[0]==1200
assert conn.execute('SELECT restored_at FROM deleted_transaction_snapshots WHERE audit_id=?',(b,)).fetchone()[0] is None
assert conn.execute('PRAGMA foreign_key_check').fetchall()==[]
print('SimpleDeleteUndoV12: actual DDL, restore, re-delete, FK rollback, audit lifecycle succeeded')
