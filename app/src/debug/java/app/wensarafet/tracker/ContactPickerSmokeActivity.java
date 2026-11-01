package app.wensarafet.tracker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Debug-only picker probe. Never accesses transactions, license, PIN, USSD or a full address book. */
public final class ContactPickerSmokeActivity extends Activity {
    private static final int CONTACT_PICK_REQUEST=582;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(30,50,30,30);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView info=new TextView(this);
        info.setText("اختبار فتح جهات الاتصال فقط\nلا توجد حوالة ولا اتصال ولا قراءة لدفتر العناوين كاملًا.");
        info.setTextSize(18);
        layout.addView(info);
        Button pick=new Button(this);
        pick.setAllCaps(false);
        pick.setText("👤 اختر رقمًا من جهات الاتصال");
        pick.setOnClickListener(v->openPicker());
        layout.addView(pick);
        status=new TextView(this);
        status.setText("بعد الاختيار سيظهر الرقم هنا فقط. لا تضغط أي أزرار للتحويل.");
        status.setTextSize(17);
        layout.addView(status);
        setContentView(layout);
    }

    private void openPicker() {
        Intent[] choices=new Intent[] {
                new Intent(Intent.ACTION_PICK)
                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE),
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
        };
        for (Intent choice:choices) {
            choice.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(choice,CONTACT_PICK_REQUEST);
                status.setText("تم تشغيل منتقي جهات الاتصال، اختر رقمًا للتجربة.");
                return;
            } catch (RuntimeException unavailable) {
                // Try the next OEM-compatible intent; no address-book permission is requested.
            }
        }
        status.setText("تعذّر فتح جهات الاتصال بكل الطرق المتوافقة.");
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=CONTACT_PICK_REQUEST)return;
        if(resultCode!=RESULT_OK || data==null || data.getData()==null) {
            status.setText("ألغيت اختيار جهة الاتصال.");
            return;
        }
        try (Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null)) {
            if(cursor==null || !cursor.moveToFirst())
                throw new IllegalArgumentException("لم يُرجع تطبيق جهات الاتصال رقمًا");
            int number=cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if(number<0)throw new IllegalArgumentException("لا يوجد عمود رقم هاتف");
            String local=PalestinianMobileNumber.normalize(cursor.getString(number));
            status.setText(local==null
                    ? "تم اختيار جهة الاتصال، لكن الرقم ليس فلسطينيًا صالحًا (059 أو 056)."
                    : "✅ نجح الاختيار والقراءة: "+local);
        } catch(Exception error) {
            status.setText("تم فتح المنتقي، لكن فشلت قراءة الرقم المختار: "+
                    error.getClass().getSimpleName());
        }
    }
}
