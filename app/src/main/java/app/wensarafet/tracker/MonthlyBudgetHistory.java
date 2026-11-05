package app.wensarafet.tracker;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Historical month caps are snapshots: editing the current cap must never rewrite
 * a completed month's cap. An older installation may only have the carried cap.
 */
public final class MonthlyBudgetHistory {
    private MonthlyBudgetHistory() {}

    public static String monthKey(long epochMillis, TimeZone zone) {
        if (zone == null) throw new IllegalArgumentException("missing timezone");
        Calendar month = Calendar.getInstance(zone, Locale.ROOT);
        month.setTimeInMillis(epochMillis);
        return String.format(Locale.ROOT, "%04d-%02d",
                month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1);
    }

    public static String previousMonthKey(long epochMillis, TimeZone zone) {
        if (zone == null) throw new IllegalArgumentException("missing timezone");
        Calendar month = Calendar.getInstance(zone, Locale.ROOT);
        month.setTimeInMillis(epochMillis);
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.add(Calendar.MONTH, -1);
        return monthKey(month.getTimeInMillis(), zone);
    }

    /**
     * Prefer a recorded cap, including an explicit zero (not configured).
     * For legacy periods without snapshots use the existing rolling monthly cap.
     */
    public static long completedMonthCap(long rollingCapMinor, Long snapshotCapMinor) {
        if (rollingCapMinor < 0 || (snapshotCapMinor != null && snapshotCapMinor < 0))
            throw new IllegalArgumentException("invalid monthly cap");
        return snapshotCapMinor == null ? rollingCapMinor : snapshotCapMinor;
    }
}
