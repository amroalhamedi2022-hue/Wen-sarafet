#!/usr/bin/env python3
"""Regression for the v12 5-column cursor crash, and SQLite-safe category Undo.

Runs source projection/guard queries against real SQLite. It is deliberately not an
Android instrumentation test; it checks the exact SQL embedded in FinanceDbHelper.
"""
from pathlib import Path
import json
import re
import sqlite3

SRC = (Path(__file__).resolve().parents[1] / 'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java').read_text()

def tokens(s):
    return [json.loads(t) for t in re.findall(r'"(?:\\.|[^"\\])*"', s)]

method = SRC.split('public void deleteTransaction(long transactionId) {',1)[1].split('private void requireDebtPerson',1)[0]
projection = re.search(r'try \(Cursor c = writable\.rawQuery\((.*?)new String\[\]\{String\.valueOf\(transactionId\)\}\)\) \{', method, re.S)
assert projection is not None, 'Missing delete transaction cursor projection'
query = ''.join(tokens(projection.group(1)))
assert query.count('?') == 1 and query.startswith('SELECT '), query
columns = [x.strip() for x in query.split(' FROM ')[0].removeprefix('SELECT ').split(',')]
assert len(columns) == len(set(columns)), columns
select_block = method[projection.end():].split('\n            }', 1)[0]
indices = [int(i) for i in re.findall(r'c\.(?:getLong|getInt|getString|isNull)\((\d+)\)', select_block)]
assert indices and max(indices) < len(columns), f'cursor reads index {max(indices)} from {len(columns)} SQL columns'
assert len(columns) >= 20, 'FX / debt / metadata must be projected for safe eligibility'
for mandatory in ('base_affects_balance','category_id','currency','note','raw_text','source',
                  'needs_review','created_at','special_kind','debt_person_id','debt_direction',
                  'destination_amount_minor','exchange_rate','exchange_rate_source','exchange_rate_checked_at'):
    assert mandatory in columns, mandatory
assert all(f'!c.isNull({i})' in select_block for i in range(13,20)), 'unsafe snapshot FX/debt fields'

conn = sqlite3.connect(':memory:')
conn.execute('''CREATE TABLE transactions(
  id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,account_id INTEGER,
  destination_account_id INTEGER,affects_balance INTEGER,base_affects_balance INTEGER,
  category_id INTEGER,currency TEXT,note TEXT,raw_text TEXT,source TEXT,needs_review INTEGER,
  created_at INTEGER,special_kind TEXT,debt_person_id INTEGER,debt_direction TEXT,
  destination_amount_minor INTEGER,exchange_rate TEXT,exchange_rate_source TEXT,
  exchange_rate_checked_at INTEGER)''')
conn.execute('''INSERT INTO transactions(id,type,amount_minor,account_id,affects_balance,
 base_affects_balance,category_id,currency,note,source,needs_review,created_at)
 VALUES(91,'EXPENSE',200,2,1,1,3,'ILS','test','MANUAL',0,1000)''')
row = conn.execute(query, (91,)).fetchone()
assert len(row) == 20 and row[6] == 3 and row[10] == 'MANUAL'
conn.execute('''UPDATE transactions SET destination_amount_minor=311, exchange_rate='3.11',
 exchange_rate_source='BANK',exchange_rate_checked_at=1234 WHERE id=91''')
row = conn.execute(query,(91,)).fetchone()
assert row[16] == 311 and row[17] == '3.11' and row[18] == 'BANK' and row[19] == 1234

# Extract the one predicate used by both the display query and the action query.
safe_section = SRC.split('private static final String SAFE_CATEGORY_UNDO_SQL =',1)[1].split(';',1)[0]
safe_sql = ''.join(tokens(safe_section))
assert SRC.count('SAFE_CATEGORY_UNDO_SQL +') >= 2, 'display and action must reuse one guard'
assert all(s in safe_sql for s in ("t.source='MANUAL'", 't.needs_review=0',
  't.exchange_rate_checked_at IS NULL', 'debt_movements', 'capture_inbox',
  'transaction_splits', 'oldcat.kind=t.type', 'newcat.kind=t.type'))

hist_section = SRC.split('public List<AuditEntry> getChangeHistory(int limit) {',1)[1].split('public void undoLatestSimpleCategoryChange',1)[0]
marker = 'SAFE_CATEGORY_UNDO_SQL +'
raw_history = hist_section.split('rawQuery(',1)[1].split('new String[]{String.valueOf(Math.min(',1)[0]
history_sql = ''.join(tokens(raw_history.split(marker)[0])) + safe_sql + ''.join(tokens(raw_history.split(marker)[1]))
assert history_sql.count('?') == 1

conn.executescript('''
 CREATE TABLE change_history(id INTEGER PRIMARY KEY,table_name TEXT,record_id INTEGER,
 action TEXT, previous TEXT,current TEXT, created_at INTEGER,undone INTEGER,
 previous_category_id INTEGER,current_category_id INTEGER,previous_type TEXT,current_type TEXT,
 previous_account_id INTEGER,current_account_id INTEGER,previous_amount_minor INTEGER,current_amount_minor INTEGER);
 CREATE TABLE categories(id INTEGER PRIMARY KEY,kind TEXT);
 CREATE TABLE transaction_splits(transaction_id INTEGER);
 CREATE TABLE debt_movements(transaction_id INTEGER);
 CREATE TABLE capture_inbox(transaction_id INTEGER);
 CREATE TABLE deleted_transaction_snapshots(audit_id INTEGER,restored_at INTEGER);
 INSERT INTO categories VALUES(3,'EXPENSE'),(4,'EXPENSE');
 UPDATE transactions SET category_id=4 WHERE id=91;
 UPDATE transactions SET destination_amount_minor=NULL,exchange_rate=NULL,exchange_rate_source=NULL,exchange_rate_checked_at=NULL WHERE id=91;
 INSERT INTO change_history VALUES(1,'transactions',91,'EDIT','before','after',1,0,3,4,'EXPENSE','EXPENSE',2,2,200,200);
''')
def undoable():
    result=conn.execute(history_sql, (30,)).fetchone()
    return result[7]
assert undoable() == 1
cases=[
  ('captured source',"UPDATE transactions SET source='CAPTURED' WHERE id=91","UPDATE transactions SET source='MANUAL' WHERE id=91"),
  ('under review',"UPDATE transactions SET needs_review=1 WHERE id=91","UPDATE transactions SET needs_review=0 WHERE id=91"),
  ('FX quote',"UPDATE transactions SET exchange_rate='3.10' WHERE id=91","UPDATE transactions SET exchange_rate=NULL WHERE id=91"),
  ('debt movement',"INSERT INTO debt_movements VALUES(91)","DELETE FROM debt_movements"),
  ('split',"INSERT INTO transaction_splits VALUES(91)","DELETE FROM transaction_splits"),
  ('capture link',"INSERT INTO capture_inbox VALUES(91)","DELETE FROM capture_inbox"),
  ('wrong old category type',"UPDATE categories SET kind='INCOME' WHERE id=3","UPDATE categories SET kind='EXPENSE' WHERE id=3"),
]
for title,change,reset in cases:
    conn.execute(change)
    assert undoable() == 0, f'unsafe category undo was offered: {title}'
    conn.execute(reset)
    assert undoable() == 1, f'category undo did not recover after {title}'

undo = SRC.split('public void undoLatestSimpleCategoryChange(long auditId) {',1)[1].split('    /** Deletion is permanent',1)[0]
assert undo.index('writable.beginTransaction()') < undo.index('writable.rawQuery(')
assert undo.index('reclassifyTransaction(') < undo.index('writable.update("change_history"')
assert undo.index('writable.update("change_history"') < undo.index('writable.setTransactionSuccessful()')
assert 'writable.endTransaction();' in undo
assert 'if(writable.update("change_history",cv,"id=? AND undone=0"' in undo
assert 'reversal.put("action","UNDO")' in undo
assert "AND id>? AND action='EDIT'" in undo
restore = SRC.split('public void undoDeletedSimpleTransaction(long auditId) {',1)[1].split('private static boolean canSnapshotSimpleDelete',1)[0]
assert 'restoreEvent.put("action","RESTORE")' in restore
assert "AND action='ADD'" in restore
raw_action = undo.split('writable.rawQuery(',1)[1].split('new String[]{String.valueOf(auditId)}',1)[0]
assert raw_action.count('SAFE_CATEGORY_UNDO_SQL +') == 1
before,after = raw_action.split('SAFE_CATEGORY_UNDO_SQL +',1)
action_sql = ''.join(tokens(before)) + safe_sql + ''.join(tokens(after))
assert action_sql.count('?') == 1
assert conn.execute(action_sql,(1,)).fetchone()[:2] == (91,3)
conn.execute("UPDATE transactions SET source='CAPTURED' WHERE id=91")
assert conn.execute(action_sql,(1,)).fetchone() is None
conn.execute("UPDATE transactions SET source='MANUAL' WHERE id=91")
assert conn.execute(action_sql,(1,)).fetchone() is not None

# Real SQLite rollback demonstration of the pair of account/category + audit actions.
conn.commit()
try:
    conn.execute('BEGIN')
    conn.execute('UPDATE transactions SET category_id=3 WHERE id=91')
    # Bad audit update should throw in actual Java => rollback.
    assert conn.execute('UPDATE change_history SET undone=1 WHERE id=-1').rowcount == 0
    raise RuntimeError('audit update failed')
except RuntimeError:
    conn.rollback()
assert conn.execute('SELECT category_id FROM transactions WHERE id=91').fetchone()[0] == 4
assert conn.execute('SELECT undone FROM change_history WHERE id=1').fetchone()[0] == 0
print('DeleteCursorAndCategoryUndo: real 20-column SQL, metadata/fx guards, stale-undo rejection, atomic rollback passed')
