import app.wensarafet.tracker.ArabicSmsParserPilot;
import app.wensarafet.tracker.FinanceRules;

public final class ArabicSmsParserPilotTest {
    private static void ok(boolean yes, String description) {
        if (!yes) throw new AssertionError(description);
    }
    private static void amount(long wanted, String message, String type) {
        ArabicSmsParserPilot.Candidate candidate=ArabicSmsParserPilot.analyse(message);
        ok(candidate.requiresReview, "pilot must never approve automatic posting");
        ok(candidate.amountMinor != null && candidate.amountMinor == wanted, "amount: "+message);
        ok("ILS".equals(candidate.currency), "ILS currency: "+message);
        ok(type.equals(candidate.type), "direction: "+message);
    }
    public static void main(String[] args) {
        amount(12345L,"تم خصم مبلغ ١٢٣٫٤٥ شيكل من حسابك. الرصيد المتاح ٨٧٦٫٥٥ شيكل",
                FinanceRules.EXPENSE);
        amount(5000L,"حوالة واردة بمبلغ ٥٠٫٠٠ شيكل إلى محفظتك",
                FinanceRules.INCOME);
        amount(25000L,"تم إيداع ٢٥٠ شيكل في حسابك",
                FinanceRules.INCOME);
        amount(100000L,"حوالة صادرة بقيمة ١٬٠٠٠ شيكل",
                FinanceRules.EXPENSE);
        ArabicSmsParserPilot.Candidate balanceOnly=ArabicSmsParserPilot.analyse(
                "الرصيد الحالي ٢٥٠ شيكل");
        ok(balanceOnly.amountMinor == null, "balance-only SMS must not create a transaction");
        ok(balanceOnly.balanceMinor != null && balanceOnly.balanceMinor==25000L,
                "balance-only read-only hint");
        ok(balanceOnly.requiresReview,"balance SMS must stay review-only");

        ArabicSmsParserPilot.Candidate unclear=ArabicSmsParserPilot.analyse(
                "تم تحويل ٥٠ شيكل إلى المحفظة");
        ok(unclear.amountMinor==5000L && unclear.type==null && unclear.requiresReview,
                "ambiguous transfer direction must require review");
        ArabicSmsParserPilot.Candidate conflict=ArabicSmsParserPilot.analyse(
                "تم خصم مبلغ ٥٠ شيكل وتم إيداع مبلغ ٥٠ شيكل");
        ok(conflict.type==null && conflict.requiresReview,
                "conflicting credit and debit cues must not choose direction");

        ArabicSmsParserPilot.Candidate verification=ArabicSmsParserPilot.analyse(
                "رمز التحقق هو ١٢٣٤٥٦");
        ok(verification.verification && verification.amountMinor==null && verification.type==null,
                "OTP must never be treated as a financial transaction");
        ArabicSmsParserPilot.Candidate garbage=ArabicSmsParserPilot.analyse(null);
        ok(garbage.amountMinor==null && garbage.requiresReview,"null SMS");
        System.out.println("ArabicSmsParserPilotTest PASS: Arabic/Persian digits, currencies, balance separation, ambiguity and OTP; always review-only");
    }
}
