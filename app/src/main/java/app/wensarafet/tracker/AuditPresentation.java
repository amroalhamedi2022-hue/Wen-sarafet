package app.wensarafet.tracker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Human-readable view of old audit snapshots; never changes stored audit data.
 * Unknown or old fields remain available from Details, without inventing values.
 */
public final class AuditPresentation {
    private AuditPresentation() {}

    public static Map<String,String> fields(String raw) {
        Map<String,String> fields=new LinkedHashMap<>();
        if(raw==null || raw.isEmpty())return fields;
        for(String token:raw.split(",\\s*")) {
            int equals=token.indexOf('=');
            if(equals<1)continue;
            String key=token.substring(0,equals).trim();
            String value=token.substring(equals+1).trim();
            if(!key.isEmpty())fields.put(key,value);
        }
        return fields;
    }

    public static String title(String action,String table,String before,String after) {
        Map<String,String> data=fields(after==null?before:after);
        String kind=data.get("type");
        String object="transactions".equals(table)
                ? "EXPENSE".equals(kind)?"مصروف":"INCOME".equals(kind)?"دخل":
                  "TRANSFER".equals(kind)?"تحويل":"عملية مالية"
                : "categories".equals(table)?"تصنيف":
                  "debts".equals(table)||"debt_movements".equals(table)?"حركة دين":
                  "transaction_splits".equals(table)?"تقسيم مصروف":"سجل";
        if("ADD".equals(action))return "إضافة "+object;
        if("EDIT".equals(action))return "تعديل "+object;
        if("DELETE".equals(action))return "حذف "+object;
        if("RESTORE".equals(action))return "استرجاع "+object;
        if("UNDO".equals(action))return "تراجع عن تعديل "+object;
        return "تغيير في "+object;
    }

    public static List<String> details(String action,String before,String after,
            Map<Long,String> accounts, Map<Long,String> categories,
            Map<Long,String> accountCurrencies) {
        Map<String,String> old=fields(before), now=fields(after);
        List<String> results=new ArrayList<>();
        for(String key:new String[]{"type","amount","account","category","destination"}) {
            String former=old.get(key), current=now.get(key);
            if(former==null && current==null)continue;
            String label="type".equals(key)?"نوع العملية":"amount".equals(key)?"المبلغ":
                    "account".equals(key)?"الحساب":"category".equals(key)?"التصنيف":"الحساب المستلم";
            String left=display(key,former,accounts,categories,accountCurrencies,old);
            String right=display(key,current,accounts,categories,accountCurrencies,now);
            if(former!=null && current!=null && !former.equals(current))
                results.add(label+": كان "+left+"، وأصبح "+right);
            else if(former==null && current!=null)
                results.add(label+": "+right);
            else if(current==null && former!=null && "DELETE".equals(action))
                results.add(label+": "+left);
            else if(!"EDIT".equals(action) && !"UNDO".equals(action))
                results.add(label+": "+(current!=null?right:left));
        }
        if(results.isEmpty())
            results.add("لا تتوفر تفاصيل إضافية قابلة للعرض في هذا السجل القديم.");
        return results;
    }

    private static String display(String key,String value,Map<Long,String> accounts,
            Map<Long,String> categories,Map<Long,String> accountCurrencies,Map<String,String> row) {
        if(value==null || value.isEmpty())return "غير محدد";
        if("type".equals(key))return "EXPENSE".equals(value)?"مصروف":
                "INCOME".equals(value)?"دخل":"TRANSFER".equals(value)?"تحويل":value;
        if("amount".equals(key)) {
            try {
                long minor=Long.parseLong(value);
                long account=Long.parseLong(row.getOrDefault("account","-1"));
                String currency=accountCurrencies.getOrDefault(account,"");
                return Money.format(minor,currency).trim();
            }catch(NumberFormatException ignored){return value;}
        }
        if("account".equals(key)||"destination".equals(key)||"category".equals(key)) {
            try {
                long id=Long.parseLong(value);
                String name=("category".equals(key)?categories:accounts).get(id);
                return name==null?"رقم "+id:name;
            } catch(NumberFormatException ignored){return value;}
        }
        return value;
    }
}
