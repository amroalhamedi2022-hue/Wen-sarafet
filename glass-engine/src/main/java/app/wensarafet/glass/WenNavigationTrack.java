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
    }

    public WenOpticalNavigationOverlay getOptic() { return optic; }

    void invalidateOptic() {
        optic.invalidate();
        opticalForeground.invalidateSelf();
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
