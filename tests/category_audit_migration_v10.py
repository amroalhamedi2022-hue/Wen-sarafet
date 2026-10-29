#!/usr/bin/env python3
"""Execute actual createAuditSchema SQLite DDL extracted from Java and verify no old rows lost."""
import json, re, sqlite3
from pathlib import Path
src=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java').read_text()
assert 'DB_VERSION = 13' in src
segment=src.split('private static void createAuditSchema(SQLiteDatabase db) {',1)[1].split('    private static void recreateAuditTriggers',1)[0]
sql=[]
for match in re.finditer(r'db\.execSQL\((.*?)\);\s*\n',segment,re.S):
    terms=re.findall(r'"(?:\\.|[^"\\])*"',match.group(1))
    sql.append(''.join(json.loads(term) for term in terms))
assert len(sql)==13, (len(sql),sql)
c=sqlite3.connect(':memory:')
c.execute('PRAGMA foreign_keys=ON')
c.executescript('''
CREATE TABLE accounts(id INTEGER PRIMARY KEY, balance_minor INTEGER);
CREATE TABLE categories(id INTEGER PRIMARY KEY, name TEXT,kind TEXT,emoji TEXT,position INTEGER,exclude_from_expense INTEGER DEFAULT 0);
CREATE TABLE transactions(id INTEGER PRIMARY KEY, type TEXT NOT NULL, amount_minor INTEGER NOT NULL,currency TEXT NOT NULL,account_id INTEGER NOT NULL,destination_account_id INTEGER,category_id INTEGER,note TEXT NOT NULL DEFAULT '',affects_balance INTEGER NOT NULL DEFAULT 1,base_affects_balance INTEGER NOT NULL DEFAULT 1,special_kind TEXT,debt_person_id INTEGER,debt_direction TEXT,destination_amount_minor INTEGER,exchange_rate TEXT,exchange_rate_source TEXT,exchange_rate_checked_at INTEGER, FOREIGN KEY(category_id) REFERENCES categories(id));
CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY,transaction_id INTEGER NOT NULL,category_id INTEGER,amount_minor INTEGER NOT NULL, FOREIGN KEY(category_id) REFERENCES categories(id));
CREATE TABLE debt_movements(id INTEGER PRIMARY KEY,debt_id INTEGER NOT NULL,transaction_id INTEGER,kind TEXT NOT NULL,amount_minor INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '');
INSERT INTO accounts VALUES(1,10000);
INSERT INTO categories VALUES(12,'مواصلات','EXPENSE','🚕',1,0);
INSERT INTO transactions(id,type,amount_minor,currency,account_id,category_id) VALUES(41,'EXPENSE',200,'ILS',1,12);
''')
# Apply the same v10 migration that app applies, then actual Java DDL.
c.execute('ALTER TABLE categories ADD COLUMN archived INTEGER NOT NULL DEFAULT 0')
for q in sql:c.execute(q)
assert c.execute('SELECT COUNT(*) FROM change_history').fetchone()[0]==0, 'migration must not generate fake historical edits'
assert c.execute('SELECT amount_minor FROM transactions WHERE id=41').fetchone()[0]==200
c.execute("UPDATE categories SET archived=1 WHERE id=12")
assert c.execute('SELECT category_id FROM transactions WHERE id=41').fetchone()[0]==12
assert c.execute('SELECT archived FROM categories WHERE id=12').fetchone()[0]==1
try:
 c.execute('DELETE FROM categories WHERE id=12')
 raise AssertionError('referenced category deletion accepted')
except sqlite3.IntegrityError:pass
c.execute('UPDATE transactions SET category_id=NULL WHERE id=41')
c.execute('INSERT INTO transaction_splits VALUES(1,41,12,200)')
c.execute('DELETE FROM transaction_splits WHERE id=1')
c.execute('DELETE FROM transactions WHERE id=41')
logs=c.execute('SELECT action FROM change_history ORDER BY id').fetchall()
assert ('EDIT',) in logs and ('DELETE',) in logs and ('SPLIT_ADD',) in logs and ('SPLIT_REMOVE',) in logs and ('CATEGORY_EDIT',) in logs,logs
c.execute("INSERT INTO debt_movements VALUES(8,3,NULL,'OPEN',500,'manual')")
c.execute("UPDATE debt_movements SET amount_minor=450 WHERE id=8")
c.execute("DELETE FROM debt_movements WHERE id=8")
debt_logs=c.execute("SELECT action FROM change_history WHERE table_name='debt_movements' ORDER BY id").fetchall()
assert debt_logs==[('DEBT_ADD',),('DEBT_EDIT',),('DEBT_DELETE',)],debt_logs
c.execute('INSERT INTO categories(id,name,kind,emoji,position,exclude_from_expense,archived) VALUES(15,\'Unused\',\'EXPENSE\',\'🏷️\',2,0,0)')
c.execute('DELETE FROM categories WHERE id=15')
assert c.execute("SELECT COUNT(*) FROM change_history WHERE action='CATEGORY_DELETE'").fetchone()[0]==1
assert c.execute("SELECT COUNT(*) FROM change_history WHERE action='CATEGORY_ADD'").fetchone()[0]==1
before_reinstall=c.execute('SELECT id,table_name,record_id,action FROM change_history ORDER BY id').fetchall()
trigger_names=[row[0] for row in c.execute("SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'audit_%'")]
assert len(trigger_names)==11,trigger_names
for name in trigger_names:c.execute('DROP TRIGGER IF EXISTS '+name)
for q in sql:c.execute(q)
assert c.execute('SELECT id,table_name,record_id,action FROM change_history ORDER BY id').fetchall()==before_reinstall
assert len([row[0] for row in c.execute("SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'audit_%'")])==11
assert 'if (oldVersion < 13)' in src and 'recreateAuditTriggers(db);' in src
print('AuditMigrationV13: historical preservation plus transaction, split, category and debt triggers passed')
