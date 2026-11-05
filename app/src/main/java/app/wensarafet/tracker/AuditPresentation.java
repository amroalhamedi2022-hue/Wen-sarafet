package app.wensarafet.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Human-readable and read-only audit projection. Never creates or edits transactions.
 * Old records can lack fields: show what is verifiable, without guessing missing data.
 */
public final class AuditPresentation {
    private AuditPresentation() {}

    public static Map<String,String> fields(String raw) {
        Map<String,String> out=new LinkedHashMap<>();
        if(raw==null || raw.trim().isEmpty())return out;
        for(String part:raw.split(",\\s*(?=[a-z_]+=)")) {
            int eq=part.indexOf('=');
            if(eq<1)continue;
            String key=part.substring(0,eq).trim();
            if(!key.isEmpty())out.put(key,part.substring(eq+1).trim());
        }
        return out;
    }

    public static String title(String action,String table,String before,String after) {
        Map<String,String> data=fields(after==null?before:after);
        String kind=data.get("kind");
        if("ACCOUNT_ADJUST".equals(action))return "تسوية رصيد الحساب يدويًا";
        if("debt_movements".equals(table)) {
            String movement="OPEN".equals(kind)?"أصل الدين":
                    "PAYMENT".equals(kind)?"سداد مالي":
                    "MANUAL_PAYMENT".equals(kind)?"سداد في الدفتر":"حركة دين";
            if("DEBT_ADD".equals(action))return "تسجيل "+movement;
            if("DEBT_EDIT".equals(action))return "تعديل "+movement;
            if("DEBT_DELETE".equals(action))return "حذف "+movement;
            return "تغيير في "+movement;
        }
        if("transaction_splits".equals(table))
            return "SPLIT_ADD".equals(action)?"إضافة جزء من عملية مقسمة":
                    "SPLIT_REMOVE".equals(action)?"حذف جزء من عملية مقسمة":"تعديل تقسيم مصروف";
        if("categories".equals(table))
            return "CATEGORY_ADD".equals(action)?"إضافة تصنيف":
                    "CATEGORY_EDIT".equals(action)?"تعديل تصنيف":
                    "CATEGORY_DELETE".equals(action)?"حذف تصنيف":"تغيير تصنيف";
        String type=data.get("type");
        String special=data.get("special");
        String object="DEBT".equals(special)?"دين مرتبط بحساب":
                "DEBT_PAYMENT".equals(special)?"سداد دين مرتبط بحساب":
                "EXPENSE".equals(type)?"مصروف":
                "INCOME".equals(type)?"دخل":
                "TRANSFER".equals(type)?"تحويل بين حسابات":"عملية مالية";
        if("ADD".equals(action))return "إضافة "+object;
        if("EDIT".equals(action))return "تعديل "+object;
        if("DELETE".equals(action))return "حذف "+object;
        if("RESTORE".equals(action))return "استرجاع "+object;
        if("UNDO".equals(action))return "تراجع عن تعديل "+object;
        return "تغيير في "+object;
    }

    public static List<String> details(String action,String before,String after,
                                       Map<Long,String> accounts,Map<Long,String> categories,
                                       Map<Long,String> accountCurrencies) {
        return details(action,before,after,accounts,categories,accountCurrencies,
                Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap());
    }

    public static List<String> details(String action,String before,String after,
                                       Map<Long,String> accounts,Map<Long,String> categories,
                                       Map<Long,String> accountCurrencies,
                                       Map<Long,String> debtNames,Map<Long,String> debtCurrencies,
                                       Map<Long,String> personNames) {
        Map<String,String> old=fields(before),now=fields(after);
        List<String> lines=new ArrayList<>();
        String[] keys={"type","kind","amount","balance","account","destination",
                "category","name","emoji","excluded","archived","debt","debt_person","debt_direction","special",
                "destination_amount","rate","rate_source","affects","note"};
        for(String key:keys) {
            String left=old.get(key),right=now.get(key);
            if(left==null && right==null)continue;
            String label=label(key);
            if(left!=null && right!=null && !left.equals(right))
                lines.add(label+": كان "+display(key,left,old,accounts,categories,accountCurrencies,
                        debtNames,debtCurrencies,personNames)+
                        "، وأصبح "+display(key,right,now,accounts,categories,accountCurrencies,
                        debtNames,debtCurrencies,personNames));
            else if(!"EDIT".equals(action) && !"UNDO".equals(action))
                lines.add(label+": "+display(key,right!=null?right:left,right!=null?now:old,
                        accounts,categories,accountCurrencies,debtNames,debtCurrencies,personNames));
        }
        if(lines.isEmpty() && (action.startsWith("CATEGORY_"))) {
            if(before!=null && after!=null && !before.equals(after))
                lines.add("التصنيف: كان "+before+"، وأصبح "+after);
            else if(after!=null)lines.add("التصنيف: "+after);
            else if(before!=null)lines.add("التصنيف المحذوف: "+before);
        }
        if(lines.isEmpty() && before!=null && after!=null && !before.equals(after))
            lines.add("تغيّر سجل قديم، ولكن لم تُحفظ تفاصيل الحقول التي تغيّرت.");
        if(lines.isEmpty())
            lines.add("لا تتوفر تفاصيل تاريخية كافية لهذا الحدث؛ السجل الأصلي محفوظ دون تعديل.");
        return lines;
    }

    private static String label(String key) {
        switch(key) {
            case "type": return "نوع العملية";
            case "kind": return "نوع حركة الدين";
            case "amount": return "المبلغ";
            case "balance": return "رصيد الحساب";
            case "account": return "الحساب";
            case "destination": return "الحساب المستلم";
            case "category": return "التصنيف";
            case "name": return "اسم التصنيف";
            case "emoji": return "رمز التصنيف";
            case "excluded": return "مستبعد من المصروفات";
            case "archived": return "مؤرشف";
            case "debt": return "سجل الدين";
            case "debt_person": return "الشخص";
            case "debt_direction": return "اتجاه الدين";
            case "special": return "ارتباط العملية";
            case "destination_amount": return "المبلغ المستلم";
            case "rate": return "سعر الصرف";
            case "rate_source": return "مصدر سعر الصرف";
            case "affects": return "تأثير العملية على الرصيد";
            case "note": return "الملاحظة";
            default: return key;
        }
    }

    private static String display(String key,String value,Map<String,String> row,
                                  Map<Long,String> accounts,Map<Long,String> categories,
                                  Map<Long,String> accountCurrencies,Map<Long,String> debtNames,
                                  Map<Long,String> debtCurrencies,Map<Long,String> personNames) {
        if(value==null || value.isEmpty() || "-1".equals(value))return "غير محدد";
        switch(key) {
            case "type":
                return "EXPENSE".equals(value)?"مصروف":"INCOME".equals(value)?"دخل":
                        "TRANSFER".equals(value)?"تحويل":value;
            case "kind":
                return "OPEN".equals(value)?"أصل الدين":"PAYMENT".equals(value)?"سداد عبر حساب":
                        "MANUAL_PAYMENT".equals(value)?"سداد في الدفتر":value;
            case "debt_direction":
                return "RECEIVABLE".equals(value)?"دين إليّ":"PAYABLE".equals(value)?"دين عليّ":value;
            case "special":
                return "DEBT".equals(value)?"أصل الدين":"DEBT_PAYMENT".equals(value)?"سداد دين":value;
            case "affects":
            case "excluded":
            case "archived":
                return "1".equals(value)?"نعم":"0".equals(value)?"لا":value;
            case "account":
            case "destination":
            case "category":
            case "debt":
            case "debt_person":
                try {
                    long id=Long.parseLong(value);
                    Map<Long,String> lookup="category".equals(key)?categories:
                            "debt".equals(key)?debtNames:
                            "debt_person".equals(key)?personNames:accounts;
                    String found=lookup.get(id);
                    return found!=null?found:"رقم "+id;
                }catch(NumberFormatException ignored){return value;}
            case "amount":
            case "balance":
            case "destination_amount":
                try {
                    long minor=Long.parseLong(value);
                    long account=Long.parseLong(row.getOrDefault("account","-1"));
                    String currency=accountCurrencies.get(account);
                    if(currency==null) {
                        long debt=Long.parseLong(row.getOrDefault("debt","-1"));
                        currency=debtCurrencies.getOrDefault(debt,"");
                    }
                    return Money.format(minor,currency==null?"":currency).trim();
                }catch(NumberFormatException ignored){return value;}
            default: return value;
        }
    }
}
