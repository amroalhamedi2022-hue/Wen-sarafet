package app.wensarafet.tracker;

import java.util.List;

public final class FinanceRules {
    public static final String EXPENSE = "EXPENSE";
    public static final String INCOME = "INCOME";
    public static final String TRANSFER = "TRANSFER";
    public static final String SPECIAL_DEBT = "DEBT";
    public static final String SPECIAL_DEBT_PAYMENT = "DEBT_PAYMENT";
    public static final String DEBT_RECEIVABLE = "RECEIVABLE";
    public static final String DEBT_PAYABLE = "PAYABLE";

    private FinanceRules() {}

    public static long[] balancesAfter(String type, long amount, long source, Long destination) {
        if (amount <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        switch (type) {
            case EXPENSE: return new long[]{Math.subtractExact(source, amount)};
            case INCOME: return new long[]{Math.addExact(source, amount)};
            case TRANSFER:
                if (destination == null) throw new IllegalArgumentException("الحساب المستلم مطلوب");
                return new long[]{Math.subtractExact(source, amount), Math.addExact(destination, amount)};
            default: throw new IllegalArgumentException("نوع عملية غير معروف");
        }
    }

    public static boolean splitsMatch(long total, List<Long> parts) {
        if (total <= 0 || parts == null || parts.isEmpty()) return false;
        long sum = 0;
        for (Long part : parts) {
            if (part == null || part <= 0) return false;
            try { sum = Math.addExact(sum, part); }
            catch (ArithmeticException overflow) { return false; }
        }
        return sum == total;
    }

    public static long expenseBalanceImpact(long total, boolean wholeExcluded, List<Long> includedSplitParts) {
        if (total <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        if (includedSplitParts == null || includedSplitParts.isEmpty()) return wholeExcluded ? 0L : total;
        long included = 0L;
        for (Long part : includedSplitParts) {
            if (part == null || part < 0) throw new IllegalArgumentException("جزء الرصيد غير صالح");
            included = Math.addExact(included, part);
        }
        if (included > total) throw new IllegalArgumentException("أجزاء الرصيد تتجاوز مبلغ العملية");
        return included;
    }
}
