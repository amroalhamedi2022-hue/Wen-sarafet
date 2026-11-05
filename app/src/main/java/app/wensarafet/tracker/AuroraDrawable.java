package app.wensarafet.tracker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public final class AuroraDrawable extends Drawable {
    private final int[] colors;
    private final float radius;
    private final boolean soft;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;

    public AuroraDrawable(int[] colors, float radius) {
        this(colors, radius, false);
    }

    public AuroraDrawable(int[] colors, float radius, boolean soft) {
        this.colors = colors;
        this.radius = radius;
        this.soft = soft;
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        int save = canvas.save();
        Path clip = new Path();
        clip.addRoundRect(new RectF(b), radius, radius, Path.Direction.CW);
        canvas.clipPath(clip);
        paint.setAlpha(alpha);
        paint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, colors[0], colors[1], Shader.TileMode.CLAMP));
        canvas.drawRect(b, paint);
        paint.setShader(null);

        float w = b.width(), h = b.height();

        if (soft) {
            drawSoftLayers(canvas, b, w, h);
            canvas.restoreToCount(save);
            return;
        }

        // Harmonious overlapping sheets.  Their contrast is deliberately restrained
        // so titles and values always sit on a calm, readable field.
        Path upperSheet = new Path();
        upperSheet.moveTo(b.left - w * .05f, b.top + h * .04f);
        upperSheet.cubicTo(b.left + w * .28f, b.top - h * .04f,
                b.left + w * .55f, b.top + h * .30f,
                b.right + w * .05f, b.top + h * .15f);
        upperSheet.lineTo(b.right, b.top); upperSheet.lineTo(b.left, b.top); upperSheet.close();
        drawLayer(canvas, upperSheet, colors[2], h * .018f, .055f, .58f);

        Path lowerSheet = new Path();
        lowerSheet.moveTo(b.left - w * .04f, b.top + h * .90f);
        lowerSheet.cubicTo(b.left + w * .25f, b.top + h * .72f,
                b.left + w * .58f, b.top + h * .96f,
                b.right + w * .05f, b.top + h * .72f);
        lowerSheet.lineTo(b.right, b.bottom); lowerSheet.lineTo(b.left, b.bottom); lowerSheet.close();
        drawLayer(canvas, lowerSheet, colors[3], h * .016f, .045f, .52f);
        canvas.restoreToCount(save);
    }

    private void drawSoftLayers(Canvas canvas, Rect b, float w, float h) {
        // Soft cards keep their centre nearly uniform. Decorations stay at the
        // top and bottom edges so an amount never lands on a contrasting band.
        Path topCorner = new Path();
        topCorner.moveTo(b.left, b.top);
        topCorner.lineTo(b.right, b.top);
        topCorner.lineTo(b.right, b.top + h * .16f);
        topCorner.cubicTo(b.left + w * .72f, b.top + h * .24f,
                b.left + w * .36f, b.top + h * .04f,
                b.left, b.top + h * .18f);
        topCorner.close();
        drawLayer(canvas, topCorner, colors[2], h * .01f, .025f, .42f);

        Path bottomCorner = new Path();
        bottomCorner.moveTo(b.left, b.top + h * .86f);
        bottomCorner.cubicTo(b.left + w * .28f, b.top + h * .70f,
                b.left + w * .70f, b.top + h * .92f,
                b.right, b.top + h * .76f);
        bottomCorner.lineTo(b.right, b.bottom);
        bottomCorner.lineTo(b.left, b.bottom);
        bottomCorner.close();
        drawLayer(canvas, bottomCorner, colors[3], h * .01f, .02f, .38f);
    }

    private void drawLayer(Canvas canvas, Path layer, int color, float shadowOffset,
                           float shadowOpacity, float layerOpacity) {
        int shadowSave = canvas.save();
        canvas.translate(0, shadowOffset);
        paint.setShader(null); paint.setColor(0xFF000000);
        paint.setAlpha(Math.round(alpha * shadowOpacity));
        canvas.drawPath(layer, paint);
        canvas.restoreToCount(shadowSave);
        paint.setColor(color); paint.setAlpha(Math.round(alpha * layerOpacity));
        canvas.drawPath(layer, paint);
    }

    @Override public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
