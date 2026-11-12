package app.wensarafet.tracker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

/**
 * Very small, bounded, in-memory backdrop sampler for the + button only.
 * The 4dp blur is applied to the sampled 48-82dp image by RenderEffect on
 * its ImageView; it DOES NOT blur the FAB's + glyph or its uniform glass tint.
 *
 * No View root snapshot, no storage, no network, no financial callbacks.
 * Refresh after scroll settles, reusing one bitmap to avoid extra work mid-gesture.
 */
final class WenFabBackdropBlur {
    private final FrameLayout root;
    private final ScrollView scroll;
    private final View fab;
    private final ImageView frosted;
    private Bitmap sample;
    private Canvas sampleCanvas;
    private final Runnable idleRefresh = this::refresh;

    WenFabBackdropBlur(FrameLayout root, ScrollView scroll, View fab, ImageView frosted) {
        this.root = root;
        this.scroll = scroll;
        this.fab = fab;
        this.frosted = frosted;
    }

    void refreshIfDue() {
        // No screenshot work in the active 60/90/120Hz scroll path.
        // The latest scroll position wins when gestures settle.
        frosted.removeCallbacks(idleRefresh);
        frosted.postDelayed(idleRefresh, 130L);
    }

    void refresh() {
        if (!fab.isAttachedToWindow() || fab.getWidth() <= 0 ||
                fab.getHeight() <= 0 || !scroll.isAttachedToWindow()) return;
        final int w=fab.getWidth(), h=fab.getHeight();
        if (sample == null || sample.getWidth()!=w || sample.getHeight()!=h) {
            if (sample!=null) sample.recycle();
            sample=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            sampleCanvas=new Canvas(sample);
            frosted.setImageBitmap(sample);
        }
        sample.eraseColor(Color.TRANSPARENT);
        Canvas c=sampleCanvas;
        int saved=c.save();
        // Coordinates are in root-local space. Root and the existing ScrollView
        // are drawn without any FAB or bottom-navigation overlays.
        c.translate(-fab.getLeft(), -fab.getTop());
        Drawable rootBackground=root.getBackground();
        if(rootBackground!=null) rootBackground.draw(c);
        View premiumBackdrop=root.findViewWithTag(WenGoldGlassSurface.BACKDROP_TAG);
        if(premiumBackdrop!=null && premiumBackdrop!=fab &&
                premiumBackdrop.getWidth()>0 && premiumBackdrop.getHeight()>0) {
            int saveBackdrop=c.save();
            c.translate(premiumBackdrop.getLeft(),premiumBackdrop.getTop());
            premiumBackdrop.draw(c);
            c.restoreToCount(saveBackdrop);
        }
        // Golden and other liquid scenes already contain heavyweight layered
        // optics. Never re-draw their whole ScrollView into a software bitmap;
        // the tiny frosted FAB can sample the existing static backdrop instead.
        // This also prevents extra per-gesture work on 120Hz gold themes.
        if (!Ui.isLiquidTheme(root.getContext())) {
            int sub=c.save();
            c.translate(scroll.getLeft(), scroll.getTop());
            try {
                scroll.draw(c);
            } catch (RuntimeException unsupportedCanvas) {
                // Cosmetic blur must never crash finance UI on unusual devices.
            } finally {
                c.restoreToCount(sub);
            }
        }
        c.restoreToCount(saved);
        frosted.invalidate();
    }
}
