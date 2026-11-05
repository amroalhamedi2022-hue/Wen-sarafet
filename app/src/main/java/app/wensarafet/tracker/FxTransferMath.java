package app.wensarafet.tracker;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Amounts in minor currency units; rate is DESTINATION units per 1 SOURCE unit. */
public final class FxTransferMath {
    private FxTransferMath() {}

    public static long destinationMinor(long sourceMinor, String rate) {
        if (sourceMinor <= 0) throw new IllegalArgumentException("مبلغ التحويل يجب أن يكون موجبًا");
        if (rate == null || rate.trim().isEmpty()) throw new IllegalArgumentException("اكتب سعر الصرف");
        try {
            BigDecimal decimal = new BigDecimal(rate.trim().replace(',', '.'));
            if (decimal.signum() <= 0 || decimal.scale() > 12 || decimal.compareTo(new BigDecimal("1000000")) > 0)
                throw new IllegalArgumentException("سعر الصرف غير صالح");
            long result = decimal.multiply(BigDecimal.valueOf(sourceMinor))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (result <= 0) throw new IllegalArgumentException("المبلغ المستلم صغير جدًا");
            return result;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("سعر الصرف أو المبلغ غير صالح", ex);
        }
    }

    public static long[] balancesAfter(long sourceBalance, long destinationBalance,
                                        long amountMinor, long destinationAmountMinor) {
        if(amountMinor<=0 || destinationAmountMinor<=0) throw new IllegalArgumentException("التحويل غير صالح");
        return new long[]{Math.subtractExact(sourceBalance, amountMinor),
                          Math.addExact(destinationBalance, destinationAmountMinor)};
    }
}
