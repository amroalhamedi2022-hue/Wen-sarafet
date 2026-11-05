package app.wensarafet.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!LicenseManager.isActivated(context)) return;
        ReminderScheduler.scheduleDaily(context);
        BalanceWidgetProvider.updateAll(context);
        BudgetAlertManager.checkAll(context);
    }
}
