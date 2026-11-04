package app.wensarafet.smslab;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.wensarafet.tracker.ArabicSmsParserPilot;
import app.wensarafet.tracker.FinanceRules;
import app.wensarafet.tracker.Money;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Standalone installable read-only Arabic message pilot. No account screen,
 * no finance database, no original app files, no Internet permission, no import.
 */
public final class LabActivity extends Activity {
    private static final int SMS_REQUEST=3411;
    private static final int NAVY=Color.rgb(22,39,56);
    private static final int TEAL=Color.rgb(0,121,111);
    private static final int MUTED=Color.rgb(99,115,127);
    private static final int LIGHT=Color.rgb(245,248,250);
    private static final int STROKE=Color.rgb(219,229,234);
    private LinearLayout content;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.WHITE);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if(content!=null)render();
    }

    private int px(float dp) { return (int)(dp*getResources().getDisplayMetrics().density+0.5f); }

    private GradientDrawable surface(int color,int radius,boolean border) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(px(radius));
        if(border)d.setStroke(px(1),STROKE);
        return d;
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setLineSpacing(px(2),1.04f);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private LinearLayout panel(String title,String subtitle) {
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(17),px(17),px(17),px(15));
        panel.setBackground(surface(Color.WHITE,20,true));
        panel.addView(text(title,18,NAVY,true));
        if(subtitle!=null&&!subtitle.isEmpty()){
            TextView hint=text(subtitle,12,MUTED,false);
            LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
            hp.topMargin=px(6);panel.addView(hint,hp);
        }
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin=px(14);
        content.addView(panel,params);
        return panel;
    }

    private Button button(String label,boolean primary) {
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label);b.setTextSize(14);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(px(46));
        b.setPadding(px(10),px(8),px(10),px(8));
        b.setTextColor(primary?Color.WHITE:NAVY);
        b.setBackground(surface(primary?TEAL:LIGHT,13,!primary));
        return b;
    }

    private void addButton(LinearLayout parent,Button child) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(52));
        p.topMargin=px(9);
        parent.addView(child,p);
    }

    private boolean notificationsPermissionGranted() {
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        return manager!=null && manager.isNotificationListenerAccessGranted(
                new ComponentName(this,LabNotificationListener.class));
    }

    private void render() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(LIGHT);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(16),px(18),px(16),px(22));

        LinearLayout banner=new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(px(19),px(20),px(19),px(20));
        banner.setBackground(surface(NAVY,21,false));
        banner.addView(text("وين صرفت  |  مختبر الرسائل العربية",22,Color.WHITE,true));
        TextView intro=text("نسخة اختبار مستقلة  •  لا تسجّل أي عملية مالية ولا تغيّر أي رصيد. تحليل فقط، والقرار دائمًا للمستخدم.",13,Color.rgb(205,229,225),false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=px(11);banner.addView(intro,ip);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.bottomMargin=px(16);
        content.addView(banner,bp);

        LinearLayout sms=panel("١. استقبال الرسائل الجديدة",
                "لا تُقرأ الرسائل القديمة. اسم مرسل SMS قد يختلف عن رقم المرسل على واتساب؛ لا نعتمد عليه في التحليل.");
        boolean smsGranted=checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                ==PackageManager.PERMISSION_GRANTED;
        sms.addView(text("الإذن: "+(smsGranted?"ممنوح":"غير ممنوح")+
                "   |   الاستقبال: "+(LabStore.smsEnabled(this)?"مفعّل":"متوقف"),
                13,smsGranted&&LabStore.smsEnabled(this)?TEAL:MUTED,true));
        Button toggleSms=button(LabStore.smsEnabled(this)?"إيقاف مراقبة SMS التجريبية":
                "السماح باستقبال SMS الجديدة",!LabStore.smsEnabled(this));
        toggleSms.setOnClickListener(v->{
            if(LabStore.smsEnabled(this)){
                LabStore.setSmsEnabled(this,false);render();
            }else if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED) {
                LabStore.setSmsEnabled(this,true);render();
            }else requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},SMS_REQUEST);
        });
        addButton(sms,toggleSms);

        LinearLayout notifications=panel("٢. إشعارات المحافظ والبنك",
                "اختياري: بنك فلسطين، جوال باي، PalPay فقط. لا تُقرأ إشعارات التطبيقات الأخرى لأغراض التحليل.");
        boolean hasAccess=notificationsPermissionGranted();
        notifications.addView(text("إذن النظام: "+(hasAccess?"ممنوح":"غير ممنوح")+
                        "   |   التجربة: "+(LabStore.notificationsEnabled(this)?"مفعّلة":"متوقفة"),
                13,hasAccess&&LabStore.notificationsEnabled(this)?TEAL:MUTED,true));
        Button open=button("فتح إعدادات الوصول للإشعارات",false);
        open.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        addButton(notifications,open);
        Button toggleNotifications=button(LabStore.notificationsEnabled(this)?
                "إيقاف تحليل إشعارات المحافظ":
                "تفعيل تحليل إشعارات المحافظ",!LabStore.notificationsEnabled(this));
        toggleNotifications.setOnClickListener(v->{
            if(!LabStore.notificationsEnabled(this)&&!notificationsPermissionGranted()){
                new AlertDialog.Builder(this).setTitle("الإذن غير مفعّل")
                        .setMessage("فعّل الوصول إلى إشعارات «وين صرفت — مختبر إشعارات البنك» من إعدادات Android أولًا.")
                        .setPositiveButton("فتح الإعدادات",(d,w)->
                                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                        .setNegativeButton("إلغاء",null).show();
                return;
            }
            LabStore.setNotificationsEnabled(this,!LabStore.notificationsEnabled(this));
            render();
        });
        addButton(notifications,toggleNotifications);

        LinearLayout samples=panel("٣. جرّب نص رسالة يدويًا",
                "الصق رسالة بلا بيانات شخصية. النص لا يُحفظ عند التحليل اليدوي ولا يُرسل إلى أي خادم.");
        EditText input=new EditText(this);
        input.setHint("مثال: تم تحويل ILS 5.00 إلى متجر تجريبي رصيدك الحالي ILS 90.00");
        input.setTextSize(14);
        input.setTextColor(NAVY);input.setHintTextColor(MUTED);
        input.setGravity(Gravity.TOP|Gravity.RIGHT);
        input.setMinLines(3);input.setMaxLines(7);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setPadding(px(12),px(12),px(12),px(12));
        input.setBackground(surface(LIGHT,12,true));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);
        fp.topMargin=px(12);samples.addView(input,fp);
        Button analyse=button("تحليل تجريبي — دون حفظ",true);
        analyse.setOnClickListener(v->{
            String raw=input.getText().toString().trim();
            if(raw.isEmpty()) {
                input.setError("الصق نص رسالة أولًا");
                return;
            }
            ArabicSmsParserPilot.Candidate result=ArabicSmsParserPilot.analyse(raw);
            String resultText="الاتجاه المقترح: "+direction(result.type)+"\n"
                    +"المبلغ: "+money(result.amountMinor,result.currency)+"\n"
                    +"الرصيد المذكور: "+money(result.balanceMinor,result.currency)+"\n"
                    +"الحالة: "+(result.verification?"رمز تحقق — مستبعد":"للمراجعة فقط")+"\n"
                    +"السبب: "+result.reason+"\n\nلن تُسجَّل أي عملية مالية.";
            new AlertDialog.Builder(this).setTitle("نتيجة التحليل")
                    .setMessage(resultText).setPositiveButton("إغلاق",null).show();
        });
        addButton(samples,analyse);

        List<JSONObject> events=LabStore.events(this);
        LinearLayout history=panel("٤. نتائج الرسائل المستلمة ("+events.size()+")",
                "تُحفظ محليًا ملخصات مجردة دون نص الرسالة أو اسم الشخص أو رقمه. لا توجد أي عملية مالية.");
        Button refresh=button("تحديث القائمة",false);
        refresh.setOnClickListener(v->render());addButton(history,refresh);
        if(events.isEmpty()){
            TextView empty=text("لا يوجد أي استقبال بعد. فعّل الصلاحية وانتظر رسالة مالية جديدة، أو استخدم الاختبار اليدوي أعلاه.",13,MUTED,false);
            LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=px(14);
            history.addView(empty,ep);
        }else {
            SimpleDateFormat formatter=new SimpleDateFormat("dd/MM/yyyy  hh:mm a",new Locale("ar"));
            for(JSONObject item:events) {
                String source=item.optString("source","SMS");
                String when=formatter.format(new Date(item.optLong("at",System.currentTimeMillis())));
                String type=direction(item.optString("direction",""));
                String amount=item.isNull("amount")?"غير معروف":money(item.optLong("amount"),
                        item.optString("currency",""));
                String balance=item.isNull("balance")?"غير معروف":money(item.optLong("balance"),
                        item.optString("currency",""));
                LinearLayout itemPanel=new LinearLayout(this);
                itemPanel.setOrientation(LinearLayout.VERTICAL);
                itemPanel.setPadding(px(11),px(11),px(11),px(11));
                itemPanel.setBackground(surface(LIGHT,12,true));
                itemPanel.addView(text(source+"  •  "+type+"  •  "+when,13,NAVY,true));
                TextView values=text("المبلغ: "+amount+"    |    الرصيد المذكور: "+balance+
                        "\n"+item.optString("reason","مراجعة مطلوبة"),12,MUTED,false);
                LinearLayout.LayoutParams vParams=new LinearLayout.LayoutParams(-1,-2);
                vParams.topMargin=px(5);itemPanel.addView(values,vParams);
                LinearLayout.LayoutParams itemP=new LinearLayout.LayoutParams(-1,-2);
                itemP.topMargin=px(9);history.addView(itemPanel,itemP);
            }
        }
        Button clear=button("حذف سجل التجارب فقط",false);
        clear.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("مسح سجل المختبر؟")
                .setMessage("سيُحذف سجل الملخصات داخل هذا التطبيق التجريبي فقط؛ لن يتأثر «وين صرفت» الأصلي.")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("مسح",(d,w)->{LabStore.clear(this);render();}).show());
        addButton(history,clear);
        LinearLayout privacy=panel("حماية الحسابات والخصوصية",
                "هذا تطبيق اختبار مستقل ولا يحتوي حسابات أو ديون أو قاعدة بيانات «وين صرفت». لا يطلب الإنترنت أو قراءة الرسائل القديمة، ولا يحفظ أرقام الهواتف أو النصوص الأصلية أو رموز التحقق. كل نتيجة للمراجعة فقط.");
        privacy.addView(text("مهم: تأكد من اسم التطبيق في قائمة التطبيقات. لا تستخدم هذا المختبر بدل برنامج المصروفات الأصلي.",12,TEAL,true));
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private String direction(String value) {
        if(FinanceRules.EXPENSE.equals(value))return "مصروف / صادر";
        if(FinanceRules.INCOME.equals(value))return "دخل / وارد";
        if(FinanceRules.TRANSFER.equals(value))return "تحويل يحتاج مراجعة";
        return "غير مؤكد";
    }

    private String money(Long minor,String currency) {
        return minor==null?"غير معروف":Money.format(minor,currency==null?"":currency);
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==SMS_REQUEST) {
            boolean granted=results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED;
            LabStore.setSmsEnabled(this,granted);
            render();
        }
    }
}
