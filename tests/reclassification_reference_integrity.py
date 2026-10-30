#!/usr/bin/env python3
"""Exercise the actual production category/account reference SQL in SQLite.

No Android runtime is required. Verifies reclassification rejects cross-type,
archived, unknown category IDs and new inactive account destinations without
breaking an existing transaction that belongs to an archived account.
"""
import re
import json
import sqlite3
from pathlib import Path

src = (Path(__file__).resolve().parents[1] /
       "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
method = src.split("public void reclassifyTransaction(", 1)[1].split("public long addDebtPerson(", 1)[0]
for expression in (
    "requireCategoryForType(writable, categoryId, newType);",
    "requireCategoryForType(writable, split.categoryId, newType);",
    "sourceId != oldSourceId && !isActiveAccount(writable, sourceId)",
    "!destinationId.equals(oldDestinationId)",
    "!isActiveAccount(writable, destinationId)",
):
    assert expression in method, expression
assert method.index("requireCategoryForType(writable, categoryId, newType)") < method.index(
    "if (affectsBalance) reverseBalanceEffect")

def java_literal(s):
    return "".join(json.loads(value) for value in re.findall(r'"(?:\\.|[^"\\])*"',s))

required_method = src.split("private static void requireCategoryForType(",1)[1].split(
    "private static boolean isActiveAccount(",1)[0]
category_sql = java_literal(required_method.split("writable.rawQuery(", 1)[-1]) if False else (
    java_literal(required_method.split("rawQuery(", 1)[1].split("new String[]{", 1)[0]))
)
active_method = src.split("private static boolean isActiveAccount(",1)[1].split(
    "private void adjustBalance(",1)[0]
active_sql = java_literal(active_method.split("rawQuery(",1)[1].split("new String[]{",1)[0])
assert "kind FROM categories WHERE id=? AND archived=0" in category_sql
assert "accounts WHERE id=? AND active=1" in active_sql

db = sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY, kind TEXT, archived INTEGER NOT NULL);
CREATE TABLE accounts(id INTEGER PRIMARY KEY, active INTEGER NOT NULL);
INSERT INTO categories VALUES
 (1,'EXPENSE',0),(2,'INCOME',0),(3,'EXPENSE',1),(4,'INCOME',1);
INSERT INTO accounts VALUES (10,1),(11,1),(12,0);
""")
def valid_category(id, kind):
    row = db.execute(category_sql,(id,)).fetchone()
    return bool(row and row[0] == kind)
def active_account(id):
    return db.execute(active_sql,(id,)).fetchone() is not None

assert valid_category(1,"EXPENSE")
assert valid_category(2,"INCOME")
for category,kind in ((1,"INCOME"),(2,"EXPENSE"),(3,"EXPENSE"),(4,"INCOME"),(999,"EXPENSE")):
    assert not valid_category(category,kind), (category,kind)

assert active_account(10) and active_account(11)
assert not active_account(12) and not active_account(999)
# Existing archived account 12 can be recategorized in place, but a new move to it is blocked.
def allowed_account(proposed, original):
    return proposed == original or active_account(proposed)
assert allowed_account(12,12)
assert not allowed_account(12,10)
assert allowed_account(11,10)
print("ReclassificationReferenceIntegrity PASS: typed/live categories, active new accounts, historical inactive preservation")
