package app.wensarafet.glass;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

/**
 * Three opt-in reference palettes, never a replacement for the 22 existing themes.
 *
 * PRISM (22): luminous magenta, purple and blue, floating violet glass bar.
 * BLOOM (23): peach, sunset amber and soft green with a warm circular glass FAB.
 * ROYAL (24): obsidian black, champagne-gold, polished metal and a jewel-like FAB.
 *
 * Entirely passive: shaders are rebuilt only on size change, there is no
 * timer, Choreographer, bitmap, blur capture or additional touch listener.
 * Navigation refraction / hold+drag remain owned by the existing MIT material.
 */
public final class WenReferenceGlassDrawable extends Drawable {
    public static final int PRISM = 22;
    public static final int BLOOM = 23;
    public static final int ROYAL = 24;

    public enum Surface { BACKDROP, HEADER, CARD, NAVIGATION, FAB, ACCENT }

    private final boolean prism;
    private final boolean royal;
    private final boolean dark;
    private final boolean lowPower;
    private final Surface surface;
    private final float density;
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final RectF innerBounds = new RectF();
    private final Path shape = new Path();
    private float radius;
    private int alpha = 255;

    public WenReferenceGlassDrawable(Context context, int theme, Surface surface,
                                      boolean darkMode) {
        if (theme != PRISM && theme != BLOOM && theme != ROYAL)
            throw new IllegalArgumentException("Reference glass requires theme 22, 23 or 24");
        this.prism = theme == PRISM;
        this.royal = theme == ROYAL;
        this.dark = darkMode;
        this.surface = surface;
        this.density = context.getResources().getDisplayMetrics().density;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager activity = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        lowPower = (power != null && power.isPowerSaveMode())
                || (activity != null && activity.isLowRamDevice());
        border.setStyle(Paint.Style.STROKE);
        innerRim.setStyle(Paint.Style.STROKE);
    }

    private static int colour(int rgb, int opacity) {
        return Color.argb(opacity, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    private int[] stops() {
        if (prism) {
            switch (surface) {
                case BACKDROP: return dark
                        ? new int[]{0xFF311449, 0xFF34225D, 0xFF121936}
                        : new int[]{0xFFF8C2EC, 0xFFE7B9FA, 0xFFDAD6FF};
                case HEADER: return new int[]{0xA9B72BD4, 0xB8E044BE, 0xA9633DD2};
                case NAVIGATION: return dark
                        ? new int[]{0xA2451A70, 0xAF642485, 0xA756299B}
                        : new int[]{0xA9813EAC, 0xA57931B5, 0xA85643AD};
                case FAB: return new int[]{0xFFD92DAB, 0xFFAB3DD3, 0xFF7845DA};
                case ACCENT: return new int[]{0xFFA648E0, 0xFFCD62E9, 0xFFEB79BE};
                default: return dark
                        ? new int[]{0xCE342342, 0xC52B2447, 0xC025223C}
                        : new int[]{0xD8FDF3FF, 0xD3FFF5FD, 0xCEF1EAFF};
            }
        }
        if (royal) {
            // Five polished-metal stops avoid the flat yellow appearance of a
            // solid gradient. Dark obsidian maintains contrast for Arabic text.
            // No animation and no changes to the two established palettes.
            switch (surface) {
                case BACKDROP: return new int[]{
                        0xFF121418, 0xFF191715, 0xFF282015, 0xFF17191C, 0xFF0C1018};
                case HEADER: return new int[]{
                        0xAA513B21, 0xB1C59B51, 0xB7F3DDA4, 0xAA9E6D29, 0xA5352B1F};
                case NAVIGATION: return new int[]{
                        0xA0171B23, 0xA93A3022, 0xB0A87932, 0xA430271B, 0xA0121824};
                case FAB: return new int[]{
                        0xFF86601F, 0xFFE0B963, 0xFFFFF0BE, 0xFFBB8633, 0xFF684516};
                case ACCENT: return new int[]{
                        0xFF886122, 0xFFDBB66B, 0xFFFFE9AC, 0xFFC18A37, 0xFF654721};
                default: return new int[]{
                        0xCD24252A, 0xD034302D, 0xD3443A28, 0xD0292B31, 0xCB1B2029};
            }
        }
        switch (surface) {
            case BACKDROP: return dark
                    ? new int[]{0xFF33231E, 0xFF3C292B, 0xFF202B28}
                    : new int[]{0xFFFFDEB8, 0xFFFFE9C6, 0xFFDBEADA};
            case HEADER: return new int[]{0xABC47A3E, 0xB6EBA95B, 0xAAC76B61};
            case NAVIGATION: return dark
                    ? new int[]{0xA6A16C49, 0xAD8D625A, 0xA66D7767}
                    : new int[]{0xA8FFE6C9, 0xB0FFD1A5, 0xA8EED6B9};
            case FAB: return new int[]{0xFFCB7D43, 0xFFF9BE71, 0xFFBE7153};
            case ACCENT: return new int[]{0xFFF1A85D, 0xFFF9C783, 0xFFEBA389};
            default: return dark
                    ? new int[]{0xCC3F352F, 0xC6352E2D, 0xC12B302B}
                    : new int[]{0xD7FFF9EC, 0xD2FFF4E3, 0xCEF5F9EF};
        }
    }

    private boolean scene() { return surface == Surface.BACKDROP; }
    private boolean saturated() {
        return surface == Surface.HEADER || surface == Surface.NAVIGATION
                || surface == Surface.FAB || surface == Surface.ACCENT;
    }

    @Override protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        bounds.set(rect);
        shape.reset();
        if (rect.isEmpty()) return;
        float w = bounds.width(), h = bounds.height();
        radius = scene() ? 0f : surface == Surface.FAB || surface == Surface.NAVIGATION
                ? h / 2f : Math.min(32f * density, Math.min(w, h) * .35f);
        shape.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        int[] c = stops();
        base.setShader(new LinearGradient(bounds.left, bounds.top,
                bounds.right, bounds.bottom, c,
                royal ? new float[]{0f, .23f, .51f, .76f, 1f}
                        : new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        if (!lowPower) {
            int a = royal ? 0xFFFFDF8D : prism ? 0xFFFF36C7 : 0xFFFF9C40;
            int b = royal ? 0xFFC28735 : prism ? 0xFF6454F8 : 0xFF9BC5A3;
            float strength = saturated() ? .68f : scene() ? .55f : .19f;
            float span = Math.max(1f, Math.max(w, h) * (scene() ? .68f : .90f));
            lightA.setShader(new RadialGradient(
                    bounds.left + w * .15f, bounds.top + h * .12f, span,
                    new int[]{colour(a, Math.round(185f * strength)), colour(a, 0)},
                    null, Shader.TileMode.CLAMP));
            lightB.setShader(new RadialGradient(
                    bounds.left + w * .82f, bounds.top + h * .87f, span,
                    new int[]{colour(b, Math.round(185f * strength)), colour(b, 0)},
                    null, Shader.TileMode.CLAMP));
        } else {
            lightA.setShader(null);
            lightB.setShader(null);
        }
        float inset = Math.max(.7f, density * .95f);
        innerBounds.set(bounds.left + inset, bounds.top + inset,
                bounds.right - inset, bounds.bottom - inset);
        border.setStrokeWidth(Math.max(1f, density * (surface == Surface.FAB ? 1.7f : 1.15f)));
        border.setColor(scene() ? 0 : royal ? 0xBFE4BD71
                : saturated() ? 0xA5FFFFFF : 0x88FFFFFF);
        innerRim.setStrokeWidth(Math.max(.75f, density * .65f));
        innerRim.setColor(scene() ? 0 : royal ? 0x8AFFF0C3
                : saturated() ? 0x4DFFFFFF : 0x6FFFFFFF);
    }

    @Override public void draw(Canvas canvas) {
        if (bounds.isEmpty()) return;
        int save = canvas.save();
        canvas.clipPath(shape);
        canvas.drawRoundRect(bounds, radius, radius, base);
        if (!lowPower) {
            canvas.drawRect(bounds, lightA);
            canvas.drawRect(bounds, lightB);
        }
        canvas.restoreToCount(save);
        if (!scene()) {
            canvas.drawRoundRect(innerBounds, Math.max(0f, radius - density),
                    Math.max(0f, radius - density), border);
            if (surface == Surface.FAB || surface == Surface.NAVIGATION)
                canvas.drawRoundRect(bounds, radius, radius, innerRim);
        }
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        base.setAlpha(alpha);
        lightA.setAlpha(alpha);
        lightB.setAlpha(alpha);
        border.setAlpha(Math.round(alpha * .73f));
        innerRim.setAlpha(Math.round(alpha * .59f));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        base.setColorFilter(filter);
        lightA.setColorFilter(filter);
        lightB.setColorFilter(filter);
        border.setColorFilter(filter);
        innerRim.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
