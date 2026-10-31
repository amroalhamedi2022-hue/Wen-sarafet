package app.wensarafet.tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import java.io.IOException;

public final class NotificationPreferences {
    public static final String REVIEW = "review";
    public static final String SAVED = "saved";
    public static final String REMINDER = "reminder";
    public static final String BUDGET = "budget";
    public static final String SYNC = "sync";
    private static final String PREFS = "notification_preferences";

    private NotificationPreferences() {}

    public static boolean sound(Context context, String type) {
        return prefs(context).getBoolean(type + "_sound", true);
    }

    public static boolean vibration(Context context, String type) {
        return prefs(context).getBoolean(type + "_vibration", true);
    }

    public static void save(Context context, String type, boolean sound, boolean vibration) {
        prefs(context).edit().putBoolean(type + "_sound", sound).putBoolean(type + "_vibration", vibration).apply();
    }

    public static String soundUri(Context context, String type) {
        return prefs(context).getString(type + "_sound_uri", "");
    }

    public static void saveSoundUri(Context context, String type, String uri) {
        prefs(context).edit().putString(type + "_sound_uri", uri == null ? "" : uri).apply();
    }

    /** Explicit opt-in: generate originals first, then atomically save tone preferences. */
    public static void useDistinctiveSounds(Context context) throws IOException {
        String[] kinds={REVIEW,SAVED,REMINDER,BUDGET,SYNC};
        for(String type:kinds)NotificationSoundProvider.ensureSound(context,type);
        SharedPreferences.Editor edit=prefs(context).edit();
        for(String type:kinds)edit.putString(type+"_sound_uri",
                NotificationSoundProvider.uri(type).toString());
        edit.apply();
    }

    public static String ensureChannel(Context context, String type) {
        boolean sound = sound(context, type), vibration = vibration(context, type);
        String storedUri = soundUri(context, type);
        String id = "ws_" + type + "_" + (sound ? "s" : "q") + (vibration ? "v" : "n") +
                "_" + Integer.toHexString(storedUri.hashCode()) + "_v3";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(id) != null) return id;
        int importance = REVIEW.equals(type) ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(id, title(type, sound, vibration), importance);
        channel.setDescription(description(type)); channel.enableVibration(vibration);
        if (vibration) channel.setVibrationPattern(new long[]{0, 150, 90, 150});
        if (sound) {
            AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
            Uri selected = storedUri.isEmpty()
                    ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    : Uri.parse(storedUri);
            if(NotificationSoundProvider.isOurSound(selected)) {
                try {
                    NotificationSoundProvider.ensureSound(context,type);
                } catch(IOException error) {
                    selected=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            channel.setSound(selected, attributes);
        } else channel.setSound(null, null);
        manager.createNotificationChannel(channel); return id;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String title(String type, boolean sound, boolean vibration) {
        String base;
        if (REVIEW.equals(type)) base = "عمليات تحتاج مراجعة";
        else if (SAVED.equals(type)) base = "عمليات محفوظة تلقائيًا";
        else if (REMINDER.equals(type)) base = "تذكير مصاريف اليوم";
        else if (BUDGET.equals(type)) base = "تنبيه الميزانية";
        else base = "مشكلة مزامنة النسخة الاحتياطية";
        return base + " — " + (sound ? "صوت" : "صامت") + (vibration ? " واهتزاز" : "");
    }

    private static String description(String type) {
        if (REVIEW.equals(type)) return "عمليات التقطها التطبيق وتحتاج تحديدًا";
        if (SAVED.equals(type)) return "تأكيد حفظ العمليات الواضحة تلقائيًا";
        if (REMINDER.equals(type)) return "تذكير مراجعة وتثبيت مصاريف اليوم";
        if (BUDGET.equals(type)) return "تنبيه واحد عند تجاوز الميزانية الشهرية";
        return "تنبيه عند تعذر مزامنة النسخة المشفّرة مع المجلد المرتبط";
    }
}
