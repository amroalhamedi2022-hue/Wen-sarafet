package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;

/** Monthly category caps are display-only preferences, not entries in the ledger. */
public final class CategoryMonthlyBudgetStore {
    private static final String PREFS="monthly_category_caps";
    private CategoryMonthlyBudgetStore() {}

    public static long get(Context context,long categoryId,String currency) {
        return preferences(context).getLong(key(categoryId,currency),0L);
    }
    public static void save(Context context,long categoryId,String currency,long budgetMinor) {
        if(budgetMinor<0)throw new IllegalArgumentException("الميزانية لا يمكن أن تكون سالبة");
        SharedPreferences.Editor edit=preferences(context).edit();
        if(budgetMinor==0)edit.remove(key(categoryId,currency));
        else edit.putLong(key(categoryId,currency),budgetMinor);
        edit.apply();
    }
    private static String key(long categoryId,String currency) {
        if(categoryId<=0 || (!"ILS".equals(currency) && !"USD".equals(currency) && !"JOD".equals(currency)))
            throw new IllegalArgumentException("التصنيف أو العملة غير صالحين");
        return "category_"+categoryId+"_"+currency;
    }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}
