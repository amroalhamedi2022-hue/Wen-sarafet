import app.wensarafet.tracker.MonthlyBudgetHistory;
import java.util.Calendar;
import java.util.TimeZone;

public final class MonthlyBudgetHistoryTest {
    private static void ok(boolean condition) {
        if (!condition) throw new AssertionError("month budget history");
    }
    private static long instant(int year, int month, int day, int hour, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(year, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }
    public static void main(String[] args) {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        ok("2026-12".equals(MonthlyBudgetHistory.previousMonthKey(
                instant(2027,1,1,0,utc), utc)));
        ok("2024-02".equals(MonthlyBudgetHistory.previousMonthKey(
                instant(2024,3,1,0,utc), utc)));
        TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        long edge = instant(2027,1,1,0,utc);
        ok("2026-12".equals(MonthlyBudgetHistory.monthKey(edge,la)));
        ok("2026-11".equals(MonthlyBudgetHistory.previousMonthKey(edge,la)));
        // A missing snapshot on a legacy installation inherits its recurring cap.
        ok(MonthlyBudgetHistory.completedMonthCap(50000L,null)==50000L);
        // A newer month's edit must not rewrite an existing older snapshot.
        ok(MonthlyBudgetHistory.completedMonthCap(80000L,50000L)==50000L);
        // Zero is deliberately recorded when no previous cap existed.
        ok(MonthlyBudgetHistory.completedMonthCap(80000L,0L)==0L);
        try {
            MonthlyBudgetHistory.completedMonthCap(-1L,null);
            throw new AssertionError("negative cap accepted");
        } catch (IllegalArgumentException expected) {}
        System.out.println("MonthlyBudgetHistoryTest passed");
    }
}
