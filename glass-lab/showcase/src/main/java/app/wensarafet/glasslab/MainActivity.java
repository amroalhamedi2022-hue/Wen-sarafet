package app.wensarafet.glasslab;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.wensarafet.glass.GlassPalette;
import app.wensarafet.glass.GlassSurfaces;

/**
 * Independent demonstration application. All amounts, categories, and names
 * below are invented sample data. It has no relationship to installed finance data.
 */
public final class MainActivity extends Activity {
    private int themeIndex = 0;
    private int currentPage = 0;
    private boolean eco = false;
    private boolean bars = false;
    private GlassPalette theme;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        render();
    }

    private int d(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        v.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        v.setIncludeFontPadding(true);
        return v;
    }

    private void tap(View v) {
        v.setOnTouchListener((view, event) -> {
            // This animator runs only in direct response to touch, never while idle.
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().cancel();
                view.animate().scaleX(.965f).scaleY(.965f).setDuration(85).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().cancel();
                view.animate().scaleX(1f).scaleY(1f).setDuration(210)
                        .setInterpolator(new OvershootInterpolator(1.10f)).start();
            }
            return false;
        });
    }

    private LinearLayout glassCard(boolean hero, int radius, int padding) {
        LinearLayout v = column(this);
        v.setPadding(d(padding), d(padding), d(padding), d(padding));
        v.setBackground(hero ? GlassSurfaces.hero(this, theme, radius, eco)
                : GlassSurfaces.frosted(this, theme, radius, eco));
        v.setElevation(d(hero ? 3 : 1));
        return v;
    }

    private TextView pill(String label, boolean selected, boolean prominent) {
        TextView v = text(label, 12f, selected ? Color.WHITE : theme.ink, selected);
        v.setGravity(Gravity.CENTER);
        v.setPadding(d(10), d(10), d(10), d(10));
        v.setMinHeight(d(40));
        v.setBackground(selected
                ? GlassSurfaces.hero(this, theme, 16, eco)
                : GlassSurfaces.row(this, theme, 16));
        if (prominent) v.setElevation(d(2));
        tap(v);
        return v;
    }

    private void section(LinearLayout body, String title, String desc) {
        LinearLayout titleRow = row(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 18, theme.ink, true);
        TextView subView = text(desc, 11, theme.muted, false);
        subView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(0, d(40), 1));
        titleRow.addView(subView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, d(40)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(44));
        lp.topMargin = d(12);
        body.addView(titleRow, lp);
    }

    private void render() {
        theme = GlassPalette.at(themeIndex);
        getWindow().setStatusBarColor(theme.deep);
        getWindow().setNavigationBarColor(theme.background);
        int barFlags = theme.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(barFlags);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(GlassSurfaces.backdrop(this, theme, eco));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = column(this);
        body.setPadding(d(14), d(16), d(14), d(108));
        scroll.addView(body);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        buildHeader(body);
        buildSelectors(body);
        if (currentPage == 0) showDashboard(body);
        else if (currentPage == 1) showOperations(body);
        else if (currentPage == 2) showAnalytics(body);
        else showDebts(body);

        LinearLayout navigation = row(this);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        navigation.setPadding(d(6), d(7), d(6), d(7));
        navigation.setBackground(GlassSurfaces.frosted(this, theme, 27, eco));
        navigation.setElevation(d(8));
        String[] titles = {"الرئيسية", "العمليات", "التحليل", "الديون"};
        for (int i = 0; i < titles.length; i++) {
            final int tab = i;
            TextView item = pill(titles[i], currentPage == i, false);
            item.setTextSize(11f);
            item.setOnClickListener(v -> { currentPage = tab; render(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
            lp.setMargins(d(2), 0, d(2), 0);
            navigation.addView(item, lp);
        }
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                -1, d(68), Gravity.BOTTOM);
        navParams.setMargins(d(12), 0, d(12), d(8));
        root.addView(navigation, navParams);

        setContentView(root);
    }

    private void buildHeader(LinearLayout body) {
        LinearLayout hero = glassCard(true, 28, 19);
        TextView micro = text("WEN GLASS  /  DESIGN LAB", 10, 0xFFDCEDFF, true);
        micro.setLetterSpacing(.10f);
        hero.addView(micro);
        TextView title = text("مختبر الزجاج السائل", 25, Color.WHITE, true);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2);
        titleP.topMargin = d(8);
        hero.addView(title, titleP);
        hero.addView(text("معاينة مستقلة • بدون حماية أو بيانات أو اتصال", 12,
                0xFFF0F6FF, false));
        TextView stamp = text("تأثير بصري ساكن • لا عمل في الخلفية", 11,
                0xFFDBF2FF, false);
        LinearLayout.LayoutParams stampP = new LinearLayout.LayoutParams(-1, -2);
        stampP.topMargin = d(17);
        hero.addView(stamp, stampP);
        body.addView(hero, new LinearLayout.LayoutParams(-1, d(142)));
    }

    private void buildSelectors(LinearLayout body) {
        section(body, "الثيمات", "4 تصاميم مستقلة");
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout items = row(this);
        String[] names = {"أزرق سائل", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView button = pill(names[i], i == themeIndex, false);
            button.setOnClickListener(v -> { themeIndex = index; render(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d(118), d(50));
            lp.setMargins(d(3), 0, d(3), 0);
            items.addView(button, lp);
        }
        scroll.addView(items);
        body.addView(scroll);

        LinearLayout controls = row(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        TextView ecoSwitch = pill(eco ? "🔋 موفر البطارية: مفعل" : "💎 جودة الزجاج: مفعل", false, false);
        ecoSwitch.setOnClickListener(v -> { eco = !eco; render(); });
        controls.addView(ecoSwitch, new LinearLayout.LayoutParams(0, d(50), 1));
        TextView detail = text("تبديل الجودة للتجربة", 11, theme.muted, false);
        detail.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0, d(50), 1);
        controls.addView(detail, detailParams);
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(-1, d(54));
        outer.topMargin = d(7);
        body.addView(controls, outer);
    }

    private void showDashboard(LinearLayout body) {
        section(body, "نظرة عامة", "أرقام تجريبية فقط");
        LinearLayout balances = row(this);
        String[] codes = {"ILS", "USD", "JOD"};
        String[] amounts = {"209.83", "80.00", "0.00"};
        for (int i = 0; i < 3; i++) {
            LinearLayout card = glassCard(false, 22, 11);
            TextView code = text(codes[i], 11, theme.muted, false);
            card.addView(code);
            TextView amount = text(amounts[i], 19, theme.ink, true);
            amount.setSingleLine(true);
            amount.setAutoSizeTextTypeUniformWithConfiguration(12, 19, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
            card.addView(amount);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(87), 1);
            lp.setMargins(d(4), 0, d(4), 0);
            balances.addView(card, lp);
        }
        body.addView(balances);

        section(body, "ملخص المصروفات", "بالشيكل");
        LinearLayout summary = row(this);
        String[] caps = {"مصاريف اليوم", "آخر 7 أيام", "هذا الشهر"};
        String[] values = {"0.00", "393.80", "2,210.80"};
        for (int i = 0; i < 3; i++) {
            LinearLayout card = glassCard(true, 21, 11);
            card.addView(text(caps[i], 11, 0xFFE5F0FF, false));
            TextView amount = text(values[i], 18, Color.WHITE, true);
            amount.setSingleLine(true);
            amount.setAutoSizeTextTypeUniformWithConfiguration(12, 18, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
            card.addView(amount);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(78), 1);
            lp.setMargins(d(4), 0, d(4), 0);
            summary.addView(card, lp);
        }
        body.addView(summary);
        chart(body);
        section(body, "الحسابات", "بطاقات خفيفة");
        addAccount(body, "بنك مثال", "ILS 101.26", "🏦");
        addAccount(body, "محفظة تجريبية", "ILS 81.79", "💠");
        section(body, "عمليات حديثة", "بيانات وهمية");
        addOperation(body, "أكل وشرب", "− ILS 12.00", "🥣", 0xFFA44865);
        addOperation(body, "استرداد مبلغ", "+ ILS 6.00", "↩", 0xFF008D79);
    }

    private void showOperations(LinearLayout body) {
        section(body, "كل العمليات", "تجربة تمرير بدون حسابات فعلية");
        String[] names = {"أكل وشرب", "مواصلات", "مواد منزلية", "استرداد مبلغ",
                "تسوق", "مستلزمات", "صيانة", "فواتير", "أكل وشرب", "مواصلات",
                "مصروف متكرر", "استرداد مبلغ", "شراء", "اشتراك", "مشتريات", "مكافأة"};
        String[] icons = {"🍲", "🚕", "🏠", "↩", "🛍", "📦", "🔧", "💡",
                "🥗", "🚌", "🛒", "↩", "🏬", "📱", "🛒", "★"};
        for (int i = 0; i < names.length; i++) {
            boolean income = i == 3 || i == 11 || i == 15;
            addOperation(body, names[i], (income ? "+ ILS " : "− ILS ") +
                    String.format(java.util.Locale.US, "%.2f", 3.5 + i * 2.25),
                    icons[i], income ? 0xFF008D79 : 0xFFA44865);
        }
        TextView note = text("العناصر المعروضة مجرد أمثلة. لا يوجد حفظ أو قاعدة بيانات.", 11,
                theme.muted, false);
        note.setGravity(Gravity.CENTER);
        body.addView(note);
    }

    private void showAnalytics(LinearLayout body) {
        section(body, "تحليل المصروفات", "محتوى تجريبي");
        LinearLayout stats = row(this);
        String[] names = {"اليوم", "الأسبوع", "الشهر"};
        String[] amounts = {"ILS 0.00", "ILS 393.80", "ILS 2,210.80"};
        for (int i = 0; i < 3; i++) {
            LinearLayout c = glassCard(true, 20, 10);
            c.addView(text(names[i], 11, Color.WHITE, false));
            c.addView(text(amounts[i], 13, Color.WHITE, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(79), 1);
            lp.setMargins(d(3), 0, d(3), 0);
            stats.addView(c, lp);
        }
        body.addView(stats);
        chart(body);
        section(body, "التصنيفات", "تجربة مكونات الزجاج");
        addProgress(body, "أكل وشرب", .80f);
        addProgress(body, "المواصلات", .54f);
        addProgress(body, "الفواتير", .27f);
    }

    private void showDebts(LinearLayout body) {
        section(body, "دفتر الديون", "أمثلة للعرض فقط");
        addAccount(body, "شخص تجريبي أ", "ILS 125.00", "👤");
        addAccount(body, "شخص تجريبي ب", "USD 25.00", "👤");
        addAccount(body, "شخص تجريبي ج", "ILS 40.00", "👤");
        section(body, "المسددة والمغلقة", "نموذج تصميم");
        addAccount(body, "دين مسدد", "ILS 0.00", "✓");
        LinearLayout info = glassCard(false, 22, 17);
        info.addView(text("لا توجد أموال أو ديون حقيقية داخل المختبر.", 15, theme.ink, true));
        info.addView(text("زر الجودة والثيمات بالأعلى يغيران المظهر هنا أيضًا.", 12,
                theme.muted, false));
        body.addView(info);
    }

    private void chart(LinearLayout body) {
        LinearLayout outer = glassCard(false, 23, 12);
        LinearLayout heading = row(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("مصاريف الأيام السبعة", 15, theme.ink, true),
                new LinearLayout.LayoutParams(0, d(39), 1));
        TextView toggle = pill(bars ? "خط بياني" : "أعمدة", false, false);
        toggle.setOnClickListener(v -> { bars = !bars; render(); });
        heading.addView(toggle, new LinearLayout.LayoutParams(d(100), d(36)));
        outer.addView(heading);
        LabChartView chart = new LabChartView(this, theme, bars);
        chart.setContentDescription("منحنى مصروفات تجريبية لسبعة أيام");
        outer.addView(chart, new LinearLayout.LayoutParams(-1, d(172)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(233));
        lp.topMargin = d(12);
        body.addView(outer, lp);
    }

    private void addAccount(LinearLayout body, String title, String amount, String icon) {
        LinearLayout card = row(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(14), d(10), d(14), d(10));
        card.setBackground(GlassSurfaces.row(this, theme, 19));
        TextView ico = pill(icon, false, false);
        ico.setTextSize(20);
        card.addView(ico, new LinearLayout.LayoutParams(d(49), d(49)));
        LinearLayout textCol = column(this);
        textCol.setPadding(d(12), 0, d(12), 0);
        textCol.addView(text(title, 14, theme.ink, true));
        textCol.addView(text("حساب للمعاينة فقط", 11, theme.muted, false));
        card.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(text(amount, 13, theme.ink, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(79));
        lp.setMargins(0, d(5), 0, d(5));
        body.addView(card, lp);
    }

    private void addOperation(LinearLayout body, String title, String amount, String icon, int color) {
        LinearLayout card = row(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(14), d(8), d(14), d(8));
        card.setBackground(GlassSurfaces.row(this, theme, 19));
        TextView emblem = pill(icon, false, false);
        emblem.setTextSize(19);
        card.addView(emblem, new LinearLayout.LayoutParams(d(46), d(46)));
        LinearLayout titleCol = column(this);
        titleCol.setPadding(d(12), 0, d(8), 0);
        titleCol.addView(text(title, 14, theme.ink, true));
        titleCol.addView(text("مثال • معاملة وهمية", 11, theme.muted, false));
        card.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        TextView amountView = text(amount, 13, color, true);
        amountView.setSingleLine(true);
        card.addView(amountView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(75));
        lp.setMargins(0, d(5), 0, d(5));
        body.addView(card, lp);
    }

    private void addProgress(LinearLayout body, String name, float progress) {
        LinearLayout c = glassCard(false, 19, 12);
        c.addView(text(name, 14, theme.ink, true));
        FrameLayout track = new FrameLayout(this);
        track.setBackground(GlassSurfaces.row(this, theme, 7));
        View fill = new View(this);
        fill.setBackground(GlassSurfaces.gradient(this, theme.deep, theme.brand, 7, false));
        FrameLayout.LayoutParams fillP = new FrameLayout.LayoutParams(
                -1, -1, Gravity.START);
        fill.setLayoutParams(fillP);
        track.addView(fill);
        track.post(() -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = Math.round(track.getWidth() * progress);
            fill.setLayoutParams(lp);
        });
        LinearLayout.LayoutParams trackP = new LinearLayout.LayoutParams(-1, d(9));
        trackP.topMargin = d(11);
        c.addView(track, trackP);
        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1, d(76));
        rowP.bottomMargin = d(8);
        body.addView(c, rowP);
    }

    /** Fake seven-day chart; geometry is cached on resize and no frame clock exists. */
    private static final class LabChartView extends View {
        private final GlassPalette p;
        private final boolean bars;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint area = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint points = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path linePath = new Path();
        private final Path areaPath = new Path();
        private final float[] heights = {.47f,.74f,.94f,.43f,.09f,.27f,.10f};
        private float chartBottom;
        private float slot;
        private float[] y = new float[7];

        LabChartView(Context context, GlassPalette palette, boolean barMode) {
            super(context);
            p = palette;
            bars = barMode;
            line.setColor(p.brand);
            line.setStrokeWidth(4f * context.getResources().getDisplayMetrics().density);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setStyle(Paint.Style.STROKE);
            points.setColor(p.brand);
        }

        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            if (w <= 0 || h <= 0) return;
            slot = w / 7f;
            chartBottom = h * .91f;
            linePath.reset();
            areaPath.reset();
            for (int i=0;i<7;i++) {
                float x = slot * (i + .5f);
                y[i] = chartBottom - heights[i] * h * .78f;
                if (i==0) {
                    linePath.moveTo(x,y[i]);
                    areaPath.moveTo(x,chartBottom);
                    areaPath.lineTo(x,y[i]);
                } else {
                    linePath.lineTo(x,y[i]);
                    areaPath.lineTo(x,y[i]);
                }
            }
            areaPath.lineTo(slot * 6.5f, chartBottom);
            areaPath.close();
            area.setShader(new LinearGradient(0, 0, 0, chartBottom,
                    new int[]{(p.brand & 0x00FFFFFF) | 0x55000000,
                              (p.accent & 0x00FFFFFF) | 0x00000000},
                    null, Shader.TileMode.CLAMP));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (slot <= 0) return;
            if (bars) {
                points.setColor(p.brand);
                for (int i=0;i<7;i++) {
                    float mid=slot*(i+.5f);
                    canvas.drawRoundRect(mid-slot*.18f,y[i],mid+slot*.18f,
                            chartBottom,slot*.1f,slot*.1f,points);
                }
            } else {
                canvas.drawPath(areaPath, area);
                canvas.drawPath(linePath, line);
                points.setColor(p.brand);
                for (int i=0;i<7;i++) canvas.drawCircle(slot*(i+.5f),y[i],dpx(),points);
            }
        }
        private float dpx() { return 4f * getResources().getDisplayMetrics().density; }
    }
}
