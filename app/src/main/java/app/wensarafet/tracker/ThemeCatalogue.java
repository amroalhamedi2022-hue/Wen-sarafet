package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Appearance-only catalogue facade. Stable existing theme IDs are never
 * renumbered, recoloured or deleted from storage; this only controls which
 * optional presets appear in Settings. No money, account or database access.
 */
public final class ThemeCatalogue {
    private static final String PREFS = "appearance";
    private static final String HIDDEN_PREFIX = "owner_hidden_theme_";

    // The owner requested concise English names for every selectable theme.
    private static final String[] ENGLISH_NAMES = {
        "Mint", "Royal Indigo", "Teal", "Night Indigo",
        "Warm Sand", "Burgundy", "Crimson", "Amber",
        "Apricot", "Soft Rose", "Lavender", "Ice",
        "Dark Forest", "Charcoal", "Pearl", "Copper",
        "Liquid Aurora", "Liquid Fusion", "Blue Glass", "Aqua Bubbles",
        "Night Aurora", "Frozen Pearl", "Prism Glow", "Golden Bloom",
        "Royal Gold", "Royal Dark", "Imperial Dark", "Champagne Light",
        "Retired", "Retired"
    };

    private ThemeCatalogue() {}

    public static String displayName(int themeIndex) {
        return themeIndex >= 0 && themeIndex < ENGLISH_NAMES.length
                ? ENGLISH_NAMES[themeIndex] : "Theme";
    }

    public static boolean isVisible(Context context, int themeIndex) {
        if (themeIndex < 0 || themeIndex >= ENGLISH_NAMES.length) return false;
        // Final owner scope: TWO black gold designs plus ONE ivory-gold.
        // Permanently retire 28/29 from selection and restoration.
        // An already active older theme is shown for manual switching,
        // without changing the owner's account or security preferences.
        if (themeIndex >= 28) return false;
        if (themeIndex < 25) return Ui.themeIndex(context) == themeIndex;
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(HIDDEN_PREFIX + themeIndex, false);
    }

    public static int visibleCount(Context context) {
        int count = 0;
        for (int i = 0; i < ENGLISH_NAMES.length; i++)
            if (isVisible(context, i)) count++;
        return count;
    }

    /**
     * Hide a non-active theme, preserving all stored IDs and drawables. This
     * intentionally does not call Ui.saveTheme: no surprise change to the
     * owner's installed theme or application financial state.
     */
    public static boolean hide(Context context, int index) {
        if (index == Ui.themeIndex(context) || !isVisible(context, index)
                || visibleCount(context) <= 1) return false;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(HIDDEN_PREFIX + index, true).apply();
        return true;
    }

    public static void restoreUserHidden(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < ENGLISH_NAMES.length; i++)
            edit.remove(HIDDEN_PREFIX + i);
        edit.apply();
        // Older additions 22-24 stay retired regardless of restore.
    }
}
