#!/usr/bin/env python3
"""Verify C7 detail rows exactly reconcile to earned/received analytics totals."""
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
db = (ROOT / "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
ui = (ROOT / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text()

def literals(s):
    return "".join(json.loads(x) for x in re.findall(r'"(?:\\.|[^"\\])*"', s))

total_method = db.split("public long getIncomeTotal(", 1)[1].split(
    "public List<Models.Transaction> getIncomeTransactions(", 1)[0]
details_method = db.split("public List<Models.Transaction> getIncomeTransactions(", 1)[1].split(
    "public long getExpenseTotal(", 1)[0]
total_base = literals(total_method.split("String sql=", 1)[1].split("(receivedOnly?", 1)[0])
details_sql_parts = details_method.split("String sql =", 1)[1].split("String[] args", 1)[0]
details_base = literals(details_sql_parts.split("(receivedOnly ?", 1)[0])
details_order = literals(details_sql_parts.split(")\n                + ", 1)[1])
expected_start = ("WHERE t.type=? AND t.currency=? AND t.special_kind IS NULL "
                  "AND t.created_at>=? AND t.created_at<? AND t.category_id IS NOT NULL")
assert expected_start in total_base and expected_start in details_base
assert details_order.strip() == "ORDER BY t.created_at DESC,t.id DESC"

conn = sqlite3.connect(":memory:")
conn.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,name TEXT);
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,emoji TEXT);
CREATE TABLE transaction_splits(transaction_id INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,currency TEXT,
 account_id INTEGER,destination_account_id INTEGER,category_id INTEGER,
 note TEXT,raw_text TEXT,needs_review INTEGER,created_at INTEGER,affects_balance INTEGER,special_kind TEXT);
INSERT INTO accounts VALUES(1,'نقدي'),(2,'البنك');
INSERT INTO categories VALUES(1,'راتب','💼'),(2,'حوالة مستلمة','↙️'),(3,'هدية','🎁');
INSERT INTO transactions VALUES
(1,'INCOME',100,'ILS',1,NULL,1,'راتب',NULL,0,101,1,NULL),
(2,'INCOME',200,'ILS',1,NULL,2,'حوالة',NULL,0,102,1,NULL),
(3,'INCOME',350,'ILS',2,NULL,3,'هدية',NULL,0,103,0,NULL),
(4,'INCOME',900,'ILS',1,NULL,NULL,'دين',NULL,0,104,1,'DEBT'),
(5,'TRANSFER',9000,'ILS',1,2,NULL,'نقل',NULL,0,105,1,NULL),
(6,'EXPENSE',1000,'ILS',1,NULL,1,'شراء',NULL,0,106,1,NULL),
(7,'INCOME',800,'USD',1,NULL,2,'عملة أخرى',NULL,0,107,1,NULL),
(8,'INCOME',700,'ILS',1,NULL,2,'خارج الفترة',NULL,0,9999,1,NULL),
(9,'INCOME',999,'ILS',1,NULL,NULL,'غير مصنفة',NULL,1,108,1,NULL);
""")
for only, expected_ids, expected_sum in [(False, [3, 1], 450), (True, [2], 200)]:
    condition = ("AND c.name='حوالة مستلمة'" if only else "AND c.name!='حوالة مستلمة'")
    args = ("INCOME", "ILS", "100", "200")
    total = conn.execute(total_base + condition, args).fetchone()[0]
    detail_sql = details_base + condition + " " + details_order
    items = conn.execute(detail_sql, args).fetchall()
    assert [x[0] for x in items] == expected_ids, (only, items)
    assert sum(x[2] for x in items) == total == expected_sum, (only, total, items)

assert "earnedIncomeCard.setOnClickListener" in ui
assert "receivedIncomeCard.setOnClickListener" in ui
assert "db.getIncomeTransactions(currency, startInclusive, endExclusive, receivedOnly)" in ui
print("IncomeDrilldownContract passed: detail totals match income/received, excluding debt, transfers, unclassified, wrong currency and dates.")
