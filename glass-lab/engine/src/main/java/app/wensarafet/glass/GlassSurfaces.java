package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** Component mapping with fast static list rows and higher fidelity heroes. */
public final class GlassSurfaces {
    private GlassSurfaces() {}
    private static int dp(Context c, float dp) {
        return Math.max(1, Math.round(dp * c.getResources().getDisplayMetrics().density));
    }
    private static int blend(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return Color.rgb(Math.round(Color.red(a) * (1-k) + Color.red(b) * k),
            Math.round(Color.green(a) * (1-k) + Color.green(b) * k),
            Math.round(Color.blue(a) * (1-k) + Color.blue(b) * k));
    }
    public static Drawable backdrop(Context c, GlassPalette p, boolean eco) {
        return eco ? new ColorDrawable(p.background)
                   : new WenGlassBackdropDrawable(c, p.background, p.accent, p.brand, p.dark);
    }
    public static Drawable hero(Context c, GlassPalette p, int radius, boolean eco) {
        if (eco) return gradient(c, p.deep, p.brand, radius, false);
        return new WenGlassDrawable(c, dp(c, radius),
                blend(p.deep, p.brand, .25f), p.brand, p.accent, true);
    }
    public static Drawable frosted(Context c, GlassPalette p, int radius, boolean eco) {
        if (eco) return gradient(c, p.soft, p.surface, radius, true);
        int top = p.dark ? blend(p.surface, p.brand, .20f) : blend(p.soft, p.brand, .16f);
        int bottom = p.dark ? blend(p.surface, p.accent, .12f) : blend(p.soft, p.surface, .53f);
        return new WenGlassDrawable(c, dp(c, radius), top, bottom, p.accent, false);
    }
    public static Drawable row(Context c, GlassPalette p, int radius) {
        // Avoid per-item shaders for long transaction/reader lists.
        int start = p.dark ? blend(p.surface, p.brand, .10f) : blend(p.surface, p.soft, .52f);
        int end = p.dark ? blend(p.surface, p.accent, .08f) : blend(p.surface, p.soft, .21f);
        return gradient(c, start, end, radius, true);
    }
    public static Drawable gradient(Context c, int first, int second, int radius, boolean border) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{first, second});
        d.setCornerRadius(dp(c, radius));
        if (border) d.setStroke(dp(c, 1), Color.argb(150, 255, 255, 255));
        return d;
    }
}
