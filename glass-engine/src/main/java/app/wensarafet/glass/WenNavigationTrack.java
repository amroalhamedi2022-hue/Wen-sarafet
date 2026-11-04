package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

/**
 * Single optical foreground on the native nav-strip drawing surface.
 * FrameLayout draws its foreground AFTER its children, and owns its
 * Drawable.Callback. This avoids unreliable detached View rendering or
 * a custom dispatchDraw pass on some Android compositors.
 */
public final class WenNavigationTrack extends FrameLayout {
    private final WenOpticalNavigationOverlay optic;
    private final Drawable opticalForeground;
    private boolean externalGlassActive;
    private boolean opticalCrownDrawConfirmed;
    /** Gesture-only contract. The glass-engine has NO dependency on sandbox SDK. */
    public interface GlassGestureBridge {
        void morph(float fraction);
        void follow(float pointerX);
        void reset();
        /** The glass caustic is painted AFTER glyphs in the SAME track canvas. */
        void drawForeground(Canvas canvas);
    }
    private GlassGestureBridge externalGlassBridge;

    public void setGlassGestureBridge(GlassGestureBridge bridge) {
        externalGlassBridge = bridge;
    }

    public GlassGestureBridge getGlassGestureBridge() {
        return externalGlassActive ? externalGlassBridge : null;
    }

    public WenNavigationTrack(Context context, boolean dark) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        optic = new WenOpticalNavigationOverlay(context, dark);
        optic.setHostTrack(this);
        opticalForeground = new Drawable() {
            @Override public void draw(Canvas canvas) { if (optic.getParent() == null) optic.renderOnto(canvas); }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter filter) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
        // This foreground is drawn over the four tab labels by Framework,
        // rather than competing with their z-order or cached display lists.
        setForeground(opticalForeground);
        setForegroundGravity(android.view.Gravity.FILL);
        // A real attached View has a hardware display list and repaints reliably
        // on Android devices where a custom foreground callback remains cached.
        // MainActivity brings this passive layer above its tab labels.
        addView(optic, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        // Own the final composite on THIS real parent display list. On several
        // devices a transparent child draw is cached even after invalidation.
        // The child still supplies size/capture/lifecycle, but only the track
        // paints its refraction over the actual tab glyphs.
        optic.setAlpha(0f);
    }

    public WenOpticalNavigationOverlay getOptic() {
        return externalGlassActive ? null : optic;
    }

    /** Sandbox-only: a separately tested native View material owns the optics. */
    public void setExternalGlassActive(boolean active) {
        externalGlassActive = active;
        opticalCrownDrawConfirmed = false;
        optic.discard();
        invalidate();
    }

    void invalidateOptic() {
        optic.invalidate();
        opticalForeground.invalidateSelf();
        invalidate();
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        // Guaranteed post-children optical composite, same coordinates as pill.
        if (externalGlassActive) {
            if (externalGlassBridge != null) {
                externalGlassBridge.drawForeground(canvas);
                if (!opticalCrownDrawConfirmed && canvas.isHardwareAccelerated()) {
                    opticalCrownDrawConfirmed = true;
                    android.util.Log.i("WenGlassProbe", "MIT_NAV_FRONT_ABOVE_GLYPHS");
                }
            }
        } else {
            optic.renderOnto(canvas);
        }
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        // Overlay is now a child laid out by FrameLayout, not an off-tree helper.
    }

    @Override protected void onDetachedFromWindow() {
        optic.discard();
        super.onDetachedFromWindow();
    }
}
