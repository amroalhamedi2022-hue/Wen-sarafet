import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.Money;
import app.wensarafet.tracker.UssdCodes;
import app.wensarafet.tracker.TransactionParser;
import app.wensarafet.tracker.DebtLedgerRules;

import java.util.Arrays;

public final class FinanceCoreTest {
    public static void main(String[] args) {
        equalsLong(1_000L, Money.parseMinor("10"), "whole amount");
        equalsLong(4_920L, Money.parseMinor("49.2"), "single decimal");
        equalsLong(117_569L, Money.parseMinor("1175.69"), "two decimals");
        equalsLong(1_350L, Money.parseMinor("13,50"), "comma decimal");
        if (Money.parseMinor("abc") != null) fail("invalid amount accepted");

        long[] expense = FinanceRules.balancesAfter(FinanceRules.EXPENSE, 500, 2_000, null);
        equalsLong(1_500L, expense[0], "expense balance");
        long[] income = FinanceRules.balancesAfter(FinanceRules.INCOME, 500, 2_000, null);
        equalsLong(2_500L, income[0], "income balance");
        long[] transfer = FinanceRules.balancesAfter(FinanceRules.TRANSFER, 500, 2_000, 800L);
        equalsLong(1_500L, transfer[0], "transfer source");
        equalsLong(1_300L, transfer[1], "transfer destination");
        equalsLong(2_800L, transfer[0] + transfer[1], "transfer combined balance");
        try { FinanceRules.balancesAfter(FinanceRules.INCOME, 1L, Long.MAX_VALUE, null); fail("income overflow accepted"); }
        catch (ArithmeticException expected) {}
        try { FinanceRules.balancesAfter(FinanceRules.EXPENSE, 1L, Long.MIN_VALUE, null); fail("expense overflow accepted"); }
        catch (ArithmeticException expected) {}

        if (!FinanceRules.splitsMatch(10_000L, Arrays.asList(6_000L, 3_000L, 1_000L))) fail("valid split rejected");
        if (FinanceRules.splitsMatch(10_000L, Arrays.asList(6_000L, 3_000L))) fail("invalid split accepted");
        if (FinanceRules.splitsMatch(Long.MAX_VALUE, Arrays.asList(Long.MAX_VALUE, 1L))) fail("split overflow accepted");
        equalsLong(0L, FinanceRules.expenseBalanceImpact(10_000L, true, null), "brother-only expense balance impact");
        equalsLong(10_000L, FinanceRules.expenseBalanceImpact(10_000L, false, null), "normal expense balance impact");
        equalsLong(6_000L, FinanceRules.expenseBalanceImpact(10_000L, false, Arrays.asList(6_000L)), "split brother expense balance impact");
        equalsLong(6_000L, DebtLedgerRules.remaining(10_000L, 4_000L), "partial debt remaining");
        long[] allocations = DebtLedgerRules.allocatePayment(7_000L, Arrays.asList(5_000L, 4_000L));
        equalsLong(5_000L, allocations[0], "debt payment first balance");
        equalsLong(2_000L, allocations[1], "debt payment second balance");
        try { DebtLedgerRules.allocatePayment(10_000L, Arrays.asList(5_000L, 4_000L)); fail("overpayment accepted"); }
        catch (IllegalArgumentException expected) {}
        equalsText(FinanceRules.SPECIAL_DEBT_PAYMENT,
                DebtLedgerRules.movementKind(FinanceRules.INCOME, FinanceRules.DEBT_RECEIVABLE),
                "incoming receivable repayment");
        equalsText(FinanceRules.SPECIAL_DEBT,
                DebtLedgerRules.movementKind(FinanceRules.EXPENSE, FinanceRules.DEBT_RECEIVABLE),
                "outgoing receivable opening");
        equalsText(FinanceRules.SPECIAL_DEBT_PAYMENT,
                DebtLedgerRules.movementKind(FinanceRules.EXPENSE, FinanceRules.DEBT_PAYABLE),
                "outgoing payable repayment");
        equalsText(FinanceRules.SPECIAL_DEBT,
                DebtLedgerRules.movementKind(FinanceRules.INCOME, FinanceRules.DEBT_PAYABLE),
                "incoming payable opening");
        try { DebtLedgerRules.allocatePayment(1L, Arrays.asList(Long.MAX_VALUE, 1L)); fail("debt total overflow accepted"); }
        catch (ArithmeticException expected) {}

        equalsText("*110*1*1234*0591234567*25.5*1#", UssdCodes.jawwalFriend("1234", "0591234567", "25.5"), "jawwal friend code");
        equalsText("*110*2*1234*0591234567*25.5*1#", UssdCodes.jawwalMerchant("1234", "0591234567", "25.5"), "jawwal merchant code");
        equalsText("*267*1234#", UssdCodes.bankOfPalestine("1234"), "bank code");
        equalsText("*370*1*1*0591234567*25.5#", UssdCodes.palPay("0591234567", "25.5"), "palpay code");

        parsed("BOP", "حوالة صادرة من حسابك بمبلغ 1200 شيكل. رصيدكم المتوفر هو 23.87", 120_000L, "ILS", FinanceRules.EXPENSE);
        parsed("BOP", "تسديد دفعة فاتورة: الأقساط الجامعية بمبلغ 1175.69 USD", 117_569L, "USD", FinanceRules.EXPENSE);
        parsed("BOP", "شحن: شحن رصيد جوال بمبلغ 10.00 ILS", 1_000L, "ILS", FinanceRules.EXPENSE);
        parsed("JAWAL", "A Money Transfer transaction with an amount of: ILS 13.00, from: 00970592896349 is done successfully. Your current balance is: ILS 148.89", 1_300L, "ILS", FinanceRules.INCOME);
        parsed("JAWAL", "Your Pay to merchant transaction with an amount of: ILS 5.00 for merchant: Al Abdessa Supermarket for Groceries is done successfully.", 500L, "ILS", FinanceRules.EXPENSE);
        parsed("JAWAL", "Your Money Transfer transaction with an amount of: ILS 35.00, to customer: 00970595786268 is done successfully.", 3_500L, "ILS", FinanceRules.EXPENSE);
        parsed("JAWAL", "amro ibrahim has transferred an amount of ILS 150.00 to your account. Available balance is: ILS 150.89.", 15_000L, "ILS", FinanceRules.INCOME);
        TransactionParser.Result ambiguous = TransactionParser.parse("BOP", "تحويل دفع لصديق: محمد أحمد، بمبلغ 120.00 شيكل");
        if (ambiguous.type != null) fail("ambiguous friend transfer was classified");
        TransactionParser.Result withBalance = TransactionParser.parse("BOP", "حوالة صادرة من حسابك بمبلغ 49.2 دولار. رصيدكم المتوفر هو 1175.71");
        equalsLong(117_571L, withBalance.balanceMinor, "available balance");
        TransactionParser.Result mobileTopUp = TransactionParser.parse("BOP", "شحن: شحن رصيد جوال بمبلغ 5.93 USD");
        equalsLong(593L, mobileTopUp.amountMinor, "top-up amount");
        if (mobileTopUp.balanceMinor != null) throw new AssertionError("top-up amount must not be parsed as final balance");
        if (!TransactionParser.isVerificationMessage("رمز التأكيد الخاص بك هو 428191")) throw new AssertionError("Arabic OTP detection");
        if (!TransactionParser.isVerificationMessage("كود الدخول إلى حسابك 428191")) throw new AssertionError("Arabic login-code detection");
        if (!TransactionParser.isVerificationMessage("Your verification code is 428191")) throw new AssertionError("English OTP detection");
        if (!TransactionParser.isVerificationMessage("Your one-time passcode is 428191")) throw new AssertionError("English passcode detection");
        if (TransactionParser.isVerificationMessage("حوالة صادرة من حسابك بمبلغ 66.24 دولار. رصيدكم المتوفر هو 129.6")) throw new AssertionError("financial message is not OTP");
        parsed("JAWAL", "amro ibrahim sewilm alhamidi has transferred an amount of ILS 150.00 to your account.", 15_000L, "ILS", FinanceRules.TRANSFER);
        parsed("JAWAL", "amro ibrahim sewilm alhmidi has transferred an amount of ILS 150.00 to your account.", 15_000L, "ILS", FinanceRules.TRANSFER);
        parsed("BOP", "تحويل دفع لصديق: عمرو ابراهيم سويلم الحميدي، بمبلغ 120.00 شيكل", 12_000L, "ILS", FinanceRules.TRANSFER);
        TransactionParser.Result customName = TransactionParser.parse("BOP", "تحويل دفع لصديق: أحمَد مُحمد، بمبلغ 75 شيكل", Arrays.asList("احمد محمد"));
        equalsText(FinanceRules.TRANSFER, customName.type, "custom own-name transfer");
        System.out.println("FinanceCoreTest: all checks passed");
    }

    private static void equalsLong(long expected, Long actual, String label) {
        if (actual == null || expected != actual) fail(label + ": expected " + expected + ", got " + actual);
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private static void equalsText(String expected, String actual, String label) {
        if (!expected.equals(actual)) fail(label + ": expected " + expected + ", got " + actual);
    }

    private static void parsed(String provider, String text, long amount, String currency, String type) {
        TransactionParser.Result result = TransactionParser.parse(provider, text);
        equalsLong(amount, result.amountMinor, "parsed amount");
        equalsText(currency, result.currency, "parsed currency");
        equalsText(type, result.type, "parsed type");
    }
}
