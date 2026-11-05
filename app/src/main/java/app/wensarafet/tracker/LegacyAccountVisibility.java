package app.wensarafet.tracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit owner-invoked recovery of previously hidden accounts. Preserves
 * existing account IDs, currencies, amounts, transactions and debt relations.
 * Never infers currencies from names or silently reactivates anything.
 */
public final class LegacyAccountVisibility {
    private LegacyAccountVisibility() {}

    public static final class AccountInfo {
        public final long id,balanceMinor;
        public final String name,currency;
        public final boolean active;
        AccountInfo(long id,String name,String currency,long balance,boolean active) {
            this.id=id;this.name=name;this.currency=currency;this.balanceMinor=balance;this.active=active;
        }
    }
    public static List<AccountInfo> list(FinanceDbHelper helper) {
        List<AccountInfo> result=new ArrayList<>();
        try(Cursor c=helper.getReadableDatabase().rawQuery(
                "SELECT id,name,currency,balance_minor,active FROM accounts ORDER BY active DESC,position,id",
                null)) {
            while(c.moveToNext())
                result.add(new AccountInfo(c.getLong(0),c.getString(1),c.getString(2),
                        c.getLong(3),c.getInt(4)==1));
        }
        return result;
    }

    public static void restore(FinanceDbHelper helper,long id) {
        SQLiteDatabase db=helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String currency;
            int active;
            try(Cursor c=db.rawQuery("SELECT currency,active FROM accounts WHERE id=?",
                    new String[]{String.valueOf(id)})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("الحساب غير موجود");
                currency=c.getString(0);active=c.getInt(1);
            }
            if(active!=0)throw new IllegalArgumentException("الحساب ظاهر أصلًا");
            if(!"ILS".equals(currency)&&!"USD".equals(currency)&&!"JOD".equals(currency))
                throw new IllegalArgumentException("عملة الحساب قديمة أو غير معروفة. لا يمكن تغييرها تلقائيًا حفاظًا على الأرصدة");
            ContentValues values=new ContentValues();
            values.put("active",1);
            if(db.update("accounts",values,"id=? AND active=0",new String[]{String.valueOf(id)})!=1)
                throw new IllegalStateException("تعذّر استعادة ظهور الحساب");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
