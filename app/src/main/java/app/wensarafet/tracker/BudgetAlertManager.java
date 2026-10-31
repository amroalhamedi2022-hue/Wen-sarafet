package app.wensarafet.tracker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public final class BudgetAlertManager {
    public static final String PREFS = "monthly_budgets";
    private BudgetAlertManager() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context, String currency) {
        return prefs(context).getBoolean("alert_" + currency, false);
    }

    public static void setEnabled(Context context, String currency, boolean enabled) {
        prefs(context).edit().putBoolean("alert_" + currency, enabled).apply();
        if (enabled) checkAll(context);
    }

    public static void checkAll(Context context) {
        // POST_NOTIFICATIONS is a runtime permission only on Android 13+;
        // older devices must still receive budget warnings.
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences preferences = prefs(context);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(start.getTime());
        FinanceDbHelper db = new FinanceDbHelper(context);
        try {
            for (String currency : Arrays.asList("ILS", "USD", "JOD")) {
                if (!preferences.getBoolean("alert_" + currency, false)) continue;
                long budget = preferences.getLong("budget_" + currency, 0L);
                if (budget <= 0 || monthKey.equals(preferences.getString("notified_" + currency, ""))) continue;
                long spent = db.getExpenseTotal(currency, start.getTimeInMillis(), System.currentTimeMillis() + 1);
                if (spent > budget) {
                    if(notifyExceeded(context, currency, spent, budget))
                        preferences.edit().putString("notified_" + currency, monthKey).apply();
                }
            }
        } finally { db.close(); }
    }

    private static boolean notifyExceeded(Context context, String currency, long spent, long budget) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager == null || !manager.areNotificationsEnabled()) return false;
        String channelId = NotificationPreferences.ensureChannel(context, NotificationPreferences.BUDGET);
        if(android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(channelId);
            if(channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        }
        Intent openIntent = new Intent(context, MainActivity.class)
                .putExtra("open_analytics", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 9100 + currency.hashCode(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long exceeded = spent - budget;
        android.app.Notification notification = new android.app.Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(181, 54, 71))
                .setContentTitle("تجاوزت ميزانية " + currency)
                .setContentText("تجاوزت ميزانية الشهر بـ " + Money.format(exceeded, currency))
                .setStyle(new android.app.Notification.BigTextStyle().bigText(
                        "صرفت " + Money.format(spent, currency) + " من ميزانية " + Money.format(budget, currency) + ". سيظهر هذا التنبيه مرة واحدة فقط هذا الشهر."))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        try {
            manager.notify(9200 + Math.abs(currency.hashCode() % 100), notification);
            return true;
        } catch(SecurityException permissionRevoked) {
            // The user can revoke permission between checkAll and notify().
            // Do not mark the budget as already warned; retry after permission returns.
            return false;
        }
    }
}
