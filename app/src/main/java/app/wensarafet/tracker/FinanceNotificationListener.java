package app.wensarafet.tracker;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class FinanceNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (!LicenseManager.isActivated(this)) return;
        String pkg = sbn.getPackageName();
        String provider;
        if ("com.pcnc.bop".equals(pkg)) provider = "BOP";
        else if ("com.pcnc.wallet".equals(pkg)) provider = "PALPAY";
        else if ("ps.Jawwal.JawwalPayNewPlus".equals(pkg)) provider = "JAWAL";
        else return;
        Bundle extras = sbn.getNotification().extras;
        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE));
        String big = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String text = value(extras.getCharSequence(Notification.EXTRA_TEXT));
        String body = !big.isEmpty() ? big : text;
        if (!title.isEmpty() && !body.startsWith(title)) body = title + " — " + body;
        CaptureEngine.handle(this, provider, "NOTIFICATION", pkg, body, sbn.getPostTime());
    }

    private String value(CharSequence value) { return value == null ? "" : value.toString().trim(); }
}
