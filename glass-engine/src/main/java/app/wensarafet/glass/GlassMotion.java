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
            lens.animate().translationX(targetX).setDuration(340)
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
