import app.wensarafet.tracker.SimpleUndoRules;
public class SimpleUndoRulesTest {
    static void check(boolean ok){if(!ok)throw new AssertionError("safe undo rule");}
    public static void main(String[]args){
        check(SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,true,false,false,false,false,0));
        check(SimpleUndoRules.isEligible("INCOME","MANUAL",200,true,false,false,false,false,0));
        check(!SimpleUndoRules.isEligible("TRANSFER","MANUAL",200,true,false,false,false,false,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","SMS",200,true,false,false,false,false,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,true,false,true,false,false,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,true,false,false,true,false,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,true,false,false,false,true,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,false,false,false,false,false,0));
        check(!SimpleUndoRules.isEligible("EXPENSE","MANUAL",200,true,false,false,false,false,1));
        check(SimpleUndoRules.restoredBalance(1000,"EXPENSE",200)==800);
        check(SimpleUndoRules.restoredBalance(1000,"INCOME",200)==1200);
        check(SimpleUndoRules.restoredBalance(1000,"EXPENSE",0)==1000);
        try{SimpleUndoRules.restoredBalance(Long.MAX_VALUE,"INCOME",1);throw new AssertionError("overflow allowed");}
        catch(ArithmeticException expected){}
        System.out.println("SimpleUndoRulesTest passed");
    }
}
