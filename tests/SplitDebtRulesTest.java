import app.wensarafet.tracker.*;
import java.util.*;

public class SplitDebtRulesTest {
  static void ok(boolean yes, String why) { if (!yes) throw new AssertionError(why); }
  static void reject(Runnable r) { try {r.run(); throw new AssertionError("invalid input accepted");}catch (IllegalArgumentException | ArithmeticException expected){} }
  public static void main(String[] args) {
    Models.SplitInput ordinary = new Models.SplitInput(10, 12000);
    Models.SplitInput open = Models.SplitInput.debt(5000, FinanceRules.SPECIAL_DEBT, 7, FinanceRules.DEBT_RECEIVABLE);
    Models.SplitInput pay = Models.SplitInput.debt(3000, FinanceRules.SPECIAL_DEBT_PAYMENT, 8, FinanceRules.DEBT_PAYABLE);
    List<Models.SplitInput> parts = Arrays.asList(ordinary, open, pay);
    ok(SplitDebtRules.checkParts(20000, parts)==20000, "sum exactly in minor units");
    ok(SplitDebtRules.affectsOutgoingBalance(ordinary,false), "normal expense decreases account");
    ok(!SplitDebtRules.affectsOutgoingBalance(open,false), "debt opening is reference only");
    ok(SplitDebtRules.affectsOutgoingBalance(pay,false), "repayment decreases account");
    ok(!SplitDebtRules.affectsOutgoingBalance(ordinary,true), "excluded category unchanged");
    long impact=0;for(Models.SplitInput p:parts)if(SplitDebtRules.affectsOutgoingBalance(p,false))impact+=p.amountMinor;
    ok(impact==15000, "single account debit not full 20000 and not duplicated");
    reject(()->SplitDebtRules.checkParts(19999, parts));
    reject(()->SplitDebtRules.checkParts(20001, parts));
    reject(()->SplitDebtRules.checkParts(20000, Arrays.asList(ordinary)));
    reject(()->SplitDebtRules.checkParts(20000, Arrays.asList(new Models.SplitInput(0,12000),open,pay)));
    reject(()->SplitDebtRules.checkParts(20000, Arrays.asList(ordinary,Models.SplitInput.debt(5000,FinanceRules.SPECIAL_DEBT,7,FinanceRules.DEBT_PAYABLE),pay)));
    reject(()->SplitDebtRules.checkParts(20000, Arrays.asList(ordinary,Models.SplitInput.debt(5000,FinanceRules.SPECIAL_DEBT_PAYMENT,7,FinanceRules.DEBT_RECEIVABLE),pay)));
    reject(()->SplitDebtRules.checkParts(Long.MAX_VALUE,Arrays.asList(new Models.SplitInput(1,Long.MAX_VALUE),new Models.SplitInput(1,1))));
    ok(FinanceRules.SPECIAL_DEBT.equals(DebtLedgerRules.movementKind(FinanceRules.EXPENSE,FinanceRules.DEBT_RECEIVABLE)),"expense receivable opens debt");
    ok(FinanceRules.SPECIAL_DEBT_PAYMENT.equals(DebtLedgerRules.movementKind(FinanceRules.EXPENSE,FinanceRules.DEBT_PAYABLE)),"expense payable repays debt");
    ok(FinanceRules.SPECIAL_DEBT_PAYMENT.equals(DebtLedgerRules.movementKind(FinanceRules.INCOME,FinanceRules.DEBT_RECEIVABLE)),"income receivable repays debt");
    ok(FinanceRules.SPECIAL_DEBT.equals(DebtLedgerRules.movementKind(FinanceRules.INCOME,FinanceRules.DEBT_PAYABLE)),"income payable opens debt");
    System.out.println("SplitDebtRulesTest: mixed categories, direction, balances, precision, overflow passed");
  }
}
