#!/usr/bin/env python3
"""Execute exact Java-built search queries against in-memory SQLite, not a fake rewrite."""
import base64
import sqlite3
import subprocess
from pathlib import Path

root=Path(__file__).resolve().parents[1]
db_source=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")

assert "TransactionSearchRules.build(text,type,currency,pageSize,offset)" in db_source
assert "getReadableDatabase().rawQuery(query.sql,query.args)" in db_source
assert "populateSpecial(tx);" in db_source.split("searchTransactions(",1)[1].split(
    "getExpenseTransactions(",1)[0]
assert "page=db.searchTransactions(search.getText().toString(),chosenType,chosenCurrency," in ui
assert "pageSize+1,offset[0]" in ui
assert "results.removeView(more)" in ui
assert "loadNext[0].run();" in ui
assert "search.addTextChangedListener(new TextWatcher()" in ui
assert "type.setOnItemSelectedListener(new SimpleItemSelected(reset))" in ui
assert "currency.setOnItemSelectedListener(new SimpleItemSelected(reset))" in ui
assert "List<Models.Transaction> all = db.getTransactions(limit)" not in ui.split(
    "private LinearLayout transactionsContent(int limit)",1)[1].split(
    "private View sectionTitle(",1)[0]

dump=subprocess.run(
    ["java","-cp",str(root/"build/core-test"),"TransactionSearchDump"],
    capture_output=True,text=True,check=True
).stdout
samples={}
for line in dump.splitlines():
    fields=line.split("|")
    name=fields[0]
    data=[base64.b64decode(item).decode("utf-8") for item in fields[1:]]
    samples[name]=(data[0],data[1:])
assert len(samples)==7,samples.keys()

db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,name TEXT);
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,emoji TEXT);
CREATE TABLE transactions(
 id INTEGER PRIMARY KEY,type TEXT,amount_minor INTEGER,currency TEXT,
 account_id INTEGER,destination_account_id INTEGER,category_id INTEGER,
 note TEXT,raw_text TEXT,needs_review INTEGER,created_at INTEGER,affects_balance INTEGER);
CREATE TABLE transaction_splits(transaction_id INTEGER);
INSERT INTO accounts VALUES(1,'نقدي'),(2,'بنك فلسطين');
INSERT INTO categories VALUES(1,'طعام','🍽️'),(2,'راتب','💼');
INSERT INTO transactions VALUES
(1,'EXPENSE',1000,'ILS',1,NULL,1,'من مخبز القدس','',0,101,1),
(2,'EXPENSE',5000,'ILS',2,NULL,1,'مطعم 50.00','',0,102,1),
(3,'INCOME',1234,'USD',2,NULL,2,'Salary','',0,103,1),
(4,'EXPENSE',3000,'ILS',1,NULL,1,'خصم 20%','',0,104,1),
(5,'TRANSFER',555,'ILS',2,1,NULL,'تحويل نقدي','',0,105,1),
(6,'EXPENSE',5000,'ILS',1,NULL,1,'شراء عادي','',0,106,1);
""")
expected={
 "all":[6,5,4,3],
 "merchant":[1],
 "percent":[4],
 "amount":[6,2],
 "page":[5,4],
 "incomeUSD":[3],
 "injection":[]
}
for name, ids in expected.items():
    sql,args=samples[name]
    assert "ORDER BY t.created_at DESC,t.id DESC LIMIT ? OFFSET ?" in sql,name
    assert len(args)==sql.count("?"),name
    found=[row[0] for row in db.execute(sql,args)]
    assert found==ids,(name,found,ids)
print("TransactionSearchSQLite PASS: actual Java query paging, amount, Arabic, filters, wildcard and injection tests")
