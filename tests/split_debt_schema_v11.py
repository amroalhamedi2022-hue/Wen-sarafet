#!/usr/bin/env python3
"""Run actual v11 SQL strings extracted from FinanceDbHelper against SQLite in memory.
Static Android source contracts + real SQLite DDL and rollback assertions; NOT device integration.
"""
import json,re,sqlite3
from pathlib import Path
src=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java').read_text()
ui=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/MainActivity.java').read_text()
assert 'DB_VERSION = 13' in src
assert 'SplitDebtRules.checkParts(amount, splits)' in src
assert 'LEFT JOIN debt_people p ON p.id=s.debt_person_id' in src
assert 'for (Long debtId : affectedDebts) recomputeDebt(writable, debtId);\n            writable.delete("transaction_splits"' in src
assert 'الدين مرتبط بمصروف مقسّم' in src
assert 'addDebtSplitRow(rowsBox, rows, categories, people)' in ui
assert 'SplitInput.debt(value' in ui
assert 'public long addDebtTransaction(' in src
assert 'String actualKind = DebtLedgerRules.movementKind(type, direction);' in src
assert 'if (!actualKind.equals(specialKind))' in src
assert 'db.addDebtTransaction(type,amountMinor,currency,accountId,note,affectsBalance,specialKind' in ui
assert 'addDebtSplitRow(splitRowsBox, splitRows, filteredCategories, debtPeople)' in ui
assert 'splits.add(Models.SplitInput.debt(partAmount' in ui

# Extract verbatim SQL assembled by Java literals, not hand-maintained replacement SQL.
def statements(s):
    out=[]
    for m in re.finditer(r'db\.execSQL\((.*?)\);',s,re.S):
        literals=re.findall(r'"(?:\\.|[^"\\])*"',m.group(1))
        out.append(''.join(json.loads(literal) for literal in literals))
    return out
on_upgrade=src.split('if (oldVersion < 11) {',1)[1].split('\n        }',1)[0]
queries=statements(on_upgrade)
assert len(queries)==4,len(queries)
assert queries[0].startswith('CREATE TABLE transaction_splits_v11')
assert queries[1].startswith('INSERT INTO transaction_splits_v11')
assert queries[2].startswith('DROP TABLE transaction_splits')
assert queries[3].startswith('ALTER TABLE transaction_splits_v11')

c=sqlite3.connect(':memory:')
c.execute('PRAGMA foreign_keys=ON')
c.executescript('''
CREATE TABLE accounts (id INTEGER PRIMARY KEY,balance_minor INTEGER);
CREATE TABLE categories(id INTEGER PRIMARY KEY, name TEXT NOT NULL,exclude_from_expense INTEGER NOT NULL DEFAULT 0);
CREATE TABLE transactions (id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,account_id INTEGER);
CREATE TABLE debt_people (id INTEGER PRIMARY KEY,name TEXT NOT NULL);
CREATE TABLE debts(id INTEGER PRIMARY KEY,person_id INTEGER,direction TEXT,currency TEXT,closed_at INTEGER);
CREATE TABLE debt_movements(id INTEGER PRIMARY KEY,transaction_id INTEGER,debt_id INTEGER,kind TEXT,amount_minor INTEGER,FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, UNIQUE(transaction_id,debt_id,kind));
CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER NOT NULL,category_id INTEGER NOT NULL,amount_minor INTEGER NOT NULL, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, FOREIGN KEY(category_id) REFERENCES categories(id));
INSERT INTO accounts VALUES(1,50000);
INSERT INTO categories VALUES (10,'transport',0);
INSERT INTO transactions VALUES(41,'EXPENSE',20000,1);
INSERT INTO debt_people VALUES(7,'person A');
INSERT INTO debt_people VALUES(8,'person B');
INSERT INTO debts VALUES(80,8,'PAYABLE','ILS',NULL);
INSERT INTO debt_movements VALUES(1,NULL,80,'OPEN',10000);
INSERT INTO transaction_splits VALUES(71,41,10,12000);
INSERT INTO transaction_splits VALUES(72,41,10,8000);
''')
for q in queries:c.execute(q)
assert c.execute('SELECT id,transaction_id,category_id,amount_minor,special_kind FROM transaction_splits ORDER BY id').fetchall()==[(71,41,10,12000,None),(72,41,10,8000,None)]
assert c.execute('PRAGMA foreign_key_check').fetchall()==[]
# Existing data preserved; new rows can include ordinary/debt/repayment on same transaction.
c.execute('DELETE FROM transaction_splits WHERE transaction_id=41')
c.execute('INSERT INTO transaction_splits(transaction_id,category_id,amount_minor) VALUES(41,10,12000)')
c.execute("INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,NULL,5000,'DEBT',7,'RECEIVABLE')")
c.execute("INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,NULL,3000,'DEBT_PAYMENT',8,'PAYABLE')")
assert c.execute('SELECT SUM(amount_minor) FROM transaction_splits WHERE transaction_id=41').fetchone()[0]==20000
assert c.execute('SELECT SUM(s.amount_minor) FROM transaction_splits s LEFT JOIN categories cat ON cat.id=s.category_id WHERE s.transaction_id=41 AND (s.special_kind=\'DEBT_PAYMENT\' OR (s.special_kind IS NULL AND cat.exclude_from_expense=0))').fetchone()[0]==15000
assert c.execute('SELECT SUM(s.amount_minor) FROM transaction_splits s JOIN categories cat ON cat.id=s.category_id WHERE s.transaction_id=41 AND s.special_kind IS NULL').fetchone()[0]==12000
for q in ["INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,NULL,5,'DEBT',NULL,'RECEIVABLE')",
          "INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,10,5,'DEBT',7,'RECEIVABLE')",
          "INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,NULL,5,'DEBT',7,'PAYABLE')",
          "INSERT INTO transaction_splits(transaction_id,category_id,amount_minor,special_kind,debt_person_id,debt_direction) VALUES(41,NULL,5,'DEBT_PAYMENT',8,'RECEIVABLE')"]:
 try:c.execute(q);raise AssertionError('invalid split was accepted')
 except sqlite3.IntegrityError: pass
# Reconcile a deliberately invalid settlement within a transaction: rollback every side effect.
c.commit()
initial=c.execute('SELECT balance_minor FROM accounts WHERE id=1').fetchone()[0]
old_rows=c.execute('SELECT COUNT(*) FROM transaction_splits WHERE transaction_id=41').fetchone()[0]
try:
 c.execute('BEGIN')
 c.execute('UPDATE accounts SET balance_minor=balance_minor-15000 WHERE id=1')
 c.execute("INSERT INTO debt_movements(transaction_id,debt_id,kind,amount_minor) VALUES(41,80,'PAYMENT',3000)")
 raise ValueError('simulated failure after partially mutating DB')
except ValueError:c.rollback()
assert c.execute('SELECT balance_minor FROM accounts WHERE id=1').fetchone()[0]==initial
assert c.execute('SELECT COUNT(*) FROM transaction_splits WHERE transaction_id=41').fetchone()[0]==old_rows
assert c.execute('SELECT COUNT(*) FROM debt_movements WHERE transaction_id=41').fetchone()[0]==0
assert c.execute('PRAGMA foreign_key_check').fetchall()==[]
print('SplitDebtSchemaV11: real SQL migration, FK, mixed expense/debt/payment, and atomic rollback passed')
