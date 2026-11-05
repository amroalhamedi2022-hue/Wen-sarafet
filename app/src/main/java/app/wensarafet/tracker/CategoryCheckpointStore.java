package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

/**
 * Category/currency-only UI checkpoint, deliberately separate from the
 * immutable financial Safe Zone and from monthly category budget preferences.
 */
public final class CategoryCheckpointStore {
    private static final String PREFS="category_analysis_checkpoints";
    private CategoryCheckpointStore() {}

    public static List<Long> get(Context context,long categoryId,String currency) {
        return CategoryCheckpointRules.decode(prefs(context).getString(key(categoryId,currency),""));
    }

    /** Synchronous commit: never show a successful reset until stored. */
    public static synchronized long create(Context context,long categoryId,String currency) {
        SharedPreferences prefs=prefs(context);
        String k=key(categoryId,currency);
        long now=System.currentTimeMillis();
        String next=CategoryCheckpointRules.append(prefs.getString(k,""),now);
        if(!prefs.edit().putString(k,next).commit())
            throw new IllegalStateException("تعذّر حفظ نقطة البداية الجديدة");
        return now;
    }

    public static synchronized void undoLast(Context context,long categoryId,String currency) {
        SharedPreferences prefs=prefs(context);
        String k=key(categoryId,currency);
        String next=CategoryCheckpointRules.removeLast(prefs.getString(k,""));
        SharedPreferences.Editor edit=prefs.edit();
        if(next.isEmpty())edit.remove(k);else edit.putString(k,next);
        if(!edit.commit())
            throw new IllegalStateException("تعذّر إلغاء نقطة البداية");
    }

    private static String key(long id,String currency) {
        if(id<=0 || (!"ILS".equals(currency)&&!"USD".equals(currency)&&!"JOD".equals(currency)))
            throw new IllegalArgumentException("التصنيف أو العملة غير صالحين");
        return "checkpoint_"+id+"_"+currency;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}
