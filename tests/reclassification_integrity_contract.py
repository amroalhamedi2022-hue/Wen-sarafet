#!/usr/bin/env python3
"""Financial reclassification guard: reject FX and debt rows before touching balances."""
from pathlib import Path
import re
import json
import sqlite3
root=Path(__file__).resolve().parents[1]
source=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
method=source.split("public void reclassifyTransaction(",1)[1].split("String oldType;",1)[0]
assert method.index("writable.beginTransaction()") < method.index("SELECT 1 FROM transactions")
assert method.index("SELECT 1 FROM transactions") < method.index("String oldType") if "String oldType" in method else True
query=method.split('restricted=writable.rawQuery(',1)[1].split('new String[]{String.valueOf(transactionId)}',1)[0]
terms=re.findall(r'"(?:\\.|[^"\\])*"',query)
sql="".join(json.loads(x) for x in terms)
for token in ("destination_amount_minor", "exchange_rate", "special_kind",
              "debt_person_id", "debt_direction", "debt_movements"):
    assert token in sql,token
assert "capture_inbox" not in sql, "Capture review must remain available"
db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE transactions(id INTEGER PRIMARY KEY,destination_amount_minor INTEGER,
 exchange_rate TEXT,special_kind TEXT,debt_person_id INTEGER,debt_direction TEXT);
CREATE TABLE debt_movements(transaction_id INTEGER);
INSERT INTO transactions(id) VALUES (1),(2),(3),(4),(5),(6),(7);
UPDATE transactions SET destination_amount_minor=500 WHERE id=2;
UPDATE transactions SET exchange_rate='3.14' WHERE id=3;
UPDATE transactions SET special_kind='DEBT' WHERE id=4;
UPDATE transactions SET debt_person_id=9 WHERE id=5;
UPDATE transactions SET debt_direction='RECEIVABLE' WHERE id=6;
INSERT INTO debt_movements VALUES(7);
""")
for txid in range(1,8):
    blocked=bool(db.execute(sql,(txid,)).fetchone())
    assert blocked==(txid!=1),(txid,blocked)
print("ReclassificationIntegrityContract PASS: normal transaction accepted, FX/debt/ledger blocked.")
