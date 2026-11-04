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
            // The native AGSL renderer uses PX. Convert material thickness
            // from dp so the same lens survives 1x emulator and 3x phones.
            final float d = lens.getResources().getDisplayMetrics().density;
            glass.setUseShaderPipeline(true);
            glass.setEnableDynamicBackground(false); // redraw only on gestures
            glass.setEnablePressEffect(false); // original tab owns touch
            glass.setEnableBackdropBlur(true);
            glass.setBlurAmount(.018f);
            glass.setEnableChromaticAberration(true);
            glass.setEnableShadow(false);
            glass.setEnableEdgeHighlight(true);
            glass.setCornerRadius(999f);
            glass.setBevelWidth(9.5f * d);
            glass.setRefractionFalloff(1.4f);
            glass.setDispersionStrength(.025f);
            glass.setDisplacementScale(35f);
            glass.setRefractionHeight(9.5f * d);
            glass.setEdgeSoftness(1.8f * d);
            glass.setAberrationIntensity(.38f);
            glass.setSaturation(106f);
            glass.setGlassTint(Color.WHITE, .045f);
            glass.setEdgeHighlightBorderWidth(.65f * d);
            glass.setEdgeHighlightOpacity(27f);
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
                private float dragDirection;
                private float amount;
                @Override public void morph(float fraction) {
                    float t = Math.max(0f, Math.min(1f, fraction));
                    amount = t;
                    track.postInvalidateOnAnimation();
                    // Dynamic lens profile. The middle remains quiet; only
                    // the bevel gains thickness, light and dispersion on hold.
                    glass.setBevelWidth((9.5f + 1.7f * t) * d);
                    glass.setRefractionHeight((9.5f + 14f * t) * d);
                    glass.setDispersionStrength(.025f + .11f * t);
                    glass.setDisplacementScale(35f + 55f * t);
                    glass.setAberrationIntensity(.38f + .85f * t);
                    glass.setGlassTint(Color.WHITE, .045f + .022f * t);
                    glass.setScaleX(1f + (.047f + .018f * Math.abs(dragDirection)) * t);
                    glass.setScaleY(1f + .027f * t);
                    sheen.setMotion(t, dragDirection);
                    glass.invalidate();
                    if (t >= .8f && !reported) {
                        reported = true;
                        android.util.Log.i("WenGlassProbe",
                                "MIT_VIEW_GLASS_PRESS_ACTIVE");
                    }
                    if (t <= .01f) reported = false;
                }
                @Override public void follow(float pointerX) {
                    if (!Float.isNaN(pointerLast)) {
                        float delta = pointerX - pointerLast;
                        dragDirection = Math.max(-1f, Math.min(1f, delta / (16f * d)));
                    }
                    pointerLast = pointerX;
                    track.postInvalidateOnAnimation();
                    sheen.setMotion(1f, dragDirection);
                    // Gesture owner repositions the lens. A fresh frame
                    // samples the content at that location, not stale glyphs.
                    glass.invalidate();
                }
                @Override public void reset() {
                    pointerLast = Float.NaN;
                    dragDirection = 0f;
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
                            amount, dragDirection);
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
                // The root contains the navigation glyphs. Capturing it makes
                // the lens draw a SECOND distorted copy of its own labels.
                // Capture only the underlying scroll viewport instead.
                View source = findUnderlay(container);
                glass.setBackdropSource(source != null ? source : container);
                android.util.Log.i("WenGlassProbe",
                        "MIT_VIEW_GLASS_BACKDROP_BOUND source="
                                + (source != null ? "scroll" : "root-fallback"));
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

    /** Native circular material underneath the EXISTING accessible + TextView. */
    public static boolean attachFab(FrameLayout host, FrameLayout root, TextView fab) {
        if (host == null || root == null || fab == null) return false;
        try {
            final float d = host.getResources().getDisplayMetrics().density;
            final LiquidGlassView glass = new LiquidGlassView(host.getContext());
            glass.setClickable(false);
            glass.setFocusable(false);
            glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            glass.setEnablePressEffect(false);
            glass.setUseShaderPipeline(true);
            glass.setEnableDynamicBackground(false);
            glass.setEnableBackdropBlur(true);
            glass.setBlurAmount(.045f);
            glass.setEnableShadow(false);
            glass.setEnableChromaticAberration(true);
            glass.setEnableEdgeHighlight(true);
            glass.setCornerRadius(999f);
            glass.setBevelWidth(10.5f * d);
            glass.setRefractionFalloff(1.35f);
            glass.setRefractionHeight(8f * d);
            glass.setDispersionStrength(.05f);
            glass.setEdgeSoftness(1.2f * d);
            glass.setEdgeHighlightBorderWidth(.70f * d);
            glass.setEdgeHighlightOpacity(45f);
            glass.setDisplacementScale(37f);
            glass.setSaturation(107f);
            glass.setGlassTint(Color.WHITE, .16f);
            host.setClipChildren(false);
            host.setClipToPadding(false);
            host.addView(glass, new FrameLayout.LayoutParams(-1, -1));
            final OpticalSheen sheen = new OpticalSheen(host.getContext(), true);
            host.addView(sheen, new FrameLayout.LayoutParams(-1, -1));
            final float[] progress = {0f};
            // A non-clickable last child is the visible optical front surface.
            // It paints OVER the "+" glyph; the original TextView alone owns
            // accessibility, touch and the Add Transaction click action.
            final OpticalCrown fabCrown = new OpticalCrown(d, true);
            final View frontSurface = new View(host.getContext()) {
                private boolean pressedDrawConfirmed;
                @Override protected void onDraw(Canvas canvas) {
                    fabCrown.paint(canvas, getWidth(), getHeight(), progress[0], 0f);
                    if (!pressedDrawConfirmed && progress[0] > .65f
                            && canvas.isHardwareAccelerated()) {
                        pressedDrawConfirmed = true;
                        android.util.Log.i("WenGlassProbe",
                                "MIT_FAB_FRONT_PRESS_DRAWN");
                    }
                }
            };
            frontSurface.setWillNotDraw(false);
            frontSurface.setClickable(false);
            frontSurface.setFocusable(false);
            frontSurface.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (!(fab.getParent() instanceof FrameLayout)) {
                throw new IllegalStateException("FAB must have its optical host");
            }
            ((FrameLayout) fab.getParent()).addView(frontSurface,
                    new FrameLayout.LayoutParams(-1, -1));
            android.util.Log.i("WenGlassProbe", "MIT_FAB_FRONT_ABOVE_GLYPH");
            final ValueAnimator[] animator = {null};
            final boolean[] active = {false};
            fab.setOnTouchListener((v, event) -> {
                final int action = event.getActionMasked();
                if (action != MotionEvent.ACTION_DOWN
                        && action != MotionEvent.ACTION_MOVE
                        && action != MotionEvent.ACTION_UP
                        && action != MotionEvent.ACTION_CANCEL) return false;
                final boolean outside = event.getX() < 0f || event.getY() < 0f
                        || event.getX() > v.getWidth() || event.getY() > v.getHeight();
                final boolean pressing = action == MotionEvent.ACTION_DOWN
                        || (action == MotionEvent.ACTION_MOVE && !outside && active[0]);
                if (pressing == active[0] && action != MotionEvent.ACTION_DOWN)
                    return false;
                active[0] = pressing;
                final float target = pressing ? 1f : 0f;
                if (animator[0] != null) animator[0].cancel();
                if (!ValueAnimator.areAnimatorsEnabled()) {
                    progress[0] = target;
                    renderFabMaterial(glass, sheen, frontSurface, d, target);
                } else {
                    ValueAnimator next = ValueAnimator.ofFloat(progress[0], target);
                    next.setDuration(pressing ? 165L : 250L);
                    next.setInterpolator(new PathInterpolator(.20f, .75f, .23f, 1f));
                    next.addUpdateListener(a -> {
                        progress[0] = (Float) a.getAnimatedValue();
                        renderFabMaterial(glass, sheen, frontSurface, d, progress[0]);
                    });
                    animator[0] = next;
                    next.start();
                }
                return false; // Original Add Transaction OnClick remains sole owner.
            });
            fab.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) { }
                @Override public void onViewDetachedFromWindow(View view) {
                    if (animator[0] != null) animator[0].cancel();
                    glass.setScaleX(1f);
                    glass.setScaleY(1f);
                    sheen.setMotion(0f, 0f);
                    frontSurface.invalidate();
                }
            });
            host.post(() -> {
                if (!host.isAttachedToWindow()) return;
                // FAB is partly outside the scroll viewport. Sampling that
                // view produced TRANSPARENT BLACK pixels and a black orb.
                // Root includes the scene background and avoids empty capture.
                // The native backdrop recorder excludes this glass branch.
                glass.setBackdropSource(root);
                android.util.Log.i("WenGlassProbe",
                        "MIT_FAB_BACKDROP_BOUND source=root");
            });
            // Remove Android's rectangular/elevated background entirely:
            // the actual circular AGSL material lives below the + glyph.
            fab.setBackground(null);
            fab.setForeground(null);
            fab.setStateListAnimator(null);
            fab.setElevation(0f);
            fab.setTextColor(Ui.DARK ? Color.WHITE : 0xFF315D94);
            android.util.Log.i("WenGlassProbe", "MIT_FAB_GLASS_ATTACHED");
            return true;
        } catch (RuntimeException | LinkageError failure) {
            android.util.Log.e("WenGlassProbe", "MIT_FAB_GLASS_FAILED", failure);
            host.removeAllViews();
            return false;
        }
    }

    private static void renderFabMaterial(LiquidGlassView glass,
                                          OpticalSheen sheen, View frontSurface,
                                          float density, float t) {
        glass.setRefractionHeight((8f + 14f * t) * density);
        glass.setDispersionStrength(.05f + .12f * t);
        glass.setDisplacementScale(37f + 57f * t);
        glass.setBevelWidth((10.5f + 2.0f * t) * density);
        glass.setGlassTint(Color.WHITE, .16f + .055f * t);
        glass.setScaleX(1f + .037f * t);
        glass.setScaleY(1f - .042f * t);
        sheen.setMotion(t, 0f);
        frontSurface.postInvalidateOnAnimation();
        glass.invalidate();
    }

    /**
     * Light only, NOT a substitute for backdrop refraction. Single upper
     * glint and a faint thin perimeter; no opaque white rings or Siri orb.
     * Shaders and matrices are reused and driven only by actual gestures.
     */
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
                    new int[]{0x8AFFFFFF, 0x3BAADFF6, 0x0AEDD9FF, 0},
                    new float[]{0f, .17f, .46f, 1f}, Shader.TileMode.CLAMP);
            glint.setShader(shader);
            if (circle) {
                frost.setShader(new RadialGradient(w*.5f, h*.5f,
                        Math.max(1f, Math.min(w,h)*.51f),
                        new int[]{0xECFFFFFF,0xDCFFFFFF,0x72FFFFFF},
                        new float[]{0f,.62f,1f}, Shader.TileMode.CLAMP));
            } else {
                frost.setShader(new LinearGradient(0f,0f,0f,h,
                        new int[]{0x9AFFFFFF,0xEBFFFFFF,0xE1FFFFFF,0xA9FFFFFF},
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
                        new int[]{0xCEFFFFFF, 0x4DE6F8FF, 0xB4FFFFFF, 0x74AFD7FF},
                        new float[]{0f, .35f, .72f, 1f}, Shader.TileMode.CLAMP);
                spectrumShader = new LinearGradient(0f, 0f, width, 0f,
                        new int[]{0x00FFFFFF, 0xDB6BDFFF, 0xD8E6A9FF,
                                0xE6FFFFFF, 0xDEFFBD88, 0x00FFFFFF},
                        new float[]{0f, .17f, .36f, .53f, .77f, 1f},
                        Shader.TileMode.CLAMP);
                glintShader = new LinearGradient(0f, 0f, width, height * .55f,
                        new int[]{0x00FFFFFF, 0x99FFFFFF, 0x62D7F5FF,
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

            // Subtle physical glass perimeter at rest; during Hold the
            // spectrally split rim visibly travels over the menu glyphs.
            rim.setStrokeWidth((.8f + .75f * t) * d);
            rim.setAlpha((int)(102f + 77f * t));
            canvas.drawRoundRect(bounds, radius, radius, rim);
            spectrum.setStrokeWidth((.7f + 1.5f * t) * d);
            spectrum.setAlpha((int)(24f + 190f * t));
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
