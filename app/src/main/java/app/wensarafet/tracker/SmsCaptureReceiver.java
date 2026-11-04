package app.wensarafet.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.Locale;

/** Friend's Arabic-enabled full app: unknown finance senders require manual review. */
public final class SmsCaptureReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(!LicenseManager.isActivated(context))return;
        if(intent==null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;
        SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if(messages==null || messages.length==0)return;
        String sender=messages[0].getDisplayOriginatingAddress();
        String lower=sender==null?"":sender.toLowerCase(Locale.ROOT).replace(" ","");
        StringBuilder body=new StringBuilder();
        long timestamp=messages[0].getTimestampMillis();
        for(SmsMessage message:messages)
            if(message!=null && message.getDisplayMessageBody()!=null)
                body.append(message.getDisplayMessageBody());
        String raw=body.toString();
        if(raw.isEmpty() || FriendArabicCapturePolicy.isSecurityMessage(raw))return;
        String provider,channel="SMS";
        if(lower.contains("bop"))provider="BOP";
        else if(lower.contains("jawwalpay") || lower.contains("jawalpay"))provider="JAWAL";
        else if(lower.contains("palpay"))provider="PALPAY";
        else {
            // The forwarded WhatsApp contact is NOT the bank sender ID.
            // Never infer a wallet or post cash without explicit account selection.
            if(!FriendArabicCapturePolicy.isFinancialMessage(raw))return;
            provider="OTHER";
            channel="SMS_UNVERIFIED";
        }
        CaptureEngine.handle(context,provider,channel,sender,raw,timestamp);
    }
}
