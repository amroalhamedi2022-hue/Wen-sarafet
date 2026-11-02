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
    private final Drawable opticalForeground = new Drawable() {
        @Override public void draw(Canvas canvas) { optic.renderOnto(canvas); }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    };

    public WenNavigationTrack(Context context, boolean dark) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        optic = new WenOpticalNavigationOverlay(context, dark);
        optic.setHostTrack(this);
        // This foreground is drawn over the four tab labels by Framework,
        // rather than competing with their z-order or cached display lists.
        setForeground(opticalForeground);
        setForegroundGravity(android.view.Gravity.FILL);
    }

    public WenOpticalNavigationOverlay getOptic() { return optic; }

    void invalidateOptic() {
        opticalForeground.invalidateSelf();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        optic.layout(0, 0, width, height);
    }

    @Override protected void onDetachedFromWindow() {
        optic.discard();
        super.onDetachedFromWindow();
    }
}
