package app.wensarafet.tracker;

import java.util.List;

/**
 * UI-only calculation for the LAST category split entry.
 * Exact minor monetary units; does not post transactions or touch the ledger.
 * Existing SplitDebtRules.checkParts remains the save-time authority.
 */
public final class SplitRemainderRules {
    private SplitRemainderRules() {}

    public static long lastPart(long totalMinor, List<Long> precedingParts) {
        if (totalMinor <= 0) throw new IllegalArgumentException("أدخل المبلغ الكامل أولاً");
        if (precedingParts == null || precedingParts.isEmpty())
            throw new IllegalArgumentException("أدخل مبلغ الجزء الأول");
        long used = 0L;
        for (Long part : precedingParts) {
            if (part == null || part <= 0)
                throw new IllegalArgumentException("أدخل مبلغ كل جزء سابق");
            used = Math.addExact(used, part);
            if (used >= totalMinor)
                throw new IllegalArgumentException("مجموع الأجزاء الأولى يجب أن يقل عن المبلغ الكامل");
        }
        return Math.subtractExact(totalMinor, used);
    }
}
