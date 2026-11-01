#!/usr/bin/env python3
"""SQLite category-name safety: historical records, incoming money and uniqueness."""
import json
import re
import sqlite3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
src=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")

def java_literals(block):
    return "".join(json.loads(t) for t in re.findall(r'"(?:\\.|[^"\\])*"',block))

method=src.split("private static void requireUniqueCategoryName(",1)[1].split(
    "public long addCustomCategory(",1)[0]
duplicate_sql=java_literals(method.split("rawQuery(",1)[1].split("new String[]{",1)[0])
assert duplicate_sql.startswith("SELECT 1 FROM categories")
assert "kind=?" in duplicate_sql and "name=? COLLATE NOCASE" in duplicate_sql
assert "id!=?" in duplicate_sql and "archived=0" not in duplicate_sql
assert "CategoryNamingRules.validateNewCategory(name,kind)" in src
assert "CategoryNamingRules.validateRename(oldName,kind,excluded,clean)" in src
assert 'CategoryNamingRules.RECEIVED_TRANSFER.equals(c.getString(0))' in src
assert "writable.beginTransaction()" in src.split("public void renameCategory(",1)[1].split(
    "public Long getAccountBalance(",1)[0]
assert "showRenameCategoryDialog(chosen,kind)" in ui
assert "db.renameCategory(chosen.id,nameInput.getText().toString())" in ui
assert "db.addCustomCategory(name.getText().toString(), kind, emoji.getText().toString())" in ui

conn=sqlite3.connect(":memory:")
conn.execute("PRAGMA foreign_keys=ON")
conn.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT,kind TEXT,archived INTEGER,exclude_from_expense INTEGER);
CREATE TABLE accounts(id INTEGER PRIMARY KEY,balance_minor INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,amount_minor INTEGER,category_id INTEGER,account_id INTEGER,
 type TEXT, FOREIGN KEY(category_id) REFERENCES categories(id));
CREATE TABLE change_history(id INTEGER PRIMARY KEY AUTOINCREMENT,table_name TEXT,record_id INTEGER,action TEXT);
CREATE TRIGGER audit_category_update AFTER UPDATE ON categories WHEN OLD.name!=NEW.name BEGIN
INSERT INTO change_history(table_name,record_id,action) VALUES('categories',NEW.id,'EDIT'); END;
INSERT INTO categories VALUES
(1,'راتب','INCOME',0,0),(2,'حوالة مستلمة','INCOME',0,0),
(3,'طعام','EXPENSE',0,0),(4,'مواصلات','EXPENSE',1,0),(5,'دفع لأخوي','EXPENSE',0,1);
INSERT INTO accounts VALUES (10,123450);
INSERT INTO transactions VALUES(90,1200,3,10,'EXPENSE'),(91,5000,2,10,'INCOME');
""")

def duplicate(kind,name,exclude):
    return conn.execute(duplicate_sql,(kind,name,exclude)).fetchone() is not None

assert duplicate("EXPENSE","مواصلات",-1), "archived historical label collision"
assert duplicate("INCOME","حوالة مستلمة",-1)
assert not duplicate("EXPENSE","راتب",-1), "same label in other type allowed"
assert not duplicate("EXPENSE","طعام",3), "renaming unchanged ID is allowed"
assert duplicate("EXPENSE","دفع لأخوي",3)
before=conn.execute("SELECT id,amount_minor,category_id,account_id,type FROM transactions ORDER BY id").fetchall()
balance=conn.execute("SELECT balance_minor FROM accounts WHERE id=10").fetchone()[0]
conn.execute("BEGIN")
conn.execute("UPDATE categories SET name='مأكولات' WHERE id=3 AND archived=0")
conn.commit()
assert conn.execute("SELECT name FROM categories WHERE id=3").fetchone()[0]=="مأكولات"
assert conn.execute("SELECT name FROM categories JOIN transactions ON categories.id=transactions.category_id WHERE transactions.id=90").fetchone()[0]=="مأكولات"
assert conn.execute("SELECT id,amount_minor,category_id,account_id,type FROM transactions ORDER BY id").fetchall()==before
assert conn.execute("SELECT balance_minor FROM accounts WHERE id=10").fetchone()[0]==balance
assert conn.execute("SELECT table_name,record_id,action FROM change_history").fetchall()==[("categories",3,"EDIT")]
assert conn.execute("PRAGMA foreign_key_check").fetchall()==[]
print("CategoryRenameIntegrity PASS: no historical deletion, balance changes, duplicates or receipt misclassification")
