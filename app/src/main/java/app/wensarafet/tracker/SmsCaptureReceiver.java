package app.wensarafet.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.Locale;

public final class SmsCaptureReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!LicenseManager.isActivated(context)) return;
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;
        String sender = messages[0].getDisplayOriginatingAddress();
        String lower = sender == null ? "" : sender.toLowerCase(Locale.ROOT).replace(" ", "");
        String provider;
        if (lower.contains("bop")) provider = "BOP";
        else if (lower.contains("jawwalpay") || lower.contains("jawalpay")) provider = "JAWAL";
        else return;
        StringBuilder body = new StringBuilder();
        long timestamp = messages[0].getTimestampMillis();
        for (SmsMessage message : messages) body.append(message.getDisplayMessageBody());
        CaptureEngine.handle(context, provider, "SMS", sender, body.toString(), timestamp);
    }
}
