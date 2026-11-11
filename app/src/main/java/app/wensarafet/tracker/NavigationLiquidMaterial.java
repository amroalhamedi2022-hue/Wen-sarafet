package app.wensarafet.tracker;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.PowerManager;
import android.view.View;
import android.widget.FrameLayout;

import app.wensarafet.glass.WenNavigationLensDrawable;
import app.wensarafet.glass.WenNavigationTrack;
import com.example.liquidglass.LiquidGlassView;

/** Live refraction behind the tab labels, with a neutral optical rim above them. */
public final class NavigationLiquidMaterial {
    private NavigationLiquidMaterial() {}

    public static boolean attach(FrameLayout lens, WenNavigationTrack track) {
        PowerManager power = lens.getContext().getSystemService(PowerManager.class);
        if (power != null && power.isPowerSaveMode()) return false;
        try {
            LiquidGlassView glass = new LiquidGlassView(lens.getContext());
            float density = lens.getResources().getDisplayMetrics().density;
            glass.setClickable(false);
            glass.setFocusable(false);
            glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            glass.setUseShaderPipeline(true);
            glass.setEnableDynamicBackground(false);
            glass.setVisibility(View.INVISIBLE);
            glass.setEnablePressEffect(false);
            glass.setEnableBackdropBlur(true);
            glass.setBlurAmount(.045f);
            glass.setEnableChromaticAberration(false);
            glass.setDispersionStrength(0f);
            glass.setAberrationIntensity(0f);
            glass.setSaturation(100f);
            glass.setEnableShadow(false);
            glass.setEnableEdgeHighlight(false);
            glass.setCornerRadius(999f);
            glass.setBevelWidth(8f * density);
            glass.setRefractionFalloff(2.15f);
            glass.setRefractionHeight(11f * density);
            glass.setDisplacementScale(16f);
            glass.setEdgeSoftness(1.6f * density);
            glass.setGlassTint(Color.WHITE, 0f);
            // The native renderer samples the page. Blend that sample over the
            // live scene so it cannot form an opaque selected tab.
            glass.setAlpha(.52f);
            FrameLayout.LayoutParams opticalBounds = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            int inset = Ui.dp(lens.getContext(), 2);
            opticalBounds.setMargins(inset, inset, inset, inset);
            lens.addView(glass, opticalBounds);

            WenNavigationLensDrawable rim = new WenNavigationLensDrawable(lens.getContext(), false, true);
            track.setGlassGestureBridge(new WenNavigationTrack.GlassGestureBridge() {
                private float press;
                private float anchor = Float.NaN;
                private float pull;

                private void render() {
                    float strain = ValueAnimator.areAnimatorsEnabled() ? pull * press : 0f;
                    glass.setBevelWidth((8f + 2f * press + 2f * strain) * density);
                    glass.setRefractionHeight((11f + 7f * press + 4f * strain) * density);
                    glass.setDisplacementScale(16f + 10f * press + 8f * strain);
                    rim.setMorph(press);
                    track.invalidateLensPosition();
                }

                @Override public void positionChanged() {
                    glass.invalidate();
                }

                @Override public void morph(float fraction) {
                    press = fraction;
                    render();
                }

                @Override public void follow(float pointerX) {
                    if (Float.isNaN(anchor)) anchor = pointerX;
                    pull = Math.min(1f, Math.abs(pointerX - anchor) / (48f * density));
                    render();
                }

                @Override public void reset() {
                    anchor = Float.NaN;
                    pull = 0f;
                    morph(0f);
                }

                @Override public void drawForeground(Canvas canvas) {
                    if (lens.getWidth() <= 0 || lens.getHeight() <= 0) return;
                    rim.setBounds(0, 0, lens.getWidth(), lens.getHeight());
                    int saved = canvas.save();
                    canvas.translate(lens.getX(), lens.getY());
                    rim.draw(canvas);
                    canvas.restoreToCount(saved);
                }
            });

            track.post(() -> {
                if (!track.isAttachedToWindow() || !(track.getParent() instanceof View)) return;
                View dock = (View) track.getParent();
                if (!(dock.getParent() instanceof View)) return;
                // A sibling source keeps the dock and glyphs out of the
                // capture entirely, including when the page is translated.
                View underlay = ((View) dock.getParent())
                        .findViewWithTag(WenScreenVisualMotion.PRIMARY_SCROLL_TAG);
                if (underlay == null) return;
                glass.setBackdropSource(underlay);
                glass.setVisibility(View.VISIBLE);
                android.util.Log.i("WenGlassProbe",
                        "MIT_VIEW_GLASS_BACKDROP_BOUND source=primary-scroll");
            });
            return true;
        } catch (RuntimeException | LinkageError unavailable) {
            lens.removeAllViews();
            track.setGlassGestureBridge(null);
            return false;
        }
    }
}
