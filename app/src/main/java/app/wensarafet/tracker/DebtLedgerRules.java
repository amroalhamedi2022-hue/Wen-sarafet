package app.wensarafet.tracker;

import java.util.List;

public final class DebtLedgerRules {
    private DebtLedgerRules() {}

    /**
     * Interprets a real money movement against the selected debt direction.
     * Money received from somebody who owes me, or money paid to somebody I
     * owe, is a repayment. The opposite combinations increase the debt.
     */
    public static String movementKind(String transactionType, String direction) {
        boolean payment = (FinanceRules.DEBT_RECEIVABLE.equals(direction) && FinanceRules.INCOME.equals(transactionType)) ||
                (FinanceRules.DEBT_PAYABLE.equals(direction) && FinanceRules.EXPENSE.equals(transactionType));
        return payment ? FinanceRules.SPECIAL_DEBT_PAYMENT : FinanceRules.SPECIAL_DEBT;
    }

    public static long remaining(long original, long paid) {
        if (original < 0 || paid < 0) throw new IllegalArgumentException("قيمة دين غير صالحة");
        return Math.max(0L, original - paid);
    }

    public static long[] allocatePayment(long payment, List<Long> openRemainders) {
        if (payment <= 0) throw new IllegalArgumentException("مبلغ السداد غير صالح");
        if (openRemainders == null || openRemainders.isEmpty()) throw new IllegalArgumentException("لا يوجد دين نشط");
        long available = 0;
        for (Long value : openRemainders) {
            if (value == null || value < 0) throw new IllegalArgumentException("رصيد دين غير صالح");
            available += value;
        }
        if (available < payment) throw new IllegalArgumentException("مبلغ السداد أكبر من المتبقي");
        long[] result = new long[openRemainders.size()]; long left = payment;
        for (int i = 0; i < result.length && left > 0; i++) {
            result[i] = Math.min(left, openRemainders.get(i)); left -= result[i];
        }
        return result;
    }
}
