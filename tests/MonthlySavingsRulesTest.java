import app.wensarafet.tracker.MonthlySavingsRules;
public class MonthlySavingsRulesTest {
  private static void ok(boolean yes){if(!yes)throw new AssertionError("monthly cap");}
  public static void main(String[]args){
    MonthlySavingsRules.State inProgress=MonthlySavingsRules.calculate(50000,12000,false);
    ok(inProgress.remainderMinor==38000 && inProgress.savedMinor==0 && !inProgress.completed);
    MonthlySavingsRules.State completed=MonthlySavingsRules.calculate(50000,12000,true);
    ok(completed.savedMinor==38000 && completed.completed);
    MonthlySavingsRules.State exceeded=MonthlySavingsRules.calculate(50000,60000,true);
    ok(exceeded.exceeded && exceeded.overspendMinor==10000 && exceeded.savedMinor==0);
    ok(!MonthlySavingsRules.calculate(0,0,true).configured);
    System.out.println("MonthlySavingsRulesTest passed");
  }
}
