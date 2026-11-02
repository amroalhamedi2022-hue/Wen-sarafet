package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * A real optical layer for the bottom navigation strip: it magnifies the
 * sampled tab glyphs beneath a moving, rounded glass droplet. A snapshot of
 * the very small tab strip is captured ONCE at finger-down; each pointer move
 * only updates a shader matrix. No whole-screen blur, PixelCopy, timer or
 * extra draw allocations. The source tabs remain fully functional and own
 * the gesture. The overlay is passive and accessibility-hidden.
 */
public final class WenOpticalNavigationOverlay extends View {
    private final float density;
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint magnified = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix sampleMatrix = new Matrix();
    private final RectF droplet = new RectF();
    private WenNavigationTrack hostTrack;
    private Bitmap source;
    private BitmapShader sample;
    private float centreX, opacity;
    private boolean captured;
    private boolean reportedDraw;
    private final boolean dark;

    public WenOpticalNavigationOverlay(Context context, boolean darkTheme) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        dark = darkTheme;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
        // This view is an off-hierarchy painting helper; WenNavigationTrack
        // invokes its onDraw() in the track's actual child-composition pass.
        body.setColor(dark ? Color.rgb(59, 57, 96) : Color.rgb(212, 220, 252));
        rim.setColor(dark ? 0xD8F2F3FF : 0xEEFFFFFF);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(density * 1.6f);
        innerRim.setColor(dark ? 0x8FB5BAFF : 0xC0B6CCFF);
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(density * .85f);
        glow.setColor(dark ? 0x48B8B2FF : 0x70FFFFFF);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(density * 3f);
        setVisibility(INVISIBLE);
    }

    void setHostTrack(WenNavigationTrack track) { hostTrack = track; }

    void renderOnto(Canvas canvas) {
        if (captured && opacity > .005f) onDraw(canvas);
    }

    private void requestPaint() {
        if (hostTrack != null) hostTrack.invalidateOptic();
    }

    /** Tab-only content snapshot, not any financial screen or personal data. */
    public void begin(View tabStrip, float pointerX) {
        // Keep a small tab-only buffer between presses. Recycle only when
        // detached, not while RenderThread may still reference its texture.
        captured = false;
        reportedDraw = false;
        opacity = 0f;
        if (tabStrip == null || tabStrip.getWidth() <= 0 || tabStrip.getHeight() <= 0) return;
        try {
            if (source == null || source.getWidth() != tabStrip.getWidth()
                    || source.getHeight() != tabStrip.getHeight()) {
                source = Bitmap.createBitmap(tabStrip.getWidth(), tabStrip.getHeight(),
                        Bitmap.Config.ARGB_8888);
            } else source.eraseColor(Color.TRANSPARENT);
            Canvas capture = new Canvas(source);
            tabStrip.draw(capture);
            sample = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            magnified.setShader(sample);
            captured = true;
            android.util.Log.i("WenGlassProbe", "SNAPSHOT width=" + source.getWidth()
                    + " height=" + source.getHeight()
                    + " overlayWidth=" + getWidth());
            move(pointerX);
            if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
            requestPaint();
        } catch (RuntimeException | OutOfMemoryError error) {
            android.util.Log.e("WenGlassProbe", "SNAPSHOT_ERROR", error);
            discard(); // Fallback is the existing glass pill, never a crash.
        }
    }

    public void move(float pointerX) {
        if (!captured) return;
        float radius = Math.min(getHeight() * .52f, getWidth() * .145f);
        centreX = Math.max(radius, Math.min(getWidth() - radius, pointerX));
        requestPaint();
    }

    /** Driven by the same finite press animator that morphs the rear pill. */
    public void progress(float next) {
        if (!captured) return;
        opacity = Math.max(0f, Math.min(1f, next));
        // CRITICAL: ValueAnimator always emits its initial value (normally 0).
        // Discarding the bitmap on that first tick permanently killed the
        // optical lens before the user saw any magnification.
        // A zero alpha hides the overlay; the captured bitmap is retained
        // until the next begin() or detach() instead.
        final int desired = opacity <= .005f ? INVISIBLE : VISIBLE;
        if (getVisibility() != desired) setVisibility(desired);
        requestPaint();
    }

    public void discard() {
        captured = false;
        opacity = 0f;
        sample = null;
        magnified.setShader(null);
        // No immediate Bitmap.recycle(): hardware canvas drawing can still
        // be in flight on RenderThread after the view becomes invisible.
        source = null;
        if (getVisibility() != INVISIBLE) setVisibility(INVISIBLE);
        requestPaint();
    }

    @Override protected void onDetachedFromWindow() {
        discard();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!captured || sample == null || opacity <= .005f) return;
        if (!reportedDraw && opacity >= .6f) {
            reportedDraw = true;
            android.util.Log.i("WenGlassProbe", "DRAW_ACTIVE magnification=" + (1f + .76f * opacity)
                    + " width=" + getWidth());
        }
        final float cy = getHeight() * .5f;
        final float rx = getHeight() * (.42f + .15f * opacity);
        final float ry = getHeight() * (.40f + .11f * opacity);
        droplet.set(centreX - rx, cy - ry, centreX + rx, cy + ry);

        // Hide the unmagnified glyphs beneath this locally opaque optical
        // surface, then resample their original pixels at 1.3x into the lens.
        body.setAlpha((int)(245f * opacity));
        canvas.drawRoundRect(droplet, ry, ry, body);
        // Optical magnification, not a slight icon-scale animation.
        float zoom = 1f + .76f * opacity;
        sampleMatrix.reset();
        sampleMatrix.setScale(zoom, zoom, centreX, cy);
        sample.setLocalMatrix(sampleMatrix);
        magnified.setAlpha((int)(255f * opacity));
        canvas.drawRoundRect(droplet, ry, ry, magnified);

        glow.setAlpha((int)(105f * opacity));
        canvas.drawRoundRect(droplet, ry, ry, glow);
        rim.setAlpha((int)(242f * opacity));
        canvas.drawRoundRect(droplet, ry, ry, rim);
        droplet.inset(density * 2.5f, density * 2.5f);
        innerRim.setAlpha((int)(162f * opacity));
        canvas.drawRoundRect(droplet, ry - density * 2f, ry - density * 2f, innerRim);
    }
}
