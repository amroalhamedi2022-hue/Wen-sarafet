package app.wensarafet.glass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

/**
 * Reusable, gesture-safe interaction motion for Android View apps.
 * All animations are finite and triggered by direct user interaction.
 * Does not depend on the host app's data, finance, app activation or settings.
 */
public final class GlassMotion {
    private static final PathInterpolator COMPRESS = new PathInterpolator(.22f, .78f, .20f, 1f);
    private static final OvershootInterpolator RELEASE = new OvershootInterpolator(.76f);
    private static final OvershootInterpolator NAV_SPRING = new OvershootInterpolator(.40f);

    private GlassMotion() {}

    /** Low power keeps short scale feedback, but skips the extra glow animator. */
    private static boolean lowPower(Context c) {
        PowerManager power = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isPowerSaveMode();
    }

    /** Bind without consuming click, click-and-hold or touch-scroll events. */
    public static void bindPress(View view, int accent) {
        if (!view.isClickable() && !view.isLongClickable()) return;
        final PressState state = new PressState(view, accent);
        view.setOnTouchListener((v, event) -> state.onTouch(event));
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { state.reset(); }
        });
    }


    /**
     * A single captured pointer glides the shared lens across RTL/LTR tab centers.
     * Tap: release over the original tab. Hold + drag + release: switch to the
     * hovered tab. Source tab remains touch owner while the finger moves across
     * sibling tabs, so tapping never starts a second business/navigation action.
     * No recurring timer, handler, or background frame callback.
     */
    public static void bindNavigationTab(View tab, FrameLayout track, View lens,
                                         View[] tabs, int currentIndex, int targetIndex,
                                         int accent, Runnable beforeNavigate) {
        final NavigationGesture gesture = new NavigationGesture(
                tab, track, lens, tabs, currentIndex, targetIndex, beforeNavigate);
        tab.setOnTouchListener(gesture);
        tab.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { gesture.detach(); }
        });
    }

    private static final class NavigationGesture implements View.OnTouchListener {
        private final View origin;
        private final FrameLayout track;
        private final View lens;
        private final View[] tabs;
        private final int selected;
        private final int originIndex;
        private final Runnable beforeNavigate;
        private final int slop;
        private boolean tracking;
        private boolean cancelled;
        private float downX, downY;
        private int hovered;
        private ValueAnimator morphAnimator;
        private final WenOpticalNavigationOverlay optic;
        private final WenNavigationTrack.GlassGestureBridge materialBridge;
        private float materialProgress;

        NavigationGesture(View origin, FrameLayout track, View lens, View[] tabs,
                          int selected, int originIndex, Runnable beforeNavigate) {
            this.origin=origin; this.track=track; this.lens=lens;
            this.tabs=tabs; this.selected=selected; this.originIndex=originIndex;
            this.beforeNavigate=beforeNavigate;
            this.slop=ViewConfiguration.get(origin.getContext()).getScaledTouchSlop();
            this.hovered=originIndex;
            optic = track instanceof WenNavigationTrack
                    ? ((WenNavigationTrack) track).getOptic() : null;
            materialBridge = track instanceof WenNavigationTrack
                    ? ((WenNavigationTrack) track).getGlassGestureBridge() : null;
            if (optic != null) optic.bindPhysicalPill(lens);
        }

        private int nearest(float fingerX) {
            int nearest=0;
            float distance=Float.MAX_VALUE;
            for (int i=0;i<tabs.length;i++) {
                float midpoint=tabs[i].getLeft()+tabs[i].getWidth()*.5f;
                float gap=Math.abs(fingerX-midpoint);
                if(gap<distance) { distance=gap; nearest=i; }
            }
            return nearest;
        }

        private boolean insideVertical(float y) {
            return y >= -slop*2f && y <= track.getHeight()+slop*2f;
        }

        /** Align the droplet to the actual finger, not the centre of its icon. */
        private float fingerLensX(float fingerX) {
            if (lens.getWidth() <= 0) return origin.getLeft();
            float min=Float.MAX_VALUE, max=-Float.MAX_VALUE;
            for (View item : tabs) {
                min=Math.min(min, item.getLeft());
                max=Math.max(max, item.getLeft());
            }
            return Math.max(min, Math.min(max, fingerX-lens.getWidth()*.5f));
        }

        private WenNavigationLensDrawable opticalLens() {
            return lens.getBackground() instanceof WenNavigationLensDrawable
                    ? (WenNavigationLensDrawable) lens.getBackground() : null;
        }

        /** A one-shot fluid transition; detached/cancelled gestures own no clock. */
        private void morphTo(float destination, long duration) {
            if (morphAnimator != null) {
                morphAnimator.cancel();
                morphAnimator = null;
            }
            WenNavigationLensDrawable glass = opticalLens();
            if (glass == null && materialBridge == null) return;
            if (!ValueAnimator.areAnimatorsEnabled() || lowPower(origin.getContext())) {
                if (glass != null) glass.setMorph(destination);
                if (optic != null) optic.progress(destination);
                if (materialBridge != null) {
                    materialProgress = destination;
                    materialBridge.morph(destination);
                }
                return;
            }
            morphAnimator = ValueAnimator.ofFloat(
                    glass != null ? glass.getMorph() : materialProgress, destination);
            morphAnimator.setDuration(duration);
            morphAnimator.setInterpolator(COMPRESS);
            morphAnimator.addUpdateListener(a -> {
                float progress = (Float) a.getAnimatedValue();
                if (glass != null) glass.setMorph(progress);
                if (optic != null) optic.progress(progress);
                if (materialBridge != null) {
                    materialProgress = progress;
                    materialBridge.morph(progress);
                }
            });
            morphAnimator.start();
        }

        private void returnToSelected() {
            morphTo(0f, 190);
            if (lens.getWidth()<=0) return;
            float target=selected>=0 && selected<tabs.length
                    ? tabs[selected].getLeft() : origin.getLeft();
            lens.animate().cancel();
            if (ValueAnimator.areAnimatorsEnabled() && !lowPower(origin.getContext())) {
                lens.animate().translationX(target).scaleX(1f).scaleY(1f)
                        .setDuration(190).setInterpolator(COMPRESS).start();
            } else {
                lens.setTranslationX(target); lens.setScaleX(1f); lens.setScaleY(1f);
            }
        }

        @Override public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    android.util.Log.i("WenGlassProbe", "DOWN tab=" + originIndex
                            + " lensWidth=" + lens.getWidth()
                            + " optic=" + (optic != null));
                    tracking=true; cancelled=false; hovered=originIndex;
                    downX=event.getX(); downY=event.getY();
                    lens.animate().cancel();
                    // Only the existing selected-tab pill receives optics on hold.
                    // Never draw a second circle or enlarge the whole pill.
                    if (optic != null) {
                        optic.begin((View) view.getParent(),
                                view.getLeft() + event.getX());
                        optic.progress(.02f);
                    }
                    morphTo(1f, 190);
                    android.util.Log.i("WenGlassProbe", "DOWN_MORPH completed");
                    if (ValueAnimator.areAnimatorsEnabled() && !lowPower(view.getContext())) {
                        // Press creates a compact rounded bubble; hold leaves it
                        // inflated until release, with no repeating animator.
                        lens.animate().translationX(fingerLensX(view.getLeft()+event.getX()))
                                .scaleX(1f).scaleY(1f).setDuration(160)
                                .setInterpolator(COMPRESS).start();
                    } else if (lens.getWidth()>0) {
                        lens.setTranslationX(fingerLensX(view.getLeft()+event.getX()));
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tracking) return true;
                    float x=origin.getLeft()+event.getX();
                    float y=origin.getTop()+event.getY();
                    if (!insideVertical(y)) {
                        // Hold the last optical position while the pointer is
                        // temporarily out of bounds. Returning during MOVE made
                        // the lens jump home and then forward if it re-entered.
                        // Only the final UP/CANCEL decides whether to snap back.
                        cancelled=true;
                        return true;
                    }
                    cancelled=false;
                    int previousHover = hovered;
                    hovered=nearest(x);
                    if (hovered != previousHover)
                        android.util.Log.i("WenGlassProbe", "HOVER tab=" + hovered);
                    if (optic != null) optic.move(x);
                    if (materialBridge != null) materialBridge.follow(x);
                    float move=Math.abs(event.getX()-downX);
                    if (move>slop && lens.getWidth()>0) {
                        // Direct translation tracks the fingertip; the animation
                        // clock is not restarted for every ACTION_MOVE.
                        float destination=fingerLensX(x);
                        lens.animate().cancel();
                        lens.setTranslationX(destination);
                        if (ValueAnimator.areAnimatorsEnabled() && !lowPower(view.getContext())) {
                            lens.setScaleX(1f); lens.setScaleY(1f);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    android.util.Log.i("WenGlassProbe", "UP tab=" + originIndex
                            + " tracking=" + tracking);
                    if(!tracking) return true;
                    tracking=false;
                    // A transient excursion during MOVE must not veto an inside
                    // release: only the final UP position decides the tab.
                    if (!insideVertical(origin.getTop()+event.getY())) {
                        returnToSelected();
                        return true;
                    }
                    hovered=nearest(origin.getLeft()+event.getX());
                    android.util.Log.i("WenGlassProbe", "UP_TARGET tab=" + hovered
                            + " from=" + selected + " releasedX="
                            + (origin.getLeft()+event.getX()));
                    if (hovered == selected) {
                        returnToSelected();
                    } else {
                        // Pin to the actual destination BEFORE the old View is
                        // removed. The next page will materialise at this same tab.
                        // Never animate back to the previous selected position.
                        lens.animate().cancel();
                        lens.setTranslationX(tabs[hovered].getLeft());
                        // Invoke exactly one of the pre-existing View.OnClick
                        // callbacks. Never bypass its functional validation.
                        beforeNavigate.run();
                        tabs[hovered].performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN:
                    tracking=false; cancelled=true;
                    returnToSelected();
                    return true;
                default:
                    return tracking;
            }
        }

        void detach() {
            tracking=false;
            if (optic != null) optic.discard();
            if (materialBridge != null) materialBridge.reset();
            if (morphAnimator != null) {
                morphAnimator.cancel();
                morphAnimator = null;
            }
            WenNavigationLensDrawable glass = opticalLens();
            if (glass != null) glass.setMorph(0f);
            origin.animate().cancel();
            origin.setScaleX(1f); origin.setScaleY(1f);
            lens.animate().cancel();
            lens.setScaleX(1f); lens.setScaleY(1f);
        }
    }

    private static final class PressState {
        private final View view;
        private final int accent;
        private final int slop;
        private float startX, startY;
        private boolean pressed;
        private ValueAnimator glowAnimator;
        private GlowDrawable glow;

        PressState(View view, int accent) {
            this.view = view;
            this.accent = accent;
            slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        }

        private void glowTo(float target, long duration) {
            if (glow == null) return;
            if (glowAnimator != null) glowAnimator.cancel();
            glowAnimator = ValueAnimator.ofFloat(glow.intensity, target);
            glowAnimator.setDuration(duration);
            glowAnimator.setInterpolator(COMPRESS);
            glowAnimator.addUpdateListener(a -> {
                if (glow != null) glow.setIntensity((Float) a.getAnimatedValue());
            });
            glowAnimator.start();
        }

        private void begin(MotionEvent e) {
            pressed = true;
            startX = e.getX();
            startY = e.getY();
            view.animate().cancel();
            if (!ValueAnimator.areAnimatorsEnabled()) {
                view.setScaleX(1f);
                view.setScaleY(1f);
                return;
            }
            view.animate().scaleX(.958f).scaleY(.958f).setDuration(95)
                    .setInterpolator(COMPRESS).start();
            if (lowPower(view.getContext())) return;
            // Lazy allocation: list rows do not own Paint/Shader until actually pressed.
            if (glow == null && view.getForeground() == null) {
                glow = new GlowDrawable(accent);
                view.setForeground(glow);
            }
            if (glow != null) {
                glow.setTouch(startX, startY);
                // Held press settles to a clear bubble highlight after 340 ms.
                glowTo(1f, 340);
            }
        }

        private void finish() {
            if (!pressed) return;
            pressed = false;
            view.animate().cancel();
            if (!ValueAnimator.areAnimatorsEnabled()) {
                reset();
                return;
            }
            view.animate().scaleX(1f).scaleY(1f).setDuration(225)
                    .setInterpolator(RELEASE).start();
            glowTo(0f, 175);
        }

        boolean onTouch(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    begin(e);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pressed) {
                        float dx = e.getX() - startX;
                        float dy = e.getY() - startY;
                        if (Math.abs(dx) > slop || Math.abs(dy) > slop
                                || e.getX() < 0 || e.getY() < 0
                                || e.getX() > view.getWidth() || e.getY() > view.getHeight()) {
                            finish();
                        } else if (glow != null) {
                            glow.setTouch(e.getX(), e.getY());
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    finish();
                    break;
                default:
                    break;
            }
            // Crucial: original OnClick, OnLongClick and scroll dispatch still work.
            return false;
        }

        void reset() {
            pressed = false;
            if (glowAnimator != null) {
                glowAnimator.cancel();
                glowAnimator = null;
            }
            if (glow != null) glow.setIntensity(0f);
            view.animate().cancel();
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    /**
     * Visual continuity after the destination screen replaces the old tab strip.
     * The replacement starts as the same rounded lens and settles in-place into
     * the selected pill; there is no intermediate trip to the former tab.
     */
    public static void finishNavigationLensHandoff(FrameLayout track, View lens) {
        if (!(lens.getBackground() instanceof WenNavigationLensDrawable)) return;
        WenNavigationLensDrawable glass = (WenNavigationLensDrawable) lens.getBackground();
        if (!ValueAnimator.areAnimatorsEnabled() || lowPower(track.getContext())) {
            glass.setMorph(0f);
            return;
        }
        glass.setMorph(1f);
        track.post(() -> {
            if (!lens.isAttachedToWindow()) return;
            ValueAnimator settle = ValueAnimator.ofFloat(1f, 0f);
            settle.setDuration(205);
            settle.setInterpolator(COMPRESS);
            settle.addUpdateListener(a -> glass.setMorph((Float)a.getAnimatedValue()));
            settle.start();
        });
    }

    /** A single shared lens follows actual RTL/LTR tab coordinates, no fixed x. */
    public static void slideLens(FrameLayout container, View lens, View[] tabs,
                                 int previousIndex, int selectedIndex) {
        if (tabs.length == 0 || selectedIndex < 0 || selectedIndex >= tabs.length) return;
        container.post(() -> {
            if (container.getWindowToken() == null) return;
            View selected = tabs[selectedIndex];
            if (selected.getWidth() <= 0 || selected.getHeight() <= 0) return;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) lens.getLayoutParams();
            if (lp.width != selected.getWidth() || lp.height != selected.getHeight()) {
                lp.width = selected.getWidth();
                lp.height = selected.getHeight();
                lens.setLayoutParams(lp);
            }
            lens.animate().cancel();
            lens.setTranslationY(selected.getTop());
            float targetX = selected.getLeft();
            boolean canSlide = previousIndex >= 0 && previousIndex < tabs.length
                    && previousIndex != selectedIndex
                    && ValueAnimator.areAnimatorsEnabled() && !lowPower(container.getContext());
            if (!canSlide) {
                lens.setTranslationX(targetX);
                return;
            }
            lens.setTranslationX(tabs[previousIndex].getLeft());
            // Incoming full-screen content was previously alpha-zero for 280ms,
            // hiding most of the 340ms slide. Animate with the screen's short
            // transition so the movement is visible after the first rendered frame.
            lens.animate().translationX(targetX).setDuration(225)
                    .setInterpolator(NAV_SPRING).start();
        });
    }

    /** No timers, allocations, or CPU work in draw(); one cached gradient per touch. */
    private static final class GlowDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final int color;
        private float x, y;
        private float radius;
        private float intensity;
        private int alpha = 255;

        GlowDrawable(int color) { this.color = color; }

        @Override protected void onBoundsChange(Rect rect) {
            super.onBoundsChange(rect);
            bounds.set(rect);
            radius = Math.max(1f, Math.max(bounds.width(), bounds.height()) * .70f);
            setTouch(x <= 0f ? bounds.centerX() : x, y <= 0f ? bounds.centerY() : y);
        }

        void setTouch(float x, float y) {
            if (radius <= 0f) return;
            if (Math.abs(this.x - x) < 5f && Math.abs(this.y - y) < 5f
                    && paint.getShader() != null) return;
            this.x = x;
            this.y = y;
            paint.setShader(new RadialGradient(x, y, radius,
                    new int[]{Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)),
                            Color.argb(22, 255, 255, 255), Color.TRANSPARENT},
                    new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
            invalidateSelf();
        }

        void setIntensity(float next) {
            intensity = Math.max(0f, Math.min(1f, next));
            paint.setAlpha(Math.round(alpha * intensity));
            invalidateSelf();
        }

        @Override public void draw(Canvas canvas) {
            if (intensity <= 0f || bounds.isEmpty()) return;
            float r = Math.min(bounds.width(), bounds.height()) * .24f;
            canvas.drawRoundRect(bounds, r, r, paint);
        }

        @Override public void setAlpha(int value) {
            alpha = Math.max(0, Math.min(255, value));
            paint.setAlpha(Math.round(alpha * intensity));
            invalidateSelf();
        }

        @Override public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
