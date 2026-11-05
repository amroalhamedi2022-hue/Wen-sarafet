package app.wensarafet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class BalanceTripleWidgetProvider extends AppWidgetProvider {
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) updateAll(context);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        if (!LicenseManager.isActivated(context)) {
            for (int id : ids) manager.updateAppWidget(id, views(context, id, null, null, null));
            return;
        }
        FinanceDbHelper db = new FinanceDbHelper(context);
        try {
            Long jawwal = db.getAccountBalance("Jawwal Pay", "ILS");
            Long bop = db.getAccountBalance("بنك فلسطين", "ILS");
            Long palpay = db.getAccountBalance("PalPay", "ILS");
            for (int id : ids) manager.updateAppWidget(id, views(context, id, jawwal, bop, palpay));
        } finally { db.close(); }
    }

    private RemoteViews views(Context context, int id, Long jawwal, Long bop, Long palpay) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balances_triple);
        boolean active = LicenseManager.isActivated(context);
        views.setTextViewText(R.id.widget_triple_jawwal_balance, active ? value(jawwal) : "يتطلب تفعيل");
        views.setTextViewText(R.id.widget_triple_bop_balance, active ? value(bop) : "يتطلب تفعيل");
        views.setTextViewText(R.id.widget_triple_palpay_balance, active ? value(palpay) : "يتطلب تفعيل");
        Intent intent = new Intent(context, MainActivity.class);
        intent.setData(android.net.Uri.parse("wensarafet://balances-all/" + id));
        PendingIntent pending = PendingIntent.getActivity(context, 13000 + id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_balance_triple_root, pending);
        return views;
    }

    private String value(Long balance) { return balance == null ? "غير موجود" : Money.format(balance, "ILS"); }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, BalanceTripleWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        Intent update = new Intent(context, BalanceTripleWidgetProvider.class);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(update);
    }
}
