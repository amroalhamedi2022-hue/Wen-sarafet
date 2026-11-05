package app.wensarafet.tracker;

/**
 * Safe Zone: financial source account may never be overdrawn by a NEW operation.
 * Historical negative balances are not silently rewritten or "corrected".
 * Call inside the same SQLite transaction before persisting money changes.
 */
public final class NonNegativeBalanceRules {
    public static final String INSUFFICIENT = "لا يوجد رصيد كافٍ لإتمام هذه العملية";
    private NonNegativeBalanceRules() {}

    public static long afterDelta(long previous, long delta) {
        final long result;
        try {
            result = Math.addExact(previous, delta);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("المبلغ أو الرصيد يتجاوز الحدود المسموحة", overflow);
        }
        requireAllowed(previous, delta, result);
        return result;
    }

    public static long debit(long available, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        // Avoid signed negation overflow, even if a caller is passed invalid data.
        if (amount > Math.max(available, 0L))
            throw new IllegalArgumentException(INSUFFICIENT);
        return afterDelta(available, -amount);
    }

    public static long credited(long previous, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        return afterDelta(previous, amount);
    }

    public static long requireNonNegative(long balance) {
        if (balance < 0) throw new IllegalArgumentException(INSUFFICIENT);
        return balance;
    }

    private static void requireAllowed(long previous, long delta, long result) {
        // Legacy debt data could contain a negative account: do not further
        // decrease it, but permit a positive corrective credit toward zero.
        if (result < 0 && !(previous < 0 && delta > 0 && result > previous))
            throw new IllegalArgumentException(INSUFFICIENT);
    }
}
