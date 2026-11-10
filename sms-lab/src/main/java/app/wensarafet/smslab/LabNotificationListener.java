package app.wensarafet.smslab;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Only the three supported finance application packages are considered.
 * Access to other personal notifications is never used or stored.
 */
public final class LabNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if(!LabStore.notificationsEnabled(this))return;
        if(notification==null || notification.getNotification()==null)return;
        String app=notification.getPackageName();
        if(!"com.pcnc.bop".equals(app) &&
                !"com.pcnc.wallet".equals(app) &&
                !"ps.Jawwal.JawwalPayNewPlus".equals(app))return;
        Bundle extras=notification.getNotification().extras;
        if(extras==null)return;
        String title=text(extras.getCharSequence(Notification.EXTRA_TITLE));
        String big=text(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String small=text(extras.getCharSequence(Notification.EXTRA_TEXT));
        String body=big.isEmpty()?small:big;
        if(!title.isEmpty() && !body.startsWith(title))body=title+" — "+body;
        if(body.trim().isEmpty())return;
        String source="com.pcnc.bop".equals(app)?"BOP":
                "com.pcnc.wallet".equals(app)?"PalPay":"Jawwal Pay";
        LabStore.consider(this,source,body,notification.getPostTime());
    }
    private static String text(CharSequence value) {
        return value==null?"":value.toString().trim();
    }
}
