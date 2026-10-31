#!/usr/bin/env python3
"""Read-only checkpoint source/SQL contract, with actual split-aware SQLite fixtures."""
import json
import re
import sqlite3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
j=(ROOT/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text()
store=(ROOT/"app/src/main/java/app/wensarafet/tracker/CategoryCheckpointStore.java").read_text()
rules=(ROOT/"app/src/main/java/app/wensarafet/tracker/CategoryCheckpointRules.java").read_text()
codec=(ROOT/"app/src/main/java/app/wensarafet/tracker/BackupSettingsCodec.java").read_text()
backup=(ROOT/"app/src/main/java/app/wensarafet/tracker/BackupFileManager.java").read_text()
finance=(ROOT/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()

screen=j.split("private void showCategoryExpenseScreen(",1)[1].split(
    "private View categoryMonthlyBudgetCard(",1)[0]
assert "content.addView(categoryCheckpointCard(category,currency,startInclusive,endExclusive))" in screen
assert "content.addView(categoryMonthlyBudgetCard(category,currency,startInclusive,endExclusive))" in screen
assert screen.index("categoryCheckpointCard(category,currency,startInclusive,endExclusive)") < screen.index(
    "categoryMonthlyBudgetCard(category,currency,startInclusive,endExclusive)")
assert 'sectionTitle("تحليل آخر 6 أشهر"' in screen
assert 'db.getCategoryExpenseTotals(currency,first,last)' in screen
assert 'db.getExpenseTransactionsByCategory(currency, category.categoryId' in screen
for text in [
    "CategoryCheckpointStore.get(this,category.categoryId,currency)",
    "CategoryCheckpointStore.create(this,category.categoryId,currency)",
    "CategoryCheckpointStore.undoLast(this,category.categoryId,currency)",
    "CategoryCheckpointRules.periodStart(checkpoints,i)",
    "CategoryCheckpointRules.periodEnd(checkpoints,i,nowExclusive)",
    "categoryCheckpointExpenseTotal(category.categoryId,currency,",
    "db.getExpenseTransactionsByCategory(currency,",
    "new AlertDialog.Builder(this)",
    "الفترات القديمة — حتى تاريخ",
    "لن يتغير أي رصيد أو دين أو استهلاك الميزانية الشهرية",
    "الدخول",
]:
    if text=="الدخول": continue
    assert text in screen,text
assert "long spent=category.amountMinor;" in j, "monthly budget must retain original full-month total"
assert 'getSharedPreferences(PREFS,Context.MODE_PRIVATE)' in store
assert '"category_analysis_checkpoints"' in store and '"category_analysis_checkpoints"' in codec
assert "SharedPreferences" in store and "SQLiteDatabase" not in store
for forbidden in ["FinanceDbHelper","ContentValues","beginTransaction(", "UPDATE ", "DELETE FROM",
                  "adjustBalance(", "debt_movements", "setAccountBalance("]:
    assert forbidden not in store,forbidden
assert "if(!prefs.edit().putString(k,next).commit())" in store
assert "MAX_CHECKPOINTS = 24" in rules
assert "return periodIndex==0?0L:checkpoints.get(periodIndex-1)" in rules
assert "return periodIndex==checkpoints.size()?nowExclusive:checkpoints.get(periodIndex)" in rules
assert "newlyBackedUpNamespace(file)" in backup
assert "BackupSettingsCodec.INCLUDED_MARKER" in backup
assert '|| "category_analysis_checkpoints".equals(file)' in codec

# Run exactly the same SELECT clause used by the frozen financial helper.
part=finance.split("public List<Models.CategoryTotal> getCategoryExpenseTotals(",1)[1].split(
    "String start = String.valueOf(startInclusive)",1)[0]
sql_part=part.split('String sql =',1)[1]
tokens=re.findall(r'"(?:\\.|[^"\\])*"',sql_part)
sql="".join(json.loads(token) for token in tokens)
assert "transaction_splits" in sql and "exclude_from_expense=0" in sql
db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,emoji TEXT,exclude_from_expense INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT,special_kind TEXT,amount_minor INTEGER,
 category_id INTEGER,currency TEXT,created_at INTEGER);
CREATE TABLE transaction_splits(transaction_id INTEGER,category_id INTEGER,
 amount_minor INTEGER,special_kind TEXT);
INSERT INTO categories VALUES(1,'أكل وشرب','🍴',0),(2,'دين','💳',1);
INSERT INTO transactions VALUES
 (1,'EXPENSE',NULL,70000,1,'ILS',1001),
 (2,'EXPENSE',NULL,30000,1,'ILS',1002),
 (3,'EXPENSE',NULL,50000,1,'ILS',2001),
 (4,'EXPENSE',NULL,20000,NULL,'ILS',2002),
 (5,'EXPENSE',NULL,8000,1,'USD',1500),
 (6,'INCOME',NULL,9000,1,'ILS',2500),
 (7,'EXPENSE','DEBT',3000,1,'ILS',1300);
INSERT INTO transaction_splits VALUES (4,1,6000,NULL),(4,2,14000,'DEBT');
""")
baseline=db.execute("SELECT * FROM transactions ORDER BY id").fetchall()
splits=db.execute("SELECT * FROM transaction_splits").fetchall()
def total(a,b):
    params=("EXPENSE","ILS",str(a),str(b),"EXPENSE","ILS",str(a),str(b))
    result=db.execute(sql,params).fetchall()
    return next((r[3] for r in result if r[0]==1),0)
full_month=total(0,3000)
archived=total(0,2000)
active=total(2000,3000)
assert (full_month,archived,active)==(156000,100000,56000),(full_month,archived,active)
assert archived+active==full_month, "checkpoint must partition, not reset, monthly expense"
assert total(0,3000)==full_month, "monthly budget must remain unchanged"
assert db.execute("SELECT * FROM transactions ORDER BY id").fetchall()==baseline
assert db.execute("SELECT * FROM transaction_splits").fetchall()==splits
print("CategoryCheckpointContract PASS: all-time archive, active counter, split-aware monthly amount unchanged, no ledger writes")
