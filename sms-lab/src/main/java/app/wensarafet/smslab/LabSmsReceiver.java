package app.wensarafet.smslab;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public final class LabSmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;
        if(!LabStore.smsEnabled(context))return;
        if(context.checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED)return;
        SmsMessage[] parts=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if(parts==null || parts.length==0)return;
        StringBuilder message=new StringBuilder();
        for(SmsMessage part:parts)if(part!=null && part.getDisplayMessageBody()!=null)
            message.append(part.getDisplayMessageBody());
        if(message.length()==0)return;
        // DO NOT use the sender or store full message; only extracted predictions.
        LabStore.consider(context,"SMS",message.toString(),System.currentTimeMillis());
    }
}
