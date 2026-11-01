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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

/**
 * Reusable, battery-aware, static liquid-glass surface for Android Views.
 *
 * This intentionally does not simulate live background refraction. It is the
 * low-energy first layer of the visual engine: precomputed geometry/shaders,
 * zero animation ticks and zero ongoing work when a screen is not redrawn.
 * The host supplies colors and shape; no finance-app dependency exists.
 */
public final class WenGlassDrawable extends Drawable {
    private final float radiusPx;
    private final int startColor;
    private final int endColor;
    private final int accentColor;
    private final boolean strong;
    private final boolean lowPower;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final RectF border = new RectF();
    private final Path clipping = new Path();
    private final Path upperGlint = new Path();
    private final Path lowerWave = new Path();
    private int alpha = 255;

    public WenGlassDrawable(Context context, int radiusPx, int top, int bottom,
                            int accent, boolean strongSurface) {
        this.radiusPx = Math.max(0, radiusPx);
        this.startColor = top;
        this.endColor = bottom;
        this.accentColor = accent;
        this.strong = strongSurface;

        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager activity = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.lowPower = (power != null && power.isPowerSaveMode())
                || (activity != null && activity.isLowRamDevice());

        fill.setStyle(Paint.Style.FILL);
        glint.setColor(Color.WHITE);
        glint.setStyle(Paint.Style.FILL);
        wave.setColor(accentColor);
        wave.setStyle(Paint.Style.FILL);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(context.getResources().getDisplayMetrics().density);
        outline.setColor(Color.WHITE);
        applyAlpha();
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.isEmpty()) return;

        surface.set(bounds);
        border.set(bounds);
        float halfStroke = outline.getStrokeWidth() / 2f;
        border.inset(halfStroke, halfStroke);
        float width = surface.width();
        float height = surface.height();

        // Shader and paths are created on bounds changes, not every draw/frame.
        fill.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom, startColor, endColor, Shader.TileMode.CLAMP));
        clipping.reset();
        clipping.addRoundRect(surface, radiusPx, radiusPx, Path.Direction.CW);

        upperGlint.reset();
        upperGlint.moveTo(surface.left, surface.top);
        upperGlint.lineTo(surface.right, surface.top);
        upperGlint.lineTo(surface.right, surface.top + height * .18f);
        upperGlint.cubicTo(surface.left + width * .69f, surface.top + height * .32f,
                surface.left + width * .31f, surface.top + height * .02f,
                surface.left, surface.top + height * .16f);
        upperGlint.close();

        lowerWave.reset();
        lowerWave.moveTo(surface.left, surface.bottom - height * .18f);
        lowerWave.cubicTo(surface.left + width * .23f, surface.bottom - height * .35f,
                surface.left + width * .65f, surface.bottom - height * .06f,
                surface.right, surface.bottom - height * .25f);
        lowerWave.lineTo(surface.right, surface.bottom);
        lowerWave.lineTo(surface.left, surface.bottom);
        lowerWave.close();
    }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty()) return;
        int saved = canvas.save();
        canvas.clipPath(clipping);
        canvas.drawRoundRect(surface, radiusPx, radiusPx, fill);
        // Battery saver and low-RAM devices bypass the two decorative layers.
        if (!lowPower) {
            canvas.drawPath(upperGlint, glint);
            canvas.drawPath(lowerWave, wave);
        }
        canvas.restoreToCount(saved);
        canvas.drawRoundRect(border,
                Math.max(0, radiusPx - outline.getStrokeWidth() / 2f),
                Math.max(0, radiusPx - outline.getStrokeWidth() / 2f), outline);
    }

    private void applyAlpha() {
        fill.setAlpha(alpha);
        glint.setAlpha(Math.round(alpha * (strong ? .20f : .32f)));
        wave.setAlpha(Math.round(alpha * (strong ? .15f : .07f)));
        outline.setAlpha(Math.round(alpha * (strong ? .54f : .73f)));
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
        glint.setColorFilter(filter);
        wave.setColorFilter(filter);
        outline.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
