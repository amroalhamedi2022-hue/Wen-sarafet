package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Neutral lens fallback and shared specular rim for the live refraction path. */
public final class WenNavigationLensDrawable extends Drawable {
    private final float density;
    private final Paint body;
    private final Paint rim;
    private final RectF capsule = new RectF();
    private float morph;
    private int alpha = 255;

    public WenNavigationLensDrawable(Context context, boolean drawBody, boolean drawRim) {
        density = context.getResources().getDisplayMetrics().density;
        body = drawBody ? new Paint(Paint.ANTI_ALIAS_FLAG) : null;
        rim = drawRim ? new Paint(Paint.ANTI_ALIAS_FLAG) : null;
        if (rim != null) {
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(Math.max(.75f, density));
        }
    }

    @Override protected void onBoundsChange(Rect bounds) {
        capsule.set(bounds);
        capsule.inset(2f * density, 2f * density);
        if (capsule.isEmpty()) return;
        if (body != null) {
            body.setShader(new LinearGradient(capsule.left, capsule.top,
                    capsule.left, capsule.bottom,
                    new int[]{0x38FFFFFF, 0x10FFFFFF, 0x22909090},
                    new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        }
        if (rim != null) {
            rim.setShader(new LinearGradient(capsule.left, capsule.top, capsule.right, capsule.bottom,
                    new int[]{0xE2FFFFFF, 0x43FFFFFF, 0x2F202020, 0xAAFFFFFF},
                    new float[]{0f, .38f, .73f, 1f}, Shader.TileMode.CLAMP));
        }
    }

    public float getMorph() { return morph; }

    public void setMorph(float fraction) {
        float next = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(next - morph) < .003f) return;
        morph = next;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if (capsule.isEmpty()) return;
        float radius = Math.min(capsule.width(), capsule.height()) * .5f;
        if (body != null) {
            body.setAlpha(Math.round(alpha * (.78f + .22f * morph)));
            canvas.drawRoundRect(capsule, radius, radius, body);
        }
        if (rim != null) {
            rim.setAlpha(Math.round(alpha * (.77f + .23f * morph)));
            canvas.drawRoundRect(capsule, radius, radius, rim);
        }
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        if (body != null) body.setColorFilter(filter);
        if (rim != null) rim.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
