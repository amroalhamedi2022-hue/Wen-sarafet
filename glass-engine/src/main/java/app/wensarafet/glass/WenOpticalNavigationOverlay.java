package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.LinearGradient;
import android.graphics.PixelFormat;
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
    // Peripheral refraction is optically stronger than centre magnification.
    private final Paint refractedEdge = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint reflectedLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spectrum = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caustic = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF highlightBounds = new RectF();
    private final Matrix sampleMatrix = new Matrix();
    private final Matrix edgeMatrix = new Matrix();
    private final Matrix spectrumMatrix = new Matrix();
    private final RectF droplet = new RectF();
    private WenNavigationTrack hostTrack;
    private View physicalPill;
    private Bitmap source;
    private BitmapShader sample;
    private BitmapShader edgeSample;
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
        // An attached, non-touchable View is composited above the tab glyphs
        // and always receives invalidate() from the UI renderer.
        // Almost transparent coating: never cover the real underlying tab glyph.
        body.setColor(dark ? 0x2F8C91B0 : 0x27FFFFFF);
        refractedEdge.setStyle(Paint.Style.STROKE);
        refractedEdge.setStrokeWidth(density * 6.5f);
        reflectedLight.setStyle(Paint.Style.STROKE);
        reflectedLight.setStrokeWidth(density * 2.4f);
        shadowLine.setStyle(Paint.Style.STROKE);
        shadowLine.setStrokeWidth(density * 1.1f);
        shadowLine.setColor(dark ? 0x8C22253C : 0x5B34456C);
        spectrum.setStyle(Paint.Style.STROKE);
        spectrum.setStrokeWidth(density * 1.85f);
        caustic.setStyle(Paint.Style.STROKE);
        caustic.setStrokeWidth(density * 3.1f);
        caustic.setColor(dark ? 0xA0ADDFFF : 0x9EFFFFFF);
        specular.setColor(0xDFFFFFFF);
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

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // One prismatic, position-stable highlight shader per resize, never per frame.
        spectrum.setShader(new LinearGradient(0f, 0f, Math.max(1, w), 0f,
                new int[]{0x99AEF2FF, 0xFF8BA2FF, 0xFFDCC8FF,
                          0xFFFFECAD, 0xFF91EAF4, 0xFF9A9FFC},
                new float[]{0f, .19f, .39f, .58f, .78f, 1f},
                Shader.TileMode.CLAMP));
        specular.setShader(new LinearGradient(0f, 0f, 0f, Math.max(1, h),
                0xECFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        reflectedLight.setShader(new LinearGradient(0f, 0f,
                Math.max(1, w), Math.max(1, h),
                new int[]{0x22FFFFFF, 0xCCFFFFFF, 0x2CFFFFFF, 0xC4E8FCFF,
                        0x28FFFFFF, 0xA0FFFFFF}, null, Shader.TileMode.CLAMP));
    }

    void setHostTrack(WenNavigationTrack track) { hostTrack = track; }
    /** Same bounds as the already rendered selected-tab pill; never a second bubble. */
    public void bindPhysicalPill(View pill) { physicalPill = pill; }

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
            edgeSample = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            refractedEdge.setShader(edgeSample);
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
        // The foreground clip is anchored to the existing pill, not the finger.
        // Drag moves the real pill: the optical treatment follows it exactly.
        centreX = pointerX;
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
        edgeSample = null;
        magnified.setShader(null);
        refractedEdge.setShader(null);
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
        if (physicalPill == null || physicalPill.getWidth() < 1
                || physicalPill.getHeight() < 1) return;
        // This is ONE bubble: clip the sampled navigation text to the exact
        // painted bounds of the selected-tab pill. No separately coloured halo,
        // independent circle, second outline, or detached fingertip droplet.
        final float left = physicalPill.getX();
        final float top = physicalPill.getY();
        final float width = physicalPill.getWidth() * physicalPill.getScaleX();
        final float height = physicalPill.getHeight() * physicalPill.getScaleY();
        final float inset = density * 3f;
        droplet.set(left+inset, top+inset, left+width-inset, top+height-inset);
        if (droplet.isEmpty()) return;
        final float cx = droplet.centerX();
        final float cy = droplet.centerY();
        final float radius = droplet.height() * .49f;
        // Two-power optical sampling: the actual pixels of the captured
        // tab strip bend more at the periphery than in the calm lens centre.
        float strength = opacity;
        float zoom = 1f + .20f * strength;
        float bend = 1f + .42f * strength;
        float pointerOffset = Math.max(-droplet.width() * .5f,
                Math.min(droplet.width() * .5f, centreX - cx));
        float prismShift = pointerOffset * .058f * strength;
        sampleMatrix.reset();
        sampleMatrix.setScale(zoom, zoom, cx, cy);
        sampleMatrix.postTranslate(prismShift, -density * .50f * strength);
        sample.setLocalMatrix(sampleMatrix);

        canvas.save();
        canvas.clipRect(droplet);
        body.setAlpha((int) (42f * strength));
        canvas.drawRoundRect(droplet, radius, radius, body);
        magnified.setAlpha((int) (242f * strength));
        canvas.drawRoundRect(droplet, radius, radius, magnified);

        edgeMatrix.reset();
        edgeMatrix.setScale(bend, bend, cx, cy);
        edgeMatrix.postTranslate(-prismShift * 1.8f, density * 1.25f * strength);
        edgeSample.setLocalMatrix(edgeMatrix);
        refractedEdge.setAlpha((int) (224f * strength));
        highlightBounds.set(droplet);
        highlightBounds.inset(density * 3.8f, density * 3.8f);
        canvas.drawRoundRect(highlightBounds, radius, radius, refractedEdge);
        canvas.restore();

        // Asymmetric caustics: bright upper glass, shadowed underside and
        // chromatic dispersion, all INSIDE the same one-piece physical pill.
        shadowLine.setAlpha((int) (118f * strength));
        highlightBounds.set(droplet);
        highlightBounds.inset(density * 1.0f, density * 1.0f);
        canvas.drawRoundRect(highlightBounds, radius, radius, shadowLine);
        caustic.setAlpha((int) (185f * strength));
        canvas.drawRoundRect(droplet, radius, radius, caustic);
        // Keep the rainbow physically attached to the moving lens; the
        // underlying shader is created only at resize, never per touch frame.
        spectrumMatrix.reset();
        spectrumMatrix.setScale(droplet.width() / Math.max(1f, getWidth()), 1f);
        spectrumMatrix.postTranslate(droplet.left, 0f);
        if (spectrum.getShader() != null)
            spectrum.getShader().setLocalMatrix(spectrumMatrix);
        spectrum.setAlpha((int) (252f * strength));
        canvas.drawRoundRect(droplet, radius, radius, spectrum);
        rim.setAlpha((int) (224f * strength));
        canvas.drawRoundRect(droplet, radius, radius, rim);
        reflectedLight.setAlpha((int) (242f * strength));
        highlightBounds.set(droplet);
        highlightBounds.inset(density * 2.5f, density * 2.5f);
        canvas.drawRoundRect(highlightBounds, radius, radius, reflectedLight);
        innerRim.setAlpha((int) (136f * strength));
        highlightBounds.inset(density * 1.4f, density * 1.4f);
        canvas.drawRoundRect(highlightBounds, radius, radius, innerRim);

        highlightBounds.set(droplet.left + density * 9f,
                droplet.top + density * 1.5f,
                droplet.right - density * 9f,
                droplet.top + droplet.height() * .28f);
        specular.setAlpha((int) (150f * strength));
        canvas.drawOval(highlightBounds, specular);
    }
}
