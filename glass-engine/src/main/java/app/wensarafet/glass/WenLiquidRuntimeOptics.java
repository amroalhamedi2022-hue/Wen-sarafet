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
            "  float2 d = p - centre;\n" +
            "  float hw = max(extent.x * 0.5, 1.0);\n" +
            "  float hr = max(extent.y * 0.5, 1.0);\n" +
            "  float straight = max(hw - hr, 0.0);\n" +
            "  float2 shell = float2(d.x - clamp(d.x, -straight, straight), d.y);\n" +
            "  float shellLength = length(shell);\n" +
            "  float depth = clamp((hr - shellLength) / hr, 0.0, 1.0);\n" +
            "  float opticalEdge = 1.0 - smoothstep(0.02, 0.43, depth);\n" +
            "  float2 normal = shell / max(shellLength, 0.01);\n" +
            "  float bendPx = amount * (1.4 + 5.8 * opticalEdge);\n" +
            "  float2 refractedCoordinate = p - normal * bendPx;\n" +
            "  float split = 0.48 * amount * opticalEdge;\n" +
            "  half4 middle = image.eval(refractedCoordinate);\n" +
            "  half4 red = image.eval(refractedCoordinate + normal * split);\n" +
            "  half4 blue = image.eval(refractedCoordinate - normal * split);\n" +
            "  half a = max(max(red.a, middle.a), blue.a);\n" +
            "  half3 refracted = half3(red.r, middle.g, blue.b);\n" +
            "  float nx = d.x / hw;\n" +
            "  float ny = d.y / hr;\n" +
            "  float drift = (pointer - centre.x) / hw;\n" +
            "  float arch = -0.12 + 0.07 * sin(nx * 2.7 + drift * 0.45) + 0.06 * nx * nx;\n" +
            "  float beam = exp(-72.0 * pow(ny - arch, 2.0));\n" +
            "  beam *= 1.0 - smoothstep(0.76, 0.97, abs(nx));\n" +
            "  float u = clamp(0.5 + 0.5 * nx + 0.10 * drift, 0.0, 1.0);\n" +
            "  half3 ice = half3(0.25, 0.78, 1.0);\n" +
            "  half3 rose = half3(0.98, 0.37, 0.72);\n" +
            "  half3 gold = half3(1.0, 0.88, 0.45);\n" +
            "  half3 violet = half3(0.46, 0.52, 1.0);\n" +
            "  half3 leftSpectrum = mix(ice, rose, half(smoothstep(0.1, 0.48, u)));\n" +
            "  half3 rightSpectrum = mix(gold, violet, half(smoothstep(0.52, 0.96, u)));\n" +
            "  half3 spectrum = mix(leftSpectrum, rightSpectrum, half(smoothstep(0.44, 0.64, u)));\n" +
            "  float upperGlint = exp(-48.0 * pow(ny + 0.82, 2.0));\n" +
            "  float glint = upperGlint * (1.0 - smoothstep(0.65, 0.98, abs(nx)));\n" +
            "  float light = amount * (0.28 * beam + 0.075 * glint + 0.045 * opticalEdge);\n" +
            "  a = max(a, half(light * 0.6));\n" +
            "  refracted = mix(refracted, spectrum * a, half(clamp(light, 0.0, 0.36)));\n" +
            "  return half4(min(refracted, half3(a)), a);\n" +
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
            paint.setAlpha(Math.round(200f * strength));
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
