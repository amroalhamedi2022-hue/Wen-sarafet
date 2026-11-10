package app.wensarafet.tracker;

import android.content.Context;
import android.content.res.Configuration;

/** Per-viewport appearance ONLY: no data, theme persistence or navigation. */
final class WenPremiumLayout {
    private WenPremiumLayout() {}

    static boolean compact(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return app.wensarafet.glass.WenPremiumLiquidDrawable.supports(
                Ui.themeIndex(context))
                && config.orientation == Configuration.ORIENTATION_PORTRAIT
                && config.screenHeightDp < 800;
    }

    static int dp(Context context, int regular, int referenceCompact) {
        return Ui.dp(context, compact(context) ? referenceCompact : regular);
    }

    static int navigationHeight(Context context) {
        return WenGoldGlassSurface.enabled(context)
                ? dp(context, 86, 76) : Ui.dp(context, 72);
    }

    static int navigationSideInsetDp(Context context) {
        return WenGoldGlassSurface.enabled(context) ? 16 : 10;
    }

    static int navigationBottomInsetDp(Context context) {
        return WenGoldGlassSurface.enabled(context) ? 10 : 8;
    }

    static int fabBottomOffset(Context context) {
        // Leave a visible gap above the taller dock, never cover a tab label.
        return WenGoldGlassSurface.enabled(context)
                ? dp(context, 114, 104) : Ui.dp(context, 88);
    }
}
