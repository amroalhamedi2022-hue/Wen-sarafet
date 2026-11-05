package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/**
 * Fixed-size expense chart. Its line/area paths and gradient are cached on
 * resize rather than reconstructed on every draw. Hardware accelerated:
 * no software layer, shadow blur, background ticker or offscreen bitmap.
 */
public final class ExpenseChartView extends View {
    private final long[] values;
    private final String[] labels;
    private final boolean lineMode;
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path areaPath = new Path();
    private final float[] pointsX;
    private final float[] pointsY;
    private final float dp;
    private float top;
    private float bottom;
    private float maximum;
    private float slot;
    private float barWidth;

    public ExpenseChartView(Context context, long[] values, String[] labels) {
        this(context, values, labels, true);
    }

    public ExpenseChartView(Context context, long[] values, String[] labels, boolean lineMode) {
        super(context);
        this.values = values;
        this.labels = labels;
        this.lineMode = lineMode;
        this.dp = context.getResources().getDisplayMetrics().density;
        this.pointsX = new float[values.length];
        this.pointsY = new float[values.length];

        linePaint.setColor(Ui.VIOLET);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f * dp);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        whiteRimPaint.setColor(Color.argb(205, 255, 255, 255));
        whiteRimPaint.setStyle(Paint.Style.STROKE);
        whiteRimPaint.setStrokeWidth(6f * dp);
        whiteRimPaint.setStrokeCap(Paint.Cap.ROUND);
        whiteRimPaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setColor(Ui.VIOLET);
        pointPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(Ui.MUTED);
        labelPaint.setTextSize(11f * dp);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        barPaint.setStyle(Paint.Style.FILL);
        areaPaint.setStyle(Paint.Style.FILL);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        top = 18f * dp;
        bottom = height - 30f * dp;
        if (values.length == 0 || width <= 0 || bottom <= top) return;
        long maxValue = 1L;
        for (long value : values) if (value > maxValue) maxValue = value;
        maximum = (float) maxValue;
        slot = width / (float) values.length;
        barWidth = Math.min(slot * .46f, 30f * dp);

        linePath.reset();
        areaPath.reset();
        for (int i = 0; i < values.length; i++) {
            float proportion = Math.max(0f, values[i] / maximum);
            float barHeight = Math.max(values[i] > 0 ? 7f * dp : 3f * dp,
                    (bottom - top) * proportion);
            float center = slot * i + slot / 2f;
            float y = bottom - barHeight;
            pointsX[i] = center;
            pointsY[i] = y;
            if (i == 0) {
                linePath.moveTo(center, y);
                areaPath.moveTo(center, bottom);
                areaPath.lineTo(center, y);
            } else {
                linePath.lineTo(center, y);
                areaPath.lineTo(center, y);
            }
        }
        areaPath.lineTo(pointsX[values.length - 1], bottom);
        areaPath.close();
        areaPaint.setShader(new LinearGradient(0, top, 0, bottom,
                new int[]{Color.argb(108, Color.red(Ui.SKY), Color.green(Ui.SKY), Color.blue(Ui.SKY)),
                        Color.argb(0, Color.red(Ui.SKY), Color.green(Ui.SKY), Color.blue(Ui.SKY))},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length == 0 || getWidth() <= 0 || bottom <= top) return;
        if (lineMode) {
            canvas.drawPath(areaPath, areaPaint);
            canvas.drawPath(linePath, whiteRimPaint);
            canvas.drawPath(linePath, linePaint);
            for (int i = 0; i < values.length; i++) {
                canvas.drawCircle(pointsX[i], pointsY[i], 5f * dp, pointPaint);
            }
        } else {
            for (int i = 0; i < values.length; i++) {
                float center = pointsX[i];
                float barHeight = bottom - pointsY[i];
                barPaint.setColor(values[i] > 0 ? Ui.VIOLET : Ui.BRAND_SOFT);
                // Hardware Canvas float API; no new RectF allocation on each frame.
                canvas.drawRoundRect(center - barWidth / 2, bottom - barHeight,
                        center + barWidth / 2, bottom, barWidth / 2, barWidth / 2, barPaint);
            }
        }
        for (int i = 0; i < values.length; i++) {
            canvas.drawText(i < labels.length && labels[i] != null ? labels[i] : "",
                    pointsX[i], getHeight() - 8f * dp, labelPaint);
        }
    }
}
