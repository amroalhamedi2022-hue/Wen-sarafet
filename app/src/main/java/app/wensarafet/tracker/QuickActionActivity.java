package app.wensarafet.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.Button;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class QuickActionActivity extends Activity {
    private static final int CONTACT_PICK_REQUEST=421;
    private EditText pendingContactPhone;
    private TextView selectedContactLabel;
    @Override protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        if (Ui.DARK) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(savedInstanceState);
        if (!LicenseManager.isActivated(this)) {
            startActivity(new Intent(this, MainActivity.class)); finish(); return;
        }
        String action = getIntent().getStringExtra("action");
        boolean jawwal = "jawwal_friend".equals(action) || "jawwal_merchant".equals(action);
        if (jawwal && SecurePinStore.read(this, "jawwal_pin") == null) { missingPin("جوال باي"); return; }
        showForm(action);
    }

    private void showForm(String action) {
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 22), Ui.dp(this, 8), Ui.dp(this, 22), 0);
        TextView phoneLabel = Ui.text(this, "رقم الجوال — 10 خانات", 13, Ui.TEXT, true); form.addView(phoneLabel);
        LinearLayout phoneRow = new LinearLayout(this); phoneRow.setOrientation(LinearLayout.HORIZONTAL); phoneRow.setGravity(Gravity.CENTER_VERTICAL); phoneRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView prefix = Ui.text(this, "05", 18, Ui.BRAND, true); prefix.setGravity(Gravity.CENTER); prefix.setTextDirection(View.TEXT_DIRECTION_LTR);
        EditText phone = field("ثمانية أرقام", InputType.TYPE_CLASS_PHONE); phone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)}); phone.setTextDirection(View.TEXT_DIRECTION_LTR);
        phoneRow.addView(prefix, new LinearLayout.LayoutParams(Ui.dp(this, 58), ViewGroup.LayoutParams.MATCH_PARENT)); phoneRow.addView(phone, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        EditText amount = field("المبلغ", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout contactPickerRow=Ui.row(this);
        Button chooseContact=new Button(this);
        chooseContact.setText("👤 من جهات الاتصال");
        chooseContact.setTextSize(12);
        chooseContact.setAllCaps(false);
        chooseContact.setOnClickListener(v->openContactPicker());
        contactPickerRow.addView(chooseContact,
                new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));
        selectedContactLabel=Ui.text(this,"",12,Ui.MUTED,false);
        selectedContactLabel.setVisibility(View.GONE);
        form.addView(phoneRow,params());
        form.addView(contactPickerRow,params());
        form.addView(selectedContactLabel,params());
        form.addView(amount, params());
        pendingContactPhone=phone;
        String title = "jawwal_friend".equals(action) ? "دفع لصديق" : "jawwal_merchant".equals(action) ? "دفع لتاجر" : "تحويل PalPay";
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(form)
                .setNegativeButton("إلغاء", (d, w) -> finish()).setPositiveButton("فتح الهاتف", null).create();
        dialog.setOnCancelListener(d -> finish());
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String tail=phone.getText().toString().trim();
            String mobile=PalestinianMobileNumber.normalize("05"+tail);
            String money = amount.getText().toString().trim().replace(',', '.');
            if (mobile==null) { phone.setError("اختر رقم جوال فلسطيني صالح (059 أو 056)"); return; }
            Long minor = Money.parseMinor(money);
            if (minor == null || minor <= 0) { amount.setError("اكتب مبلغًا صحيحًا"); return; }
            String code;
            if ("jawwal_friend".equals(action)) code = UssdCodes.jawwalFriend(SecurePinStore.read(this, "jawwal_pin"), mobile, money);
            else if ("jawwal_merchant".equals(action)) code = UssdCodes.jawwalMerchant(SecurePinStore.read(this, "jawwal_pin"), mobile, money);
            else code = UssdCodes.palPay(mobile, money);
            dialog.dismiss(); dial(code);
        }));
        dialog.show();
        if(getIntent().getBooleanExtra("pick_contact",false))
            phone.post(this::openContactPicker);
    }

    private void openContactPicker() {
        if(pendingContactPhone==null || isFinishing() || isDestroyed())return;
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
        for(int i=0;i<choices.length;i++) {
            Intent choice=choices[i];
            choice.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(choice,CONTACT_PICK_REQUEST);
                return;
            } catch(RuntimeException error) {
                // Another OEM system picker may support the next method.
                android.util.Log.w("WenContactPicker",
                        "Phone picker option "+i+" unavailable: "+error.getClass().getSimpleName());
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("تعذّر فتح جهات الاتصال")
                .setMessage("هذا الجهاز لا يوفّر حاليًا نافذة متوافقة لاختيار رقم من جهات الاتصال. افتح تطبيق جهات الاتصال أو فعّله ثم جرّب مرة ثانية، أو أدخل الرقم يدويًا. لا يحتاج وين صرفت إلى إذن قراءة كل الأسماء.")
                .setPositiveButton("حسنًا",null)
                .show();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=CONTACT_PICK_REQUEST || resultCode!=RESULT_OK ||
                data==null || data.getData()==null || pendingContactPhone==null)return;
        try(Cursor c=getContentResolver().query(data.getData(),null,null,null,null)) {
            if(c==null || !c.moveToFirst())
                throw new IllegalArgumentException("الجهة المختارة لا تحتوي رقم هاتف");
            // OEM pickers may omit DISPLAY_NAME or return a different projection.
            // Query only the ONE URI selected by the user, not their address book.
            int numberIndex=c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if(numberIndex<0)
                throw new IllegalArgumentException("لم يرجع تطبيق جهات الاتصال رقمًا");
            String mobile=PalestinianMobileNumber.normalize(c.getString(numberIndex));
            if(mobile==null)throw new IllegalArgumentException(
                    "الرقم المختار ليس جوالًا فلسطينيًا صالحًا (059 أو 056)");
            pendingContactPhone.setText(mobile.substring(2));
            if(selectedContactLabel!=null) {
                int nameIndex=c.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String displayName=nameIndex>=0?c.getString(nameIndex):null;
                selectedContactLabel.setText("✓ "+
                        (displayName==null || displayName.trim().isEmpty()?
                                "الجهة المختارة":displayName)+"  •  "+mobile);
                selectedContactLabel.setVisibility(View.VISIBLE);
            }
        } catch(Exception error) {
            android.util.Log.w("WenContactPicker",
                    "Selected phone could not be read: "+error.getClass().getSimpleName());
            Toast.makeText(this,error instanceof IllegalArgumentException?
                            error.getMessage():"تعذّر قراءة الرقم المختار. جرّب جهة اتصال أخرى.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private EditText field(String hint, int type) {
        EditText field = new EditText(this);
        field.setHint(hint); field.setInputType(type); field.setTextColor(Ui.TEXT); field.setHintTextColor(Ui.MUTED);
        field.setBackground(Ui.outlined(Ui.SURFACE, Ui.BRAND_SOFT, 14, this)); field.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        return field;
    }

    private LinearLayout.LayoutParams params() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56));
        p.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6)); return p;
    }

    private void missingPin(String service) {
        new AlertDialog.Builder(this).setTitle("PIN غير محفوظ")
                .setMessage("افتح إعدادات وين صرفت؟ واحفظ رمز " + service + " المكوّن من 4 أرقام أولًا.")
                .setPositiveButton("فتح التطبيق", (d, w) -> { startActivity(new Intent(this, MainActivity.class)); finish(); })
                .setNegativeButton("إلغاء", (d, w) -> finish()).setOnCancelListener(d -> finish()).show();
    }

    private void dial(String code) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)));
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
