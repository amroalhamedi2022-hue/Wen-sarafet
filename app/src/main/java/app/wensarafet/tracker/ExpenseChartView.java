package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public final class ExpenseChartView extends View {
    private final long[] values;
    private final String[] labels;
    private final boolean lineMode;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ExpenseChartView(Context context, long[] values, String[] labels) {
        this(context, values, labels, true);
    }

    public ExpenseChartView(Context context, long[] values, String[] labels, boolean lineMode) {
        super(context);
        this.values = values;
        this.labels = labels;
        this.lineMode = lineMode;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float top = Ui.dp(getContext(), 18);
        float bottom = height - Ui.dp(getContext(), 30);
        long max = 1;
        for (long value : values) if (value > max) max = value;

        float slot = width / values.length;
        float barWidth = Math.min(slot * 0.46f, Ui.dp(getContext(), 30));
        float previousX = 0, previousY = 0;
        for (int i = 0; i < values.length; i++) {
            float ratio = values[i] / (float) max;
            float barHeight = Math.max(values[i] > 0 ? Ui.dp(getContext(), 7) : Ui.dp(getContext(), 3), (bottom - top) * ratio);
            float center = slot * i + slot / 2f;
            float pointY = bottom - barHeight;
            if (lineMode) {
                paint.setColor(Ui.VIOLET); paint.setStrokeWidth(Ui.dp(getContext(), 4)); paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND); paint.setShadowLayer(Ui.dp(getContext(), 7), 0, Ui.dp(getContext(), 2), Color.argb(70, 93, 63, 170));
                if (i > 0) canvas.drawLine(previousX, previousY, center, pointY, paint);
                paint.setStyle(Paint.Style.FILL); canvas.drawCircle(center, pointY, Ui.dp(getContext(), 5), paint);
                previousX = center; previousY = pointY;
            } else {
                paint.setColor(values[i] > 0 ? Ui.VIOLET : Color.rgb(229, 226, 241));
                paint.setShadowLayer(values[i] > 0 ? Ui.dp(getContext(), 6) : 0, 0, Ui.dp(getContext(), 2), Color.argb(60, 93, 63, 170));
                // Avoid allocating RectF objects on every frame while scrolling or animating.
                canvas.drawRoundRect(center - barWidth / 2, bottom - barHeight,
                        center + barWidth / 2, bottom, barWidth / 2, barWidth / 2, paint);
            }

            paint.clearShadowLayer();
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.dp(getContext(), 11));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(i < labels.length && labels[i] != null ? labels[i] : "",
                    center, height - Ui.dp(getContext(), 8), paint);
        }
    }
}
