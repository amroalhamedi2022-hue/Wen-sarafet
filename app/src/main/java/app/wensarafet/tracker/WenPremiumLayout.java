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
        return Ui.dp(context, 60);
    }

    static int navigationSideInsetDp(Context context) {
        int widthDp = context.getResources().getConfiguration().screenWidthDp;
        // Keep four usable tabs on narrow screens and cap the dock on tablets.
        int preferredInset = Math.max(22, (widthDp - 344) / 2);
        return Math.min(preferredInset, Math.max(0, (widthDp - 240) / 2));
    }

    static int navigationBottomInsetDp(Context context) {
        return 10;
    }

    static int fabSideInsetDp(Context context) {
        return Math.min(30, Math.max(16, navigationSideInsetDp(context)));
    }

    static int fabBottomOffset(Context context) {
        return navigationHeight(context)
                + Ui.dp(context, navigationBottomInsetDp(context) + 12);
    }
}
