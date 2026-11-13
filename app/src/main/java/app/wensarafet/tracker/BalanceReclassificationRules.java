package app.wensarafet.tracker;

/** Pure balance math for replacing one posted effect on the same account. */
public final class BalanceReclassificationRules {
    private BalanceReclassificationRules() {}

    public static long netDelta(String type, long oldImpact, long newImpact) {
        if (oldImpact < 0 || newImpact < 0)
            throw new IllegalArgumentException("أثر مالي غير صالح");
        if (FinanceRules.INCOME.equals(type))
            return Math.subtractExact(newImpact, oldImpact);
        if (FinanceRules.EXPENSE.equals(type))
            return Math.subtractExact(oldImpact, newImpact);
        throw new IllegalArgumentException("نوع العملية غير مدعوم");
    }
}
