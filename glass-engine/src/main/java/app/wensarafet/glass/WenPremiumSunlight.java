package app.wensarafet.glass;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.os.Build;

/**
 * Android 13+ light scattering for the BACKDROP only. Soft angular Gaussian
 * solar rays rather than hard triangular/diagonal stripes crossing cards.
 *
 * This is composited as a subtle static optical field, never a clock-driven
 * animation. Pre-Android-13 devices keep existing Canvas radial fallbacks.
 * One RuntimeShader and one Paint per backdrop, uniforms set on size change;
 * allocation-free and timer-free draw().
 */
final class WenPremiumSunlight {
    private static final String FRAGMENT =
            "uniform float4 uRect;\n" +
            "uniform float2 uLight;\n" +
            "uniform float4 uBeam;\n" +
            "uniform float3 uColor;\n" +
            "float bell(float a, float sigma) {\n" +
            "  float q=a/max(sigma,0.001); return exp(-0.5*q*q);\n" +
            "}\n" +
            "half4 main(float2 p) {\n" +
            "  float2 uv=(p-uRect.xy)/max(uRect.zw,float2(1.0));\n" +
            "  float2 delta=uv-uLight;\n" +
            "  delta.x *= uRect.z / max(uRect.w,1.0);\n" +
            "  float radius=length(delta);\n" +
            "  float theta=atan(delta.y,delta.x);\n" +
            "  float first=bell(sin(theta-uBeam.x),uBeam.z);\n" +
            "  float second=bell(sin(theta-uBeam.y),uBeam.z*0.77);\n" +
            "  float radial=exp(-radius*uBeam.w);\n" +
            "  float flare=bell(radius,0.18);\n" +
            "  float shafts=(0.7*first+0.47*second)*radial;\n" +
            "  float glow=flare*0.41+shafts*0.39;\n" +
            "  glow *= smoothstep(-0.15,0.13,uv.x);\n" +
            "  return half4(half3(uColor),half(clamp(glow,0.0,0.39)));\n" +
            "}\n";

    private final boolean supported;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RuntimeShader shader;

    WenPremiumSunlight(boolean powerSafe) {
        supported = Build.VERSION.SDK_INT >= 33 && !powerSafe;
        if (supported) {
            // Shader can be dynamically disabled if a renderer rejects it.
            try {
                shader = new RuntimeShader(FRAGMENT);
                paint.setShader(shader);
            } catch (RuntimeException ignored) {
                shader = null;
            }
        }
    }

    void configure(RectF area, int theme) {
        if (shader == null || area.isEmpty()) return;
        float keyX,keyY,thetaA,thetaB,spread,fade,r,g,b;
        switch (theme) {
            case 25: // Royal: upper-left sunlight with bronze feathered shafts.
                keyX=.12f;keyY=.14f;thetaA=.16f;thetaB=.63f;
                spread=.27f;fade=1.24f;r=1f;g=.70f;b=.37f;
                break;
            case 26: // Imperial: brighter low-angle side light on charcoal.
                keyX=.21f;keyY=.07f;thetaA=.92f;thetaB=1.38f;
                spread=.25f;fade=1.40f;r=1f;g=.81f;b=.57f;
                break;
            case 27: // Champagne: tiny ivory liquid-silk highlight, not dark.
                keyX=.35f;keyY=.03f;thetaA=.53f;thetaB=1.90f;
                spread=.34f;fade=1.24f;r=1f;g=.94f;b=.78f;
                break;
            case 28: // Peach emerald: warm sunset spills into cool green.
                keyX=.08f;keyY=.20f;thetaA=.14f;thetaB=.83f;
                spread=.41f;fade=1.09f;r=1f;g=.70f;b=.38f;
                break;
            default: // Violet: restrained pink fill, no gold rays.
                keyX=.16f;keyY=.42f;thetaA=.08f;thetaB=-.49f;
                spread=.33f;fade=1.52f;r=1f;g=.43f;b=.79f;
                break;
        }
        try {
            shader.setFloatUniform("uRect",area.left,area.top,
                    area.width(),area.height());
            shader.setFloatUniform("uLight",keyX,keyY);
            shader.setFloatUniform("uBeam",thetaA,thetaB,spread,fade);
            shader.setFloatUniform("uColor",r,g,b);
        } catch (RuntimeException unsupported) {
            shader = null;
            paint.setShader(null);
        }
    }

    void draw(Canvas canvas, RectF bounds) {
        if (shader == null || !canvas.isHardwareAccelerated()) return;
        canvas.drawRect(bounds,paint);
    }
}
