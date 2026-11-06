package app.wensarafet.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * DEBUG-ONLY launcher helper for real MainActivity screenshot QA.
 * Sets a visual theme index in the disposable emulator's own preferences.
 * Never touches money, backups, activation, databases or production builds.
 */
public final class OwnerThemeCaptureReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int index = intent == null ? -1 : intent.getIntExtra("theme", -1);
        if (index >= 25 && index <= 29) {
            Ui.saveTheme(context, index);
            android.util.Log.i("WenRealPreview", "DEBUG_THEME_SET index=" + index);
        }
    }
}
