package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.liquidglass.LiquidGlassView;

/**
 * Opt-in REAL liquid-glass material for the three GOLD themes (25, 26, 27).
 *
 * A rendered glass view is mounted BEHIND existing text / controls and samples
 * the ACTUAL gold backdrop via backdropSource. No value, callback, account
 * icon, finance query, navigation action or storage API is touched here.
 * All non-gold themes retain their exact previous Android View presentation.
 *
 * The third-party MIT AGSL pipeline performs the refraction/blur. The
 * lightweight cached upper-edge specular gives the 15%-height glass shine
 * required by the owner's references. No draw-loop allocations or clocks.
 */
final class WenGoldGlassSurface {
    static final String BACKDROP_TAG = "wen_gold_background_source";

    private WenGoldGlassSurface() {}

    // Preserve the palette-specific text colors above the neutral dock.
    static int navigationInk(int theme, boolean selected) {
        switch (theme) {
            case 25: return selected ? 0xFFFFE2A9 : 0xFFD4C6AE;
            case 26: return selected ? 0xFFFFEAC8 : 0xFFCBC7BF;
            case 27: return selected ? 0xFF5C4028 : 0xFF80694E;
            default: return selected ? Ui.TEXT : Ui.MUTED;
        }
    }

    /**
     * A single, non-interactive physical backdrop across Home, Debt and
     * Analytics. Gold cards sample this same real View; no
     * financial data, click listeners or screen navigation is modified.
     */
    static void attachGoldBackdrop(FrameLayout root) {
        if (!enabled(root.getContext())) return;
        View backdrop = new View(root.getContext());
        backdrop.setTag(BACKDROP_TAG);
        backdrop.setBackground(Ui.glassScreenBackground(root.getContext()));
        root.setBackground(null);
        root.addView(backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    static boolean enabled(Context context) {
        int theme=Ui.themeIndex(context);
        return theme >= 25 && theme <= 27;
    }

    /** Only call for passive content cards with existing measured heights. */
    static View wrap(View content, int radiusDp) {
        Context context=content.getContext();
        if (!enabled(context)) return content;

        FrameLayout host=new FrameLayout(context);
        // Preserve caller-provided LinearLayout.LayoutParams, weight and
        // margins exactly, especially the three financial statistic cards.
        ViewGroup.LayoutParams originalParams=content.getLayoutParams();
        if(originalParams!=null)host.setLayoutParams(originalParams);
        host.setClipChildren(false);
        host.setClipToPadding(false);
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        // Recover the same passive drawable under the optical glass. Removing
        // the solid card background from its TEXT child is essential: an
        // opaque content layer would conceal every refracted pixel below it.
        Drawable original=content.getBackground();
        // Preserve the old visual drawable as a subtle warm optical underlay,
        // never a thick opaque plastic surface blocking live refraction.
        // Each card owns its own Drawable instance; mutate() prevents bleed
        // into sibling tiles and old themes.
        if (original != null) {
            Drawable underlay=original.mutate();
            underlay.setAlpha(Ui.themeIndex(context)==27 ? 32 : 24);
            host.setBackground(underlay);
        }
        content.setBackground(null);
        // The old tile's rectangular elevation outline leaked beneath the
        // rounded optical glass and created a square press/shadow artifact.
        // Only the passive visual elevation changes; host still forwards
        // original accessibility and click behavior to its real content.
        content.setElevation(0f);
        // Repeated statistic tiles keep true shader refraction and edge light,
        // but avoid six simultaneous expensive live-blur/aberration passes.
        LiquidGlassView glass=buildGlass(context,radiusDp);
        host.addView(glass,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        View specular=new View(context);
        specular.setBackground(new Specular(context,Ui.themeIndex(context),radiusDp));
        specular.setClickable(false);
        specular.setFocusable(false);
        specular.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.addView(specular,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        host.addView(content,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        bindBackdrop(host,glass);
        return host;
    }

    private static LiquidGlassView buildGlass(Context context, int radiusDp) {
        LiquidGlassView glass=new LiquidGlassView(context);
        glass.setClickable(false);
        glass.setFocusable(false);
        glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final float density=context.getResources().getDisplayMetrics().density;
        final int theme=Ui.themeIndex(context);
        // Native SDF lens is a single GPU pass on Android 13+. Do not insert
        // bitmap snapshots, per-frame shader allocations or animation timers.
        glass.setUseShaderPipeline(true);
        glass.setEnableDynamicBackground(false);
        glass.setEnableBackdropBlur(false);
        glass.setBlurAmount(0f);
        glass.setEnableChromaticAberration(false);
        glass.setEnablePressEffect(false);
        glass.setEnableShadow(false);
        glass.setEnableEdgeHighlight(true);
        glass.setCornerRadius(radiusDp*density);
        glass.setBevelWidth((theme==27 ? 12.0f : 14.0f)*density);
        glass.setRefractionHeight((theme==27 ? 28f : 35f)*density);
        glass.setRefractionFalloff(2.35f);
        glass.setDispersionStrength(0f);
        glass.setDisplacementScale(35f);
        glass.setEdgeSoftness(2.0f*density);
        glass.setAberrationIntensity(0f);
        glass.setSaturation(111f);
        glass.setGlassTint(theme==27?0xFFFFEAD1:0xFFFFD6A0,
                theme==27?.018f:.020f);
        glass.setEdgeHighlightBorderWidth(.8f*density);
        glass.setEdgeHighlightOpacity(theme==27?55f:47f);
        return glass;
    }

    private static void bindBackdrop(View host,LiquidGlassView glass) {
        host.post(() -> {
            if (!host.isAttachedToWindow()) return;
            View target=host.getRootView().findViewWithTag(BACKDROP_TAG);
            if (target!=null && target.isAttachedToWindow() && target!=glass) {
                glass.setBackdropSource(target);
            } else {
                // A screen mounted outside the home has no tagged backdrop.
                // Sample the decorated parent as the existing lens does.
                android.util.Log.w("WenGoldGlass","Backdrop not mounted; fallback surface");
            }
        });
    }

    /** Paint only top 15% + one perimeter stroke; cached until bounds change. */
    private static final class Specular extends Drawable {
        private final Paint light=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds=new RectF();
        private final RectF rimBounds=new RectF();
        private final Path clip=new Path();
        private final int theme;
        private final float radius;
        private int opacity=255;

        Specular(Context context,int index,int radiusDp) {
            theme=index;
            radius=radiusDp*context.getResources().getDisplayMetrics().density;
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Math.max(.8f,
                    context.getResources().getDisplayMetrics().density*.85f));
        }

        @Override protected void onBoundsChange(Rect b) {
            bounds.set(b);
            if (b.isEmpty()) return;
            clip.reset();
            clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);
            int white=theme==27?0xA9FFFFFF:0xB1FFF3DF;
            light.setShader(new LinearGradient(b.left,b.top,b.left,
                    b.top+b.height()*.15f,
                    new int[]{white,theme==27?0x50FFFFFF:0x50FFE1AB,0},
                    new float[]{0f,.40f,1f},Shader.TileMode.CLAMP));
            float inset=edge.getStrokeWidth()*.6f;
            rimBounds.set(bounds.left+inset,bounds.top+inset,
                    bounds.right-inset,bounds.bottom-inset);
            edge.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,
                    new int[]{theme==27?0xB0FFFFFF:0x66FFE3B1,0xC7FFFFFF,
                            theme==27?0x80E4BC82:0x7AF9B55C,0xB9FFFFFF},
                    new float[]{0f,.25f,.72f,1f},Shader.TileMode.CLAMP));
        }

        @Override public void draw(Canvas canvas) {
            if(bounds.isEmpty())return;
            int save=canvas.save();
            canvas.clipPath(clip);
            canvas.drawRect(bounds.left,bounds.top,bounds.right,
                    bounds.top+bounds.height()*.15f,light);
            canvas.restoreToCount(save);
            canvas.drawRoundRect(rimBounds,radius,radius,edge);
        }
        @Override public void setAlpha(int alpha) {
            opacity=alpha;
            light.setAlpha(opacity);edge.setAlpha(opacity);invalidateSelf();
        }
        @Override public void setColorFilter(ColorFilter filter) {
            light.setColorFilter(filter);edge.setColorFilter(filter);invalidateSelf();
        }
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
