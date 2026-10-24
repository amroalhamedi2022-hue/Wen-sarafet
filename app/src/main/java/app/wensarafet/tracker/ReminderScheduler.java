package app.wensarafet.tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public final class ReminderScheduler {
    public static final String PREFS = "daily_reminder";
    public static final String ACTION_DAILY = "app.wensarafet.tracker.DAILY_REMINDER";
    public static final String ACTION_SNOOZE = "app.wensarafet.tracker.SNOOZE_REMINDER";

    private ReminderScheduler() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void scheduleDaily(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.getBoolean("enabled", false)) { cancel(context); return; }
        int hour = p.getInt("hour", 20), minute = p.getInt("minute", 0);
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour); target.set(Calendar.MINUTE, minute); target.set(Calendar.SECOND, 0); target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= System.currentTimeMillis()) target.add(Calendar.DAY_OF_YEAR, 1);
        schedule(context, target.getTimeInMillis(), ACTION_DAILY, 8101);
    }

    public static void scheduleSnooze(Context context) {
        int minutes = prefs(context).getInt("snooze_minutes", 30);
        schedule(context, System.currentTimeMillis() + minutes * 60_000L, ACTION_SNOOZE, 8102);
    }

    private static void schedule(Context context, long when, String action, int requestCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(action);
        PendingIntent pending = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_DAILY);
        PendingIntent pending = PendingIntent.getBroadcast(context, 8101, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) manager.cancel(pending);
    }
}
