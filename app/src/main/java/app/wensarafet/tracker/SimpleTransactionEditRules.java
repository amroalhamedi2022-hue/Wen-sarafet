package app.wensarafet.tracker;

/**
 * Balance adjustment when the amount of a normal manual expense/income is edited.
 * The transaction type, account and excluded category remain unchanged.
 */
public final class SimpleTransactionEditRules {
    private SimpleTransactionEditRules() {}

    public static long balanceDelta(String type, long beforeMinor, long afterMinor,
                                    boolean affectsBalance, boolean excludedExpense) {
        if (beforeMinor <= 0 || afterMinor <= 0)
            throw new IllegalArgumentException("مبلغ العملية غير صحيح");
        if (!FinanceRules.EXPENSE.equals(type) && !FinanceRules.INCOME.equals(type))
            throw new IllegalArgumentException("لا يمكن تغيير مبلغ هذا النوع من الحركات");
        if (!affectsBalance || (FinanceRules.EXPENSE.equals(type) && excludedExpense))
            return 0L;
        // On an expense, restoring the old debit and applying the new one yields before-after.
        // On income, reversing the previous credit yields after-before.
        return FinanceRules.EXPENSE.equals(type)
                ? Math.subtractExact(beforeMinor, afterMinor)
                : Math.subtractExact(afterMinor, beforeMinor);
    }
}
