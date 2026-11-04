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
        Map<Long,String> debtNames=new HashMap<>();
        debtNames.put(12L,"أحمد • إلي");
        Map<Long,String> debtCurrencies=new HashMap<>();
        debtCurrencies.put(12L,"ILS");
        Map<Long,String> people=new HashMap<>();
        people.put(9L,"أحمد");
        String debtBefore="debt=12, kind=OPEN, amount=5000, note=أصل الدين";
        String debtAfter="debt=12, kind=OPEN, amount=4500, note=تعديل المبلغ";
        check(AuditPresentation.title("DEBT_ADD","debt_movements",null,debtBefore).equals("تسجيل أصل الدين"),
                "Debt opening title");
        check(AuditPresentation.title("DEBT_EDIT","debt_movements",debtBefore,debtAfter).equals("تعديل أصل الدين"),
                "Debt edit title");
        contains(AuditPresentation.details("DEBT_EDIT",debtBefore,debtAfter,accounts,cats,currencies,
                debtNames,debtCurrencies,people),"كان 50.00 ILS، وأصبح 45.00 ILS");
        contains(AuditPresentation.details("DEBT_ADD",null,debtBefore,accounts,cats,currencies,
                debtNames,debtCurrencies,people),"سجل الدين: أحمد • إلي");
        String oldBalance="account=3, balance=90000";
        String newBalance="account=3, balance=87000";
        check(AuditPresentation.title("ACCOUNT_ADJUST","accounts",oldBalance,newBalance)
                        .equals("تسوية رصيد الحساب يدويًا"),"Manual balance audit title");
        contains(AuditPresentation.details("ACCOUNT_ADJUST",oldBalance,newBalance,accounts,cats,currencies),
                "كان 900.00 ILS، وأصبح 870.00 ILS");
        contains(AuditPresentation.details("CATEGORY_EDIT","طعام / 🍽","مواصلات / 🚕",accounts,cats,currencies),
                "كان طعام / 🍽، وأصبح مواصلات / 🚕");
        check("مشتريات, لأخوي".equals(AuditPresentation.fields("note=مشتريات, لأخوي, amount=1000")
                        .get("note")),"Comma-containing notes retained by audit parser");
        check(AuditPresentation.fields(null).isEmpty(),"null snapshot");
        System.out.println("AuditPresentationTest PASS: clear Arabic changes, typed amounts, historical labels");
    }
}
