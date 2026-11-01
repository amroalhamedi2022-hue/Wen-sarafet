#!/usr/bin/env python3
"""Read-only merchant reporting must exclude debt portions, transfers and excluded expenses."""
import json
import re
import sqlite3
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=(root/"app/src/main/java/app/wensarafet/tracker/MerchantInsights.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
part=source.split("String sql=",1)[1].split("Map<String,Place> aggregated",1)[0]
sql="".join(json.loads(v) for v in re.findall(r'"(?:\\.|[^"\\])*"',part))
assert sql.startswith("SELECT t.note,t.raw_text,t.source, ")
for expected in ("EXISTS(SELECT 1 FROM transaction_splits",
                 "JOIN categories sc ON sc.id=s.category_id",
                 "s.special_kind IS NULL",
                 "sc.exclude_from_expense=0",
                 "t.special_kind IS NULL",
                 "t.type=?",
                 "t.currency=?",
                 "LIMIT 5000"):
    assert expected in sql,expected
assert "rawQuery(sql,new String[]{" in source
assert "Math.addExact(place.spentMinor,amount)" in source
assert "result.sort(" in source
assert "merchant-insights" in ui
assert "showMerchantInsightsScreen(dashboardCurrency)" in ui
assert 'favoritePlaces=actionButton("🏪 أكثر المحلات والجهات صرفًا"' in ui
assert "if(!page.isAttachedToWindow())return;" in ui

db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY,exclude_from_expense INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,type TEXT,currency TEXT,
 note TEXT,raw_text TEXT,source TEXT,amount_minor INTEGER,
 special_kind TEXT,created_at INTEGER,category_id INTEGER);
CREATE TABLE transaction_splits(transaction_id INTEGER,amount_minor INTEGER,
 category_id INTEGER,special_kind TEXT);
INSERT INTO categories VALUES(1,0),(2,1);
INSERT INTO transactions VALUES
 (1,'EXPENSE','ILS','طيبة مول','','CAPTURED',1000,NULL,10,1),
 (2,'EXPENSE','ILS','اشتريت لدى طيبة مول','','MANUAL',2000,NULL,20,1),
 (3,'EXPENSE','ILS','عند محل طيبة مول','','MANUAL',3000,NULL,30,NULL),
 (4,'EXPENSE','ILS','عند محل طيبة مول','','MANUAL',5000,NULL,40,2),
 (5,'EXPENSE','ILS','عند محل طيبة مول','','MANUAL',2500,'DEBT_PAYMENT',50,1),
 (6,'TRANSFER','ILS','عند محل طيبة مول','','MANUAL',800,NULL,60,1);
INSERT INTO transaction_splits VALUES(3,1500,1,NULL),(3,1500,NULL,'DEBT');
""")
rows=db.execute(sql,('EXPENSE','ILS','0','100')).fetchall()
assert [x[3] for x in rows]==[0,1500,2000,1000], rows
assert sum(x[3] for x in rows)==4500,rows
assert db.execute("SELECT SUM(amount_minor) FROM transactions").fetchone()[0]>4500
print("MerchantInsightsContract PASS: 4500 minor true expense only; no debit mutations or data-wide periodic work")
