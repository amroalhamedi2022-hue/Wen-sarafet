import app.wensarafet.tracker.AuditPresentation;
import java.util.*;

public final class AuditPresentationTest {
    private static void check(boolean condition,String why) {
        if(!condition)throw new AssertionError(why);
    }
    private static void contains(List<String> rows,String expected) {
        if(rows.stream().noneMatch(s->s.contains(expected)))
            throw new AssertionError("Missing '"+expected+"' from "+rows);
    }
    public static void main(String[] args) {
        Map<Long,String> accounts=new HashMap<>();
        accounts.put(3L,"بنك فلسطين");
        Map<Long,String> currencies=new HashMap<>();
        currencies.put(3L,"ILS");
        Map<Long,String> cats=new HashMap<>();
        cats.put(23L,"أكل وشرب");cats.put(24L,"مواصلات");
        String a="type=EXPENSE, amount=800, category=23, account=3";
        String b="type=EXPENSE, amount=900, category=24, account=3";
        check(AuditPresentation.title("ADD","transactions",null,a).equals("إضافة مصروف"),
                "Arabic title ADD");
        check(AuditPresentation.title("EDIT","transactions",a,b).equals("تعديل مصروف"),
                "Arabic title EDIT");
        check(AuditPresentation.title("DELETE","transactions",a,null).equals("حذف مصروف"),
                "Arabic title DELETE");
        check(AuditPresentation.title("RESTORE","transactions",null,b).startsWith("استرجاع"),
                "Arabic restore");
        check(AuditPresentation.title("UNDO","transactions",a,b).startsWith("تراجع"),
                "Arabic undo");
        List<String> changed=AuditPresentation.details("EDIT",a,b,accounts,cats,currencies);
        contains(changed,"كان 8.00 ILS، وأصبح 9.00 ILS");
        contains(changed,"كان أكل وشرب، وأصبح مواصلات");
        check(changed.stream().noneMatch(x->x.contains("type=EXPENSE")),
                "No raw database coding exposed");
        List<String> created=AuditPresentation.details("ADD",null,a,accounts,cats,currencies);
        contains(created,"المبلغ: 8.00 ILS");
        contains(created,"الحساب: بنك فلسطين");
        List<String> deleted=AuditPresentation.details("DELETE",a,null,accounts,cats,currencies);
        contains(deleted,"المبلغ: 8.00 ILS");
        check(AuditPresentation.details("EDIT","garbage",null,accounts,cats,currencies).size()==1,
                "Legacy corrupted snapshots must not crash");
        check(AuditPresentation.fields(null).isEmpty(),"null snapshot");
        System.out.println("AuditPresentationTest PASS: clear Arabic changes, typed amounts, historical labels");
    }
}
