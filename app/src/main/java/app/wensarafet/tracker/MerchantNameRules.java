package app.wensarafet.tracker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative merchant name extraction for optional, on-demand analytics. */
public final class MerchantNameRules {
    private static final Pattern EXPLICIT=Pattern.compile(
            "(?iu)(?:for merchant:|merchant:|عند محل|من محل|من متجر|لدى|عند|شراء من|دفع إلى|الدفع لدى|مدفوعات إلى)\\s*([\\p{L}\\p{N}][\\p{L}\\p{N} ._\\-]{2,55})");
    private MerchantNameRules() {}

    public static String extract(String note,String raw,String source) {
        String result=findExplicit(raw);
        if(result.isEmpty())result=findExplicit(note);
        if(!result.isEmpty())return result;
        // Captured transactions usually store the counterparty as their note.
        // Do not treat every arbitrary free-text manual memo as a business.
        String clean=clean(note);
        if(("CAPTURED".equals(source) || "SMS".equals(source)) && valid(clean))
            return clean;
        // Some manual notes are just a clear shop name such as "طيبة مول".
        // Do not group generic memos like "مشتريات الأسبوع" as businesses.
        if(valid(clean) && clean.matches(
                "(?iu).*\\\\b(?:مول|ماركت|متجر|مطعم|مخبز|صيدلية|محل|سوبرماركت|كافيه)\\\\b.*"))
            return clean;
        return "";
    }

    public static String key(String merchant) {
        return CategorySuggestionRules.normalize(merchant);
    }

    private static String findExplicit(String text) {
        if(text==null)return "";
        Matcher m=EXPLICIT.matcher(text);
        return m.find()?valid(clean(m.group(1)))?clean(m.group(1)):"":"";
    }

    private static String clean(String text) {
        if(text==null)return "";
        String trimmed=text.trim().replaceAll("\\s+"," ");
        trimmed=trimmed.replaceAll("(?iu)\\s+(?:amount|مبلغ|بقيمة|والمبلغ|رقم العملية|balance|الرصيد)\\b.*$","");
        return trimmed.replaceAll("[،,.;:]+$","").trim();
    }

    private static boolean valid(String value) {
        if(value==null || value.length()<3 || value.length()>52)return false;
        if(!Pattern.compile("[\\p{L}]").matcher(value).find())return false;
        String lower=value.toLowerCase(Locale.ROOT);
        return !lower.matches("(?iu)(?:مصروف|دخل|تحويل|سداد|دين|فاتورة|طعام|اكل|مشتريات|بدون اسم|غير معروف)");
    }
}
