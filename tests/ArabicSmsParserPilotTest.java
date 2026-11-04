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
    private static void receipt(String label,String message,long amountMinor,long balanceMinor,
                                String expectedType) {
        ArabicSmsParserPilot.Candidate result=ArabicSmsParserPilot.analyse(message);
        ok(result.requiresReview,label+": must stay read-only and require review");
        ok(!result.verification,label+": not an OTP");
        ok(result.amountMinor!=null && result.amountMinor==amountMinor,
                label+": amount expected "+amountMinor+", got "+result.amountMinor);
        ok(result.balanceMinor!=null && result.balanceMinor==balanceMinor,
                label+": balance expected "+balanceMinor+", got "+result.balanceMinor);
        ok("ILS".equals(result.currency),label+": currency expected ILS");
        ok(expectedType.equals(result.type),label+": type expected "+expectedType+", got "+result.type);
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
        // Redacted fixtures with the same STRUCTURE as the six shared examples.
        // All personal names, phone numbers, times and security codes are fictitious.
        String debitToPhone = "تم تحويل مبلغ الى 0595000000 بقيمة ILS 300 بتاريخ " +
                "01-01-2026 05:21:16 PM. الرصيد المتوفر: ILS 843.50.";
        String creditFromPhone = "استلمت مبلغ ILS 300.00 من 00970590000000 " +
                "رصيدك الحالي ILS 1143.50";
        String debitToName = "تم تحويل ILS 5.00 الى شخص تجريبي " +
                "رصيدك الحالي ILS 843.50";
        String merchantPayment = "تم دفع ILS 1.50 ل متجر تجريبي " +
                "رصيدك الحالي ILS 27.50";
        String receivedPerson = "Test Person قام بتحويل مبلغ ILS 30.00 لحسابك بتاريخ " +
                "29-01-2026 05:44:31 PM. الرصيد المتوفر ILS 59.00.";
        String confirmationNotPayment = "Code: 99999 لحركة:p2m " +
                "لقيمة أقصاها: 1.50ILS الرمز يؤكد ملكيتك للحساب";

        receipt("outgoing to phone",debitToPhone,30000,84350,FinanceRules.EXPENSE);
        receipt("incoming from phone",creditFromPhone,30000,114350,FinanceRules.INCOME);
        receipt("outgoing to name",debitToName,500,84350,FinanceRules.EXPENSE);
        receipt("merchant payment",merchantPayment,150,2750,FinanceRules.EXPENSE);
        receipt("incoming from named person",receivedPerson,3000,5900,FinanceRules.INCOME);

        ArabicSmsParserPilot.Candidate code=ArabicSmsParserPilot.analyse(confirmationNotPayment);
        ok(code.verification,"mixed English/Arabic Code notification must be verification");
        ok(code.requiresReview && code.amountMinor==null && code.type==null,
                "OTP 1.50ILS maximum is NOT a 1.50 payment");
        ok(code.balanceMinor==null,"confirmation code has no real account balance");
        ArabicSmsParserPilot.Candidate repeated=ArabicSmsParserPilot.analyse(debitToPhone);
        ok(repeated.amountMinor==30000 && repeated.balanceMinor==84350 &&
                repeated.type.equals(FinanceRules.EXPENSE) && repeated.requiresReview,
                "repeated fixture must still be a pure, non-posting candidate");

        ArabicSmsParserPilot.Candidate twoAmounts=ArabicSmsParserPilot.analyse(
                "تم دفع ILS 20.00 مع رسوم ILS 1.00 الرصيد الحالي ILS 300.00");
        ok(twoAmounts.amountMinor==null && twoAmounts.type==null && twoAmounts.requiresReview,
                "amount plus fee cannot be silently confused with one transaction");
        ArabicSmsParserPilot.Candidate uncertainWallet=ArabicSmsParserPilot.analyse(
                "تم تحويل ٥٠ شيكل إلى المحفظة");
        ok(uncertainWallet.amountMinor==5000 && uncertainWallet.type==null,
                "transfer to ambiguous own wallet should require review");
        ArabicSmsParserPilot.Candidate garbage=ArabicSmsParserPilot.analyse(null);
        ok(garbage.amountMinor==null && garbage.requiresReview,"null SMS");
        System.out.println("ArabicSmsParserPilotTest PASS: five anonymized bilingual real-world templates, OTP safe, balance/amount split, repeat, fee ambiguity; review-only");
    }
}
