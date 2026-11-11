package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Clear circular glass with one continuous bevel and a contact highlight. */
public final class WenFabBubbleDrawable extends Drawable {
    private final float density;
    private final boolean dark;
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF circle = new RectF();
    private float press;
    private int alpha = 255;

    public WenFabBubbleDrawable(Context context, boolean dark) {
        density = context.getResources().getDisplayMetrics().density;
        this.dark = dark;
        bevel.setStyle(Paint.Style.STROKE);
        bevel.setStrokeWidth(density);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        float radius = Math.min(bounds.width(), bounds.height()) * .5f - density * 1.6f;
        circle.set(bounds.exactCenterX() - radius, bounds.exactCenterY() - radius,
                bounds.exactCenterX() + radius, bounds.exactCenterY() + radius);
        if (circle.isEmpty()) return;
        body.setShader(new LinearGradient(circle.left, circle.top, circle.right, circle.bottom,
                dark ? new int[]{0x62333333, 0x2D222222, 0x70090909}
                     : new int[]{0xACFFFFFF, 0x50FFFFFF, 0x85FFFFFF},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        bevel.setShader(new LinearGradient(circle.left, circle.top, circle.right, circle.bottom,
                new int[]{0xE9FFFFFF, 0x61FFFFFF, 0x38505050, 0xB2FFFFFF},
                new float[]{0f, .37f, .76f, 1f}, Shader.TileMode.CLAMP));
        highlight.setShader(new RadialGradient(circle.left + circle.width() * .29f,
                circle.top + circle.height() * .10f, circle.width() * .64f,
                0x72FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    public float getPress() { return press; }

    public void setPress(float fraction) {
        float next = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(next - press) < .005f) return;
        press = next;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if (circle.isEmpty()) return;
        float x = circle.centerX();
        float y = circle.centerY();
        float radius = circle.width() * .5f;
        body.setAlpha(alpha);
        canvas.drawCircle(x, y, radius, body);
        highlight.setAlpha(Math.round(alpha * (.48f + .42f * press)));
        canvas.drawCircle(x, y, radius, highlight);
        bevel.setAlpha(Math.round(alpha * (.72f + .28f * press)));
        canvas.drawCircle(x, y, radius, bevel);
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        body.setColorFilter(filter);
        bevel.setColorFilter(filter);
        highlight.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
