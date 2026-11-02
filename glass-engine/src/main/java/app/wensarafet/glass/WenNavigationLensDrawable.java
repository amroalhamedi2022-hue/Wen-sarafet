package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Single, reusable bottom navigation glass lens.
 *
 * Pearlescent body + graded external/inner rims, subtle top highlight,
 * no live screen blur and no constantly-running shader. Paths/gradients
 * are cached on size change, not allocated during onDraw or while idle.
 */
public final class WenNavigationLensDrawable extends Drawable {
    private final float density;
    private final boolean dark;
    private final int top;
    private final int bottom;
    private final int accent;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final RectF edge = new RectF();
    private final RectF inner = new RectF();
    private final RectF upper = new RectF();
    private float radius;
    private int alpha = 255;

    public WenNavigationLensDrawable(Context context, int first, int second,
                                     int accentColor, boolean darkTheme) {
        density = context.getResources().getDisplayMetrics().density;
        top = first;
        bottom = second;
        accent = accentColor;
        dark = darkTheme;
        outerRim.setStyle(Paint.Style.STROKE);
        outerRim.setStrokeWidth(Math.max(1f, density * 1.3f));
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(Math.max(.7f, density * .8f));
        topLight.setStyle(Paint.Style.STROKE);
        topLight.setStrokeWidth(Math.max(1f, density * 1.4f));
        topLight.setStrokeCap(Paint.Cap.ROUND);
    }

    private static int opacity(int color, int a) {
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        surface.set(bounds);
        float inset = density * 2.6f;
        surface.inset(inset, inset * .9f);
        radius = Math.max(0f, surface.height() * .49f);
        edge.set(surface);
        edge.inset(outerRim.getStrokeWidth()*.5f, outerRim.getStrokeWidth()*.5f);
        inner.set(surface);
        inner.inset(density * 2.1f, density * 2.1f);
        upper.set(inner);
        upper.inset(density * 4f, density * 2.1f);

        fill.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{top, opacity(accent, dark ? 70 : 110), bottom},
                new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
        // The primary light band is an edge, not a large opaque rectangle.
        outerRim.setShader(new LinearGradient(surface.left, surface.top,
                surface.right, surface.bottom,
                new int[]{opacity(Color.WHITE, dark ? 185 : 245),
                        opacity(accent, dark ? 140 : 95),
                        opacity(Color.WHITE, dark ? 125 : 205)},
                null, Shader.TileMode.CLAMP));
        innerRim.setColor(opacity(Color.WHITE, dark ? 78 : 176));
        topLight.setColor(opacity(Color.WHITE, dark ? 100 : 190));
        applyAlpha();
    }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty()) return;
        canvas.drawRoundRect(surface, radius, radius, fill);
        canvas.drawRoundRect(edge, Math.max(0f, radius - density), 
                Math.max(0f, radius - density), outerRim);
        canvas.drawRoundRect(inner, Math.max(0f, radius - density * 2.1f),
                Math.max(0f, radius - density * 2.1f), innerRim);
        if (upper.width()>0f) {
            canvas.drawLine(upper.left, upper.top, upper.right, upper.top, topLight);
        }
    }

    private void applyAlpha() {
        fill.setAlpha(alpha);
        outerRim.setAlpha(alpha);
        innerRim.setAlpha(alpha);
        topLight.setAlpha(alpha);
    }

    @Override public void setAlpha(int a) {
        alpha = Math.max(0, Math.min(255, a));
        applyAlpha();
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter);
        outerRim.setColorFilter(filter);
        innerRim.setColorFilter(filter);
        topLight.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
