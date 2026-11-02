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
    private final RectF surface = new RectF();
    private final RectF edge = new RectF();
    private final RectF inner = new RectF();
    private final RectF fluid = new RectF();
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint opticalCore = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(Math.max(2f, density * 3f));
        halo.setColor(opacity(Color.WHITE, dark ? 150 : 225));
        opticalCore.setColor(opacity(Color.WHITE, dark ? 55 : 120));
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
                new int[]{top, opacity(accent, dark ? 70 : 110), bottom},
                new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
        // The primary light band is an edge, not a large opaque rectangle.
        outerRim.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{opacity(Color.WHITE, dark ? 185 : 245),
                        opacity(accent, dark ? 140 : 95),
                        opacity(Color.WHITE, dark ? 125 : 205)},
                null, Shader.TileMode.CLAMP));
        innerRim.setColor(opacity(Color.WHITE, dark ? 78 : 176));
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
        float halfWidth = surface.width()*.5f*(1f-morph)
                + surface.height()*.57f*morph;
        float halfHeight = surface.height()*.5f*(1f+.16f*morph);
        fluid.set(centerX-halfWidth, centerY-halfHeight,
                centerX+halfWidth, centerY+halfHeight);
        float r = Math.max(radius, fluid.height()*.50f);
        if (morph > .005f) {
            halo.setAlpha(Math.round(alpha*morph*(dark ? .65f : .88f)));
            canvas.drawRoundRect(fluid, r, r, halo);
        }
        canvas.drawRoundRect(fluid, r, r, fill);
        if (morph > .005f) {
            // Soft core makes the held bubble visibly refractive. No straight
            // top rule: that was the unwanted line over the Home tab.
            float inset = density*(5f+2f*(1f-morph));
            inner.set(fluid);
            inner.inset(inset, inset);
            opticalCore.setAlpha(Math.round(alpha*morph*(dark ? .23f : .44f)));
            canvas.drawOval(inner, opticalCore);
        }
        edge.set(fluid);
        edge.inset(outerRim.getStrokeWidth()*.5f, outerRim.getStrokeWidth()*.5f);
        canvas.drawRoundRect(edge, r, r, outerRim);
        inner.set(fluid);
        inner.inset(density*2.1f, density*2.1f);
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
