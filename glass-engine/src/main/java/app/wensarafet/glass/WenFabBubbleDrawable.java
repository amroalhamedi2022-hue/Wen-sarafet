package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Owner-approved flat clear-glass FAB. One uniform translucent fill, one quiet
 * perimeter stroke, and NO internal radial highlight, caustic, bloom or shader.
 * Touch and navigation listeners stay in Ui/MainActivity, unchanged.
 */
public final class WenFabBubbleDrawable extends Drawable {
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float press;
    private int alpha = 255;
    private float centerX, centerY, radius;

    public WenFabBubbleDrawable(Context context, boolean dark) {
        density = context.getResources().getDisplayMetrics().density;
        fill.setColor(dark ? 0xA83D4248 : 0xB5F5FAFD);
        rim.setColor(dark ? 0xA8EAF2FA : 0xB1FFFFFF);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Math.max(density * 0.85f, 1f));
    }

    @Override protected void onBoundsChange(Rect bounds) {
        centerX = bounds.exactCenterX();
        centerY = bounds.exactCenterY();
        radius = Math.max(0f, Math.min(bounds.width(), bounds.height()) * .5f
                - Math.max(1f, density * 1.35f));
    }

    public float getPress() { return press; }

    public void setPress(float fraction) {
        float next = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(next - press) < .005f) return;
        press = next;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if (radius <= 0f) return;
        // Subtle opacity shift is UNIFORM across the complete circle during hold.
        // It cannot introduce a white spot or fractured optical interior.
        fill.setAlpha(Math.round(alpha * (1f - .08f * press)));
        rim.setAlpha(Math.round(alpha * (.82f + .12f * press)));
        canvas.drawCircle(centerX, centerY, radius, fill);
        canvas.drawCircle(centerX, centerY, radius, rim);
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        rim.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
