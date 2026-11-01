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
    private boolean optics = false;
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
        body.setPadding(d(14), d(12), d(14), d(108));
        scroll.addView(body);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        buildHeader(body);
        buildSelectors(body);
        if (currentPage == 0) showDashboard(body);
        else if (currentPage == 1) showOperations(body);
        else if (currentPage == 2) showAnalytics(body);
        else if (currentPage == 3) showDebts(body);
        else showSettings(body);

        LinearLayout navigation = row(this);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        navigation.setPadding(d(6), d(7), d(6), d(7));
        navigation.setBackground(GlassSurfaces.navigation(this, theme, 27, eco));
        navigation.setElevation(d(8));
        String[] titles = {"⌂  الرئيسية", "▤  العمليات", "◴  التحليل", "◈  الديون"};
        for (int i = 0; i < titles.length; i++) {
            final int tab = i;
            TextView item = text(titles[i], 11.5f,
                    currentPage == i ? theme.deep : theme.muted, currentPage == i);
            item.setGravity(Gravity.CENTER);
            item.setPadding(d(3), d(8), d(3), d(8));
            if (currentPage == i) {
                item.setBackground(GlassSurfaces.selectedTab(this, theme, 19));
            }
            tap(item);
            item.setOnClickListener(v -> { currentPage = tab; render(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
            lp.setMargins(d(2), 0, d(2), 0);
            navigation.addView(item, lp);
        }
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                -1, d(68), Gravity.BOTTOM);
        navParams.setMargins(d(12), 0, d(12), d(11));
        root.addView(navigation, navParams);

        setContentView(root);
    }

    private void buildHeader(LinearLayout body) {
        // Recreate the approved blue concept: compact deep-blue banner,
        // white RTL title and two circular translucent controls.
        LinearLayout hero = row(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(d(17), d(13), d(15), d(13));
        hero.setBackground(GlassSurfaces.opticalHero(this, theme, 26, eco, optics));

        LinearLayout writing = column(this);
        TextView title = text("وين صرفت؟", 25, Color.WHITE, true);
        writing.addView(title);
        TextView subtitle = text("أموالك واضحة، وخصوصيتك محفوظة", 11,
                0xFFEAF3FF, false);
        writing.addView(subtitle);
        hero.addView(writing, new LinearLayout.LayoutParams(0, -2, 1));

        TextView settings = text("⚙", 22, Color.WHITE, false);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(GlassSurfaces.headerButton(this, theme));
        settings.setContentDescription("معاينة تصميم الإعدادات");
        settings.setOnClickListener(v -> { currentPage = 4; render(); });
        tap(settings);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(d(43), d(43));
        actionParams.setMargins(d(4), 0, d(4), 0);
        hero.addView(settings, actionParams);

        TextView gridIcon = text("▦", 22, Color.WHITE, false);
        gridIcon.setGravity(Gravity.CENTER);
        gridIcon.setBackground(GlassSurfaces.headerButton(this, theme));
        gridIcon.setContentDescription("العودة إلى معاينة الرئيسية");
        gridIcon.setOnClickListener(v -> { currentPage = 0; render(); });
        tap(gridIcon);
        hero.addView(gridIcon, new LinearLayout.LayoutParams(d(43), d(43)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(104));
        lp.bottomMargin = d(10);
        body.addView(hero, lp);
    }

    private void buildSelectors(LinearLayout body) {
        // The lab controls are deliberately compact: original app content remains
        // visually comparable while quality and palette can be switched instantly.
        HorizontalScrollView themeScroll = new HorizontalScrollView(this);
        themeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout options = row(this);
        String[] names = {"أزرق سائل", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي"};
        for (int i = 0; i < names.length; i++) {
            final int option = i;
            TextView chip = pill(names[i], i == themeIndex, false);
            chip.setOnClickListener(v -> { themeIndex = option; render(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(d(116), d(43));
            p.setMargins(d(3), d(3), d(3), d(3));
            options.addView(chip, p);
        }
        themeScroll.addView(options);
        body.addView(themeScroll);

        LinearLayout performance = row(this);
        performance.setGravity(Gravity.CENTER_VERTICAL);
        TextView ecoToggle = pill(eco ? "🔋 اقتصادي" : "💎 زجاج", false, false);
        ecoToggle.setContentDescription("التبديل بين جودة الزجاج وتوفير الطاقة");
        ecoToggle.setOnClickListener(v -> { eco = !eco; render(); });
        performance.addView(ecoToggle, new LinearLayout.LayoutParams(0, d(42), 1));
        TextView opticalToggle = pill(optics ? "✨ عدسة GPU تعمل" : "✨ عدسة GPU مطفأة", false, false);
        opticalToggle.setOnClickListener(v -> { optics = !optics; render(); });
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, d(42), 1);
        op.setMargins(d(5), 0, d(5), 0);
        performance.addView(opticalToggle, op);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, d(48));
        rowParams.bottomMargin = d(7);
        body.addView(performance, rowParams);
    }

    private void showDashboard(LinearLayout body) {
        section(body, "إجمالي أرصدتك", "حسب العملة • بيانات للمعاينة");
        LinearLayout balances = row(this);
        String[] codes = {"ILS", "USD", "JOD"};
        String[] amounts = {"209.83", "0.00", "0.00"};
        for (int i = 0; i < codes.length; i++) {
            LinearLayout card = glassCard(false, 20, 12);
            TextView currency = text(codes[i], 11, theme.muted, false);
            card.addView(currency);
            TextView amount = text(codes[i] + " " + amounts[i], 17, theme.ink, true);
            amount.setSingleLine(true);
            amount.setAutoSizeTextTypeUniformWithConfiguration(12, 17, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
            LinearLayout.LayoutParams amountP = new LinearLayout.LayoutParams(-1, -2);
            amountP.topMargin = d(6);
            card.addView(amount, amountP);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, d(85), 1);
            p.setMargins(d(3), 0, d(3), 0);
            balances.addView(card, p);
        }
        body.addView(balances);

        section(body, "نظرة على مصروفك", "عملة العرض: ILS");
        LinearLayout stats = row(this);
        String[] captions = {"مصاريف اليوم", "آخر 7 أيام", "هذا الشهر"};
        String[] totals = {"0.00", "393.80", "2,210.80"};
        for (int i = 0; i < captions.length; i++) {
            LinearLayout card = glassCard(true, 21, 11);
            card.addView(text(captions[i], 11, 0xFFEAF2FF, false));
            TextView money = text("ILS " + totals[i], 17, Color.WHITE, true);
            money.setSingleLine(true);
            money.setAutoSizeTextTypeUniformWithConfiguration(11, 17, 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
            card.addView(money);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, d(78), 1);
            p.setMargins(d(3), 0, d(3), 0);
            stats.addView(card, p);
        }
        body.addView(stats);
        chart(body);

        section(body, "الحسابات", "3 حسابات تجريبية");
        addAccount(body, "بنك فلسطين • عرض", "ILS 101.26", "🏦");
        addAccount(body, "Jawwal Pay • عرض", "ILS 81.79", "💠");
        addAccount(body, "PalPay • عرض", "ILS 26.78", "◆");

        section(body, "آخر العمليات", "أحدث 5");
        addOperation(body, "استرداد مبلغ", "+ ILS 6.00", "↩", 0xFF078C79);
        addOperation(body, "أكل وشرب", "− ILS 12.00", "♨", 0xFFB34B70);
        addOperation(body, "أكل وشرب", "− ILS 13.00", "♨", 0xFFB34B70);
        addOperation(body, "أكل وشرب", "− ILS 4.00", "♨", 0xFFB34B70);
        addOperation(body, "سداد دين", "+ ILS 20.00", "✓", 0xFF376AC8);
    }

    private void showOperations(LinearLayout body) {
        section(body, "كل العمليات", "بحث وعرض تجريبي");
        TextView search = text("⌕   ابحث بالملاحظة أو الحساب أو التصنيف أو المبلغ", 13,
                theme.muted, false);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(d(17), 0, d(17), 0);
        search.setBackground(GlassSurfaces.row(this, theme, 19));
        search.setContentDescription("حقل بحث للعرض البصري فقط، بدون بيانات محفوظة");
        LinearLayout.LayoutParams searchP = new LinearLayout.LayoutParams(-1, d(50));
        searchP.bottomMargin = d(8);
        body.addView(search, searchP);
        LinearLayout filters = row(this);
        String[] names = {"كل الأنواع  ⌄", "كل العملات  ⌄"};
        for (String name : names) {
            TextView filter = pill(name, false, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(46), 1);
            lp.setMargins(d(3), 0, d(3), 0);
            filters.addView(filter, lp);
        }
        body.addView(filters);
        TextView count = text("عُرضت معاملات تجريبية • القائمة طويلة لاختبار التمرير", 11,
                theme.muted, false);
        LinearLayout.LayoutParams countP = new LinearLayout.LayoutParams(-1, d(46));
        body.addView(count, countP);

        String[] categories = {"استرداد مبلغ","أكل وشرب","أكل وشرب","أكل وشرب",
                "سداد دين","خاص بي","أكل وشرب","مواصلات","شراء","استرداد مبلغ",
                "مستلزمات","فواتير","أكل وشرب","تسوق","مكافأة"};
        String[] icons = {"↩","♨","♨","♨","✓","♙","♨","🚕","▣","↩",
                "◈","◫","♨","▣","★"};
        for (int i = 0; i < categories.length; i++) {
            boolean received = i == 0 || i == 4 || i == 9 || i == 14;
            String amount = (received ? "+ ILS " : "− ILS ") +
                    String.format(java.util.Locale.US, "%.2f", received ? 6.0 + i :
                            12.0 + (i % 5) * 1.5);
            addOperation(body, categories[i], amount, icons[i],
                    received ? 0xFF078C79 : 0xFFB34B70);
        }
    }

    private void showAnalytics(LinearLayout body) {
        section(body, "تحليل المصروفات", "معاينة تجريبية");
        LinearLayout currencies = row(this);
        for (String code : new String[]{"ILS", "USD", "JOD"}) {
            TextView chip = pill(code, "ILS".equals(code), false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(42), 1);
            lp.setMargins(d(3), 0, d(3), 0);
            currencies.addView(chip, lp);
        }
        body.addView(currencies);

        LinearLayout large = glassCard(true, 22, 16);
        large.addView(text("مصاريف اليوم", 12, 0xFFE3EDFF, false));
        large.addView(text("ILS 0.00", 24, Color.WHITE, true));
        large.addView(text("اضغط لعرض التفاصيل • معاينة فقط", 11, 0xFFDFEAFF, false));
        LinearLayout.LayoutParams largeP = new LinearLayout.LayoutParams(-1, d(100));
        largeP.topMargin = d(12);
        body.addView(large, largeP);

        LinearLayout metrics = row(this);
        for (int i = 0; i < 2; i++) {
            LinearLayout group = column(this);
            String[] names = i == 0 ?
                    new String[]{"هذا الأسبوع","الدخل هذا الشهر"} :
                    new String[]{"هذا الشهر","المستلم هذا الشهر"};
            String[] values = i == 0 ?
                    new String[]{"ILS 393.80","ILS 850.00"} :
                    new String[]{"ILS 2,210.80","ILS 500.00"};
            for (int j = 0; j < 2; j++) {
                LinearLayout card = glassCard(true, 19, 12);
                card.addView(text(names[j], 11, 0xFFE6EDFF, false));
                card.addView(text(values[j], 16, Color.WHITE, true));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, d(80));
                cp.topMargin = d(7);
                group.addView(card, cp);
            }
            LinearLayout.LayoutParams groupP = new LinearLayout.LayoutParams(0, -2, 1);
            groupP.setMargins(d(3), 0, d(3), 0);
            metrics.addView(group, groupP);
        }
        body.addView(metrics);

        LinearLayout budget = glassCard(false, 22, 16);
        budget.addView(text("ميزانية الشهر — ILS", 16, theme.ink, true));
        budget.addView(text("حدد سقفًا شهريًا لتعرف فورًا إن كنت ضمن الميزانية أو تجاوزتها.", 12,
                theme.muted, false));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, d(91));
        bp.topMargin = d(12);
        body.addView(budget, bp);

        chart(body);
        section(body, "تصنيفات هذا الشهر", "توزيع تجريبي");
        addProgress(body, "أكل وشرب", .80f);
        addProgress(body, "المواصلات", .54f);
        addProgress(body, "الفواتير", .27f);
    }

    private void showDebts(LinearLayout body) {
        section(body, "دفتر الديون", "بيانات تجريبية");
        LinearLayout actions = row(this);
        for (String label : new String[]{"＋ إضافة دين", "♧ الأشخاص"}) {
            TextView btn = pill(label, false, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(46), 1);
            lp.setMargins(d(3), 0, d(3), 0);
            actions.addView(btn, lp);
        }
        body.addView(actions);
        LinearLayout debtsFilter = row(this);
        for (String label : new String[]{"إليّ — لي عند الناس", "عليّ — للناس عندي"}) {
            TextView chip = pill(label, label.startsWith("إلي"), false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(45), 1);
            lp.setMargins(d(3), d(8), d(3), 0);
            debtsFilter.addView(chip, lp);
        }
        body.addView(debtsFilter);
        section(body, "الديون النشطة", "6 أشخاص");
        for (int i = 0; i < 6; i++) {
            LinearLayout card = row(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(d(16), 0, d(16), 0);
            card.setBackground(GlassSurfaces.row(this, theme, 19));
            LinearLayout label = column(this);
            label.addView(text("شخص تجريبي " + (i + 1), 14, theme.ink, true));
            label.addView(text("المتبقي: ILS " + (20 + 15 * i) + ".00 • 1 نشط",
                    11, theme.muted, false));
            card.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            TextView arrow = text("‹", 27, theme.brand, true);
            card.addView(arrow);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, d(74));
            cp.setMargins(0, d(5), 0, d(5));
            body.addView(card, cp);
        }
        section(body, "المسددة والمغلقة", "3 أشخاص");
        addAccount(body, "دين مغلق", "ILS 0.00", "✓");
    }

    private void showSettings(LinearLayout body) {
        section(body, "الإعدادات", "تصميم للعرض فقط");
        section(body, "مظهر التطبيق", "٤ تنسيقات");
        TextView detail = text("اختيار الثيم متاح أعلى الشاشة، ولا يتم حفظ أي إعداد في المختبر.", 12,
                theme.muted, false);
        body.addView(detail, new LinearLayout.LayoutParams(-1, d(42)));
        section(body, "تخصيص الواجهة", "مكونات زجاجية موحدة");
        addSettingsRow(body, "تخطيط الصفحة الرئيسية", "الحجم والموقع أسفل الشاشة", "تخصيص");
        addSettingsRow(body, "سجل التعديلات", "نموذج مرئي فقط", "◷");
        addSettingsRow(body, "إدارة التصنيفات والرموز", "نموذج مرئي فقط", "◇");
        section(body, "حماية التطبيق", "غير مطبقة في المختبر");
        addSettingsRow(body, "رمز دخول وبصمة", "النسخة المستقلة بلا تفعيل أو بيانات", "—");
        section(body, "تذكير المصاريف", "تجربة تصميم");
        addSettingsRow(body, "تذكير يومي", "20:00 • بدون إشعارات حقيقية", "◯");
        addSettingsRow(body, "صوت واهتزاز الإشعارات", "لا تُشغَّل خدمات خلفية", "◯");
    }

    private void addSettingsRow(LinearLayout body, String title, String subtitle, String action) {
        LinearLayout card = row(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(14), d(10), d(14), d(10));
        card.setBackground(GlassSurfaces.row(this, theme, 20));
        LinearLayout information = column(this);
        information.addView(text(title, 14, theme.ink, true));
        information.addView(text(subtitle, 11, theme.muted, false));
        card.addView(information, new LinearLayout.LayoutParams(0, -2, 1));
        TextView indicator = text(action, 12, theme.brand, true);
        indicator.setGravity(Gravity.CENTER);
        indicator.setPadding(d(9), d(6), d(9), d(6));
        indicator.setBackground(GlassSurfaces.selectedTab(this, theme, 13));
        card.addView(indicator);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(72));
        lp.setMargins(0, d(5), 0, d(5));
        body.addView(card, lp);
    }

    private void chart(LinearLayout body) {
        LinearLayout outer = glassCard(false, 23, 12);
        outer.setBackground(GlassSurfaces.chart(this, theme, 23, eco));
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
