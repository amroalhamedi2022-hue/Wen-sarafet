package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
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

    /** Existing header: keep its original button/text hierarchy untouched. */
    static void enhanceHeader(FrameLayout header,int radiusDp) {
        if (!enabled(header.getContext()) || header.getChildCount()<2) return;
        // The old opaque gold header backing would otherwise cover every
        // moving pixel sampled by the library. Keep only a faint warm wash.
        View oldBacking=header.getChildAt(0);
        oldBacking.setAlpha(Ui.themeIndex(header.getContext())==27 ? .15f : .12f);
        oldBacking.setElevation(0f);
        final LiquidGlassView glass=buildGlass(header.getContext(),radiusDp);
        header.addView(glass,1,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        View specular=new View(header.getContext());
        specular.setBackground(new Specular(header.getContext(),
                Ui.themeIndex(header.getContext()),radiusDp));
        specular.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(specular,2,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        bindBackdrop(header,glass);
    }

    private static LiquidGlassView buildGlass(Context context,int radiusDp) {
        LiquidGlassView glass=new LiquidGlassView(context);
        glass.setClickable(false);
        glass.setFocusable(false);
        glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        boolean economical=power!=null && power.isPowerSaveMode();
        final float density=context.getResources().getDisplayMetrics().density;
        final int theme=Ui.themeIndex(context);
        // Native SDF lens is a single GPU pass on Android 13+. Do not insert
        // bitmap snapshots, per-frame shader allocations or animation timers.
        glass.setUseShaderPipeline(true);
        glass.setEnableDynamicBackground(!economical);
        glass.setEnableBackdropBlur(!economical);
        glass.setBlurAmount(!economical ? (theme==27 ? .082f : .068f) : 0f);
        glass.setEnableChromaticAberration(!economical);
        glass.setEnablePressEffect(false);
        glass.setEnableShadow(false);
        glass.setEnableEdgeHighlight(true);
        glass.setCornerRadius(radiusDp*density);
        glass.setBevelWidth((theme==27 ? 12.0f : 14.0f)*density);
        glass.setRefractionHeight((theme==27 ? 28f : 35f)*density);
        glass.setRefractionFalloff(2.35f);
        glass.setDispersionStrength(!economical ? .048f : 0f);
        glass.setDisplacementScale(35f);
        glass.setEdgeSoftness(2.0f*density);
        glass.setAberrationIntensity(!economical ? .38f : 0f);
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
