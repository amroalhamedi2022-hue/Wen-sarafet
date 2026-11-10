package app.wensarafet.tracker;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import app.wensarafet.glass.WenNavigationTrack;
import com.example.liquidglass.LiquidGlassView;

/**
 * Purely visual, opt-in navigation lens adapter on protected 1.8.8.
 * No account, ledger, storage, security, activation, FAB or background action.
 * The optical implementation is MIT-licensed:
 * https://github.com/QWEA0/Liquid-Glass-Android/tree/v2.0.11
 *
 * Preserve the original tab button hit targets/callbacks. The material's
 * View only paints; it never consumes the gesture or handles finance actions.
 */
public final class NavigationLiquidMaterial {
    private NavigationLiquidMaterial() { }

    public static boolean attach(FrameLayout lens, WenNavigationTrack track) {
        if (lens == null || track == null) return false;
        try {
            final LiquidGlassView glass = new LiquidGlassView(lens.getContext());
            glass.setClickable(false);
            glass.setFocusable(false);
            glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            // The native AGSL renderer uses PX. Convert material thickness
            // from dp so the same lens survives 1x emulator and 3x phones.
            final float d = lens.getResources().getDisplayMetrics().density;
            final boolean premiumLiquid = app.wensarafet.glass.WenPremiumLiquidDrawable
                    .supports(Ui.themeIndex(lens.getContext()));
            final android.os.PowerManager powerManager =
                    (android.os.PowerManager) lens.getContext()
                            .getSystemService(android.content.Context.POWER_SERVICE);
            final boolean powerSave = powerManager != null && powerManager.isPowerSaveMode();
            // The old "liquid" looked like coloured plastic: every optical
            // feature was deliberately disabled, while the sampled source
            // contained no page detail. Opt into the library's REAL pipeline
            // only for the five owner-approved premiums.
            glass.setUseShaderPipeline(true);
            glass.setEnableDynamicBackground(premiumLiquid && !powerSave);
            glass.setEnablePressEffect(false); // keep the original tab's gesture owner
            glass.setEnableBackdropBlur(premiumLiquid && !powerSave);
            glass.setBlurAmount(premiumLiquid && !powerSave ? .045f : 0f);
            glass.setEnableChromaticAberration(premiumLiquid && !powerSave);
            glass.setEnableShadow(false);
            glass.setEnableEdgeHighlight(true);
            glass.setCornerRadius(999f);
            glass.setBevelWidth((premiumLiquid ? 13.0f : 7.5f) * d);
            glass.setRefractionFalloff(premiumLiquid ? 2.15f : 1.4f);
            glass.setDispersionStrength(premiumLiquid && !powerSave ? .016f : 0f);
            glass.setDisplacementScale(premiumLiquid ? 24f : 14f);
            glass.setRefractionHeight((premiumLiquid ? 24f : 6f) * d);
            glass.setEdgeSoftness((premiumLiquid ? 2.6f : 1.8f) * d);
            glass.setAberrationIntensity(premiumLiquid && !powerSave ? .18f : 0f);
            glass.setSaturation(premiumLiquid ? 115f : 100f);
            final int opticalTint = premiumLiquid
                    ? WenGoldGlassSurface.navigationLensTint(Ui.themeIndex(lens.getContext()))
                    : Color.WHITE;
            glass.setGlassTint(opticalTint, premiumLiquid ? .018f : .045f);
            glass.setEdgeHighlightBorderWidth(.75f * d);
            glass.setEdgeHighlightOpacity(premiumLiquid ? 45f : 27f);
            lens.setClipChildren(false);
            lens.setClipToPadding(false);
            lens.addView(glass, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            final OpticalSheen sheen = new OpticalSheen(lens.getContext(), false);
            final OpticalCrown crown = new OpticalCrown(d, false);
            lens.addView(sheen, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            // The real tab touch owner is in GlassMotion. Subscribe the optical
            // material to its SAME finite morph animator, without adding a
            // competing click listener or an idle redraw timer.
            track.setGlassGestureBridge(new WenNavigationTrack.GlassGestureBridge() {
                private boolean reported;
                private float pointerLast = Float.NaN;
                private float dragAnchor = Float.NaN;
                private float dragDirection;
                private float dragEnergy;
                private float amount;
                private boolean strainReported;
                // Power-saving and disabled system animations never get a
                // high-intensity deformation, even on a rapid finger drag.
                private final android.os.PowerManager power =
                        (android.os.PowerManager) lens.getContext().getSystemService(
                                android.content.Context.POWER_SERVICE);
                private boolean motionReduced() {
                    return !ValueAnimator.areAnimatorsEnabled()
                            || (power != null && power.isPowerSaveMode());
                }
                private void applyLiquidMaterial() {
                    final float t = amount;
                    final float strain = motionReduced() ? 0f : dragEnergy * t;
                    if (strain >= .18f && !strainReported) {
                        strainReported = true;
                        android.util.Log.i("WenGlassProbe", "NAV_LIQUID_STRETCH_ACTIVE");
                    }
                    // Elastic capsule: follows the finger like a cohesive
                    // substance; never an independent halo or second bubble.
                    glass.setBevelWidth(((premiumLiquid ? 13f : 7.5f)
                            + 2.1f * t + 2.2f * strain) * d);
                    glass.setRefractionHeight(((premiumLiquid ? 24f : 6f)
                            + 15f * t + 7f * strain) * d);
                    glass.setDisplacementScale((premiumLiquid ? 24f : 14f)
                            + 32f * t + 13f * strain);
                    glass.setGlassTint(opticalTint,
                            (premiumLiquid ? .018f : .045f) + .017f * t);
                    glass.setScaleX(1f + .028f * t + .143f * strain);
                    glass.setScaleY(1f + .012f * t - .065f * strain);
                    sheen.setMotion(t, dragDirection * strain);
                    glass.invalidate();
                    track.postInvalidateOnAnimation();
                }
                @Override public void morph(float fraction) {
                    float t = Math.max(0f, Math.min(1f, fraction));
                    amount = t;
                    // The finite press animator drives both frosted refraction
                    // and release-time elastic recovery, without extra timers.
                    applyLiquidMaterial();
                    if (t >= .8f && !reported) {
                        reported = true;
                        android.util.Log.i("WenGlassProbe",
                                "MIT_VIEW_GLASS_PRESS_ACTIVE");
                    }
                    if (t <= .01f) reported = false;
                }
                @Override public void follow(float pointerX) {
                    if (Float.isNaN(dragAnchor)) dragAnchor = pointerX;
                    if (!Float.isNaN(pointerLast)) {
                        float delta = pointerX - pointerLast;
                        float pull = pointerX - dragAnchor;
                        dragDirection = Math.signum(pull);
                        // Displacement supplies the lasting tug; per-event
                        // velocity only adds a brief responsive stretch.
                        float travelled = Math.min(1f,
                                Math.abs(pull) / Math.max(1f, track.getWidth() * .55f));
                        float impulse = Math.min(1f, Math.abs(delta) / (25f * d));
                        float newEnergy = Math.min(1f, travelled * .78f + impulse * .22f);
                        // Viscous memory: retain a little of the previous pull
                        // when the finger slows, without a perpetual animator.
                        dragEnergy = Math.min(1f, Math.max(newEnergy, dragEnergy * .86f));
                    }
                    pointerLast = pointerX;
                    applyLiquidMaterial();
                }
                @Override public void reset() {
                    pointerLast = Float.NaN;
                    dragAnchor = Float.NaN;
                    dragDirection = 0f;
                    dragEnergy = 0f;
                    strainReported = false;
                    morph(0f);
                }
                @Override public void drawForeground(Canvas canvas) {
                    if (lens.getWidth() <= 0 || lens.getHeight() <= 0) return;
                    int saved = canvas.save();
                    // Track-local coordinates, moves with the REAL selected lens
                    // across all four clickable tabs. This final pass is drawn
                    // AFTER their text/emoji, not under their display lists.
                    canvas.translate(lens.getX(), lens.getY());
                    crown.paint(canvas, lens.getWidth(), lens.getHeight(),
                            amount, dragDirection * dragEnergy);
                    canvas.restoreToCount(saved);
                }
            });

            // BackdropSource can be an ancestor in this library. It excludes
            // the glass branch while recording, so it does not capture itself.
            track.post(() -> {
                if (!track.isAttachedToWindow()) return;
                if (!(track.getParent() instanceof View)) return;
                View nav = (View) track.getParent();
                if (!(nav.getParent() instanceof ViewGroup)) return;
                ViewGroup container = (ViewGroup) nav.getParent();
                // Sample the tab glyphs INSIDE the native foreground material.
                // Sampling the scroll viewport made unrelated page text appear
                // under the lens while the actual tab glyphs stayed unwarped.
                View tabContent = track.findViewWithTag("wen_glass_tab_content");
                // The five owner-approved premiums use the REAL theme
                // background of the nav parent as their refraction source.
                // Sampling transparent text-only tabs produced an opaque grey
                // disk on Royal/Champagne/Purple instead of clear liquid glass.
                // The library explicitly excludes its glass branch when the
                // backdrop source is an ancestor; actions are unaffected.
                boolean premiumScene = app.wensarafet.glass.WenPremiumLiquidDrawable
                        .supports(Ui.themeIndex(lens.getContext()));
                if (premiumScene) {
                    // Refraction MUST sample real screen detail behind the
                    // floating bar, not the bar's own nearly-flat material.
                    // Text and tab icons stay in the foreground, so their
                    // readability is not sacrificed to the background blur.
                    View scene = findUnderlay(container);
                    View goldBackdrop = nav.getRootView().findViewWithTag(
                            WenGoldGlassSurface.BACKDROP_TAG);
                    // The real gold backdrop is a live View with its own
                    // painted scene. Using the scroll viewport had refracted
                    // bank logos and balance text behind navigation captions,
                    // producing the overlap seen in actual emulator shots.
                    // Keep that previous fallback for other Liquid themes.
                    View source = premiumScene && goldBackdrop != null
                            && goldBackdrop.isAttachedToWindow() ? goldBackdrop : scene;
                    glass.setBackdropSource(source != null ? source : nav);
                    android.util.Log.i("WenGlassProbe",
                            "MIT_VIEW_GLASS_BACKDROP_BOUND source="
                                    + (source == goldBackdrop && source != null
                                    ? "gold-scene" : scene != null
                                    ? "real-page-underlay" : "nav-fallback"));
                } else if (tabContent != null && tabContent.getWidth() > 0) {
                    glass.setBackdropSource(tabContent);
                    android.util.Log.i("WenGlassProbe",
                            "MIT_VIEW_GLASS_BACKDROP_BOUND source=tabs");
                } else {
                    // Defensive fallback for an unusual mounting order only.
                    View source = findUnderlay(container);
                    glass.setBackdropSource(source != null ? source : container);
                    android.util.Log.w("WenGlassProbe",
                            "MIT_VIEW_GLASS_BACKDROP_BOUND source=fallback");
                }
            });
            android.util.Log.i("WenGlassProbe", "MIT_VIEW_GLASS_ATTACHED");
            return true;
        } catch (RuntimeException | LinkageError problem) {
            android.util.Log.e("WenGlassProbe", "MIT_VIEW_GLASS_FAILED", problem);
            lens.removeAllViews();
            return false;
        }
    }


    /**
     * Find a scroll viewport under the floating chrome, NOT the nav/FAB
     * branch itself. The third-party library accepts any backdrop View in
     * screen coordinates, so there is no reason to sample selected glyphs.
     * Resolves once per screen mount, with no per-frame tree traversal.
     */
    private static View findUnderlay(View view) {
        if (view instanceof ScrollView) return view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i=0; i<group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof WenNavigationTrack
                    || child instanceof LiquidGlassView) continue;
            View source = findUnderlay(child);
            if (source != null) return source;
        }
        return null;
    }


    // Navigation-only optics reused from the isolated tested sandbox;
    // no experimental startup or protected app behavior crosses this boundary.
    private static final class OpticalSheen extends View {
        private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint perimeter = new Paint(Paint.ANTI_ALIAS_FLAG);
        // The backing image is real. A softly frosted CENTRE must protect tab
        // labels from giant bank logos in the scrolling underlay; keep the
        // refracting bevel comparatively clear and visually alive.
        private final Paint frost = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix translation = new Matrix();
        private final RectF bounds = new RectF();
        private final Path clipShape = new Path();
        private final float d;
        private final boolean circle;
        private RadialGradient shader;
        private float pressure;
        private float direction;
        OpticalSheen(android.content.Context context, boolean circle) {
            super(context);
            this.d = context.getResources().getDisplayMetrics().density;
            this.circle = circle;
            setWillNotDraw(false);
            setClickable(false);
            setFocusable(false);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            perimeter.setStyle(Paint.Style.STROKE);
            perimeter.setStrokeWidth(.65f * d);
            perimeter.setColor(0x38FFFFFF);
        }
        void setMotion(float press, float velocity) {
            pressure = Math.max(0f, Math.min(1f, press));
            direction = Math.max(-1f, Math.min(1f, velocity));
            invalidate();
        }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            float radius = Math.max(1f, Math.max(w, h) * .58f);
            shader = new RadialGradient(0, 0, radius,
                    new int[]{0x74FFFFFF, 0x32FFFFFF, 0x0AFFFFFF, 0},
                    new float[]{0f, .17f, .46f, 1f}, Shader.TileMode.CLAMP);
            glint.setShader(shader);
            if (circle) {
                frost.setShader(new RadialGradient(w*.5f, h*.5f,
                        Math.max(1f, Math.min(w,h)*.51f),
                        new int[]{0x48FFFFFF,0x33FFFFFF,0x13FFFFFF},
                        new float[]{0f,.62f,1f}, Shader.TileMode.CLAMP));
            } else {
                frost.setShader(new LinearGradient(0f,0f,0f,h,
                        new int[]{0x0CFFFFFF,0x19FFFFFF,0x14FFFFFF,0x08FFFFFF},
                        new float[]{0f,.27f,.68f,1f}, Shader.TileMode.CLAMP));
            }
            float margin = d * .85f;
            bounds.set(margin, margin, w - margin, h - margin);
            float rounded = circle ? Math.min(w,h)*.49f : h*.49f;
            clipShape.reset();
            if (circle) {
                clipShape.addCircle(w*.5f, h*.5f, Math.min(w,h)*.5f,
                        Path.Direction.CW);
            } else {
                clipShape.addRoundRect(bounds, rounded, rounded, Path.Direction.CW);
            }
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w=getWidth(), h=getHeight();
            if (w <= 0 || h <= 0 || shader == null) return;
            float margin = d * .85f;
            bounds.set(margin, margin, w - margin, h - margin);
            float radius = circle ? Math.min(w, h) * .49f : h * .49f;
            int save=canvas.save();
            canvas.clipPath(clipShape);
            // Frost is drawn ABOVE the real refractive optical image,
            // deliberately strongest in the centre and lighter on the bevel.
            // The centre can be read while the optical edge still bends light.
            frost.setAlpha((int)(255f - 88f * pressure));
            if (circle) canvas.drawCircle(w*.5f,h*.5f,
                    Math.min(w,h)*.5f - margin, frost);
            else canvas.drawRoundRect(bounds,radius,radius,frost);
            translation.reset();
            translation.setTranslate(w * (.25f + .24f * direction), h * (.03f + .06f * pressure));
            shader.setLocalMatrix(translation);
            glint.setAlpha((int)(54f + 87f * pressure));
            canvas.clipRect(0f, 0f, w, h * .42f);
            canvas.drawRoundRect(bounds, radius, radius, glint);
            canvas.restoreToCount(save);
            perimeter.setAlpha((int)(29f + 30f * pressure));
            if (circle) canvas.drawCircle(w*.5f, h*.5f, Math.min(w,h)*.5f-margin, perimeter);
            else canvas.drawRoundRect(bounds, radius, radius, perimeter);
        }
    }

    /**
     * The OVER-GLYPH optical surface. The native LiquidGlassView retains
     * real refraction and backdrop sampling behind labels. This tiny final
     * caustic/rim is composited above the actual text in the same frame.
     * Paints and shaders are reused; no idle timers, no touch interception.
     */
    private static final class OpticalCrown {
        private final float d;
        private final boolean circular;
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint spectrum = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final Matrix shift = new Matrix();
        private LinearGradient rimShader;
        private LinearGradient spectrumShader;
        private LinearGradient glintShader;
        private int cachedWidth = -1, cachedHeight = -1;

        OpticalCrown(float density, boolean circular) {
            this.d = density;
            this.circular = circular;
            rim.setStyle(Paint.Style.STROKE);
            spectrum.setStyle(Paint.Style.STROKE);
        }

        void paint(Canvas canvas, int width, int height, float press, float direction) {
            if (width <= 0 || height <= 0) return;
            if (cachedWidth != width || cachedHeight != height) {
                cachedWidth = width;
                cachedHeight = height;
                rimShader = new LinearGradient(0f, 0f, width, height,
                        new int[]{0xCEFFFFFF, 0x4DFFFFFF, 0xB4FFFFFF, 0x74FFFFFF},
                        new float[]{0f, .35f, .72f, 1f}, Shader.TileMode.CLAMP);
                spectrumShader = new LinearGradient(0f, 0f, width, 0f,
                        new int[]{0x00FFFFFF, 0x65FFFFFF, 0x55FFFFFF,
                                0x7AFFFFFF, 0x45FFFFFF, 0x00FFFFFF},
                        new float[]{0f, .17f, .36f, .53f, .77f, 1f},
                        Shader.TileMode.CLAMP);
                glintShader = new LinearGradient(0f, 0f, width, height * .55f,
                        new int[]{0x00FFFFFF, 0x99FFFFFF, 0x62FFFFFF,
                                0x00FFFFFF},
                        new float[]{0f, .29f, .64f, 1f}, Shader.TileMode.CLAMP);
                rim.setShader(rimShader);
                spectrum.setShader(spectrumShader);
                glint.setShader(glintShader);
            }
            float t = Math.max(0f, Math.min(1f, press));
            float inset = .9f * d;
            float radius = Math.min(width, height) * .5f - inset;
            bounds.set(inset, inset, width - inset, height - inset);
            if (radius <= 0f) return;

            // Subtle neutral glass perimeter at rest and on Hold;
            // no RGB-fringed edge or coloured halo.
            rim.setStrokeWidth((.8f + .75f * t) * d);
            rim.setAlpha((int)(102f + 77f * t));
            canvas.drawRoundRect(bounds, radius, radius, rim);
            spectrum.setStrokeWidth((.55f + .45f * t) * d);
            spectrum.setAlpha((int)(14f + 60f * t));
            shift.reset();
            shift.setTranslate(width * (.10f * direction + .08f * t), 0f);
            spectrumShader.setLocalMatrix(shift);
            canvas.drawRoundRect(bounds, radius, radius, spectrum);

            // Translucent caustic above the icons, not a second copy of text.
            int save = canvas.save();
            canvas.clipRect(0f, 0f, width, height * (.32f + .11f * t));
            glint.setStyle(Paint.Style.FILL);
            glint.setAlpha((int)(26f + 115f * t));
            shift.reset();
            shift.setTranslate(width * (.18f * direction + .05f * t), 0f);
            glintShader.setLocalMatrix(shift);
            canvas.drawRoundRect(bounds, radius, radius, glint);
            canvas.restoreToCount(save);
        }
    }
}
