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
            "Liquid Aurora", "Liquid Fusion",
            "زجاج سائل أزرق", "فقاعات أكوا", "أورورا ليلية", "لؤلؤي متجمد",
            "Prism Glow • زجاج نيوني", "Golden Bloom • زجاج شمسي",
            "Royal Gold • الذهب الملكي",
            "Royal Dark Gold • ذهبي ملكي داكن",
            "Imperial Dark Gold • ذهبي إمبراطوري",
            "Champagne Gold • شامبانيا ملكي فاتح",
            "Soft Orange-Green Glass • زجاج برتقالي أخضر",
            "Purple-Pink Liquid Glass • زجاج بنفسجي وردي"
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
            {0xFF456AD9,0xFF252D82,0xFFE7EBFF,0xFFF7F9FF,0xFFFFFFFF,0xFF1F2550,0xFF687495,0xFFC34465,0xFF2F9A91,0xFF456AD9,0xFF9A5DE1,0xFFF5A26D},
            {0xFF2965E5,0xFF1B3690,0xFFDDEBFF,0xFFF5F9FF,0xFFFFFFFF,0xFF152A4C,0xFF667A99,0xFFC74664,0xFF0C9280,0xFF2965E5,0xFF6D8CF0,0xFF9DDBFF},
            {0xFF07A7BA,0xFF00677B,0xFFDFF9F8,0xFFF4FEFD,0xFFFFFFFF,0xFF17383C,0xFF628D90,0xFFC54862,0xFF078C73,0xFF2294BD,0xFF54D8D9,0xFFC5FFF3},
            {0xFF7F73FF,0xFF31255D,0xFF252746,0xFF101429,0xFF171B30,0xFFF6F7FF,0xFFACB4D1,0xFFFF758A,0xFF64D5AF,0xFFA58AFF,0xFFD58BFF,0xFF7CCBFF},
            {0xFF7888CD,0xFF414B80,0xFFF0F0FD,0xFFFAFBFF,0xFFFFFFFF,0xFF252943,0xFF7B8198,0xFFC25570,0xFF348B83,0xFF9794D6,0xFFBCABD5,0xFFDFE9FF},
            {0xFFAD3CDB,0xFF62257F,0xFFF4E3FC,0xFFFAF0FC,0xFFFFFAFF,0xFF291638,0xFF795D87,0xFFC94376,0xFF238D86,0xFF8244DA,0xFFF471B9,0xFF8B88EE},
            {0xFFCE873E,0xFF87502D,0xFFFFE9C8,0xFFFFF7EA,0xFFFFFDF5,0xFF3D3028,0xFF857367,0xFFCA5060,0xFF54866D,0xFFB993BC,0xFFF8AA67,0xFF95BDA6},
            // Royal Gold: deep obsidian and champagne-metallic accents.
            {0xFFDCB56D,0xFF8C632D,0xFF3B3020,0xFF121418,0xFF23252A,0xFFFAEFCF,0xFFC0AD85,0xFFEF6A69,0xFF7CC6A1,0xFFEAD097,0xFFF4C773,0xFFA98440},
            // Five owner-approved additions only: indices 0-24 remain byte-identical.
            {0xFFE5BF77,0xFF907040,0xFF3E372D,0xFF0C1119,0xFF252B32,0xFFFFF3D7,0xFFD0BF9E,0xFFE97075,0xFF77BEA3,0xFFF2D59B,0xFFE6BC79,0xFFB08B50},
            {0xFFD6BE9A,0xFF80725E,0xFF3E3D3B,0xFF10151F,0xFF272F3B,0xFFF7EBDD,0xFFC9BEAB,0xFFEB747B,0xFF78BFA7,0xFFE9D4B3,0xFFD1B48B,0xFF9C8D7A},
            {0xFFB99058,0xFF8E683E,0xFFFFF0D6,0xFFF8F6F0,0xFFFFFFFF,0xFF41372D,0xFF847766,0xFFD15C65,0xFF4B8971,0xFFB99058,0xFFE3C691,0xFFFFE8BA},
            {0xFFD48C61,0xFF9D6449,0xFFFFE6D3,0xFFFFF5EF,0xFFFFFFFF,0xFF403831,0xFF7B766D,0xFFD06064,0xFF4E9679,0xFFCF9566,0xFFA9CBA9,0xFFF4CBA7},
            {0xFFB77ACA,0xFF7E509C,0xFFF4DEF6,0xFFF9F2FC,0xFFFFFFFF,0xFF382B47,0xFF7B6C84,0xFFC75C83,0xFF4C8B84,0xFFAC73C2,0xFFF0B8DD,0xFFC9B9F2}
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
            {0xFF1E2559,0xFF456AD9,0xFF9A5DE1,0xFFF5A26D},
            {0xFF1A3C9B,0xFF3269E9,0xFF6CC4F9,0xFFC4E9FF},
            {0xFF005969,0xFF08A5BA,0xFF68E6DE,0xFFC7FEED},
            {0xFF080F33,0xFF353281,0xFF6552C8,0xFFAB73EF},
            {0xFF6A719F,0xFFACB7D8,0xFFE0DFFD,0xFFF8F9FF},
            {0xFF782A9E,0xFFC435B0,0xFFE64CC2,0xFF7956EE},
            {0xFFB86E39,0xFFF2AC65,0xFFFBD6A0,0xFF99B9A1},
            {0xFF0E121A,0xFF3B3021,0xFF9D7735,0xFFFFDFA1},
            {0xFF0B1018,0xFF303237,0xFF957443,0xFFF9D99E},
            {0xFF0D1520,0xFF343E4B,0xFF8C8074,0xFFF2DEC0},
            {0xFFF5EBDD,0xFFF7F5F0,0xFFE8D0A1,0xFFFFFAED},
            {0xFFF8DCCB,0xFFFFF1E3,0xFFB9D9BD,0xFFFBECCE},
            {0xFFE4D3EE,0xFFF9F1FB,0xFFD6B5E8,0xFFFFD5EF}
    };


    // The pilot is opt-in through the two existing Liquid theme indices only.
    // No new settings keys, preferences, listeners or financial behavior.
    // The engine is an independent reusable Android graphics component.
    public static android.graphics.drawable.Drawable glassHeaderBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.HEADER);
        if (themeIndex(context) < 16) return layeredBackground(context, radiusDp);
        return new app.wensarafet.glass.WenGlassDrawable(context, dp(context, radiusDp),
                mix(BRAND_DARK, BRAND, .12f), BRAND, SKY, true);
    }

    public static android.graphics.drawable.Drawable glassSoftBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.CARD);
        if (themeIndex(context) < 16) return layeredSoftBackground(context, radiusDp);
        int upper = DARK ? mix(SURFACE, BRAND, .10f) : mix(BRAND_SOFT, SURFACE, .32f);
        int lower = DARK ? mix(SURFACE, SKY, .16f) : mix(BRAND_SOFT, SKY, .08f);
        return new app.wensarafet.glass.WenGlassDrawable(context, dp(context, radiusDp),
                upper, lower, SKY, false);
    }

    /** Reference-matched frosted balance tiles. Light fill ~#A0BDF6 to ice blue. */
    public static android.graphics.drawable.Drawable glassBalanceBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.CARD);
        if (!isLiquidTheme(context)) return layeredSoftBackground(context, radiusDp);
        int top = DARK ? mix(SURFACE, BRAND, .20f) : mix(BRAND_SOFT, BRAND, .34f);
        int bottom = DARK ? mix(SURFACE, SKY, .13f) : mix(BRAND_SOFT, SKY, .05f);
        return new app.wensarafet.glass.WenGlassDrawable(context,
                dp(context, radiusDp), top, bottom, SKY, false);
    }

    /** Cyan-blue chart atmosphere stays separate from the denser balance tiles. */
    public static android.graphics.drawable.Drawable glassChartBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.CARD);
        if (!isLiquidTheme(context)) return layeredSoftBackground(context, radiusDp);
        int top = DARK ? mix(SURFACE, BRAND, .12f) : mix(BRAND_SOFT, BRAND, .22f);
        int bottom = DARK ? mix(SURFACE, SKY, .16f) : mix(BRAND_SOFT, SKY, .20f);
        return new app.wensarafet.glass.WenGlassDrawable(context,
                dp(context, radiusDp), top, bottom, SKY, false);
    }

    public static android.graphics.drawable.Drawable glassAccentBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.ACCENT);
        if (themeIndex(context) < 16) return new app.wensarafet.glass.WenGlassDrawable(
                context, dp(context, Math.max(20, radiusDp)), BRAND_DARK, BRAND, SKY, true);
        // The existing FAB is the only consumer of accent radius 24. Keep
        // every card, button, and the three newer reference palettes intact.
        if (themeIndex(context) >= 16 && themeIndex(context) <= 21 && radiusDp == 24)
            return new app.wensarafet.glass.WenFabBubbleDrawable(context, SKY, DARK);
        return new app.wensarafet.glass.WenGlassDrawable(context, dp(context, radiusDp),
                mix(BRAND_DARK, BRAND, .18f), mix(BRAND, VIOLET, .28f), SKY, true);
    }


    /** Passive depth behind primary screens; never rendered per transaction row. */
    public static android.graphics.drawable.Drawable glassScreenBackground(Context context) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.BACKDROP);
        if (!isUnifiedGlassTheme(context)) return new android.graphics.drawable.ColorDrawable(BACKGROUND);
        return new app.wensarafet.glass.WenGlassBackdropDrawable(context,
                BACKGROUND, SKY, BRAND, DARK);
    }

    /** Append-only themes: indices 0–21 and their persisted preferences are untouched. */
    public static boolean isReferenceGlassTheme(Context context) {
        int selected = themeIndex(context);
        return selected == app.wensarafet.glass.WenReferenceGlassDrawable.PRISM
                || selected == app.wensarafet.glass.WenReferenceGlassDrawable.BLOOM
                || selected == app.wensarafet.glass.WenReferenceGlassDrawable.ROYAL
                || app.wensarafet.glass.WenPremiumLiquidDrawable.supports(selected);
    }

    public static android.graphics.drawable.Drawable glassFloatingAddBackground(Context context) {
        // One shared colourless glass FAB for EVERY theme (legacy + premium).
        // The original account icons, navigation and actions remain unchanged.
        return new app.wensarafet.glass.WenFabBubbleDrawable(
                context, Color.WHITE, DARK, -1);
    }

    private static android.graphics.drawable.Drawable referenceGlass(Context context,
            app.wensarafet.glass.WenReferenceGlassDrawable.Surface surface) {
        int selected = themeIndex(context);
        if (app.wensarafet.glass.WenPremiumLiquidDrawable.supports(selected))
            return new app.wensarafet.glass.WenPremiumLiquidDrawable(context,
                    selected, surface, DARK);
        return new app.wensarafet.glass.WenReferenceGlassDrawable(context,
                selected, surface, DARK);
    }

    /** All stored selectable presets share the same visual glass contract.
     * The original isLiquidTheme gate below still limits expensive motions. */
    public static boolean isUnifiedGlassTheme(Context context) {
        int index = themeIndex(context);
        return index >= 0 && index <= 27;
    }

    /** Motion-only gate: legacy palettes do not acquire heavy page transitions. */
    public static boolean isLiquidTheme(Context context) {
        int index = themeIndex(context);
        return index >= 16 && index < THEMES.length;
    }

    /**
     * Reusable inexpensive list surface.  It deliberately uses Android's native
     * GradientDrawable rather than blur/refraction or per-row animated shader:
     * dozens of transactions can scroll without multiple GPU offscreen passes.
     */
    public static android.graphics.drawable.Drawable glassListBackground(Context context, int radiusDp) {
        // Thousands of transaction rows must NOT allocate a native blur View
        // or run AGSL. One inexpensive clipped translucent GradientDrawable
        // per row shares the same palette, rounded outline and highlight.
        int first = mix(SURFACE, BRAND_SOFT, DARK ? .10f : .36f);
        int second = mix(SURFACE, SKY, DARK ? .08f : .11f);
        GradientDrawable plate = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(DARK ? 188 : 196, Color.red(first), Color.green(first), Color.blue(first)),
                        Color.argb(DARK ? 174 : 185, Color.red(second), Color.green(second), Color.blue(second))});
        plate.setCornerRadius(dp(context, Math.max(20, radiusDp)));
        plate.setStroke(Math.max(1, dp(context, 1)),
                DARK ? Color.argb(70, 232, 235, 255) : Color.argb(148, 255, 255, 255));
        return plate;
    }

    public static android.graphics.drawable.Drawable glassNavigationBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.NAVIGATION);
        // Even the 16 original palettes now use the floating translucent
        // navigation material; no opaque legacy plate remains behind the lens.
        // Always passive: scroll may translate/fade the bar, but never drives a glass clock.
        int first = DARK ? mix(SURFACE, BRAND, .16f) : mix(SURFACE, BRAND_SOFT, .60f);
        int second = DARK ? mix(SURFACE, SKY, .12f) : mix(SURFACE, SKY, .18f);
        return new app.wensarafet.glass.WenGlassDrawable(context, dp(context, radiusDp),
                first, second, SKY, false);
    }

    public static android.graphics.drawable.Drawable glassSelectedNavigationBackground(Context context, int radiusDp) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, app.wensarafet.glass.WenReferenceGlassDrawable.Surface.ACCENT);
        if (!isUnifiedGlassTheme(context)) return rounded(BRAND_SOFT, radiusDp, context);
        return new app.wensarafet.glass.WenGlassDrawable(context, dp(context, radiusDp),
                BRAND_SOFT, mix(BRAND_SOFT, SKY, DARK ? .12f : .20f), BRAND, false);
    }

    public static android.graphics.drawable.Drawable glassActionBackground(Context context, int radiusDp, boolean primary) {
        if (isReferenceGlassTheme(context))
            return referenceGlass(context, primary
                    ? app.wensarafet.glass.WenReferenceGlassDrawable.Surface.ACCENT
                    : app.wensarafet.glass.WenReferenceGlassDrawable.Surface.CARD);
        if (!isUnifiedGlassTheme(context)) return rounded(primary ? BRAND : BRAND_SOFT, radiusDp, context);
        if (!primary) return glassSelectedNavigationBackground(context, radiusDp);
        return glassAccentBackground(context, radiusDp);
    }

    private Ui() {}

    public static void applyTheme(Context context) {
        // Load through the visual-only compatibility resolver: retained old
        // preset 28/29 never paints again, even on an update install.
        int theme = themeIndex(context);
        if (theme < 0 || theme >= THEMES.length) theme = 25;
        // Midnight Aurora is intentionally dark even when the device uses light mode.
        // The Activity already derives system-bar contrast and native dialog theme from DARK.
        // The three new light palettes remain legible on dark-mode phones;
        // all prior 25 themes keep their original dark-mode behaviour.
        DARK = theme == 20 || theme == 24 || theme == 25 || theme == 26
                || theme == 28 || theme == 29
                || (theme < 25 && (context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
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
        int persisted = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getInt("theme", 0);
        // Retired colourful presets are VISUAL-ONLY aliases of Royal Dark.
        // Do not rewrite unrelated appearance prefs or the finance database.
        // Preserves stable IDs for old saved sessions and safe update installs.
        if (persisted == 28 || persisted == 29) return 25;
        return persisted;
    }

    public static void saveTheme(Context context, int index) {
        // Never allow selecting either retired non-gold theme via stale UI.
        if (index == 28 || index == 29) index = 25;
        if (index < 0 || index >= THEMES.length) index = 25;
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

    // All legacy headers, balances and summary panels use a lightweight
    // cached-shader glass Drawable. Preserve existing preset IDs, palette,
    // titles and every click callback; no bitmap capture or idle redraw timer.
    public static android.graphics.drawable.Drawable layeredBackground(Context context, int radiusDp) {
        return new app.wensarafet.glass.WenGlassDrawable(context,
                dp(context, Math.max(22, radiusDp)),
                mix(BRAND_DARK, BRAND, .17f), BRAND, SKY, true);
    }

    public static android.graphics.drawable.Drawable layeredSoftBackground(Context context, int radiusDp) {
        return new app.wensarafet.glass.WenGlassDrawable(context,
                dp(context, Math.max(22, radiusDp)),
                DARK ? mix(SURFACE, BRAND, .13f) : mix(BRAND_SOFT, SURFACE, .17f),
                DARK ? mix(SURFACE, SKY, .12f) : mix(SURFACE, BRAND_SOFT, .22f),
                SKY, false);
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
        if (isLiquidTheme(view.getContext())) {
            // Sandbox visual timing: one short, restrained arrival rather
            // than a second long page-level slide stacked with card motion.
            // The content is already attached and every control stays live.
            view.animate().cancel();
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(.86f);
            view.setTranslationY(dp(view.getContext(), 3));
            view.animate().alpha(1f).translationY(0f).setDuration(115)
                    .setInterpolator(new PathInterpolator(.2f, .8f, .2f, 1f)).start();
            return;
        }
        // Exactly the previous 0–15 theme animation.
        view.setAlpha(0f);
        view.setTranslationX(dp(view.getContext(), 18));
        view.setScaleX(0.992f); view.setScaleY(0.992f);
        view.animate().alpha(1f).translationX(0).scaleX(1f).scaleY(1f).setDuration(215)
                .setInterpolator(new PathInterpolator(0.2f, 0.8f, 0.2f, 1f)).start();
    }

    public static void animateChildren(ViewGroup group) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        // Sandbox visual parity for Liquid themes only: animate the leading
        // two cards quickly instead of stacking ten 300ms transitions.
        // Every child remains present and enabled. Legacy 0–15: unchanged.
        boolean liquid = isLiquidTheme(group.getContext());
        int count = Math.min(group.getChildCount(), liquid ? 2 : 4);
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            child.animate().cancel();
            child.setAlpha(0f);
            child.setTranslationY(dp(group.getContext(), liquid ? 6 : 10));
            child.animate().alpha(1f).translationY(0)
                    .setStartDelay(liquid ? i * 10L : 45L + i * 28L)
                    .setDuration(liquid ? 125 : 300)
                    .setInterpolator(new PathInterpolator(.2f, .75f, .25f, 1f)).start();
        }
    }

    public static void pressAnimation(View view) {
        if (isLiquidTheme(view.getContext())
                || view.getBackground() instanceof app.wensarafet.glass.WenFabBubbleDrawable) {
            // Restore the sandbox's true circular FAB pressure-morph, only
            // for the original glass palettes 16–21. Its listener NEVER
            // consumes input; the exact original click and long-click remain.
            if (view.getBackground() instanceof app.wensarafet.glass.WenFabBubbleDrawable
                    && (view.isClickable() || view.isLongClickable())) {
                final app.wensarafet.glass.WenFabBubbleDrawable bubble =
                        (app.wensarafet.glass.WenFabBubbleDrawable) view.getBackground();
                // A lightweight over-glyph optical crown from the sandbox.
                // ViewOverlay is visual-only and cannot receive any touch.
                final app.wensarafet.glass.WenFabOpticalCrownDrawable crown =
                        new app.wensarafet.glass.WenFabOpticalCrownDrawable(
                                view.getResources().getDisplayMetrics().density);
                crown.setBounds(0, 0, view.getWidth(), view.getHeight());
                view.getOverlay().add(crown);
                view.addOnLayoutChangeListener((v, left, top, right, bottom,
                        oldLeft, oldTop, oldRight, oldBottom) ->
                        crown.setBounds(0, 0, right - left, bottom - top));
                final android.animation.ValueAnimator[] morph = {null};
                view.setOnTouchListener((v, e) -> {
                    final int action = e.getActionMasked();
                    if (action != MotionEvent.ACTION_DOWN
                            && action != MotionEvent.ACTION_UP
                            && action != MotionEvent.ACTION_CANCEL) return false;
                    boolean down = action == MotionEvent.ACTION_DOWN;
                    if (morph[0] != null) morph[0].cancel();
                    if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
                        bubble.setPress(0f);
                        crown.setPress(0f);
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        return false;
                    }
                    morph[0] = android.animation.ValueAnimator.ofFloat(
                            bubble.getPress(), down ? 1f : 0f);
                    morph[0].setDuration(down ? 155L : 245L);
                    morph[0].setInterpolator(new PathInterpolator(.2f, .8f, .2f, 1f));
                    morph[0].addUpdateListener(a -> {
                        float t = (Float) a.getAnimatedValue();
                        bubble.setPress(t);
                        crown.setPress(t);
                    });
                    morph[0].start();
                    v.animate().cancel();
                    v.animate().scaleX(down ? .94f : 1f).scaleY(down ? .94f : 1f)
                            .setDuration(down ? 90L : 225L).start();
                    return false; // Never steal existing FAB click/hold actions.
                });
                view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override public void onViewAttachedToWindow(View v) {
                        crown.setBounds(0, 0, v.getWidth(), v.getHeight());
                        v.getOverlay().add(crown);
                    }
                    @Override public void onViewDetachedFromWindow(View v) {
                        if (morph[0] != null) morph[0].cancel();
                        bubble.setPress(0f);
                        crown.setPress(0f);
                        v.getOverlay().remove(crown);
                        v.animate().cancel();
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                    }
                });
                return;
            }
            // Reference gold/pink themes keep their established material
            // and use the pre-existing non-consuming Liquid press engine.
            app.wensarafet.glass.GlassMotion.bindPress(view, SKY);
            return;
        }
        view.setOnTouchListener((v, event) -> {
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;
            // Preserve exact existing touch behavior for the 16 original themes.
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
    }
}
