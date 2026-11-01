#!/usr/bin/env python3
"""Primary navigation, readable debt actions, and lazy category-history UI regressions.

No money or debt-mutation implementation is changed by these checks.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
debt=ui.split("private void showDebtsScreen(String direction) {",1)[1].split(
    "private LinkedHashMap<Long,List<Models.DebtSummary>> groupDebtsByPerson(",1)[0]
analytics=ui.split("private void showAnalyticsScreen() {",1)[1].split(
    "private View monthlyBudgetCard(",1)[0]
debt_card=ui.split("private View debtCard(Models.DebtSummary debt) {",1)[1].split(
    "private void showDebtHistory(",1)[0]
detail=ui.split("private void showDebtPersonDetails(",1)[1].split(
    "private View debtCard(",1)[0]
category=ui.split("private void showCategoryExpenseScreen(",1)[1].split(
    "private View categoryCheckpointCard(",1)[0]
checkpoint=ui.split("private View categoryCheckpointCard(",1)[1].split(
    "private long categoryCheckpointExpenseTotal(",1)[0]
budget=ui.split("private View categoryMonthlyBudgetCard(",1)[1].split(
    "private void showCategoryMonthlyBudgetDialog(",1)[0]
header=ui.split("private View pageHeader(String titleValue, Runnable backAction) {",1)[1].split(
    "private void showSettings() {",1)[0]
primary_nav=ui.split("private void bindPrimaryNavigationScroll(",1)[1].split(
    "private TextView navButton(",1)[0]

for screen,prefix in [(debt,"debt"),(analytics,"analytics")]:
    assert f"View {prefix}TopBar=mainTopBar();" in screen
    assert f"View {prefix}Nav=bottomNavigation(TAB_{'DEBTS' if prefix=='debt' else 'ANALYTICS'});" in screen
    assert f"bindPrimaryNavigationScroll(scroll,{prefix}TopBar,{prefix}Nav)" in screen
    assert "FrameLayout primaryRoot=new FrameLayout(this);" in screen
    assert "Gravity.BOTTOM" in screen and "primaryRoot.addView" in screen
    assert "setContentView(primaryRoot)" in screen
    assert "Ui.dp(this, 110)" in screen, "Bottom nav overlays need scroll bottom clearance"
assert "main_header_background" in primary_nav
assert "ValueAnimator.areAnimatorsEnabled()" in primary_nav
assert "nav.setVisibility(View.INVISIBLE)" in primary_nav
assert "nav.setVisibility(View.VISIBLE)" in primary_nav
assert "Ui.layeredBackground(this,20)" in header
assert "setContentDescription(\"رجوع\")" in header
assert "title.setAutoSizeTextTypeUniformWithConfiguration(16,22,1," in header

assert "categoryMonthlyHistoryCard(category,currency,startInclusive)" in category
assert "content.addView(categoryCheckpointCard(category,currency,startInclusive,endExclusive))" in category
assert "content.addView(categoryMonthlyBudgetCard(category,currency,startInclusive,endExclusive))" in category
assert category.index("categoryMonthlyBudgetCard(category,currency,startInclusive,endExclusive)") < category.index(
    "categoryMonthlyHistoryCard(category,currency,startInclusive)")
assert 'names.addView(Ui.text(this,"تحليل آخر 6 أشهر"' in category
assert "months.setVisibility(View.GONE);" in category
assert "if(expanded && months.getChildCount()==0)" in category
assert "cursor.add(Calendar.MONTH,-5);" in category
assert "months.setVisibility(expanded?View.VISIBLE:View.GONE)" in category
assert "setGravity(Gravity.CENTER_HORIZONTAL)" in category
assert "Math.min(760,availableDp)" in category
assert "getExpenseTransactionsByCategory" in category
assert "Money.format(visibleTotal, currency)" in category

assert "long spent=category.amountMinor;" in budget, "Budget uses untouched whole-month total"
assert "double ratio=Math.min(1.0,(double)spent/cap);" in budget
assert "Math.max(0,(int)(track.getWidth()*ratio))" in budget
assert "categoryCheckpointExpenseTotal(category.categoryId,currency," in checkpoint
assert "CategoryCheckpointStore.create(this,category.categoryId,currency)" in checkpoint
assert "CategoryCheckpointStore.undoLast(this,category.categoryId,currency)" in checkpoint
assert "لن يتغير أي رصيد أو دين أو استهلاك الميزانية الشهرية" in checkpoint
assert "الفترات القديمة — حتى تاريخ" in checkpoint
assert "Ui.dp(this,50)" in checkpoint
assert "getResources().getConfiguration().screenWidthDp<420" in checkpoint

assert "getResources().getConfiguration().screenWidthDp<430" in debt_card
assert "buttons.setOrientation(compact?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL)" in debt_card
assert 'actionButton("سجل الحركات",false)' in debt_card
assert 'actionButton("سداد جزئي",false)' in debt_card
assert 'actionButton("✓ تم السداد",true)' in debt_card
assert "db.markDebtPaid(debt.id)" in debt_card
assert "db.deleteDebt(debt.id)" in debt_card
assert "countDebtLinkedFinancialTransactions(debt.id)" in debt_card
assert "deleteParams.setMargins(Ui.dp(this,5),Ui.dp(this,14)," in debt_card
assert "Math.min(760,Math.max(240," in detail

print("UIPolish185Contract PASS: unified navigation, responsive debt actions, lazy history, full-budget totals and no ledger rewrites")
