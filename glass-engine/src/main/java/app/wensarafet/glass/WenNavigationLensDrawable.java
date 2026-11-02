package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Single, reusable bottom navigation glass lens.
 *
 * Pearlescent body + graded external/inner rims, subtle top highlight,
 * no live screen blur and no constantly-running shader. Paths/gradients
 * are cached on size change, not allocated during onDraw or while idle.
 */
public final class WenNavigationLensDrawable extends Drawable {
    private final float density;
    private final boolean dark;
    private final int top;
    private final int bottom;
    private final int accent;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chromaticRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final RectF edge = new RectF();
    private final RectF inner = new RectF();
    private final RectF fluid = new RectF();
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint opticalCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularCrown = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lowerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF crownBounds = new RectF();
    private final RectF shadowBounds = new RectF();
    private float morph;
    private float radius;
    private int alpha = 255;

    public WenNavigationLensDrawable(Context context, int first, int second,
                                     int accentColor, boolean darkTheme) {
        density = context.getResources().getDisplayMetrics().density;
        top = first;
        bottom = second;
        accent = accentColor;
        dark = darkTheme;
        outerRim.setStyle(Paint.Style.STROKE);
        outerRim.setStrokeWidth(Math.max(1f, density * 1.3f));
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(Math.max(.7f, density * .8f));
        chromaticRim.setStyle(Paint.Style.STROKE);
        chromaticRim.setStrokeWidth(Math.max(1f, density * 1.30f));
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(Math.max(2f, density * 3f));
        halo.setColor(opacity(Color.WHITE, dark ? 150 : 225));
        opticalCore.setColor(opacity(Color.WHITE, dark ? 55 : 120));
        specularCrown.setStyle(Paint.Style.STROKE);
        specularCrown.setStrokeWidth(density * 2.3f);
        lowerShadow.setStyle(Paint.Style.STROKE);
        lowerShadow.setStrokeWidth(density * 1.8f);
        lowerShadow.setColor(opacity(dark ? Color.BLACK : accent, dark ? 96 : 59));
    }

    private static int opacity(int color, int a) {
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        surface.set(bounds);
        float inset = density * 2.6f;
        surface.inset(inset, inset * .9f);
        radius = Math.max(0f, surface.height() * .49f);
        edge.set(surface);
        edge.inset(outerRim.getStrokeWidth()*.5f, outerRim.getStrokeWidth()*.5f);
        inner.set(surface);
        inner.inset(density * 2.1f, density * 2.1f);

        fill.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{opacity(top, dark ? 176 : 154),
                        opacity(Color.WHITE, dark ? 32 : 110),
                        opacity(bottom, dark ? 162 : 144)},
                new float[]{0f, .46f, 1f}, Shader.TileMode.CLAMP));
        // The primary light band is an edge, not a large opaque rectangle.
        outerRim.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{opacity(Color.WHITE, dark ? 185 : 245),
                        opacity(accent, dark ? 140 : 95),
                        opacity(Color.WHITE, dark ? 125 : 205)},
                null, Shader.TileMode.CLAMP));
        innerRim.setColor(opacity(Color.WHITE, dark ? 78 : 176));
        specularCrown.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{0xEFFFFFFF, 0x79D2FCFF, 0x0AFFFFFF,
                        0x8AE4EDFF, 0xD7FFFFFF}, null, Shader.TileMode.CLAMP));
        chromaticRim.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{0xFF98EBFF, 0xFFACC0FF, 0xFFE7B8F5,
                        0xFFFEEBB4, 0xFF91EBFC, 0xFFB7B1FF},
                null, Shader.TileMode.CLAMP));
        applyAlpha();
    }

    /**
     * Converts the resting tab pill into a compact, deeper optical droplet
     * on contact. No extra View, blur pass, idle clock, or draw-time allocation.
     * Values are supplied by the finite touch animator in GlassMotion.
     */
    public void setMorph(float fraction) {
        float next = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(next - morph) < .003f) return;
        morph = next;
        invalidateSelf();
    }

    public float getMorph() { return morph; }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty()) return;
        float centerX = surface.centerX(), centerY = surface.centerY();
        // Persistent selected-tab pill. Touch changes ONLY the optical pixels
        // drawn in the same bounds by WenNavigationTrack's foreground.
        // Never change dimensions/colour or draw a separate round halo.
        fluid.set(surface);
        float r = radius;
        canvas.drawRoundRect(fluid, r, r, fill);
        // The crowned top reflection and bottom shadow give the resting pill
        // measurable optical depth, without waiting for a finger gesture.
        crownBounds.set(fluid);
        crownBounds.inset(density * 1.7f, density * 1.7f);
        specularCrown.setAlpha((int) (alpha * (.57f + .26f * morph)));
        canvas.drawRoundRect(crownBounds, r, r, specularCrown);
        shadowBounds.set(fluid);
        shadowBounds.inset(density * 3.6f, density * 3.6f);
        lowerShadow.setAlpha((int) (alpha * (.33f + .20f * morph)));
        canvas.drawRoundRect(shadowBounds, r, r, lowerShadow);
        edge.set(fluid);
        edge.inset(outerRim.getStrokeWidth()*.5f, outerRim.getStrokeWidth()*.5f);
        outerRim.setAlpha((int) (alpha * (.76f + .12f * morph)));
        canvas.drawRoundRect(edge, r, r, outerRim);
        // Subtle rainbow refraction at rest, richer on hold, SAME pill shape.
        chromaticRim.setAlpha((int)(alpha * (.29f + .42f * morph)));
        canvas.drawRoundRect(edge, r, r, chromaticRim);
        inner.set(fluid);
        inner.inset(density*2.1f, density*2.1f);
        innerRim.setAlpha((int) (alpha * (.36f + .22f * morph)));
        canvas.drawRoundRect(inner, r, r, innerRim);
    }

    private void applyAlpha() {
        fill.setAlpha(alpha);
        outerRim.setAlpha(alpha);
        innerRim.setAlpha(alpha);
    }

    @Override public void setAlpha(int a) {
        alpha = Math.max(0, Math.min(255, a));
        applyAlpha();
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        outerRim.setColorFilter(filter);
        innerRim.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
