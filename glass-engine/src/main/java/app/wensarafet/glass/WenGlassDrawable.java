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
 * A lightweight optical-glass illustration for Android Views.
 *
 * Visual-only: layered color, cached radial light, refractive-looking rims,
 * and a small non-animated lens highlight on wide hero surfaces.  It does NOT
 * read or blur the content behind the View, and therefore does not claim
 * physically accurate live refraction. No per-frame clock or offscreen bitmap.
 */
public final class WenGlassDrawable extends Drawable {
    private final float radiusPx;
    private final float density;
    private final int startColor;
    private final int endColor;
    private final int accentColor;
    private final boolean strong;
    private final boolean lowPower;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ambient = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final RectF border = new RectF();
    private final RectF innerBorder = new RectF();
    private final Path clipping = new Path();
    private final Path upperGlint = new Path();
    private final Path lowerWave = new Path();
    private int alpha = 255;
    private float lensX;
    private float lensY;
    private float lensRadius;
    private boolean showLens;

    public WenGlassDrawable(Context context, int radiusPx, int top, int bottom,
                            int accent, boolean strongSurface) {
        this.radiusPx = Math.max(0, radiusPx);
        this.density = context.getResources().getDisplayMetrics().density;
        this.startColor = top;
        this.endColor = bottom;
        this.accentColor = accent;
        this.strong = strongSurface;

        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager activity = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.lowPower = (power != null && power.isPowerSaveMode())
                || (activity != null && activity.isLowRamDevice());

        fill.setStyle(Paint.Style.FILL);
        ambient.setStyle(Paint.Style.FILL);
        glint.setColor(Color.WHITE);
        wave.setColor(accentColor);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Math.max(density, 1f));
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(Math.max(density * .8f, .8f));
        bubble.setStyle(Paint.Style.FILL);
        applyAlpha();
    }

    private static int blend(int a, int b, float fraction) {
        float t = Math.max(0f, Math.min(1f, fraction));
        return Color.rgb(Math.round(Color.red(a) * (1f - t) + Color.red(b) * t),
                Math.round(Color.green(a) * (1f - t) + Color.green(b) * t),
                Math.round(Color.blue(a) * (1f - t) + Color.blue(b) * t));
    }

    private static int opacity(int color, int value) {
        return Color.argb(Math.max(0, Math.min(255, value)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        surface.set(bounds);
        if (bounds.isEmpty()) {
            showLens = false;
            return;
        }
        float w = surface.width();
        float h = surface.height();
        border.set(surface);
        border.inset(outline.getStrokeWidth() * .5f, outline.getStrokeWidth() * .5f);
        innerBorder.set(surface);
        innerBorder.inset(density * 2.4f, density * 2.4f);

        // Every shader, path and rect is cached when the size changes, not on draw().
        fill.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{startColor, blend(startColor, endColor, .45f), endColor},
                new float[]{0f, .56f, 1f}, Shader.TileMode.CLAMP));

        float ambientRadius = Math.max(density, Math.max(w, h) * .88f);
        ambient.setShader(new RadialGradient(
                surface.left + w * .25f, surface.top + h * .07f, ambientRadius,
                new int[]{opacity(accentColor, strong ? 120 : 62),
                        opacity(accentColor, strong ? 29 : 13), opacity(accentColor, 0)},
                new float[]{0f, .46f, 1f}, Shader.TileMode.CLAMP));
        ambient.setAlpha(alpha);

        clipping.reset();
        clipping.addRoundRect(surface, radiusPx, radiusPx, Path.Direction.CW);

        upperGlint.reset();
        upperGlint.moveTo(surface.left, surface.top);
        upperGlint.lineTo(surface.right, surface.top);
        upperGlint.lineTo(surface.right, surface.top + h * .17f);
        upperGlint.cubicTo(surface.left + w * .69f, surface.top + h * .35f,
                surface.left + w * .33f, surface.top - h * .025f,
                surface.left, surface.top + h * .16f);
        upperGlint.close();

        lowerWave.reset();
        lowerWave.moveTo(surface.left, surface.bottom - h * .14f);
        lowerWave.cubicTo(surface.left + w * .22f, surface.bottom - h * .32f,
                surface.left + w * .66f, surface.bottom - h * .035f,
                surface.right, surface.bottom - h * .24f);
        lowerWave.lineTo(surface.right, surface.bottom);
        lowerWave.lineTo(surface.left, surface.bottom);
        lowerWave.close();

        outline.setShader(new LinearGradient(
                surface.left, surface.top, surface.right, surface.bottom,
                new int[]{opacity(Color.WHITE, 240), opacity(accentColor, 100),
                        opacity(Color.WHITE, 155)},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        // The reference has a pearly inner rim rather than an almost-black contour.
        innerRim.setColor(Color.WHITE);

        // One static glass bubble only on wide hero surfaces, away from RTL labels.
        showLens = strong && w > h * 3.1f && !lowPower;
        lensX = surface.left + w * .075f;
        lensY = surface.top + h * .34f;
        lensRadius = Math.min(h * .19f, w * .045f);
        if (showLens) {
            bubble.setShader(new RadialGradient(
                    lensX - lensRadius * .28f, lensY - lensRadius * .36f,
                    Math.max(1f, lensRadius * 1.62f),
                    new int[]{opacity(Color.WHITE, 145), opacity(accentColor, 56),
                            opacity(Color.WHITE, 14), opacity(Color.WHITE, 0)},
                    new float[]{0f, .36f, .7f, 1f}, Shader.TileMode.CLAMP));
        } else {
            bubble.setShader(null);
        }
    }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty()) return;
        if (lowPower) {
            // Battery-saver fast path: a gradient and one stroke, no clipping.
            canvas.drawRoundRect(surface, radiusPx, radiusPx, fill);
        } else {
            int saved = canvas.save();
            canvas.clipPath(clipping);
            canvas.drawRoundRect(surface, radiusPx, radiusPx, fill);
            canvas.drawRect(surface, ambient);
            canvas.drawPath(upperGlint, glint);
            canvas.drawPath(lowerWave, wave);
            if (showLens) canvas.drawCircle(lensX, lensY, lensRadius, bubble);
            canvas.restoreToCount(saved);
        }
        float borderRadius = Math.max(0f, radiusPx - outline.getStrokeWidth() * .5f);
        canvas.drawRoundRect(border, borderRadius, borderRadius, outline);
        if (!lowPower && strong) {
            float innerRadius = Math.max(0f, radiusPx - density * 2.4f);
            canvas.drawRoundRect(innerBorder, innerRadius, innerRadius, innerRim);
        }
    }

    private void applyAlpha() {
        fill.setAlpha(alpha);
        ambient.setAlpha(alpha);
        glint.setAlpha(Math.round(alpha * (strong ? .25f : .19f)));
        wave.setAlpha(Math.round(alpha * (strong ? .17f : .10f)));
        outline.setAlpha(Math.round(alpha * (strong ? .46f : .48f)));
        innerRim.setAlpha(Math.round(alpha * .16f));
        bubble.setAlpha(Math.round(alpha * .66f));
    }

    @Override public void setAlpha(int value) {
        int next = Math.max(0, Math.min(255, value));
        if (next == alpha) return;
        alpha = next;
        applyAlpha();
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        ambient.setColorFilter(filter);
        glint.setColorFilter(filter);
        wave.setColorFilter(filter);
        outline.setColorFilter(filter);
        innerRim.setColorFilter(filter);
        bubble.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
