package app.wensarafet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class QuickActionWidgetProvider extends AppWidgetProvider {
    protected String action() { return ""; }
    protected String title() { return "وين صرفت؟"; }
    protected String subtitle() { return "تحويل سريع"; }
    protected int iconResource() { return R.mipmap.ic_launcher; }
    protected String brandWord() { return ""; }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_action);
            views.setTextViewText(R.id.widget_title, title());
            views.setTextViewText(R.id.widget_subtitle, subtitle());
            views.setImageViewResource(R.id.widget_background_image, iconResource());
            views.setTextViewText(R.id.widget_brand_word, brandWord());
            views.setViewVisibility(R.id.widget_brand_word, brandWord().isEmpty() ? View.GONE : View.VISIBLE);
            boolean active = LicenseManager.isActivated(context);
            if (!active) views.setTextViewText(R.id.widget_subtitle, "اضغط لتفعيل التطبيق");
            Intent intent = active ? new Intent(context, QuickActionActivity.class).putExtra("action", action())
                    : new Intent(context, MainActivity.class);
            intent.setData(Uri.parse("wensarafet://quick/" + action() + "/" + id));
            PendingIntent pending = PendingIntent.getActivity(context, 9000 + id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pending);
            manager.updateAppWidget(id, views);
        }
    }
}
