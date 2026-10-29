package app.wensarafet.tracker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!LicenseManager.isActivated(context)
                || !ReminderScheduler.prefs(context).getBoolean("enabled", false)) {
            ReminderScheduler.cancel(context);
            return;
        }
        String action = intent == null ? null : intent.getAction();
        if (ReminderScheduler.ACTION_SNOOZE_REQUEST.equals(action)) {
            // A tap on "Later" schedules a new alarm; it must NOT show the alert now.
            NotificationManager notifications = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notifications != null) notifications.cancel(8301);
            try (FinanceDbHelper db = new FinanceDbHelper(context)) {
                if (db.getPendingExpenseClassificationCount() > 0)
                    ReminderScheduler.scheduleSnooze(context);
            }
            return;
        }
        if (!ReminderScheduler.ACTION_DAILY.equals(action)
                && !ReminderScheduler.ACTION_SNOOZE.equals(action)) return;
        show(context);
        if (ReminderScheduler.ACTION_DAILY.equals(action))
            ReminderScheduler.scheduleDaily(context);
    }

    private void show(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Check the DB at delivery time, including snoozed alarms; never use a stale cached count.
        try (FinanceDbHelper db = new FinanceDbHelper(context)) {
            if (db.getPendingExpenseClassificationCount() == 0) {
                manager.cancel(8301);
                return;
            }
        }
        String channelId = NotificationPreferences.ensureChannel(context, NotificationPreferences.REMINDER);

        Intent openIntent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 8201, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = new android.app.Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(11, 107, 87))
                .setContentTitle("ثبّت مصاريف اليوم")
                .setContentText("راجع عمليات اليوم وأضف أي مصروف نسيته.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(new android.app.Notification.Action.Builder(null, "فتح التطبيق", open).build());
        if (ReminderScheduler.prefs(context).getBoolean("snooze_enabled", true)) {
            Intent laterIntent = new Intent(context, ReminderReceiver.class).setAction(ReminderScheduler.ACTION_SNOOZE_REQUEST);
            PendingIntent later = PendingIntent.getBroadcast(context, 8202, laterIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(new android.app.Notification.Action.Builder(null, "لاحقًا", later).build());
        }
        manager.notify(8301, builder.build());
    }
}
