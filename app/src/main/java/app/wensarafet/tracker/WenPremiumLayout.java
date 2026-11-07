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
}
