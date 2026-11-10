package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Monochrome, reference-style icons for premium bottom tabs. The original
 * TextView, its listeners, gesture semantics, and tab ordering remain intact.
 *
 * Pure vector geometry, no assets from bank icons, no allocations per frame.
 */
public final class WenPremiumTabIconDrawable extends Drawable {
    private final int tab;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filled = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int dim;
    private int opacity=255;

    WenPremiumTabIconDrawable(Context context, int theme, int tabIndex, boolean selected) {
        tab=tabIndex;
        dim=Ui.dp(context, 29);
        // Glyph strokes and Arabic labels share ONE visual token source.
        int ink = theme <= 27
                ? WenGoldGlassSurface.navigationInk(theme, selected)
                : selected ? Color.WHITE : (theme == 28 ? 0xFFE5F5E8 : 0xFFF6E6FF);
        line.setColor(ink);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(selected?2.2f:1.9f);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        filled.setColor(ink);
        filled.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override public int getIntrinsicWidth() { return dim; }
    @Override public int getIntrinsicHeight() { return dim; }

    @Override public void draw(Canvas canvas) {
        Rect bounds=getBounds();
        if (bounds.isEmpty()) return;
        final int saved=canvas.save();
        canvas.translate(bounds.left,bounds.top);
        canvas.scale(bounds.width()/24f,bounds.height()/24f);
        path.reset();
        switch(tab) {
            case 0: // outlined home, not a multicolour Android emoji
                path.moveTo(3.2f,11.3f);
                path.lineTo(12f,4f);
                path.lineTo(20.8f,11.3f);
                path.moveTo(5.7f,10f);path.lineTo(5.7f,20.1f);
                path.lineTo(18.3f,20.1f);path.lineTo(18.3f,10f);
                path.moveTo(10.0f,20.1f);path.lineTo(10.0f,14.7f);
                path.lineTo(14.0f,14.7f);path.lineTo(14.0f,20.1f);
                canvas.drawPath(path,line);
                break;
            case 1: // paper receipt with ledger lines
                canvas.drawRoundRect(5.5f,3.0f,18.5f,21.0f,1.5f,1.5f,line);
                canvas.drawLine(8.4f,8.5f,15.4f,8.5f,line);
                canvas.drawLine(8.4f,12.4f,15.7f,12.4f,line);
                canvas.drawLine(8.4f,16.3f,13.2f,16.3f,line);
                break;
            case 2: // outlined heart/debt promise with clean geometric core
                path.moveTo(12f,20.1f);
                path.cubicTo(9.5f,17.7f,3.2f,13.0f,3.2f,8.9f);
                path.cubicTo(3.2f,3.3f,9.0f,2.7f,12f,6.6f);
                path.cubicTo(15.0f,2.7f,20.8f,3.3f,20.8f,8.9f);
                path.cubicTo(20.8f,13.0f,14.5f,17.7f,12f,20.1f);
                canvas.drawPath(path,line);
                canvas.drawLine(9.2f,11.9f,14.8f,11.9f,line);
                break;
            default: // three clean analytics columns with rounded ends
                line.setStrokeWidth(3.1f);
                canvas.drawLine(5.2f,19.0f,5.2f,13.7f,line);
                canvas.drawLine(11.8f,19.0f,11.8f,5.8f,line);
                canvas.drawLine(18.4f,19.0f,18.4f,9.6f,line);
                break;
        }
        canvas.restoreToCount(saved);
    }

    @Override public void setAlpha(int alpha) {
        opacity=Math.max(0,Math.min(255,alpha));
        line.setAlpha(opacity);
        filled.setAlpha(opacity);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {
        line.setColorFilter(filter);
        filled.setColorFilter(filter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
