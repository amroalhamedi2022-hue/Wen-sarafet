package app.wensarafet.tracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/** Cautious parse of Bank of Palestine's public indicative rate page.
 * Never infer a rate from arbitrary site numbers; require an identified USD/ILS quote.
 */
public final class BopIndicativeRate {
    private BopIndicativeRate() {}
    public static String usdIlsBuyRate(String html) {
        if(html==null||html.length()>2_000_000)throw new IllegalArgumentException("بيانات السعر غير صالحة");
        String clean=html.replaceAll("(?is)<script\\b[^>]*>.*?</script>"," ")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>"," ")
                .replaceAll("(?is)<[^>]+>"," ")
                .replace("&nbsp;"," ").replace("&#160;"," ")
                .replaceAll("\\s+"," ");
        Matcher pair=Pattern.compile("(?:دولار\\s+أمريكي\\s*/\\s*شيكل|USD\\s*/\\s*ILS)",Pattern.CASE_INSENSITIVE).matcher(clean);
        if(!pair.find())throw new IllegalArgumentException("لم نجد تسعيرة الدولار مقابل الشيكل على صفحة البنك");
        String tail=clean.substring(pair.end(),Math.min(clean.length(),pair.end()+220));
        Matcher values=Pattern.compile("(?<![0-9])([1-9]\\d?\\.\\d{3,5})(?![0-9])").matcher(tail);
        if(!values.find())throw new IllegalArgumentException("سعر الشراء غير واضح");
        String buy=values.group(1);
        if(!values.find())throw new IllegalArgumentException("سعر البيع غير واضح");
        BigDecimal buying=new BigDecimal(buy), selling=new BigDecimal(values.group(1));
        if(buying.compareTo(new BigDecimal("1"))<0 || buying.compareTo(new BigDecimal("10"))>0
                || selling.compareTo(new BigDecimal("1"))<0 || selling.compareTo(new BigDecimal("10"))>0
                || buying.compareTo(selling)>0)
            throw new IllegalArgumentException("سعر الصرف المعروض غير منطقي");
        return buying.toPlainString();
    }
}
