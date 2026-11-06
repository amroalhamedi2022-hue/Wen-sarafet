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
        "Royal Gold", "Royal Dark Gold", "Imperial Gold", "Champagne Gold",
        "Orange Green", "Pink Violet"
    };

    private ThemeCatalogue() {}

    public static String displayName(int themeIndex) {
        return themeIndex >= 0 && themeIndex < ENGLISH_NAMES.length
                ? ENGLISH_NAMES[themeIndex] : "Theme";
    }

    public static boolean isVisible(Context context, int themeIndex) {
        if (themeIndex < 0 || themeIndex >= ENGLISH_NAMES.length) return false;
        // These three transitional themes followed Frozen Pearl, but are NOT
        // among the owner's five newer, separately approved designs (25-29).
        // Preserve an already selected older palette until the owner chooses
        // something else. It disappears from the catalogue after switching;
        // never silently force a different persisted theme on an update.
        if (themeIndex >= 22 && themeIndex <= 24)
            return Ui.themeIndex(context) == themeIndex;
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
