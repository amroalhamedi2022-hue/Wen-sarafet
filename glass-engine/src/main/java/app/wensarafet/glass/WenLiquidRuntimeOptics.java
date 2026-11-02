package app.wensarafet.glass;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;

/**
 * Finite-gesture GPU refraction of actual navigation pixels (Android API 33+).
 * AGSL bends the image field and performs subtle red/blue wavelength dispersion.
 * The shader is allocated once per overlay and reused; low-end or unsupported
 * hardware safely falls back to the conventional two-plane Canvas optics.
 */
final class WenLiquidRuntimeOptics {
    private static final String SOURCE =
            "uniform shader image;\n" +
            "uniform float2 centre;\n" +
            "uniform float2 extent;\n" +
            "uniform float amount;\n" +
            "uniform float pointer;\n" +
            "half4 main(float2 p) {\n" +
            "  float2 local = (p - centre) / max(extent * 0.5, float2(1.0));\n" +
            "  float radial = min(length(local), 1.3);\n" +
            "  float peripheral = smoothstep(0.20, 1.0, radial);\n" +
            "  float2 bent = p - local * extent * (0.035 * amount + 0.075 * amount * peripheral);\n" +
            "  bent.x += (pointer - centre.x) * 0.018 * amount;\n" +
            "  float splitting = 2.35 * amount * peripheral;\n" +
            "  half4 middle = image.eval(bent);\n" +
            "  half4 red = image.eval(bent + float2(splitting, 0.0));\n" +
            "  half4 blue = image.eval(bent - float2(splitting, 0.0));\n" +
            "  half3 refracted = half3(red.r, middle.g, blue.b);\n" +
            "  float x = clamp(local.x * 0.5 + 0.5, 0.0, 1.0);\n" +
            "  half3 spectrum = half3(1.0 - smoothstep(0.24, 0.58, x),\n" +
            "      smoothstep(0.18, 0.47, x) * (1.0 - smoothstep(0.64, 0.86, x)),\n" +
            "      smoothstep(0.48, 0.81, x));\n" +
            "  float curve = -0.02 + 0.10 * local.x * local.x;\n" +
            "  float band = 1.0 - smoothstep(0.02, 0.15, abs(local.y - curve));\n" +
            "  float light = band * 0.24 * amount;\n" +
            "  refracted = mix(refracted, spectrum * 0.76 + half3(0.24), half(light));\n" +
            "  half a = max(max(red.a, blue.a), max(middle.a, half(light * 0.75)));\n" +
            "  return half4(refracted, a);\n" +
            "}\n";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private RuntimeShader shader;
    private boolean enabled;
    private boolean reportedHardware;

    boolean attach(BitmapShader backdrop) {
        try {
            if (shader == null) shader = new RuntimeShader(SOURCE);
            shader.setInputShader("image", backdrop);
            paint.setShader(shader);
            enabled = true;
            return true;
        } catch (RuntimeException | LinkageError error) {
            enabled = false;
            paint.setShader(null);
            android.util.Log.w("WenGlassProbe", "AGSL_UNAVAILABLE", error);
            return false;
        }
    }

    boolean draw(Canvas canvas, RectF lens, float strength, float pointerX) {
        if (!enabled || shader == null || !canvas.isHardwareAccelerated()) return false;
        try {
            shader.setFloatUniform("centre", lens.centerX(), lens.centerY());
            shader.setFloatUniform("extent", lens.width(), lens.height());
            shader.setFloatUniform("amount", strength);
            shader.setFloatUniform("pointer", pointerX);
            paint.setAlpha(Math.round(255f * strength));
            float radius = lens.height() * .49f;
            canvas.drawRoundRect(lens, radius, radius, paint);
            if (!reportedHardware && strength >= 0.6f) {
                reportedHardware = true;
                android.util.Log.i("WenGlassProbe", "AGSL_DRAW_ACTIVE");
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            enabled = false;
            paint.setShader(null);
            android.util.Log.e("WenGlassProbe", "AGSL_RENDER_ERROR", error);
            return false;
        }
    }

    void clear() {
        enabled = false;
        paint.setShader(null);
    }
}
