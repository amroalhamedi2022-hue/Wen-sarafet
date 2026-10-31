import app.wensarafet.tracker.NonNegativeBalanceRules;

public final class NonNegativeBalanceRulesTest {
    static void equal(long expected,long actual,String name) {
        if(expected!=actual)throw new AssertionError(name+" expected "+expected+" got "+actual);
    }
    static void rejects(Runnable runnable,String name) {
        try { runnable.run(); } catch(IllegalArgumentException expected) {
            if(expected.getMessage()==null || expected.getMessage().isEmpty())
                throw new AssertionError("Missing user-visible financial error: "+name);
            return;
        }
        throw new AssertionError("Negative balance allowed: "+name);
    }
    public static void main(String[] args) {
        equal(1000,NonNegativeBalanceRules.afterDelta(2000,-1000),"ordinary expense");
        equal(0,NonNegativeBalanceRules.debit(2000,2000),"exact balance allowed");
        equal(3000,NonNegativeBalanceRules.credited(2000,1000),"income");
        equal(500,NonNegativeBalanceRules.afterDelta(0,500),"credit zero");
        equal(0,NonNegativeBalanceRules.requireNonNegative(0),"zero balance valid");
        equal(100,NonNegativeBalanceRules.requireNonNegative(100),"positive valid");
        rejects(()->NonNegativeBalanceRules.debit(999,1000),"insufficient funds");
        rejects(()->NonNegativeBalanceRules.debit(0,1),"empty account");
        rejects(()->NonNegativeBalanceRules.debit(-100,1),"historical negative account cannot spend more");
        rejects(()->NonNegativeBalanceRules.debit(100,0),"zero amount");
        rejects(()->NonNegativeBalanceRules.debit(100,-50),"negative amount");
        rejects(()->NonNegativeBalanceRules.afterDelta(99,-100),"edit reversals cannot overdraw");
        rejects(()->NonNegativeBalanceRules.afterDelta(Long.MAX_VALUE,1),"overflow");
        rejects(()->NonNegativeBalanceRules.afterDelta(Long.MIN_VALUE,-1),"underflow");
        rejects(()->NonNegativeBalanceRules.requireNonNegative(-1),"negative manual balance");
        equal(-50,NonNegativeBalanceRules.afterDelta(-100,50),"legacy negative correction allowed");
        equal(0,NonNegativeBalanceRules.afterDelta(-100,100),"correct old negative to zero");
        rejects(()->NonNegativeBalanceRules.afterDelta(-100,-1),"legacy debt must not worsen");
        System.out.println("NonNegativeBalanceRulesTest PASS: sufficient funds, zero, legacy, credit, delta, overflows");
    }
}
