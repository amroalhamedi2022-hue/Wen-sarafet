import app.wensarafet.tracker.CategoryNamingRules;
import app.wensarafet.tracker.FinanceRules;

public final class CategoryNamingRulesTest {
    private static void equal(String expected,String actual,String label) {
        if(!expected.equals(actual)) throw new AssertionError(label+": "+actual);
    }
    private static void reject(Runnable action,String label) {
        try { action.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError(label+": incorrectly accepted");
    }
    public static void main(String[] args) {
        equal("مواصلات يومية",CategoryNamingRules.cleanName("  مواصلات   يومية  "),"whitespace");
        equal("رسوم",CategoryNamingRules.validateNewCategory("رسوم",FinanceRules.EXPENSE),"new expense");
        equal("حوالة واردة",CategoryNamingRules.validateNewCategory("حوالة واردة",FinanceRules.INCOME),"new income");
        CategoryNamingRules.validateRename("أكل","EXPENSE",false,"طعام");
        CategoryNamingRules.validateRename("هدية","INCOME",false,"هدية من صديق");
        reject(()->CategoryNamingRules.cleanName(""),"blank");
        reject(()->CategoryNamingRules.cleanName("   "),"spaces");
        reject(()->CategoryNamingRules.cleanName("أ"),"single character");
        reject(()->CategoryNamingRules.cleanName("x".repeat(51)),"too long");
        reject(()->CategoryNamingRules.cleanName(null),"null");
        reject(()->CategoryNamingRules.validateNewCategory("دخل","TRANSFER"),"wrong type");
        reject(()->CategoryNamingRules.validateNewCategory("حوالة مستلمة",FinanceRules.INCOME),
                "duplicate canonical incoming-transfer category");
        reject(()->CategoryNamingRules.validateRename("حوالة مستلمة",FinanceRules.INCOME,false,"إيرادات"),
                "renaming financial receipt identity");
        reject(()->CategoryNamingRules.validateRename("دفع لأخوي",FinanceRules.EXPENSE,true,"مصروفات"),
                "excluded expense identity");
        reject(()->CategoryNamingRules.validateRename("راتب",FinanceRules.INCOME,false,"حوالة مستلمة"),
                "taking reserved receipt name");
        reject(()->CategoryNamingRules.validateRename("راتب","TRANSFER",false,"مال"),"rename wrong type");
        System.out.println("CategoryNamingRulesTest passed: normalization, reserved revenue, input and length.");
    }
}
