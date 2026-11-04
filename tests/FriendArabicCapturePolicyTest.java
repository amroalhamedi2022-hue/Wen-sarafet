import java.util.Arrays;
import java.util.List;
import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.TransactionParser;
import app.wensarafet.tracker.FriendArabicCapturePolicy;

public final class FriendArabicCapturePolicyTest {
    private static void check(boolean pass,String label) {
        if(!pass) throw new AssertionError(label);
    }
    private static final List<String> NAMES=Arrays.asList("test owner");
    private static void receipt(String text,long amount,long balance,String kind) {
        TransactionParser.Result result=FriendArabicCapturePolicy.parse("OTHER",text,NAMES);
        check(result.amountMinor!=null&&result.amountMinor==amount,"amount");
        check(result.balanceMinor!=null&&result.balanceMinor==balance,"balance");
        check("ILS".equals(result.currency),"currency");
        check(kind.equals(result.type),"direction");
        check(result.confidence<90,"Arabic must wait for manual review");
        check(FriendArabicCapturePolicy.isFinancialMessage(text),"unverified sender eligibility");
    }
    public static void main(String[] args) {
        receipt("تم تحويل مبلغ الى 0595000000 بقيمة ILS 300 بتاريخ 01-01-2026 05:21:16 PM. الرصيد المتوفر: ILS 843.50.",
                30000L,84350L,FinanceRules.EXPENSE);
        receipt("استلمت مبلغ ILS 300.00 من 00970590000000 رصيدك الحالي ILS 1143.50",
                30000L,114350L,FinanceRules.INCOME);
        receipt("تم تحويل ILS 5.00 الى شخص تجريبي رصيدك الحالي ILS 843.50",
                500L,84350L,FinanceRules.EXPENSE);
        receipt("تم دفع ILS 1.50 ل متجر تجريبي رصيدك الحالي ILS 27.50",
                150L,2750L,FinanceRules.EXPENSE);
        receipt("Test Person قام بتحويل مبلغ ILS 30.00 لحسابك بتاريخ 29-01-2026 05:44:31 PM. الرصيد المتوفر ILS 59.00.",
                3000L,5900L,FinanceRules.INCOME);
        String otp="Code: 99999 لحركة:p2m لقيمة أقصاها: 1.50ILS الرمز يؤكد ملكيتك للحساب";
        check(FriendArabicCapturePolicy.isSecurityMessage(otp),"OTP blocked");
        check(!FriendArabicCapturePolicy.isFinancialMessage(otp),"OTP not captured");
        TransactionParser.Result security=FriendArabicCapturePolicy.parse("OTHER",otp,NAMES);
        check(security.type==null&&security.amountMinor==null,"OTP no parsed cash");
        check(!FriendArabicCapturePolicy.isFinancialMessage("الرصيد الحالي ILS 800.00"),
                "balance only must not become financial movement");
        check(!FriendArabicCapturePolicy.isFinancialMessage("وصلتني رسالة تحويل اليوم"),
                "movement without amount and currency ignored");
        // English path is untouched by candidate-only Arabic enhancement.
        String english="John has transferred ILS 75.00 to your account. Current balance ILS 175.00.";
        TransactionParser.Result before=TransactionParser.parse("JAWAL",english,NAMES);
        TransactionParser.Result after=FriendArabicCapturePolicy.parse("JAWAL",english,NAMES);
        check(before.confidence==after.confidence,"English confidence unchanged");
        check(before.type==null?after.type==null:before.type.equals(after.type),"English direction unchanged");
        check(before.amountMinor==null?after.amountMinor==null:before.amountMinor.equals(after.amountMinor),
                "English amount unchanged");
        check(before.balanceMinor==null?after.balanceMinor==null:before.balanceMinor.equals(after.balanceMinor),
                "English balance unchanged");
        System.out.println("FriendArabicCapturePolicyTest PASS: five anonymized messages, OTP guard, conservative confidence, English parity");
    }
}
