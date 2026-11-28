package app.wensarafet.glass;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * 1.8.8-sandbox's optical crown above the existing FAB '+' glyph.
 *
 * Applied by ViewOverlay, never a View, gesture detector, click target or
 * substitute for the existing FAB. Shader allocation is bounds-only; drawing
 * reuses all Paint/Matrix/RectF objects. A single finite press morph in Ui
 * drives BOTH the original bubble and the optical caustic.
 */
public final class WenFabOpticalCrownDrawable extends Drawable {
    private final float density;
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caustic = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Matrix shift = new Matrix();
    private LinearGradient causticShader;
    private LinearGradient glintShader;
    private float press;
    private int alpha = 255;

    public WenFabOpticalCrownDrawable(float density) {
        this.density = density;
        rim.setStyle(Paint.Style.STROKE);
        caustic.setStyle(Paint.Style.STROKE);
    }

    public void setPress(float fraction) {
        float next = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(next - press) < .005f) return;
        press = next;
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        int w = rect.width();
        int h = rect.height();
        if (w <= 0 || h <= 0) return;
        rim.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                new int[]{0xD3FFFFFF, 0x55FFFFFF, 0xC4FFFFFF, 0x79FFFFFF},
                new float[]{0f, .35f, .72f, 1f}, Shader.TileMode.CLAMP));
        causticShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                new int[]{0x00FFFFFF, 0x76FFFFFF, 0x58FFFFFF,
                        0x83FFFFFF, 0x50FFFFFF, 0x00FFFFFF},
                new float[]{0f, .17f, .36f, .53f, .77f, 1f}, Shader.TileMode.CLAMP);
        caustic.setShader(causticShader);
        glintShader = new LinearGradient(rect.left, rect.top, rect.right,
                rect.top + h * .55f, new int[]{
                        0x00FFFFFF, 0x99FFFFFF, 0x62FFFFFF, 0x00FFFFFF},
                new float[]{0f, .29f, .64f, 1f}, Shader.TileMode.CLAMP);
        glint.setShader(glintShader);
    }

    @Override public void draw(Canvas canvas) {
        Rect r = getBounds();
        int w = r.width(), h = r.height();
        if (w <= 0 || h <= 0 || causticShader == null || glintShader == null) return;
        float inset = Math.max(.8f, .9f * density);
        float radius = Math.min(w, h) * .5f - inset;
        if (radius <= 0f) return;
        bounds.set(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset);
        float t = press;

        rim.setStrokeWidth((.8f + .75f * t) * density);
        rim.setAlpha(Math.round(alpha * (102f + 77f * t) / 255f));
        canvas.drawRoundRect(bounds, radius, radius, rim);

        caustic.setStrokeWidth((.55f + .45f * t) * density);
        caustic.setAlpha(Math.round(alpha * (14f + 60f * t) / 255f));
        shift.reset();
        shift.setTranslate(w * .08f * t, 0f);
        causticShader.setLocalMatrix(shift);
        canvas.drawRoundRect(bounds, radius, radius, caustic);

        int save = canvas.save();
        canvas.clipRect(r.left, r.top, r.right, r.top + h * (.32f + .11f * t));
        glint.setStyle(Paint.Style.FILL);
        glint.setAlpha(Math.round(alpha * (26f + 115f * t) / 255f));
        shift.reset();
        shift.setTranslate(w * .05f * t, 0f);
        glintShader.setLocalMatrix(shift);
        canvas.drawRoundRect(bounds, radius, radius, glint);
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        rim.setColorFilter(filter);
        caustic.setColorFilter(filter);
        glint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
