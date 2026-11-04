package app.wensarafet.smslab;

import android.content.Context;
import android.content.SharedPreferences;
import app.wensarafet.tracker.ArabicSmsParserPilot;
import app.wensarafet.tracker.TransactionParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Independent SMS lab. No app finance database, personal names, sender phone
 * numbers, or raw SMS are retained. Everything is only local to this package.
 */
public final class LabStore {
    private static final String PREFS="wen_sms_lab_pilot";
    private static final String EVENTS="redacted_captures";
    private static final int MAX_EVENTS=70;
    private LabStore(){}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }

    public static boolean smsEnabled(Context context) {
        return prefs(context).getBoolean("sms_enabled",false);
    }
    public static void setSmsEnabled(Context context,boolean enabled) {
        prefs(context).edit().putBoolean("sms_enabled",enabled).apply();
    }
    public static boolean notificationsEnabled(Context context) {
        return prefs(context).getBoolean("notifications_enabled",false);
    }
    public static void setNotificationsEnabled(Context context,boolean enabled) {
        prefs(context).edit().putBoolean("notifications_enabled",enabled).apply();
    }

    /** No actual sender names/numbers, raw SMS, account numbers, notes or OTP. */
    public static synchronized List<JSONObject> events(Context context) {
        List<JSONObject> values=new ArrayList<>();
        try{
            JSONArray raw=new JSONArray(prefs(context).getString(EVENTS,"[]"));
            for(int i=0;i<raw.length();i++) {
                JSONObject event=raw.optJSONObject(i);
                if(event!=null)values.add(event);
            }
        }catch(Exception ignored){}
        return values;
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(EVENTS).apply();
    }

    public static boolean looksFinancial(String raw) {
        if(raw==null || raw.length()>5000 || raw.trim().isEmpty()) return false;
        String text=ArabicSmsParserPilot.normalizeForLab(raw);
        boolean hasCurrency=text.contains("ils") || text.contains("usd") || text.contains("jod")
                || text.contains("شيكل") || text.contains("شواكل")
                || text.contains("دولار") || text.contains("دينار") || text.contains("₪");
        boolean hasMovement=text.contains("تحويل") || text.contains("حوالة")
                || text.contains("استلم") || text.contains("ايداع")
                || text.contains("خصم") || text.contains("دفع")
                || text.contains("purchase") || text.contains("transferred")
                || text.contains("paid") || text.contains("from:");
        return hasCurrency && hasMovement;
    }

    public static synchronized void consider(Context context,String channel,String raw,long timestamp) {
        if(!looksFinancial(raw))return;
        if(TransactionParser.isVerificationMessage(raw))return;
        ArabicSmsParserPilot.Candidate result=ArabicSmsParserPilot.analyse(raw);
        if(result.verification)return; // Never store one-time codes, even redacted.
        if(result.amountMinor==null && result.balanceMinor==null &&
                (result.type==null || result.type.trim().isEmpty())) {
            // Still retain a non-sensitive "not parsed" flag to debug template support.
        }
        try{
            String hash=digest(channel+"|"+raw);
            JSONArray output=new JSONArray();
            JSONObject event=new JSONObject();
            event.put("at",timestamp>0?timestamp:System.currentTimeMillis());
            event.put("source",channel);
            event.put("amount",result.amountMinor==null?JSONObject.NULL:result.amountMinor);
            event.put("balance",result.balanceMinor==null?JSONObject.NULL:result.balanceMinor);
            event.put("currency",result.currency==null?"":result.currency);
            event.put("direction",result.type==null?"":result.type);
            event.put("reason",result.reason==null?"مراجعة مطلوبة":result.reason);
            event.put("fingerprint",hash);
            output.put(event);
            for(JSONObject old:events(context)){
                // Suppress duplicate copies of the same SMS/notification for 90 seconds.
                if(hash.equals(old.optString("fingerprint"))
                        && Math.abs(event.optLong("at")-old.optLong("at"))<90000)continue;
                if(output.length()>=MAX_EVENTS)break;
                output.put(old);
            }
            prefs(context).edit().putString(EVENTS,output.toString()).apply();
        }catch(Exception ignored) {
            // Logging must never crash the receiver or change any financial state.
        }
    }

    private static String digest(String value){
        try{
            byte[] data=MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(byte b:data)out.append(String.format(Locale.ROOT,"%02x",b&0xff));
            return out.toString();
        }catch(Exception error){return "";}
    }
}
