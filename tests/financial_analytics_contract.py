#!/usr/bin/env python3
"""Execute analytics SQL extracted from FinanceDbHelper against mixed financial rows."""
import json
import re
import sqlite3
from pathlib import Path

src=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java').read_text()

def java_strings(value):
    return ''.join(json.loads(token) for token in re.findall(r'"(?:\\.|[^"\\])*"',value))

income_method=src.split('public long getIncomeTotal(',1)[1].split('public long getExpenseTotal',1)[0]
income_base=java_strings(income_method.split('(receivedOnly?',1)[0].split('String sql=',1)[1])
income_received=income_base+"AND c.name='حوالة مستلمة'"
income_earned=income_base+"AND c.name!='حوالة مستلمة'"

expense_method=src.split('public long getExpenseTotal(',1)[1].split('public List<Models.CategoryTotal>',1)[0]
expense_sql=java_strings(expense_method.split('String sql =',1)[1].split(';',1)[0])

c=sqlite3.connect(':memory:')
c.executescript('''
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,kind TEXT,exclude_from_expense INTEGER NOT NULL DEFAULT 0);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,currency TEXT,category_id INTEGER,special_kind TEXT,created_at INTEGER);
CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY,transaction_id INTEGER,category_id INTEGER,amount_minor INTEGER,special_kind TEXT);
INSERT INTO categories VALUES(1,'طعام','EXPENSE',0),(2,'مستبعد','EXPENSE',1),(3,'راتب','INCOME',0),(4,'حوالة مستلمة','INCOME',0);
INSERT INTO transactions VALUES
 (1,'EXPENSE',100,'ILS',1,NULL,100),
 (2,'EXPENSE',200,'ILS',2,NULL,100),
 (3,'EXPENSE',300,'ILS',NULL,NULL,100),
 (4,'EXPENSE',400,'ILS',NULL,'DEBT',100),
 (5,'INCOME',500,'ILS',3,NULL,100),
 (6,'INCOME',600,'ILS',4,NULL,100),
 (7,'INCOME',700,'ILS',NULL,'DEBT_PAYMENT',100),
 (8,'TRANSFER',800,'ILS',NULL,NULL,100);
INSERT INTO transaction_splits VALUES(1,3,1,100,NULL),(2,3,NULL,200,'DEBT');
''')
args=('INCOME','ILS','0','1000')
assert c.execute(income_earned,args).fetchone()[0]==500
assert c.execute(income_received,args).fetchone()[0]==600
expense_args=('EXPENSE','ILS','0','1000','EXPENSE','ILS','0','1000')
assert c.execute(expense_sql,expense_args).fetchone()[0]==200
print('FinancialAnalyticsContract: earned/received income, transfer/debt exclusions, and mixed split expense passed')
