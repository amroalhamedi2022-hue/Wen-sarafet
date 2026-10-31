import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.SimpleTransactionEditRules;

public final class SimpleTransactionEditRulesTest {
    static void eq(long want,long got,String label) {
        if(want!=got)throw new AssertionError(label+": "+want+" != "+got);
    }
    static void rejects(Runnable action,String label) {
        try { action.run(); throw new AssertionError(label+": accepted"); }
        catch(IllegalArgumentException expected) {}
        catch(ArithmeticException expected) {}
    }
    public static void main(String[] args) {
        eq(-350,SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,150,500,true,false),"increase expense");
        eq(200,SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,500,300,true,false),"decrease expense");
        eq(350,SimpleTransactionEditRules.balanceDelta(FinanceRules.INCOME,150,500,true,false),"increase income");
        eq(-200,SimpleTransactionEditRules.balanceDelta(FinanceRules.INCOME,500,300,true,false),"decrease income");
        eq(0,SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,150,500,false,false),"record-only unchanged");
        eq(0,SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,150,500,true,true),"excluded expense unchanged");
        eq(350,SimpleTransactionEditRules.balanceDelta(FinanceRules.INCOME,150,500,true,true),"income never excluded");
        rejects(()->SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,0,1,true,false),"zero original");
        rejects(()->SimpleTransactionEditRules.balanceDelta(FinanceRules.EXPENSE,1,0,true,false),"zero edited");
        rejects(()->SimpleTransactionEditRules.balanceDelta(FinanceRules.TRANSFER,1,2,true,false),"transfer");
        // No silent wraparound, even with otherwise-valid positive transaction amounts.
        // Large balance overflow is additionally guarded by FinanceDbHelper.adjustBalance.
        eq(Long.MAX_VALUE-1,SimpleTransactionEditRules.balanceDelta(FinanceRules.INCOME,1,Long.MAX_VALUE,true,false),"large valid");
        System.out.println("SimpleTransactionEditRulesTest passed: expense, income, excluded, record-only, limits.");
    }
}
