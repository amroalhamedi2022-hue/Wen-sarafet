package app.wensarafet.tracker;

import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TransactionParser {
    public static final class Result {
        public Long amountMinor;
        public String currency;
        public String type;
        public int confidence;
        public String counterparty;
        public Long balanceMinor;
    }

    private static final String NUM = "([0-9]+(?:[\\.,][0-9]{1,2})?)";
    private static final String CUR = "(ILS|USD|JOD|شيكل|شيكلاً|شيكلًا|دولار|دينار)";
    private static final Pattern AMOUNT_AFTER = Pattern.compile("(?i)(?:بمبلغ|amount\\s+of:?)\\s*" + CUR + "?\\s*" + NUM + "\\s*" + CUR + "?");
    private static final Pattern CURRENCY_FIRST = Pattern.compile("(?i)\\b(ILS|USD|JOD)\\s*" + NUM);
    private static final Pattern NUMBER_FIRST = Pattern.compile("(?i)" + NUM + "\\s*" + CUR);
    private static final Pattern BALANCE = Pattern.compile("(?i)(?:current|available)\\s+balance\\s+(?:is:?)?\\s*(ILS|USD|JOD)?\\s*" + NUM +
            "|(?:رصيدكم\\s+المتوفر|رصيدك\\s+المتوفر|الرصيد\\s+المتوفر|الرصيد\\s+الحالي|الرصيد\\s+بعد\\s+العملية)\\s*(?:هو|هو:|:)?\\s*" + NUM);
    private static final Pattern VERIFICATION = Pattern.compile("(?i)(?:(?:رمز|كود)\\s*(?:ال)?(?:تحقق|تأكيد|تاكيد|دخول|أمان|امان|مصادقة)|(?:كلمة\\s*(?:ال)?مرور|رقم\\s*سري)\\s*(?:مؤقتة?|لمرة\\s*واحدة)|verification\\s*code|confirmation\\s*code|security\\s*code|login\\s*code|authorization\\s*code|one[- ]?time\\s*(?:password|passcode)|(?:your|the)\\s+code\\s*(?:is|:)|\\bOTP\\b)");

    private TransactionParser() {}

    public static Result parse(String provider, String raw) {
        return parse(provider, raw, Arrays.asList("amro ibrahim sewilm alhamidi", "amro ibrahim sewilm alhmidi",
                "عمرو ابراهيم سويلم الحميدي", "عمرو إبراهيم سويلم الحميدي"));
    }

    public static Result parse(String provider, String raw, List<String> ownNames) {
        Result result = new Result();
        if (raw == null) return result;
        String text = normalize(raw);
        findAmount(text, result);
        findBalance(text, result);
        inferType(provider, text, ownNames, result);
        result.counterparty = extractCounterparty(text);
        return result;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.replace('\u060C', ',').replaceAll("\\s+", " ").trim();
    }

    public static boolean isVerificationMessage(String raw) {
        return raw != null && VERIFICATION.matcher(normalize(raw)).find();
    }

    private static void findAmount(String text, Result out) {
        Matcher near = AMOUNT_AFTER.matcher(text);
        if (near.find()) {
            String c1 = near.group(1), number = near.group(2), c2 = near.group(3);
            setAmount(out, number, c1 != null ? c1 : c2);
            if (out.amountMinor != null && out.currency != null) return;
        }
        Matcher first = CURRENCY_FIRST.matcher(text);
        if (first.find()) {
            setAmount(out, first.group(2), first.group(1));
            return;
        }
        Matcher last = NUMBER_FIRST.matcher(text);
        if (last.find()) setAmount(out, last.group(1), last.group(2));
    }

    private static void setAmount(Result out, String number, String currency) {
        out.amountMinor = Money.parseMinor(number);
        out.currency = normalizeCurrency(currency);
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null) return null;
        String c = currency.toUpperCase(Locale.ROOT);
        if (c.startsWith("شيكل")) return "ILS";
        if (c.startsWith("دولار")) return "USD";
        if (c.startsWith("دينار")) return "JOD";
        return c;
    }

    private static void inferType(String provider, String text, List<String> ownNames, Result out) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsOwnName(lower, ownNames)) {
            out.type = FinanceRules.TRANSFER; out.confidence = 100; return;
        }
        if (lower.contains("حوالة صادرة") || lower.contains("تسديد") || lower.contains("شحن:") ||
                lower.contains("pay to merchant") || lower.contains("to customer:")) {
            out.type = FinanceRules.EXPENSE; out.confidence = 100; return;
        }
        if (lower.contains("from:") || (lower.contains("has transferred") && lower.contains("to your account")) ||
                lower.contains("حوالة واردة") || lower.contains("إلى حسابك") || lower.contains("الى حسابك") || lower.contains("تم استلام")) {
            out.type = FinanceRules.INCOME; out.confidence = 100; return;
        }
        if (lower.contains("تحويل دفع لصديق") || lower.contains("تحويل بنكي بين الحسابات") || lower.contains("خدمة ussd")) {
            out.type = null; out.confidence = 35; return;
        }
        out.type = null;
        out.confidence = out.amountMinor == null ? 0 : 25;
    }

    public static boolean containsOwnName(String text, List<String> names) {
        String haystack = normalizeName(text);
        if (haystack.isEmpty() || names == null) return false;
        for (String name : names) {
            String needle = normalizeName(name);
            if (needle.length() >= 3 && haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String normalizeName(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ى', 'ي').replaceAll("[ًٌٍَُِّْـ]", "").replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String extractCounterparty(String text) {
        Pattern[] patterns = {
                Pattern.compile("(?i)for merchant:\\s*([^.,]+)"),
                Pattern.compile("(?i)(?:from:|to customer:)\\s*([+0-9]+)"),
                Pattern.compile("(?i)^([^.,]{3,80})\\s+has transferred"),
                Pattern.compile("تحويل دفع لصديق:\\s*([^,،]+)")
        };
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(text);
            if (m.find()) return normalize(m.group(1));
        }
        return null;
    }

    private static void findBalance(String text, Result out) {
        Matcher m = BALANCE.matcher(text);
        if (!m.find()) return;
        String currency = m.group(1);
        String number = m.group(2) != null ? m.group(2) : m.group(3);
        out.balanceMinor = Money.parseMinor(number);
        if (out.currency == null && currency != null) out.currency = normalizeCurrency(currency);
    }
}
