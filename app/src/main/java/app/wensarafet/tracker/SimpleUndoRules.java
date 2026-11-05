package app.wensarafet.tracker;

/** Small, auditable contract: no automatic restoration of financial relationships. */
public final class SimpleUndoRules {
    private SimpleUndoRules() {}
    public static boolean isEligible(String type,String source,long amount,boolean categoryExists,
                                     boolean transfer,boolean split,boolean debt,boolean captured,
                                     int needsReview) {
        return (FinanceRules.EXPENSE.equals(type)||FinanceRules.INCOME.equals(type))
                && "MANUAL".equals(source) && amount>0 && categoryExists
                && !transfer && !split && !debt && !captured && needsReview==0;
    }
    public static long restoredBalance(long current,String type,long deletedImpactMinor) {
        if(deletedImpactMinor<0)throw new IllegalArgumentException("أثر مالي غير صالح");
        if(FinanceRules.EXPENSE.equals(type))return Math.subtractExact(current,deletedImpactMinor);
        if(FinanceRules.INCOME.equals(type))return Math.addExact(current,deletedImpactMinor);
        throw new IllegalArgumentException("لا يمكن استرجاع هذا النوع تلقائيًا");
    }
}
