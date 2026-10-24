package app.wensarafet.tracker;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static int BRAND;
    public static int BRAND_DARK;
    public static int BRAND_SOFT;
    public static int BACKGROUND;
    public static int SURFACE;
    public static int TEXT;
    public static int MUTED;
    public static int EXPENSE;
    public static int INCOME;
    public static int VIOLET;
    public static int CORAL;
    public static int SKY;
    public static boolean DARK;

    public static final String[] THEME_NAMES = {
            "زمرد نعناعي", "ملكي نيلي", "تركواز", "نيلي ليلي",
            "رملي دافئ", "برغندي", "قرمزي", "كهرماني",
            "مشمشي", "وردي هادئ", "لافندر", "جليدي",
            "غابة قاتمة", "فحمي", "لؤلؤي", "نحاسي",
            "Liquid Aurora", "Liquid Fusion"
    };

    // brand, dark, soft, background, surface, text, muted, expense, income, accent1, accent2, accent3
    private static final int[][] THEMES = {
            {0xFF0B8F6B,0xFF05624C,0xFFD9F7EB,0xFFF6FBF8,0xFFFFFFFF,0xFF132B25,0xFF688079,0xFFC63D4D,0xFF0B8F6B,0xFF0B8F6B,0xFF43C59E,0xFF74DDB8},
            {0xFF2754D7,0xFF17358F,0xFFE6ECFF,0xFFF6F8FF,0xFFFFFFFF,0xFF17223F,0xFF6C7592,0xFFC53C54,0xFF0B8F75,0xFF2754D7,0xFF5D86F1,0xFF8DAAF5},
            {0xFF089EAA,0xFF06616A,0xFFDDF9FA,0xFFF6FCFC,0xFFFFFFFF,0xFF112D31,0xFF6E8285,0xFFC63D4D,0xFF008C75,0xFF089EAA,0xFF46C9CF,0xFF8DE5E2},
            {0xFF6D46CF,0xFF34218A,0xFFECE7FB,0xFFF9F7FE,0xFFFFFFFF,0xFF282038,0xFF766D88,0xFFC4435B,0xFF4A856F,0xFF6D46CF,0xFF9068F2,0xFFB9A3F3},
            {0xFFB47735,0xFF70451F,0xFFF6E8D4,0xFFFCF8F2,0xFFFFFFFF,0xFF3B2A1F,0xFF8A7667,0xFFB74242,0xFF6E8F4C,0xFFB47735,0xFFD6A763,0xFFF0C98D},
            {0xFF9B3155,0xFF5E1731,0xFFF7DEE7,0xFFFCF5F7,0xFFFFFFFF,0xFF351923,0xFF876773,0xFFB32648,0xFF477E66,0xFF9B3155,0xFFC65378,0xFFEEA7BC},
            {0xFFC52F3F,0xFF781923,0xFFFBE0E3,0xFFFFF7F7,0xFFFFFFFF,0xFF391A1D,0xFF8B696C,0xFFC52F3F,0xFF3F8165,0xFFC52F3F,0xFFE24A56,0xFFF5A0A8},
            {0xFFB87800,0xFF6B4700,0xFFFFF1C7,0xFFFFFCF4,0xFFFFFFFF,0xFF33270D,0xFF80704A,0xFFB74242,0xFF6F8140,0xFFB87800,0xFFE6A91E,0xFFFFD978},
            {0xFFD7672C,0xFF813614,0xFFFBE6DA,0xFFFFF8F4,0xFFFFFFFF,0xFF3B2217,0xFF8C6F63,0xFFC24439,0xFF57815F,0xFFD7672C,0xFFEB8A55,0xFFFFB083},
            {0xFFC34D7A,0xFF7A2748,0xFFF9E0E9,0xFFFFF7FA,0xFFFFFFFF,0xFF3A1E29,0xFF8C6D78,0xFFC33758,0xFF4F8068,0xFFC34D7A,0xFFDF7EA1,0xFFF3B4C9},
            {0xFF7A55C7,0xFF493180,0xFFEEE7FC,0xFFFAF8FF,0xFFFFFFFF,0xFF282038,0xFF756C86,0xFFC4435B,0xFF4A856F,0xFF7A55C7,0xFFA486E1,0xFFCDBBF3},
            {0xFF2D83B8,0xFF195171,0xFFE1F2FC,0xFFF7FBFE,0xFFFFFFFF,0xFF172D3A,0xFF6B7D88,0xFFC44156,0xFF238068,0xFF2D83B8,0xFF66ACD4,0xFFA9D8EF},
            {0xFF2D8C66,0xFF176447,0xFFDDF3E9,0xFFF6FBF8,0xFFFFFFFF,0xFF173128,0xFF698078,0xFFC54150,0xFF2D8C66,0xFF2D8C66,0xFF55B68A,0xFF9ADABC},
            {0xFF596579,0xFF313946,0xFFE8EBEF,0xFFF8F9FB,0xFFFFFFFF,0xFF232933,0xFF737C89,0xFFC54150,0xFF4F816B,0xFF596579,0xFF7D8AA3,0xFFB7BEC9},
            {0xFF687083,0xFF414858,0xFFECEEF3,0xFFFAFAFC,0xFFFFFFFF,0xFF222632,0xFF7B8291,0xFFC34054,0xFF4C826B,0xFF687083,0xFF929AAA,0xFFC5CAD4},
            {0xFFB65F3C,0xFF71351F,0xFFF5E0D6,0xFFFCF7F4,0xFFFFFFFF,0xFF39231B,0xFF876F65,0xFFC3433F,0xFF5A8064,0xFFB65F3C,0xFFD18463,0xFFEAB49B},
            {0xFF6857D5,0xFF312D89,0xFFEAE6FD,0xFFF7F8FF,0xFFFFFFFF,0xFF1A2443,0xFF69738F,0xFFC23A62,0xFF148E8C,0xFF6857D5,0xFF2DAFC9,0xFFFFAB8B},
            {0xFF456AD9,0xFF252D82,0xFFE7EBFF,0xFFF7F9FF,0xFFFFFFFF,0xFF1F2550,0xFF687495,0xFFC34465,0xFF2F9A91,0xFF456AD9,0xFF9A5DE1,0xFFF5A26D}
    };

    private static final int[][] THEME_LAYERS = {
            {0xFF045E49,0xFF078565,0xFF35BF91,0xFFA7EED5},
            {0xFF17358F,0xFF2754D7,0xFF5D86F1,0xFFB9CDFF},
            {0xFF06616A,0xFF089EAA,0xFF46C9CF,0xFFB5F2EF},
            {0xFF17114B,0xFF34218A,0xFF6A3FCB,0xFF9C77FF},
            {0xFF70451F,0xFFA56B32,0xFFD6A763,0xFFF2D5A4},
            {0xFF5E1731,0xFF8C294A,0xFFC65378,0xFFEEA7BC},
            {0xFF781923,0xFFAF2533,0xFFE24A56,0xFFF5A0A8},
            {0xFF6B4700,0xFFA86C00,0xFFE6A91E,0xFFFFE093},
            {0xFF813614,0xFFBA5124,0xFFEB8A55,0xFFFFC09D},
            {0xFF7A2748,0xFFA83C67,0xFFDF7EA1,0xFFF3B4C9},
            {0xFF493180,0xFF6946AC,0xFFA486E1,0xFFD7C8F8},
            {0xFF195171,0xFF2879A8,0xFF66ACD4,0xFFBDE4F5},
            {0xFF0D3B2E,0xFF176447,0xFF2D8C66,0xFF76D6AE},
            {0xFF1C2430,0xFF394456,0xFF657187,0xFFA7B0C1},
            {0xFF414858,0xFF687083,0xFF929AAA,0xFFD4D8E0},
            {0xFF71351F,0xFF9A4B2E,0xFFD18463,0xFFEAB49B},
            {0xFF242D66,0xFF6359BB,0xFF2DAFC9,0xFFFFAB8B},
            {0xFF1E2559,0xFF456AD9,0xFF9A5DE1,0xFFF5A26D}
    };

    private Ui() {}

    public static void applyTheme(Context context) {
        int theme = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("theme", 0);
        if (theme < 0 || theme >= THEMES.length) theme = 0;
        DARK = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int[] p = THEMES[theme];
        if (DARK) {
            int[] layers = THEME_LAYERS[theme];
            int darkBackground = mix(0xFF090C12, layers[0], .18f);
            int darkSurface = mix(0xFF151922, layers[0], .14f);
            int darkSoft = mix(darkSurface, layers[1], .30f);
            set(mix(p[0], Color.WHITE, .18f), layers[0], darkSoft, darkBackground, darkSurface,
                    0xFFF6F7FA, 0xFFA8AFBA, 0xFFFF707C, 0xFF58D3A0,
                    mix(p[0], Color.WHITE, .14f), mix(p[0], Color.WHITE, .28f), mix(p[0], Color.WHITE, .42f));
        } else {
            set(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11]);
        }
    }

    private static void set(int brand, int dark, int soft, int background, int surface, int text, int muted, int expense, int income, int violet, int coral, int sky) {
        BRAND = brand; BRAND_DARK = dark; BRAND_SOFT = soft; BACKGROUND = background; SURFACE = surface;
        TEXT = text; MUTED = muted; EXPENSE = expense; INCOME = income; VIOLET = violet; CORAL = coral; SKY = sky;
    }

    public static int themeIndex(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("theme", 0);
    }

    public static void saveTheme(Context context, int index) {
        if (index < 0 || index >= THEMES.length) index = 0;
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("theme", index).apply();
        applyTheme(context);
    }

    public static int iconScalePercent(Context context) {
        int value = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("icon_scale", 100);
        return Math.max(75, Math.min(145, value));
    }

    public static boolean iconsOnRight(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getBoolean("icons_on_right", true);
    }

    public static int iconDp(Context context, int baseDp) {
        return dp(context, Math.round(baseDp * iconScalePercent(context) / 100f));
    }

    public static int homeIconScalePercent(Context context, String area) {
        int value = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getInt("home_icon_" + area, 100);
        return Math.max(70, Math.min(145, value));
    }

    public static int homeIconDp(Context context, String area, int baseDp) {
        return dp(context, Math.round(baseDp * homeIconScalePercent(context, area) / 100f));
    }

    public static boolean homeHeaderOnRight(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getBoolean("home_header_right", true);
    }

    public static boolean homeFabOnRight(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getBoolean("home_fab_right", false);
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, int radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable outlined(int fill, int stroke, int radiusDp, Context context) {
        GradientDrawable drawable = rounded(fill, radiusDp, context);
        drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    public static GradientDrawable gradient(int start, int end, int radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static int[] themePreviewColors(int index) {
        if (index < 0 || index >= THEME_LAYERS.length) index = 0;
        int[] layers = THEME_LAYERS[index];
        int anchor = layers[1];
        return new int[]{
                mix(layers[0], anchor, .56f),
                anchor,
                mix(anchor, layers[2], .42f),
                mix(anchor, layers[3], .30f)
        };
    }

    public static int[] themeSoftColors(int index) {
        if (index < 0 || index >= THEMES.length) index = 0;
        if (DARK) {
            return new int[]{
                    mix(SURFACE, BRAND, .08f),
                    mix(SURFACE, BRAND, .14f),
                    mix(SURFACE, BRAND, .20f),
                    mix(SURFACE, BRAND, .26f)
            };
        }
        return new int[]{
                mix(BRAND_SOFT, SURFACE, .24f),
                BRAND_SOFT,
                mix(BRAND_SOFT, BRAND, .09f),
                mix(BRAND_SOFT, BRAND, .15f)
        };
    }

    private static int mix(int from, int to, float amountTo) {
        float a = Math.max(0f, Math.min(1f, amountTo));
        int red = Math.round(Color.red(from) * (1f - a) + Color.red(to) * a);
        int green = Math.round(Color.green(from) * (1f - a) + Color.green(to) * a);
        int blue = Math.round(Color.blue(from) * (1f - a) + Color.blue(to) * a);
        return Color.rgb(red, green, blue);
    }

    public static AuroraDrawable layeredBackground(Context context, int radiusDp) {
        return new AuroraDrawable(themePreviewColors(themeIndex(context)), dp(context, radiusDp));
    }

    public static AuroraDrawable layeredSoftBackground(Context context, int radiusDp) {
        return new AuroraDrawable(themeSoftColors(themeIndex(context)), dp(context, radiusDp), true);
    }

    public static TextView text(Context context, String value, float sp, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        return text;
    }

    public static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return layout;
    }

    public static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return layout;
    }

    public static void margins(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) raw;
            params.setMargins(left, top, right, bottom);
            view.setLayoutParams(params);
        }
    }

    public static void animateScreen(View view) {
        if (Boolean.TRUE.equals(view.getTag()) || !android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        view.setAlpha(0f);
        view.setTranslationX(dp(view.getContext(), 18));
        view.setScaleX(0.992f); view.setScaleY(0.992f);
        view.animate().alpha(1f).translationX(0).scaleX(1f).scaleY(1f).setDuration(360)
                .setInterpolator(new PathInterpolator(0.2f, 0.8f, 0.2f, 1f)).start();
    }

    public static void animateChildren(ViewGroup group) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        int count = Math.min(group.getChildCount(), 10);
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i); child.setAlpha(0f); child.setTranslationY(dp(group.getContext(), 10));
            child.animate().alpha(1f).translationY(0).setStartDelay(45L + i * 28L).setDuration(300)
                    .setInterpolator(new PathInterpolator(0.2f, 0.75f, 0.25f, 1f)).start();
        }
    }

    public static void pressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }
}
