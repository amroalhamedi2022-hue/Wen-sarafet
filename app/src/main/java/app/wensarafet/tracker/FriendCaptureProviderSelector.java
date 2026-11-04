package app.wensarafet.tracker;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Friend-only chooser for financially plausible SMS with an unknown sender.
 * Changes ONLY the unimported capture_inbox.provider field, never an account,
 * balance, transaction, category or debt record.
 */
public final class FriendCaptureProviderSelector {
    private FriendCaptureProviderSelector() {}

    public static boolean choose(FinanceDbHelper db,long captureId,String provider) {
        if(!"BOP".equals(provider) && !"JAWAL".equals(provider)
                && !"PALPAY".equals(provider))
            throw new IllegalArgumentException("اختر بنك فلسطين أو جوال باي أو PalPay");
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT 1 FROM capture_inbox WHERE id=? AND status='PENDING' " +
                "AND channel='SMS_UNVERIFIED' LIMIT 1",
                new String[]{String.valueOf(captureId)})) {
            if(!c.moveToFirst()) return false;
        }
        ContentValues values=new ContentValues();
        values.put("provider",provider);
        return db.getWritableDatabase().update("capture_inbox",values,
                "id=? AND status='PENDING' AND channel='SMS_UNVERIFIED'",
                new String[]{String.valueOf(captureId)})==1;
    }
}
