package app.wensarafet.tracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class Money {
    private Money() {}

    public static Long parseMinor(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) return null;
            BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
            return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String format(long minor, String currency) {
        BigDecimal value = BigDecimal.valueOf(minor, 2);
        return new DecimalFormat("#,##0.00").format(value) + " " + currency;
    }
}
