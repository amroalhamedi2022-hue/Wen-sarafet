package app.wensarafet.glass;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

/**
 * Optional, static Android 13 AGSL lens. It refracts a locally defined
 * synthetic gradient, NOT the live pixels behind the panel. This keeps it
 * private, deterministic, and idle-friendly without readback bitmaps.
 *
 * Available only in the isolated laboratory until user acceptance and
 * frame-time, battery and thermal measurements prove it safe.
 */
public final class WenOpticalDrawable extends Drawable {
    private static final String AGSL =
            "uniform shader scene;\n" +
            "uniform float2 size;\n" +
            "uniform float strength;\n" +
            "half4 main(float2 xy) {\n" +
            "  float2 uv = xy / max(size, float2(1.0));\n" +
            "  float2 centered = uv - float2(0.5);\n" +
            "  float rim = smoothstep(0.55, 0.98, max(abs(centered.x), abs(centered.y)) * 2.0);\n" +
            "  float2 shift = centered * rim * strength * 24.0;\n" +
            "  half4 sampleColor = scene.eval(xy + shift);\n" +
            "  float topLight = (1.0 - smoothstep(0.0, 0.42, uv.y)) * 0.16;\n" +
            "  half glow = half(min(0.37, topLight + rim * 0.20));\n" +
            "  return half4(mix(sampleColor.rgb, half3(1.0), glow), 1.0);\n" +
            "}\n";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boundsRect = new RectF();
    private final RuntimeShader runtime;
    private final WenGlassDrawable fallback;
    private final int first;
    private final int second;
    private final float radiusPx;
    private boolean enabled;
    private int alpha = 255;

    public WenOpticalDrawable(Context context, int radius, int startColor,
                              int endColor, int accentColor) {
        first = startColor;
        second = endColor;
        radiusPx = Math.max(0, radius);
        fallback = new WenGlassDrawable(context, radius, startColor, endColor,
                accentColor, true);
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager memory = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean limited = (power != null && power.isPowerSaveMode())
                || (memory != null && memory.isLowRamDevice());
        RuntimeShader result = null;
        if (!limited) {
            try {
                result = new RuntimeShader(AGSL);
            } catch (RuntimeException unsupported) {
                // A vendor shader compilation failure must never crash the host app.
                result = null;
            }
        }
        runtime = result;
        enabled = result != null;
        paint.setAlpha(alpha);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        boundsRect.set(bounds);
        fallback.setBounds(bounds);
        if (runtime == null || bounds.isEmpty()) {
            enabled = false;
            return;
        }
        try {
            runtime.setFloatUniform("size", bounds.width(), bounds.height());
            runtime.setFloatUniform("strength", 0.8f);
            // The scene is a synthetic and cached gradient, not captured screen content.
            runtime.setInputShader("scene", new LinearGradient(bounds.left, bounds.top,
                    bounds.right, bounds.bottom, first, second, Shader.TileMode.CLAMP));
            paint.setShader(runtime);
            enabled = true;
        } catch (RuntimeException unsupported) {
            enabled = false;
        }
    }

    @Override public void draw(Canvas canvas) {
        if (boundsRect.isEmpty()) return;
        if (!enabled || !canvas.isHardwareAccelerated()) {
            fallback.draw(canvas);
            return;
        }
        try {
            canvas.drawRoundRect(boundsRect, radiusPx, radiusPx, paint);
        } catch (RuntimeException unavailable) {
            enabled = false;
            fallback.draw(canvas);
        }
    }

    @Override public void setAlpha(int value) {
        int next = Math.max(0, Math.min(255, value));
        if (next == alpha) return;
        alpha = next;
        paint.setAlpha(alpha);
        fallback.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
        fallback.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
