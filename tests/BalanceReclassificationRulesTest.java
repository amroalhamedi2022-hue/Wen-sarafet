import app.wensarafet.tracker.BalanceReclassificationRules;
import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.NonNegativeBalanceRules;

public final class BalanceReclassificationRulesTest {
    private static void equal(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": " + actual);
    }

    public static void main(String[] args) {
        long receivedPayment = 10_000L;
        long currentAfterSpending = 5_000L;
        long unchangedIncomeDelta = BalanceReclassificationRules.netDelta(
                FinanceRules.INCOME, receivedPayment, receivedPayment);
        equal(0L, unchangedIncomeDelta, "income to debt payment has no cash delta");
        equal(currentAfterSpending,
                NonNegativeBalanceRules.afterDelta(currentAfterSpending, unchangedIncomeDelta),
                "classification succeeds after part of received money was spent");

        equal(0L, BalanceReclassificationRules.netDelta(
                FinanceRules.EXPENSE, 10_000L, 10_000L), "unchanged expense");
        equal(3_000L, BalanceReclassificationRules.netDelta(
                FinanceRules.INCOME, 2_000L, 5_000L), "income correction");
        equal(-3_000L, BalanceReclassificationRules.netDelta(
                FinanceRules.EXPENSE, 2_000L, 5_000L), "expense correction");
        System.out.println("BalanceReclassificationRulesTest PASS: reclassification uses one final net delta");
    }
}
