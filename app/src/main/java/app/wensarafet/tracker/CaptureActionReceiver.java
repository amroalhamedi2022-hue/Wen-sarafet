package app.wensarafet.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class CaptureActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!LicenseManager.isActivated(context)) return;
        long id = intent.getLongExtra("capture_id", -1);
        String type = intent.getStringExtra("type");
        if (id < 0) return;
        FinanceDbHelper db = new FinanceDbHelper(context);
        long tx;
        try { tx = db.importCapture(id, type); } finally { db.close(); }
        CaptureEngine.cancelNotification(context, id);
        Toast.makeText(context, tx >= 0 ? "تم حفظ العملية — اختر تصنيفها لاحقًا" : "تعذر حفظ العملية، راجع صندوق المراجعة", Toast.LENGTH_LONG).show();
    }
}
