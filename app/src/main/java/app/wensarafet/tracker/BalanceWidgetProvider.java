package app.wensarafet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class BalanceWidgetProvider extends AppWidgetProvider {
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) updateAll(context);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        if (!LicenseManager.isActivated(context)) {
            for (int id : ids) manager.updateAppWidget(id, views(context, id, null, null));
            return;
        }
        FinanceDbHelper db = new FinanceDbHelper(context);
        try {
            Long jawwal = db.getAccountBalance("Jawwal Pay", "ILS");
            Long bop = db.getAccountBalance("بنك فلسطين", "ILS");
            for (int id : ids) manager.updateAppWidget(id, views(context, id, jawwal, bop));
        } finally { db.close(); }
    }

    private RemoteViews views(Context context, int id, Long jawwal, Long bop) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balances);
        boolean active = LicenseManager.isActivated(context);
        views.setTextViewText(R.id.widget_jawwal_balance, !active ? "يتطلب تفعيل" : jawwal == null ? "غير موجود" : Money.format(jawwal, "ILS"));
        views.setTextViewText(R.id.widget_bop_balance, !active ? "يتطلب تفعيل" : bop == null ? "غير موجود" : Money.format(bop, "ILS"));
        Intent intent = new Intent(context, MainActivity.class);
        intent.setData(android.net.Uri.parse("wensarafet://balances/" + id));
        PendingIntent pending = PendingIntent.getActivity(context, 12000 + id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_balance_root, pending);
        return views;
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, BalanceWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length != 0) {
            Intent update = new Intent(context, BalanceWidgetProvider.class);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(update);
        }
        BalanceTripleWidgetProvider.updateAll(context);
    }
}
