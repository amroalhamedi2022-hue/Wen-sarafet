import app.wensarafet.tracker.Money;
import app.wensarafet.tracker.SplitRemainderRules;
import java.util.Arrays;
import java.util.Collections;

public final class SplitRemainderRulesTest {
    static void eq(long actual, long expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }
    static void reject(Runnable action, String label) {
        boolean rejected = false;
        try { action.run(); }
        catch (IllegalArgumentException | ArithmeticException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("Invalid remainder accepted: " + label);
    }
    public static void main(String[] args) {
        eq(SplitRemainderRules.lastPart(10000L, Arrays.asList(4000L)), 6000L, "two parts");
        eq(SplitRemainderRules.lastPart(10000L, Arrays.asList(4000L, 2500L)), 3500L, "three parts");
        eq(SplitRemainderRules.lastPart(10000L, Arrays.asList(5000L)), 5000L, "live edit of first part");
        eq(SplitRemainderRules.lastPart(10000L, Arrays.asList(4000L, 2500L, 1000L)), 2500L, "four parts");
        eq(SplitRemainderRules.lastPart(10001L, Arrays.asList(4002L)), 5999L, "exact pennies");
        eq(SplitRemainderRules.lastPart(Money.parseMinor("١٠٠٫٠١"), Arrays.asList(Money.parseMinor("40.02"))), 5999L, "Arabic digits and exact decimal");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList(10000L)), "no remaining");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList(11000L)), "exceeded total");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList(4000L, 6000L)), "no last positive");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList(0L)), "zero preceding");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList(-100L)), "negative preceding");
        reject(() -> SplitRemainderRules.lastPart(10000L, Arrays.asList((Long) null)), "blank preceding");
        reject(() -> SplitRemainderRules.lastPart(10000L, Collections.emptyList()), "missing preceding");
        reject(() -> SplitRemainderRules.lastPart(0L, Arrays.asList(1L)), "zero total");
        reject(() -> SplitRemainderRules.lastPart(Long.MAX_VALUE, Arrays.asList(Long.MAX_VALUE - 1, 2L)), "overflow");
        System.out.println("SplitRemainderRulesTest: 2-4 parts, exact cents, Arabic digits, edits, overage, zero, overflow PASS");
    }
}
