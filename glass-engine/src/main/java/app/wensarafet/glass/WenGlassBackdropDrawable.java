package app.wensarafet.glass;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

/**
 * Passive ambient glass atmosphere. No blur, animation, bitmap, clock, or
 * background work. This is placed once behind primary screen content, not
 * instantiated once per scrolling list row.
 */
public final class WenGlassBackdropDrawable extends Drawable {
    private final int background;
    private final int accent;
    private final int secondary;
    private final boolean dark;
    private final boolean lowPower;
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowLeft = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowRight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boundsRect = new RectF();
    private int alpha = 255;

    public WenGlassBackdropDrawable(Context context, int backgroundColor, int accentColor,
                                    int secondaryColor, boolean isDark) {
        background = backgroundColor;
        accent = accentColor;
        secondary = secondaryColor;
        dark = isDark;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager activity = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        lowPower = (power != null && power.isPowerSaveMode())
                || (activity != null && activity.isLowRamDevice());
        base.setColor(background);
    }

    private static int opacity(int color, int value) {
        return Color.argb(value, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int a, int b, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        return Color.rgb(Math.round(Color.red(a) * (1f - f) + Color.red(b) * f),
                Math.round(Color.green(a) * (1f - f) + Color.green(b) * f),
                Math.round(Color.blue(a) * (1f - f) + Color.blue(b) * f));
    }

    @Override protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        boundsRect.set(rect);
        if (rect.isEmpty()) return;
        float w = boundsRect.width();
        float h = boundsRect.height();
        if (!lowPower) {
            base.setShader(new LinearGradient(boundsRect.left, boundsRect.top,
                    boundsRect.left, boundsRect.bottom,
                    new int[]{blend(background, accent, dark ? .20f : .09f), background, background},
                    new float[]{0f, .44f, 1f}, Shader.TileMode.CLAMP));
            float radius = Math.max(1f, Math.max(w * .80f, h * .40f));
            glowLeft.setShader(new RadialGradient(
                    boundsRect.left + w * .04f, boundsRect.top + h * .12f, radius,
                    new int[]{opacity(secondary, dark ? 100 : 65),
                            opacity(secondary, 0)},
                    null, Shader.TileMode.CLAMP));
            glowRight.setShader(new RadialGradient(
                    boundsRect.right - w * .06f, boundsRect.top + h * .27f, radius,
                    new int[]{opacity(accent, dark ? 72 : 39),
                            opacity(accent, 0)},
                    null, Shader.TileMode.CLAMP));
            glowLeft.setAlpha(alpha);
            glowRight.setAlpha(alpha);
        } else {
            base.setShader(null);
            base.setColor(background);
        }
        base.setAlpha(alpha);
    }

    @Override public void draw(Canvas canvas) {
        if (boundsRect.isEmpty()) return;
        canvas.drawRect(boundsRect, base);
        if (!lowPower) {
            canvas.drawRect(boundsRect, glowLeft);
            canvas.drawRect(boundsRect, glowRight);
        }
    }

    @Override public void setAlpha(int value) {
        int next = Math.max(0, Math.min(255, value));
        if (alpha == next) return;
        alpha = next;
        base.setAlpha(alpha);
        glowLeft.setAlpha(alpha);
        glowRight.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        base.setColorFilter(filter);
        glowLeft.setColorFilter(filter);
        glowRight.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
