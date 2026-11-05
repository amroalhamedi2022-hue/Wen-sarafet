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
 * Two opt-in reference palettes, never a replacement for the 22 existing themes.
 *
 * PRISM (22): luminous magenta, purple and blue, floating violet glass bar.
 * BLOOM (23): peach, sunset amber and soft green with a warm circular glass FAB.
 *
 * Entirely passive: shaders are rebuilt only on size change, there is no
 * timer, Choreographer, bitmap, blur capture or additional touch listener.
 * Navigation refraction / hold+drag remain owned by the existing MIT material.
 */
public final class WenReferenceGlassDrawable extends Drawable {
    public static final int PRISM = 22;
    public static final int BLOOM = 23;

    public enum Surface { BACKDROP, HEADER, CARD, NAVIGATION, FAB, ACCENT }

    private final boolean prism;
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
        if (theme != PRISM && theme != BLOOM)
            throw new IllegalArgumentException("Reference glass requires theme 22 or 23");
        this.prism = theme == PRISM;
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
                case HEADER: return new int[]{0xFFB72BD4, 0xFFE044BE, 0xFF633DD2};
                case NAVIGATION: return dark
                        ? new int[]{0xEC451A70, 0xEC642485, 0xED56299B}
                        : new int[]{0xEE813EAC, 0xEB7931B5, 0xED5643AD};
                case FAB: return new int[]{0xFFD92DAB, 0xFFAB3DD3, 0xFF7845DA};
                case ACCENT: return new int[]{0xFFA648E0, 0xFFCD62E9, 0xFFEB79BE};
                default: return dark
                        ? new int[]{0xFF342342, 0xFF2B2447, 0xFF25223C}
                        : new int[]{0xFDFDF3FF, 0xF9FFF5FD, 0xF9F1EAFF};
            }
        }
        switch (surface) {
            case BACKDROP: return dark
                    ? new int[]{0xFF33231E, 0xFF3C292B, 0xFF202B28}
                    : new int[]{0xFFFFDEB8, 0xFFFFE9C6, 0xFFDBEADA};
            case HEADER: return new int[]{0xFFC47A3E, 0xFFEBA95B, 0xFFC76B61};
            case NAVIGATION: return dark
                    ? new int[]{0xF0A16C49, 0xF08D625A, 0xF06D7767}
                    : new int[]{0xF3FFE6C9, 0xF1FFD1A5, 0xF3EED6B9};
            case FAB: return new int[]{0xFFCB7D43, 0xFFF9BE71, 0xFFBE7153};
            case ACCENT: return new int[]{0xFFF1A85D, 0xFFF9C783, 0xFFEBA389};
            default: return dark
                    ? new int[]{0xFF3F352F, 0xFF352E2D, 0xFF2B302B}
                    : new int[]{0xFDFFF9EC, 0xFCFFF4E3, 0xFAF5F9EF};
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
                ? h / 2f : Math.min(26f * density, Math.min(w, h) * .28f);
        shape.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        int[] c = stops();
        base.setShader(new LinearGradient(bounds.left, bounds.top,
                bounds.right, bounds.bottom, c,
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        if (!lowPower) {
            int a = prism ? 0xFFFF36C7 : 0xFFFF9C40;
            int b = prism ? 0xFF6454F8 : 0xFF9BC5A3;
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
        border.setColor(scene() ? 0 : saturated() ? 0xA5FFFFFF : 0x88FFFFFF);
        innerRim.setStrokeWidth(Math.max(.75f, density * .65f));
        innerRim.setColor(scene() ? 0 : saturated() ? 0x4DFFFFFF : 0x6FFFFFFF);
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
