package app.wensarafet.tracker;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import app.wensarafet.glass.WenNavigationTrack;
import com.example.liquidglass.LiquidGlassView;

/**
 * Sandbox-only optical material experiment. This class is NOT compiled into
 * debug/release production variants, and is loaded reflectively only when the
 * isolated sandbox package is active. The View library is MIT-licensed:
 * https://github.com/QWEA0/Liquid-Glass-Android/tree/v2.0.11
 *
 * Preserve the original tab button hit targets/callbacks. The material's
 * View only paints; it never consumes the gesture or handles finance actions.
 */
public final class SandboxOpticalGlass {
    private SandboxOpticalGlass() { }

    public static boolean attach(FrameLayout lens, WenNavigationTrack track) {
        if (lens == null || track == null) return false;
        try {
            final LiquidGlassView glass = new LiquidGlassView(lens.getContext());
            glass.setClickable(false);
            glass.setFocusable(false);
            glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            glass.setEnableBackdropBlur(true);
            glass.setEnableChromaticAberration(true);
            glass.setEnableShadow(false);
            glass.setEnableEdgeHighlight(true);
            glass.setCornerRadius(999f);
            glass.setDisplacementScale(38f);
            glass.setRefractionHeight(17f);
            glass.setEdgeSoftness(3.5f);
            glass.setAberrationIntensity(0.8f);
            glass.setSaturation(115f);
            glass.setGlassTint(Color.WHITE, 0.06f);
            glass.setEdgeHighlightBorderWidth(1.2f);
            glass.setEdgeHighlightOpacity(70f);
            lens.addView(glass, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            // BackdropSource can be an ancestor in this library. It excludes
            // the glass branch while recording, so it does not capture itself.
            track.post(() -> {
                if (!track.isAttachedToWindow()) return;
                if (!(track.getParent() instanceof View)) return;
                View nav = (View) track.getParent();
                if (!(nav.getParent() instanceof ViewGroup)) return;
                ViewGroup container = (ViewGroup) nav.getParent();
                glass.setBackdropSource(container);
                android.util.Log.i("WenGlassProbe",
                        "MIT_VIEW_GLASS_BACKDROP_BOUND");
            });
            android.util.Log.i("WenGlassProbe", "MIT_VIEW_GLASS_ATTACHED");
            return true;
        } catch (RuntimeException | LinkageError problem) {
            android.util.Log.e("WenGlassProbe", "MIT_VIEW_GLASS_FAILED", problem);
            lens.removeAllViews();
            return false;
        }
    }
}
