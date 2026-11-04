package app.wensarafet.tracker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Isolated Arabic SMS experiment: read-only candidates; NEVER writes to accounts,
 * transactions, SMS inbox, audit ledger, or capture engine.
 *
 * Do not wire this class to automatic import before owner approval and real
 * redacted-message regression tests.
 */
public final class ArabicSmsParserPilot {
    private ArabicSmsParserPilot() {}

    public static final class Candidate {
        public Long amountMinor;
        public Long balanceMinor;
        public String currency;
        public String type;
        public boolean verification;
        public final boolean requiresReview = true;
        public String reason = "غير مؤكد: مراجعة يدوية";
    }

    private static final String NUMBER = "([0-9]+(?:[.,][0-9]+)*)";
    private static final String CURRENCY =
            "(ILS|USD|JOD|شيكل|شواكل|₪|دولار|دينار)";
    private static final Pattern NUMBER_CURRENCY = Pattern.compile(
            "(?i)" + NUMBER + "\\s*" + CURRENCY);
    private static final Pattern CURRENCY_NUMBER = Pattern.compile(
            "(?i)" + CURRENCY + "\\s*" + NUMBER);

    private static final Pattern OUT = Pattern.compile(
            "حوالة\\s+صادرة|تم\\s+(?:خصم|سحب|دفع|تسديد)|عملية\\s+شراء|دفعت\\s+مبلغ|تم\\s+ارسال\\s+مبلغ");
    private static final Pattern IN = Pattern.compile(
            "حوالة\\s+واردة|تم\\s+(?:ايداع|استلام)\\s+مبلغ|تم\\s+ايداع|استلمت\\s+مبلغ|تم\\s+استلام");
    private static final Pattern BALANCE_LABEL = Pattern.compile(
            "(?:الرصيد\\s+(?:المتاح|الحالي|المتبقي|الجديد|بعد\\s+العملية)|رصيد(?:ك|كم)\\s+(?:المتاح|الحالي|المتوفر))\\s*[:：-]?");

    public static Candidate analyse(String raw) {
        Candidate result = new Candidate();
        if (raw == null || raw.trim().isEmpty()) {
            result.reason = "رسالة فارغة";
            return result;
        }
        // Numeric glyph normalization is ONLY within the pilot: old parser is not touched.
        String text = normalize(raw);
        if (TransactionParser.isVerificationMessage(raw)) {
            result.verification = true;
            result.reason = "رسالة تحقق؛ لا يجوز تسجيل أي عملية";
            return result;
        }

        Matcher balanceLabel = BALANCE_LABEL.matcher(text);
        int balanceStart = balanceLabel.find() ? balanceLabel.start() : -1;
        if (balanceStart >= 0) {
            String suffix = text.substring(balanceLabel.end());
            MoneyToken balance = firstMoney(suffix, true);
            if (balance != null) result.balanceMinor = balance.value;
        }
        String amountText = balanceStart < 0 ? text : text.substring(0, balanceStart);
        MoneyToken amount = firstMoney(amountText, false);
        if (amount != null) {
            result.amountMinor = amount.value;
            result.currency = amount.currency;
        }
        boolean outgoing = OUT.matcher(text).find();
        boolean incoming = IN.matcher(text).find();
        if (outgoing && incoming) {
            result.reason = "تعارض بين الإيداع والخصم؛ لا يجوز التخمين";
        } else if (result.amountMinor == null || result.currency == null) {
            result.reason = "المبلغ أو العملة غير واضحين";
        } else if (outgoing) {
            result.type = FinanceRules.EXPENSE;
            result.reason = "مصروف محتمل — مراجعة فقط";
        } else if (incoming) {
            result.type = FinanceRules.INCOME;
            result.reason = "دخل محتمل — مراجعة فقط";
        } else {
            result.reason = "اتجاه العملية غير معروف";
        }
        return result;
    }

    /** Arabic and Persian digits, decimal/thousands separators, and letter variants. */
    static String normalize(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int p = 0; p < raw.length();) {
            int cp = raw.codePointAt(p);
            p += Character.charCount(cp);
            int digit = Character.digit(cp, 10);
            if (digit >= 0) out.append((char)('0' + digit));
            else if (cp == 0x066B) out.append('.');
            else if (cp == 0x066C || cp == 0x060C) out.append(',');
            else if (cp == 0x0623 || cp == 0x0625 || cp == 0x0622) out.append('ا');
            else if (cp == 0x0649) out.append('ي');
            else if (cp == 0x0640 || (cp >= 0x064B && cp <= 0x065F)) { /* harakat */ }
            else out.appendCodePoint(cp);
        }
        return out.toString().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static MoneyToken firstMoney(String fragment, boolean balance) {
        // Check both currency-before-amount and amount-before-currency.
        Matcher before = CURRENCY_NUMBER.matcher(fragment);
        Matcher after = NUMBER_CURRENCY.matcher(fragment);
        MoneyToken first = null;
        if (before.find()) first = token(before.group(2), before.group(1), before.start());
        if (after.find()) {
            MoneyToken second = token(after.group(1), after.group(2), after.start());
            if (second != null && (first == null || second.position < first.position)) first = second;
        }
        // For balances, digits without explicit currency are accepted only as a
        // *display hint*; never infer a transaction amount from a bare number.
        if (first == null && balance) {
            Matcher numbers = Pattern.compile(NUMBER).matcher(fragment);
            if (numbers.find()) {
                Long minor = Money.parseMinor(numbers.group(1));
                if (minor != null && minor >= 0) first = new MoneyToken(minor, null, numbers.start());
            }
        }
        return first;
    }

    private static MoneyToken token(String amount, String currency, int position) {
        Long value = Money.parseMinor(amount);
        if (value == null || value <= 0) return null;
        String c = currency.toUpperCase(Locale.ROOT);
        String normalized = c.equals("شيكل") || c.equals("شواكل") || c.equals("₪") ? "ILS"
                : c.equals("دولار") ? "USD"
                : c.equals("دينار") ? "JOD" : c;
        if (!normalized.equals("ILS") && !normalized.equals("USD") && !normalized.equals("JOD"))
            return null;
        return new MoneyToken(value, normalized, position);
    }

    private static final class MoneyToken {
        final long value;
        final String currency;
        final int position;
        MoneyToken(long value, String currency, int position) {
            this.value = value;
            this.currency = currency;
            this.position = position;
        }
    }
}
