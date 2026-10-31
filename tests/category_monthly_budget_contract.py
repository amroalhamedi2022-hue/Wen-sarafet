#!/usr/bin/env python3
"""Category monthly budgets never mutate ledgers; reporting reuses tested split-aware totals."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
store=(root/"app/src/main/java/app/wensarafet/tracker/CategoryMonthlyBudgetStore.java").read_text(encoding="utf-8")
source=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text(encoding="utf-8")
analytics=ui.split("private void showAnalyticsScreen() {",1)[1].split(
    "private View monthlyBudgetCard(",1)[0]
detail=ui.split("private void showCategoryExpenseScreen(",1)[1].split(
    "private View categoryMonthlyBudgetCard(",1)[0]
budget=ui.split("private View categoryMonthlyBudgetCard(",1)[1].split(
    "private void showAccountManagement()",1)[0]
assert "shownCategories.put(item.categoryId,true)" in analytics
assert "available.excludeFromExpense" in analytics
assert "filteredCategories" not in analytics
assert "categoryAnalysisRow(empty,max" in analytics
assert "categoryMonthlyBudgetCard(category,currency,startInclusive,endExclusive)" in detail
assert 'sectionTitle("تحليل آخر 6 أشهر"' in detail
assert "db.getCategoryExpenseTotals(currency,first,last)" in detail
assert "db.getExpenseTransactionsByCategory(currency, category.categoryId" in detail
for fragment in ("CategoryMonthlyBudgetStore.get(this,category.categoryId,currency)",
                 "CategoryMonthlyBudgetStore.save(this,category.categoryId,currency,value)",
                 "Money.parseMinor(input.getText().toString())",
                 "long cap=CategoryMonthlyBudgetStore.get(this,category.categoryId,currency)",
                 "double ratio=Math.min(1.0"):
    assert fragment in budget, fragment
assert "monthly_category_caps" in store
assert "SharedPreferences.Editor" in store
assert "edit.remove(key(categoryId,currency))" in store
assert '"category_"+categoryId+"_"+currency' in store
for forbidden in ("SQLiteDatabase", "FinanceDbHelper", "transactions", "UPDATE ", "DELETE FROM"):
    assert forbidden not in store, forbidden
assert "public List<Models.CategoryTotal> getCategoryExpenseTotals" in source
assert "JOIN categories c ON c.id=s.category_id" in source, "Split allocation excludes unclassified debt"
print("CategoryMonthlyBudgetContract PASS: per-currency caps, zero-expense categories, 6-month split-aware analysis and no money writes")
