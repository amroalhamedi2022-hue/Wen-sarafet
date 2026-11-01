package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;

/** UI preference only; never changes an existing transaction or financial category. */
public final class DefaultCategoryStore {
    private static final String PREFS="manual_entry_category_defaults";
    private DefaultCategoryStore() {}

    public static long get(Context context,String kind) {
        return prefs(context).getLong(key(kind),-1L);
    }
    public static void set(Context context,String kind,long categoryId) {
        if(categoryId<=0)throw new IllegalArgumentException("اختر تصنيفًا عاديًا ليصبح افتراضيًا");
        prefs(context).edit().putLong(key(kind),categoryId).apply();
    }
    public static void clear(Context context,String kind) {
        prefs(context).edit().remove(key(kind)).apply();
    }
    private static String key(String kind) {
        if(!FinanceRules.EXPENSE.equals(kind)&&!FinanceRules.INCOME.equals(kind))
            throw new IllegalArgumentException("نوع التصنيف غير صحيح");
        return "default_"+kind;
    }
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}
