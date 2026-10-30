#!/usr/bin/env python3
"""Batch 22: exercise categorization SQL and guard budget/icon feature wiring."""
import json
import re
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = (root / "app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
part = src.split("public CategorySuggestionRules.Suggestion suggestCategory(", 1)[1].split(
    "private static String extractMerchant(", 1)[0]
match = re.search(r"String sql=(.*?);\s*try \(Cursor", part, re.S)
assert match, "suggestCategory SQL not found"
query = "".join(json.loads(token) for token in re.findall(r'"(?:\\.|[^"\\])*"', match.group(1)))
assert "c.archived=0" in query and "c.kind=t.type" in query
assert "t.amount_minor>0" in query

db = sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE categories(id INTEGER PRIMARY KEY,kind TEXT,archived INTEGER);
CREATE TABLE transactions(
  id INTEGER PRIMARY KEY,type TEXT,currency TEXT,category_id INTEGER,account_id INTEGER,
  amount_minor INTEGER,note TEXT,raw_text TEXT,special_kind TEXT,needs_review INTEGER,
  created_at INTEGER
);
CREATE TABLE transaction_splits(transaction_id INTEGER);
INSERT INTO categories VALUES (1,'EXPENSE',0),(2,'EXPENSE',1),(3,'INCOME',0);
INSERT INTO transactions VALUES
  (1,'EXPENSE','ILS',1,1,200,'مخبز','',NULL,0,100),
  (2,'EXPENSE','ILS',2,1,200,'مؤرشف','',NULL,0,90),
  (3,'EXPENSE','ILS',1,1,-200,'فاسد','',NULL,0,80),
  (4,'EXPENSE','ILS',1,1,200,'مراجعة','',NULL,1,70),
  (5,'EXPENSE','ILS',1,1,200,'مقسم','',NULL,0,60),
  (6,'EXPENSE','ILS',3,1,200,'نوع خطأ','',NULL,0,50),
  (7,'EXPENSE','ILS',1,1,200,'دين','', 'DEBT',0,40);
INSERT INTO transaction_splits VALUES (5);
""")
rows = db.execute(query, ("EXPENSE", "ILS")).fetchall()
assert len(rows) == 1 and rows[0][0] == 1, rows

budget = (root / "app/src/main/java/app/wensarafet/tracker/BudgetAlertManager.java").read_text()
assert "SDK_INT >= 33" in budget and "Manifest.permission.POST_NOTIFICATIONS" in budget
assert budget.index("SDK_INT >= 33") < budget.index("context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)")
icon_ui = (root / "app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text()
assert "showCustomCategoryIconDialog(chosen,kind)" in icon_ui
assert "db.setCategoryIcon(chosen.id,symbol)" in icon_ui
assert "symbol.codePointCount(0,symbol.length())>8" in icon_ui
# C10: actual window configuration width controls responsive layouts, including split-screen.
assert icon_ui.count("getResources().getConfiguration().screenWidthDp < 390") == 3
assert "if(compactAccountCard)" in icon_ui
assert "if(compactTransactionCard) info.addView(amount);" in icon_ui
# C12: original 16 themes must remain at their original indices, followed by the new two.
ui = (root / "app/src/main/java/app/wensarafet/tracker/Ui.java").read_text()
names_block = ui.split("THEME_NAMES = {", 1)[1].split("};", 1)[0]
theme_names = re.findall(r'"([^"]+)"', names_block)
original = [
    "زمرد نعناعي", "ملكي نيلي", "تركواز", "نيلي ليلي",
    "رملي دافئ", "برغندي", "قرمزي", "كهرماني",
    "مشمشي", "وردي هادئ", "لافندر", "جليدي",
    "غابة قاتمة", "فحمي", "لؤلؤي", "نحاسي",
]
assert theme_names == original + ["Liquid Aurora", "Liquid Fusion"], theme_names
for name, per_row in (("THEMES", 12), ("THEME_LAYERS", 4)):
    palette = ui.split("int[][] " + name + " = {", 1)[1].split("};", 1)[0]
    rows = re.findall(r"\\{([^{}]+)\\}", palette)
    assert len(rows) == 18, (name, len(rows))
    assert all(len(row.split(",")) == per_row for row in rows), name
assert '.putInt("theme", index)' in ui
print("Batch22Contract: categorization SQLite, budget API guard, custom icons, compact layouts and 18 stable theme IDs passed")
