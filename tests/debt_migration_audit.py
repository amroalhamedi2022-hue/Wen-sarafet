import sqlite3

db = sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys=ON")
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY, name TEXT, currency TEXT, balance_minor INTEGER, active INTEGER);
CREATE TABLE categories(id INTEGER PRIMARY KEY, name TEXT, kind TEXT, emoji TEXT, position INTEGER, exclude_from_expense INTEGER);
CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,amount_minor INTEGER NOT NULL,currency TEXT NOT NULL,account_id INTEGER NOT NULL,destination_account_id INTEGER,category_id INTEGER,note TEXT NOT NULL DEFAULT '',raw_text TEXT,needs_review INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,source TEXT NOT NULL DEFAULT 'MANUAL',affects_balance INTEGER NOT NULL DEFAULT 1);
CREATE TABLE transaction_splits(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER NOT NULL,category_id INTEGER NOT NULL,amount_minor INTEGER NOT NULL);
INSERT INTO accounts VALUES(1,'Jawwal Pay','ILS',100000,1);
INSERT INTO transactions(type,amount_minor,currency,account_id,created_at) VALUES('EXPENSE',20000,'ILS',1,1);
ALTER TABLE transactions ADD COLUMN base_affects_balance INTEGER NOT NULL DEFAULT 1;
UPDATE transactions SET base_affects_balance=affects_balance;
ALTER TABLE transactions ADD COLUMN special_kind TEXT;
ALTER TABLE transactions ADD COLUMN debt_person_id INTEGER;
ALTER TABLE transactions ADD COLUMN debt_direction TEXT;
CREATE TABLE debt_people (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL);
CREATE TABLE debts (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, direction TEXT NOT NULL, currency TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, closed_at INTEGER, manual_closed INTEGER NOT NULL DEFAULT 0, note TEXT NOT NULL DEFAULT '', FOREIGN KEY(person_id) REFERENCES debt_people(id));
CREATE TABLE debt_movements (id INTEGER PRIMARY KEY AUTOINCREMENT, debt_id INTEGER NOT NULL, transaction_id INTEGER, kind TEXT NOT NULL, amount_minor INTEGER NOT NULL, created_at INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', FOREIGN KEY(debt_id) REFERENCES debts(id) ON DELETE CASCADE, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, UNIQUE(transaction_id,debt_id,kind));
""")

assert db.execute("SELECT base_affects_balance,special_kind FROM transactions").fetchone() == (1, None)
db.execute("INSERT INTO debt_people(name,created_at) VALUES('محمد',1)")
db.execute("INSERT INTO debts(person_id,direction,currency,created_at,updated_at) VALUES(1,'RECEIVABLE','ILS',1,1)")
db.execute("INSERT INTO debt_movements(debt_id,transaction_id,kind,amount_minor,created_at) VALUES(1,1,'OPEN',20000,1)")
try:
    db.execute("INSERT INTO debt_movements(debt_id,transaction_id,kind,amount_minor,created_at) VALUES(1,1,'OPEN',20000,1)")
    raise AssertionError("duplicate movement accepted")
except sqlite3.IntegrityError:
    pass

db.execute("INSERT INTO debt_movements(debt_id,transaction_id,kind,amount_minor,created_at) VALUES(1,NULL,'MANUAL_PAYMENT',5000,2)")
original, paid = db.execute("SELECT SUM(CASE WHEN kind='OPEN' THEN amount_minor ELSE 0 END),SUM(CASE WHEN kind IN ('PAYMENT','MANUAL_PAYMENT') THEN amount_minor ELSE 0 END) FROM debt_movements WHERE debt_id=1").fetchone()
assert (original, paid, original - paid) == (20000, 5000, 15000)
# A repayment edit removes the old ledger movement before inserting the new amount.
db.execute("INSERT INTO transactions(type,amount_minor,currency,account_id,created_at,special_kind,debt_person_id,debt_direction) VALUES('INCOME',2000,'ILS',1,3,'DEBT_PAYMENT',1,'RECEIVABLE')")
payment_tx = db.execute("SELECT MAX(id) FROM transactions").fetchone()[0]
db.execute("INSERT INTO debt_movements(debt_id,transaction_id,kind,amount_minor,created_at) VALUES(1,?,'PAYMENT',2000,3)", (payment_tx,))
db.execute("DELETE FROM debt_movements WHERE transaction_id=?", (payment_tx,))
db.execute("UPDATE transactions SET amount_minor=3000 WHERE id=?", (payment_tx,))
db.execute("INSERT INTO debt_movements(debt_id,transaction_id,kind,amount_minor,created_at) VALUES(1,?,'PAYMENT',3000,3)", (payment_tx,))
assert db.execute("SELECT COUNT(*),SUM(amount_minor) FROM debt_movements WHERE transaction_id=?", (payment_tx,)).fetchone() == (1, 3000)
db.execute("DELETE FROM debt_movements WHERE transaction_id=?", (payment_tx,))
assert db.execute("SELECT SUM(CASE WHEN kind='OPEN' THEN amount_minor ELSE -amount_minor END) FROM debt_movements WHERE debt_id=1").fetchone()[0] == 15000
db.execute("DELETE FROM transactions WHERE id=1")
assert db.execute("SELECT COUNT(*) FROM debt_movements WHERE transaction_id=1").fetchone()[0] == 0
print("DebtMigrationAudit: migration, deduplication, partial/edit/delete reversal passed")
