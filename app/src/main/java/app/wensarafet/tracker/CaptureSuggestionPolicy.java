package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Merchant suggestions are evaluated only once per new captured receipt, with
 * a 6-hour merchant cache. No scans during manual typing, no automatic posting.
 */
public final class CaptureSuggestionPolicy {
    private static final String PREFS="merchant_receipt_suggestion_cache";
    private static final long CACHE_AGE_MS=6L*60*60*1000;
    private CaptureSuggestionPolicy() {}

    public static String suggest(Context context,FinanceDbHelper db,long transactionId,
                                 TransactionParser.Result parsed,String rawText) {
        if(parsed==null || parsed.amountMinor==null || parsed.amountMinor<=0 ||
                parsed.currency==null || parsed.type==null ||
                (!FinanceRules.EXPENSE.equals(parsed.type) && !FinanceRules.INCOME.equals(parsed.type)))
            return null;
        String merchant=MerchantNameRules.extract(parsed.counterparty,rawText,"CAPTURED");
        String normalized=MerchantNameRules.key(merchant);
        if(normalized.length()<3)return null;
        Models.Transaction imported=db.getTransactionById(transactionId);
        if(imported==null || imported.accountId<=0)return null;
        String key=cacheKey(parsed.type,parsed.currency,normalized);
        SharedPreferences cache=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long now=System.currentTimeMillis();
        long last=cache.getLong(key+"_at",0L);
        long categoryId=cache.getLong(key+"_category",-1L);
        boolean fresh=last>0 && now>=last && now-last<CACHE_AGE_MS;
        if(!fresh) {
            CategorySuggestionRules.Suggestion suggestion=db.suggestCategory(
                    parsed.type,parsed.currency,imported.accountId,parsed.amountMinor,merchant);
            // The owner wants MERCHANT evidence, not amount-only guesses.
            categoryId=suggestion!=null &&
                    "بحسب المحل أو الوصف السابق".equals(suggestion.reason)
                    ?suggestion.categoryId:-1L;
            cache.edit().putLong(key+"_at",now).putLong(key+"_category",categoryId).apply();
        }
        if(categoryId<=0)return null;
        for(Models.Category category:db.getCategories(parsed.type))
            if(category.id==categoryId)return category.displayName();
        return null;
    }

    private static String cacheKey(String kind,String currency,String merchant) {
        String source=kind+"|"+currency+"|"+merchant;
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash=new StringBuilder("m_");
            for(byte b:digest)hash.append(String.format("%02x",b&0xff));
            return hash.toString();
        } catch(Exception ignored) {
            // A missing hash implementation should suppress suggestions rather
            // than store plaintext merchant names in a SharedPreferences key.
            throw new IllegalStateException("Unable to generate private merchant-cache key");
        }
    }
}
