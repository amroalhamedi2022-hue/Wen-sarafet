#!/usr/bin/env python3
"""Bank/wallet message imports must atomically persist the inbox, row and balances."""
from pathlib import Path
import sqlite3
src=(Path(__file__).resolve().parents[1]/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
def segment(start,end):
    return src.split(start,1)[1].split(end,1)[0]
for name,start,end in (
    ("expense", "public long importCapture(", "public long importCaptureTransfer("),
    ("transfer", "public long importCaptureTransfer(", "private long addCapturedTransfer(")):
    block=segment(start,end)
    for text in ("writable.beginTransaction();","if (!isCapturePending(captureId)) return -1;",
                 "writable.update(\"capture_inbox\",done,\"id=? AND status='PENDING'\"",
                 "writable.setTransactionSuccessful();","writable.endTransaction();"):
        assert text in block,(name,text)
    assert block.index("writable.beginTransaction();") < block.index("if (!isCapturePending(captureId))")
    assert block.index('writable.update("capture_inbox",done') < block.index("writable.setTransactionSuccessful();")
    assert "!=1)" in block,(name,"acknowledgement CAS required")
transfer=segment("public long importCaptureTransfer(", "private long addCapturedTransfer(")
assert "if (txId<0) return -1;" in transfer
assert "addCapturedTransfer(writable,item,sourceId,destinationId)" in transfer
post=segment("private long addCapturedTransfer(", "private long findProviderAccount(")
assert "beginTransaction()" not in post and "endTransaction()" not in post
for required in ("isActiveAccount(writable,sourceId)","isActiveAccount(writable,destinationId)",
                 'tx.put("balance_impact_minor", item.amountMinor)',
                 "NonNegativeBalanceRules.requireNonNegative(balances[0])",
                 'writable.update("accounts",from', 'writable.update("accounts",to'):
    assert required in post,required
expense=segment("public long importCapture(", "public long importCaptureTransfer(")
for required in ("capturedImpact", "balance_impact_minor", 'writable.update("accounts",balance'):
    assert required in expense,required

# Real SQLite transaction fixture: atomic capture import, CAS, duplicate reject,
# and rollback of a transfer whose acknowledgement fails.
db=sqlite3.connect(":memory:",isolation_level=None)
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY,balance INTEGER NOT NULL);
CREATE TABLE capture_inbox(id INTEGER PRIMARY KEY,status TEXT NOT NULL,transaction_id INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY,amount INTEGER NOT NULL,account_id INTEGER NOT NULL);
INSERT INTO accounts VALUES(1,100000),(2,20000);
INSERT INTO capture_inbox VALUES(10,'PENDING',NULL),(11,'PENDING',NULL);
""")
def post_once(capture,source,dest,amount,force_ack_failure=False):
    db.execute("BEGIN IMMEDIATE")
    try:
        if db.execute("SELECT 1 FROM capture_inbox WHERE id=? AND status='PENDING'",(capture,)).fetchone() is None:
            db.execute("ROLLBACK");return False
        old=db.execute("SELECT balance FROM accounts WHERE id=?",(source,)).fetchone()[0]
        if old<amount:
            db.execute("ROLLBACK");return False
        db.execute("INSERT INTO transactions(id,amount,account_id) VALUES(?,?,?)",(capture,amount,source))
        db.execute("UPDATE accounts SET balance=balance-? WHERE id=?",(amount,source))
        db.execute("UPDATE accounts SET balance=balance+? WHERE id=?",(amount,dest))
        if force_ack_failure:
            db.execute("UPDATE capture_inbox SET status='IGNORED' WHERE id=?",(capture,))
        updated=db.execute("UPDATE capture_inbox SET status='IMPORTED',transaction_id=? WHERE id=? AND status='PENDING'",(capture,capture)).rowcount
        if updated != 1:raise ValueError("stale confirmation")
        db.execute("COMMIT")
        return True
    except Exception:
        db.execute("ROLLBACK");return False
assert post_once(10,1,2,3000)
assert not post_once(10,1,2,3000),"duplicate import"
assert db.execute("SELECT balance FROM accounts ORDER BY id").fetchall()==[(97000,),(23000,)]
assert not post_once(11,1,2,5000,True),"failed CAS should roll back all changes"
assert db.execute("SELECT balance FROM accounts ORDER BY id").fetchall()==[(97000,),(23000,)]
assert db.execute("SELECT id FROM transactions ORDER BY id").fetchall()==[(10,)]
assert db.execute("SELECT status,transaction_id FROM capture_inbox WHERE id=11").fetchone()==('PENDING',None)
print("CapturedImportAtomicityContract PASS: expense/transfer same SQLite transaction, duplicate CAS, rollback/active-account guards")
