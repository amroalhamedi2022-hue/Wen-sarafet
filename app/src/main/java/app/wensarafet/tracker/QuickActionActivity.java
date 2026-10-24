package app.wensarafet.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
    @Override protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        if (Ui.DARK) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(savedInstanceState);
        if (!LicenseManager.isActivated(this)) {
            startActivity(new Intent(this, MainActivity.class)); finish(); return;
        }
        String action = getIntent().getStringExtra("action");
        if ("bop".equals(action)) {
            String pin = SecurePinStore.read(this, "bop_pin");
            if (pin == null) missingPin("بنك فلسطين"); else dial(UssdCodes.bankOfPalestine(pin));
            return;
        }
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
        form.addView(phoneRow, params());
        form.addView(amount, params());
        String title = "jawwal_friend".equals(action) ? "دفع لصديق" : "jawwal_merchant".equals(action) ? "دفع لتاجر" : "تحويل PalPay";
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(form)
                .setNegativeButton("إلغاء", (d, w) -> finish()).setPositiveButton("فتح الهاتف", null).create();
        dialog.setOnCancelListener(d -> finish());
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String tail = phone.getText().toString().trim().replace(" ", "");
            String mobile = "05" + tail;
            String money = amount.getText().toString().trim().replace(',', '.');
            if (!tail.matches("[0-9]{8}")) { phone.setError("اكتب 8 أرقام بعد 05"); return; }
            Long minor = Money.parseMinor(money);
            if (minor == null || minor <= 0) { amount.setError("اكتب مبلغًا صحيحًا"); return; }
            String code;
            if ("jawwal_friend".equals(action)) code = UssdCodes.jawwalFriend(SecurePinStore.read(this, "jawwal_pin"), mobile, money);
            else if ("jawwal_merchant".equals(action)) code = UssdCodes.jawwalMerchant(SecurePinStore.read(this, "jawwal_pin"), mobile, money);
            else code = UssdCodes.palPay(mobile, money);
            dialog.dismiss(); dial(code);
        }));
        dialog.show();
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
