package app.wensarafet.tracker;

import java.util.List;

/** Pure-money validation, deliberately independent of Android and DB state. */
public final class SplitDebtRules {
    private SplitDebtRules() {}
    public static long checkParts(long total, List<Models.SplitInput> parts) {
        if (total <= 0 || parts == null || parts.size() < 2) throw new IllegalArgumentException("التقسيم يحتاج جزأين على الأقل");
        long sum = 0;
        for (Models.SplitInput part : parts) {
            if (part == null || part.amountMinor <= 0) throw new IllegalArgumentException("مبلغ الجزء غير صالح");
            if (part.specialKind == null) {
                if (part.categoryId <= 0 || part.debtPersonId != null || part.debtDirection != null)
                    throw new IllegalArgumentException("اختر تصنيف الجزء");
            } else {
                if (part.categoryId != 0 || part.debtPersonId == null || part.debtPersonId <= 0)
                    throw new IllegalArgumentException("اختر صاحب الدين");
                if (!FinanceRules.SPECIAL_DEBT.equals(part.specialKind)
                        && !FinanceRules.SPECIAL_DEBT_PAYMENT.equals(part.specialKind))
                    throw new IllegalArgumentException("تصنيف الدّين غير صحيح");
                if ((FinanceRules.SPECIAL_DEBT.equals(part.specialKind)
                        && !FinanceRules.DEBT_RECEIVABLE.equals(part.debtDirection))
                        || (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(part.specialKind)
                        && !FinanceRules.DEBT_PAYABLE.equals(part.debtDirection)))
                    throw new IllegalArgumentException("اتجاه الدَين لا يطابق حركة المال الخارجة");
            }
            sum = Math.addExact(sum, part.amountMinor);
        }
        if (sum != total) throw new IllegalArgumentException("مجموع الأجزاء لا يساوي مبلغ العملية");
        return sum;
    }
    public static boolean affectsOutgoingBalance(Models.SplitInput part, boolean ordinaryCategoryExcluded) {
        if (FinanceRules.SPECIAL_DEBT.equals(part.specialKind)) return false;
        if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(part.specialKind)) return true;
        return !ordinaryCategoryExcluded;
    }
}
