package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Renders the optical refraction in the SAME canvas pass as the four tab
 * labels, after all children. A separate View overlay could draw while its
 * layer was omitted from the final Surface on some accelerated renderers.
 */
public final class WenNavigationTrack extends FrameLayout {
    private final WenOpticalNavigationOverlay optic;

    public WenNavigationTrack(Context context, boolean dark) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        // Rasterize only the ~72dp high tab strip as a single composited
        // surface. Android 15 SwiftShader has dropped separate optical passes
        // despite dispatchDraw running: this forces one final visible layer.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        optic = new WenOpticalNavigationOverlay(context, dark);
        optic.setHostTrack(this);
    }

    public WenOpticalNavigationOverlay getOptic() { return optic; }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        optic.layout(0, 0, width, height);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        // Canvas is precisely the same backbuffer that contains the tabs.
        // Never depend on a child overlay's separate hardware Z-layer.
        optic.renderOnto(canvas);
    }

    @Override protected void onDetachedFromWindow() {
        optic.discard();
        super.onDetachedFromWindow();
    }
}
