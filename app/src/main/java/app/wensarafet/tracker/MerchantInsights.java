package app.wensarafet.tracker;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only merchant report. Generated only when the user opens the report,
 * never during manual entry or on every new transaction.
 */
public final class MerchantInsights {
    private MerchantInsights() {}
    public static final class Place {
        public final String name;
        public long spentMinor;
        public int purchases;
        Place(String name){this.name=name;}
    }

    public static List<Place> topPlaces(FinanceDbHelper db,String currency,
                                       long startInclusive,long endExclusive) {
        if(!"ILS".equals(currency)&&!"USD".equals(currency)&&!"JOD".equals(currency))
            throw new IllegalArgumentException("العملة غير معروفة");
        String sql="SELECT t.note,t.raw_text,t.source, " +
                "CASE WHEN EXISTS(SELECT 1 FROM transaction_splits x WHERE x.transaction_id=t.id) " +
                "THEN COALESCE((SELECT SUM(s.amount_minor) FROM transaction_splits s " +
                "JOIN categories sc ON sc.id=s.category_id " +
                "WHERE s.transaction_id=t.id AND s.special_kind IS NULL AND sc.exclude_from_expense=0),0) " +
                "ELSE CASE WHEN COALESCE(c.exclude_from_expense,0)=0 THEN t.amount_minor ELSE 0 END END " +
                "FROM transactions t LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.type=? AND t.special_kind IS NULL AND t.currency=? " +
                "AND t.created_at>=? AND t.created_at<? " +
                "ORDER BY t.created_at DESC,t.id DESC LIMIT 5000";
        Map<String,Place> aggregated=new LinkedHashMap<>();
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{
                FinanceRules.EXPENSE,currency,String.valueOf(startInclusive),
                String.valueOf(endExclusive)})) {
            while(c.moveToNext()) {
                long amount=c.getLong(3);
                if(amount<=0)continue;
                String name=MerchantNameRules.extract(c.getString(0),c.getString(1),c.getString(2));
                String key=MerchantNameRules.key(name);
                if(key.length()<3)continue;
                Place place=aggregated.get(key);
                if(place==null) {
                    place=new Place(name);
                    aggregated.put(key,place);
                }
                place.spentMinor=Math.addExact(place.spentMinor,amount);
                place.purchases++;
            }
        }
        List<Place> result=new ArrayList<>(aggregated.values());
        result.sort(Comparator.comparingLong((Place item)->item.spentMinor).reversed()
                .thenComparing(item->item.name));
        if(result.size()>30)return new ArrayList<>(result.subList(0,30));
        return result;
    }
}
