package app.wensarafet.tracker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.TextView;

/** Test-only real RemoteViews host. No real accounts, database or security routes. */
public final class WidgetPreviewActivity extends Activity {
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView heading(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value); text.setTextColor(Color.WHITE);
        text.setTextSize(size); text.setGravity(Gravity.CENTER);
        text.setPadding(dp(3), dp(3), dp(3), dp(3));
        return text;
    }
    private void add(LinearLayout target, View view, int width, int height, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(bottom);
        target.addView(view,lp);
    }
    private View inflated(int layout, String theme, String action) {
        RemoteViews r = new RemoteViews(getPackageName(), layout);
        if (layout == R.layout.widget_balances) {
            r.setTextViewText(R.id.widget_jawwal_balance, "213.84 ILS");
            r.setTextViewText(R.id.widget_bop_balance, "40.99 ILS");
        } else if (layout == R.layout.widget_balances_triple) {
            r.setTextViewText(R.id.widget_triple_palpay_balance, "0.00 ILS");
            r.setTextViewText(R.id.widget_triple_jawwal_balance, "213.84 ILS");
            r.setTextViewText(R.id.widget_triple_bop_balance, "40.99 ILS");
        } else {
            int glass = "bop".equals(action) ? R.drawable.widget_crystal_quick_bop :
                "palpay".equals(action) ? R.drawable.widget_crystal_quick_palpay :
                R.drawable.widget_crystal_quick_jawwal;
            int icon = "bop".equals(action) ? R.drawable.icon_bop_mark :
                "palpay".equals(action) ? R.drawable.icon_palpay_mark :
                R.drawable.icon_jawwal_pay_mark;
            r.setImageViewResource(R.id.widget_crystal_glass, glass);
            r.setImageViewResource(R.id.widget_background_image, icon);
            r.setTextViewText(R.id.widget_title, theme);
            r.setTextViewText(R.id.widget_subtitle,
                    "bop".equals(action) ? "USSD" : "palpay".equals(action) ? "تحويل" : "صديق");
            r.setViewVisibility(R.id.widget_choose_contact,
                    "bop".equals(action) ? View.GONE : View.VISIBLE);
        }
        return r.apply(this, null);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF07121C,0xFF1D2A3E,0xFF40263C,0xFF0A1926});
        FrameLayout root = new FrameLayout(this); root.setBackground(bg);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14),dp(32),dp(14),dp(24));
        add(content, heading("DARK CRYSTAL  ·  WIDGET LAB", 16f), -1, -2, 6);
        add(content, heading("Actual Android RemoteViews preview • sandbox 2.2", 11f), -1, -2, 28);
        add(content, inflated(R.layout.widget_balances, "", ""), 300, 158, 22);
        add(content, inflated(R.layout.widget_balances_triple, "", ""), 320, 142, 22);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        View pal = inflated(R.layout.widget_quick_action,"PalPay","palpay");
        View bop = inflated(R.layout.widget_quick_action,"Bank Of Palestine","bop");
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(dp(136),dp(145));
        p1.rightMargin=dp(9);
        row.addView(pal,p1);
        row.addView(bop,new LinearLayout.LayoutParams(dp(136),dp(145)));
        add(content,row,-1,-2,17);
        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        View jaw1=inflated(R.layout.widget_quick_action,"Jawwal Pay","jawwal_friend");
        View jaw2=inflated(R.layout.widget_quick_action,"Jawwal Pay","jawwal_merchant");
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(dp(136),dp(145));p2.rightMargin=dp(9);
        row2.addView(jaw1,p2);
        row2.addView(jaw2,new LinearLayout.LayoutParams(dp(136),dp(145)));
        add(content,row2,-1,-2,22);
        add(content, heading("UI optics and wallet logic in main app are unchanged.", 11f),-1,-2,4);
        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }
}