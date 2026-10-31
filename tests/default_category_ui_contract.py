#!/usr/bin/env python3
"""UI-only default category: separate kinds, never auto-select debt and no DB writes."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
store=(root/"app/src/main/java/app/wensarafet/tracker/DefaultCategoryStore.java").read_text(encoding="utf-8")
form=ui.split("private void showAddTransaction()",1)[1].split("private void ",1)[0]
for required in ("Button defaultCategoryButton=miniButton(\"☆\")",
                 "categoryPickRow.addView(defaultCategoryButton,starWidth)",
                 "DefaultCategoryStore.get(this,kind)",
                 "DefaultCategoryStore.set(this,kind,id)",
                 "DefaultCategoryStore.clear(this,kind)",
                 "category.setSelection(preferredIndex)",
                 "filteredCategories.get(index-2).id",
                 "if(index<2 || index-2>=filteredCategories.size())"):
    assert required in form,required
assert "categoryPickRow.addView(category,pickerWidth)" in form
assert "filteredCategories.isEmpty()?0:2" in form, "Debt entries must not be default"
assert "manual_entry_category_defaults" in store
assert 'return "default_"+kind;' in store
assert "getSharedPreferences" in store
assert "db.addTransaction(" not in store and "SQLite" not in store
assert "FinanceRules.EXPENSE.equals(kind)" in store
assert "FinanceRules.INCOME.equals(kind)" in store
print("DefaultCategoryContract PASS: star control, per-kind preference, non-debt fallback and no financial writes")
