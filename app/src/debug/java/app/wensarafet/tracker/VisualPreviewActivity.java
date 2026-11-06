package app.wensarafet.tracker;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * DEBUG-ONLY screenshot fixture. It uses the *actual production drawables*,
 * typography helpers and native navigation lens, but deliberately shows
 * synthetic data and does not access FinanceDbHelper, login, SMS or accounts.
 * Images from this Activity must be labelled "emulator visual fixture":
 * they do NOT certify the real finance home layout.
 */
public final class VisualPreviewActivity extends Activity {
    private int theme;

    private int dp(int value) { return Ui.dp(this, value); }

    private TextView label(String string, float size, int colour, boolean bold) {
        TextView view = Ui.text(this, string, size, colour, bold);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return column;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return row;
    }

    private void space(LinearLayout parent, int heightDp) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void section(LinearLayout root, String title, String detail) {
        LinearLayout row = horizontal();
        TextView heading = label(title, 17, Ui.TEXT, true);
        row.addView(heading, new LinearLayout.LayoutParams(0, dp(34), 1f));
        TextView side = label(detail, 10, Ui.MUTED, false);
        side.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        row.addView(side, new LinearLayout.LayoutParams(0, dp(34), 1f));
        root.addView(row);
    }

    @Override protected void onCreate(Bundle saved) {
        theme = Math.max(25, Math.min(29, getIntent().getIntExtra("theme", 25)));
        Ui.saveTheme(this, theme);
        if (Ui.DARK) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(saved);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                Ui.DARK ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.glassScreenBackground(this));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = vertical();
        content.setPadding(dp(13), dp(12), dp(13), dp(110));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(16), dp(10));
        header.setBackground(Ui.glassHeaderBackground(this, 26));
        LinearLayout titles = vertical();
        TextView title = label("وين صرفت؟", 25,
                theme == 27 ? 0xFF65421F : Color.WHITE, true);
        TextView subtitle = label("أموالك واضحة، وخصوصيتك محفوظة", 11,
                theme == 27 ? 0xFF79572F : 0xFFF1E8D9, false);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        for (String glyph : new String[]{"⚙", "▦"}) {
            TextView circle = label(glyph, 24,
                    theme == 27 ? 0xFF65421F : Color.WHITE, true);
            circle.setGravity(Gravity.CENTER);
            circle.setBackground(Ui.glassSoftBackground(this, 26));
            LinearLayout.LayoutParams buttons =
                    new LinearLayout.LayoutParams(dp(45), dp(45));
            buttons.setMargins(dp(5), 0, 0, 0);
            header.addView(circle, buttons);
        }
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(94)));
        space(content, 17);

        section(content, "إجمالي أرصدتك", "حسب العملة");
        LinearLayout balances = horizontal();
        String[] coins = {"ILS", "USD", "JOD"};
        String[] holdings = {"96.64", "0.00", "0.00"};
        for (int i=0;i<coins.length;i++) {
            LinearLayout tile = vertical();
            tile.setGravity(Gravity.CENTER_VERTICAL);
            tile.setPadding(dp(11), dp(6), dp(11), dp(6));
            tile.setBackground(Ui.glassBalanceBackground(this, 22));
            TextView code = label(coins[i], 11, Ui.TEXT, false);
            TextView amount = label(coins[i] + " " + holdings[i], 18, Ui.TEXT, true);
            tile.addView(code);
            space(tile, 4);
            tile.addView(amount);
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(77), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            balances.addView(tile, p);
        }
        content.addView(balances);
        space(content, 15);

        section(content, "نظرة على مصروفك", "عملة الرسم: ILS");
        LinearLayout stats = horizontal();
        String[] periods = {"مصاريف اليوم", "آخر 7 أيام", "هذا الشهر"};
        String[] totals = {"ILS 0.00", "ILS 433.00", "ILS 253.00"};
        for (int i=0;i<3;i++) {
            LinearLayout tile = vertical();
            tile.setGravity(Gravity.CENTER_VERTICAL);
            tile.setPadding(dp(8), dp(6), dp(8), dp(6));
            tile.setBackground(Ui.glassAccentBackground(this, 20));
            int ink = theme == 27 ? 0xFF453329 : Color.WHITE;
            tile.addView(label(periods[i], 11, ink, false));
            space(tile, 4);
            tile.addView(label(totals[i], 16, ink, true));
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(71), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            stats.addView(tile, p);
        }
        content.addView(stats);
        space(content, 14);

        LinearLayout chart = vertical();
        chart.setPadding(dp(12), dp(10), dp(12), dp(5));
        chart.setBackground(Ui.glassChartBackground(this, 22));
        chart.addView(label("مصاريف الأيام السبعة", 15, Ui.TEXT, true));
        chart.addView(new SyntheticChartView(),
                new LinearLayout.LayoutParams(-1, dp(162)));
        content.addView(chart);
        space(content, 12);

        section(content, "الحسابات", "3 حسابات");
        int[] icons={R.drawable.icon_bop_mark,R.drawable.icon_jawwal_pay_mark,
                R.drawable.icon_palpay_mark};
        String[] names={"بنك فلسطين", "Jawwal Pay", "PalPay"};
        String[] amounts={"ILS 40.99","ILS 28.87","ILS 78.00"};
        for (int i=0;i<3;i++) {
            LinearLayout account = horizontal();
            account.setGravity(Gravity.CENTER_VERTICAL);
            account.setPadding(dp(10), dp(7), dp(10), dp(7));
            account.setBackground(Ui.glassListBackground(this, 22));
            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[i]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            account.addView(icon, new LinearLayout.LayoutParams(dp(49), dp(49)));
            LinearLayout info=vertical();
            info.setPadding(dp(10),0,0,0);
            info.addView(label(names[i], 15, Ui.TEXT, true));
            info.addView(label("ILS",11,Ui.MUTED,false));
            account.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
            TextView amount=label(amounts[i],15,Ui.TEXT,true);
            amount.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            account.addView(amount, new LinearLayout.LayoutParams(dp(94),-1));
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(-1, dp(67));
            p.setMargins(0,0,0,dp(7));
            content.addView(account,p);
        }

        LinearLayout nav = horizontal();
        nav.setPadding(dp(8),dp(6),dp(8),dp(6));
        nav.setBackground(Ui.glassNavigationBackground(this, 26));
        TextView[] tabs=new TextView[4];
        String[] labels={"🏠\nالرئيسية","📋\nالعمليات",
                "🤝\nالديون","📊\nالتحليل"};
        for (int i=0;i<4;i++) {
            TextView tab = label(labels[i], 12, Ui.TEXT, i==0);
            tab.setGravity(Gravity.CENTER);
            tab.setOnClickListener(v -> { }); // Preview only.
            nav.addView(tab,new LinearLayout.LayoutParams(0, -1,1f));
            tabs[i]=tab;
        }
        NavigationVisualDecorator.install(nav,tabs,0);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                -1,dp(74),Gravity.BOTTOM);
        navParams.setMargins(dp(12),0,dp(12),dp(10));
        root.addView(nav,navParams);

        TextView add = label("+",32,Color.WHITE,true);
        add.setGravity(Gravity.CENTER);
        add.setTag("wen_intro_fab");
        add.setBackground(Ui.glassFloatingAddBackground(this));
        add.setOnClickListener(v -> { }); // Preview only, never transactions.
        Ui.pressAnimation(add);
        FrameLayout.LayoutParams floating = new FrameLayout.LayoutParams(
                dp(68),dp(68),Gravity.BOTTOM | Gravity.LEFT);
        floating.setMargins(dp(22),0,0,dp(93));
        root.addView(add,floating);
        setContentView(root);
    }

    private final class SyntheticChartView extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path polyline = new Path();
        private final Path area = new Path();
        private final float[] y = {.64f,.31f,.12f,.72f,.68f,.77f,.77f};
        SyntheticChartView() { super(VisualPreviewActivity.this); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left=dp(17), top=dp(18), bottom=getHeight()-dp(23);
            float width=getWidth()-2*left, height=bottom-top;
            polyline.reset(); area.reset();
            for(int i=0;i<7;i++) {
                float x=left+width*i/6f;
                float v=top+y[i]*height;
                if(i==0){polyline.moveTo(x,v);area.moveTo(x,v);}
                else{polyline.lineTo(x,v);area.lineTo(x,v);}
            }
            area.lineTo(left+width,bottom);
            area.lineTo(left,bottom);
            area.close();
            int line = theme==29 ? 0xFFFFDBFF :
                    theme==28 ? 0xFFFFE9BF : theme==27 ? 0xFFD39632 :
                    0xFFFFD27B;
            fill.setShader(new LinearGradient(0,top,0,bottom,
                    new int[]{(line & 0x00FFFFFF)|0x66000000,
                    (line & 0x00FFFFFF)|0x00000000},null,Shader.TileMode.CLAMP));
            canvas.drawPath(area,fill);
            stroke.setShader(null);
            stroke.setColor(line); stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(2));stroke.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(polyline,stroke);
            point.setColor(line);point.setStyle(Paint.Style.FILL);
            for(int i=0;i<7;i++) {
                float x=left+width*i/6f;
                canvas.drawCircle(x,top+y[i]*height,dp(4),point);
            }
        }
    }
}
