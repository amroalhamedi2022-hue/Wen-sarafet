package app.wensarafet.tracker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Ported optical choreography from the isolated 1.8.8-sandbox.
 * Strictly view-local state: NEVER computes money, changes a stored preference,
 * or supplies new financial destinations. Original tab click handlers remain
 * the only route from a completed horizontal swipe.
 *
 * This class can be absent/disabled without altering any app function.
 */
public final class WenScreenVisualMotion {
    private static final PathInterpolator SOFT = new PathInterpolator(.18f,.78f,.22f,1f);
    private static final PathInterpolator RELEASE = new PathInterpolator(.2f,.8f,.2f,1f);
    static final String PRIMARY_SCROLL_TAG = "wen_primary_scroll";
    private static final int HOME = 0, LAST_TAB = 3;
    private final Context context;
    private TextView[] originalTabs;
    private int lastPrimaryTab = -1;
    private boolean firstPrimaryIcons = true;
    private boolean fabOpening;
    private boolean currentIsFabPage;
    private int fabOriginX, fabOriginY, fabRadius;
    private int visualGeneration;
    private boolean tracking, dragging;
    private float startX, startY;
    private int swipeStartTab;
    private float swipeProgress;

    public WenScreenVisualMotion(Context context) { this.context = context; }

    private boolean animate() {
        if (!ValueAnimator.areAnimatorsEnabled()) return false;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power == null || !power.isPowerSaveMode();
    }

    public void captureOriginalTabs(TextView home, TextView transactions, TextView debts,
                                    TextView analytics) {
        // The exact TextViews already have the 1.8.8 click callbacks attached.
        originalTabs = new TextView[]{home, transactions, debts, analytics};
    }

    public void markFabTap(View fab, FrameLayout root) {
        if (fab == null || root == null || !Ui.isLiquidTheme(context)) return;
        int[] position = new int[2];
        int[] origin = new int[2];
        fab.getLocationOnScreen(position);
        root.getLocationOnScreen(origin);
        fabOriginX = position[0] - origin[0] + fab.getWidth() / 2;
        fabOriginY = position[1] - origin[1] + fab.getHeight() / 2;
        fabRadius = Math.max(1, Math.min(fab.getWidth(), fab.getHeight()) / 2);
        fabOpening = true;
    }

    private static void removeIfAttached(FrameLayout host, View oldView) {
        if (oldView != null && oldView.getParent() == host) {
            oldView.animate().cancel();
            host.removeView(oldView);
        }
    }

    private static void showImmediately(FrameLayout host, View previous, View next) {
        next.animate().cancel();
        next.setVisibility(View.VISIBLE);
        next.setAlpha(1f);
        next.setTranslationX(0f);
        next.setTranslationY(0f);
        next.setScaleX(1f);
        next.setScaleY(1f);
        removeIfAttached(host, previous);
    }

    private void playReveal(FrameLayout host, View outgoing, View incoming,
                            boolean closing, int token) {
        final View animated = closing ? outgoing : incoming;
        if (animated == null || !animate()) {
            showImmediately(host, outgoing, incoming);
            return;
        }
        incoming.setAlpha(1f);
        incoming.setTranslationX(0f);
        incoming.setTranslationY(0f);
        incoming.setVisibility(closing ? View.VISIBLE : View.INVISIBLE);
        animated.post(() -> {
            if (token != visualGeneration || animated.getParent() != host
                    || animated.getWidth() == 0 || animated.getHeight() == 0
                    || !animate()) {
                if (token == visualGeneration) showImmediately(host, outgoing, incoming);
                return;
            }
            int cx = Math.max(0, Math.min(animated.getWidth(), fabOriginX));
            int cy = Math.max(0, Math.min(animated.getHeight(), fabOriginY));
            float r = (float) Math.hypot(Math.max(cx, animated.getWidth()-cx),
                    Math.max(cy, animated.getHeight()-cy));
            try {
                Animator reveal = ViewAnimationUtils.createCircularReveal(animated, cx, cy,
                        closing ? r : fabRadius, closing ? fabRadius : r);
                reveal.setDuration(265L);
                reveal.setInterpolator(SOFT);
                final boolean[] cleaned = {false};
                AnimatorListenerAdapter clean = new AnimatorListenerAdapter() {
                    private void finish() {
                        if (cleaned[0]) return;
                        cleaned[0] = true;
                        if (token == visualGeneration)
                            showImmediately(host, outgoing, incoming);
                    }
                    @Override public void onAnimationEnd(Animator animation) { finish(); }
                    @Override public void onAnimationCancel(Animator animation) { finish(); }
                };
                reveal.addListener(clean);
                incoming.setVisibility(View.VISIBLE);
                reveal.start();
            } catch (RuntimeException | LinkageError unavailable) {
                // A broken OEM shader never leaves an invisible finance screen.
                showImmediately(host, outgoing, incoming);
            }
        });
    }

    private void introIcons(View page) {
        if (!firstPrimaryIcons) return;
        firstPrimaryIcons = false;
        if (!animate()) return;
        String[] tags = {"wen_intro_header_accounts","wen_intro_header_settings",
                "wen_intro_nav_0","wen_intro_nav_1","wen_intro_nav_2",
                "wen_intro_nav_3","wen_intro_fab"};
        ArrayList<View> icons = new ArrayList<>();
        for (String tag : tags) {
            View item = page.findViewWithTag(tag);
            if (item == null) continue;
            item.animate().cancel();
            item.setAlpha(.88f);
            item.setScaleX(.95f);
            item.setScaleY(.95f);
            item.setTranslationY(Ui.dp(context,3));
            icons.add(item);
        }
        page.postOnAnimation(() -> {
            if (!page.isAttachedToWindow()) return;
            for (int i=0; i<icons.size(); i++) {
                View icon = icons.get(i);
                if (icon.isAttachedToWindow())
                    icon.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .translationY(0f).setStartDelay(i*14L).setDuration(190L)
                            .setInterpolator(RELEASE).start();
            }
        });
    }

    /**
     * Called solely after the original Activity has attached the new view.
     * Return true only when we fully own visual cleanup of the previous view.
     */
    public boolean transition(FrameLayout host, View previous, View next,
                              boolean targetPrimary, int tab) {
        if (!Ui.isLiquidTheme(context) || host == null || next == null) return false;
        final int token = ++visualGeneration;
        if (!animate()) {
            if (fabOpening) currentIsFabPage = true;
            else if (targetPrimary) currentIsFabPage = false;
            fabOpening = false;
            if (targetPrimary) lastPrimaryTab = tab;
            showImmediately(host, previous, next);
            return true;
        }
        if (fabOpening) {
            fabOpening = false;
            currentIsFabPage = true;
            playReveal(host, previous, next, false, token);
            return true;
        }
        if (currentIsFabPage && targetPrimary && previous != null) {
            currentIsFabPage = false;
            lastPrimaryTab = tab;
            // Reverse: place the home page BEHIND the existing add form.
            host.removeView(next);
            host.addView(next, 0, new FrameLayout.LayoutParams(-1,-1));
            playReveal(host, previous, next, true, token);
            return true;
        }
        if (!targetPrimary) return false;
        currentIsFabPage = false;
        if (firstPrimaryIcons) {
            lastPrimaryTab = tab;
            showImmediately(host, previous, next);
            introIcons(next);
            return true;
        }
        final int direction = lastPrimaryTab >= HOME && lastPrimaryTab <= LAST_TAB
                ? Integer.compare(tab, lastPrimaryTab) : 0;
        lastPrimaryTab = tab;
        // Single outgoing page removed immediately: avoid two full-screen
        // finance pages continuously composited by the GPU.
        showImmediately(host, previous, next);
        if (direction == 0) {
            next.setAlpha(.94f);
            next.setTranslationY(Ui.dp(context,3));
            next.animate().alpha(1f).translationY(0f).setDuration(115L)
                    .setInterpolator(RELEASE).start();
        } else {
            View content = next.findViewWithTag(PRIMARY_SCROLL_TAG);
            if (content != null) {
                content.setTranslationX(-direction * Ui.dp(context, 34));
                content.animate().translationX(0f).setDuration(165L)
                        .setInterpolator(SOFT).start();
            }
        }
        return true;
    }

    private static boolean protectedHorizontalTarget(View child, float rawX, float rawY) {
        if (child == null || child.getVisibility() != View.VISIBLE
                || child.getWidth() <= 0 || child.getHeight() <= 0) return false;
        int[] location = new int[2];
        child.getLocationOnScreen(location);
        if (rawX < location[0] || rawX > location[0]+child.getWidth()
                || rawY < location[1] || rawY > location[1]+child.getHeight()) return false;
        if (child instanceof HorizontalScrollView || child instanceof EditText
                || child instanceof SeekBar
                || child.canScrollHorizontally(-1) || child.canScrollHorizontally(1)
                || child.isClickable() || child.isLongClickable()) return true;
        if (!(child instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) child;
        for (int i=group.getChildCount()-1;i>=0;i--)
            if (protectedHorizontalTarget(group.getChildAt(i),rawX,rawY)) return true;
        return false;
    }

    private void resetSwipe(View screen, boolean committed) {
        if (dragging && originalTabs != null) {
            NavigationVisualDecorator.endSwipe(originalTabs, swipeStartTab, swipeProgress, committed);
        }
        tracking = false;
        dragging = false;
        swipeProgress = 0f;
        View content = screen == null ? null : screen.findViewWithTag(PRIMARY_SCROLL_TAG);
        if (content != null && content.getTranslationX() != 0f) {
            content.animate().cancel();
            if (!animate()) content.setTranslationX(0f);
            else content.animate().translationX(0f).setDuration(165L)
                    .setInterpolator(RELEASE).start();
        }
    }

    /**
     * Never intercepts single taps, vertical scroll, editable fields, horizontal
     * galleries or protected screens. The Activity supplies 'allowed' only for
     * an authenticated primary screen. A successful swipe calls the original
     * tab's performClick(), not any database/financial/transaction method.
     */
    public boolean interceptSwipe(MotionEvent event, FrameLayout host, View screen,
                                  boolean allowed, int tab, Consumer<MotionEvent> cancelChild) {
        int action=event.getActionMasked();
        if (!Ui.isLiquidTheme(context) || !allowed || host == null || screen == null
                || originalTabs == null || tab < HOME || tab > LAST_TAB) {
            if (tracking || dragging) resetSwipe(screen, false);
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                resetSwipe(screen, false);
                startX = event.getX();
                startY = event.getY();
                swipeStartTab=tab;
                tracking = startY > Ui.dp(context,128)
                        && startY < host.getHeight()-Ui.dp(context,100)
                        && !protectedHorizontalTarget(screen,event.getRawX(),event.getRawY());
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                resetSwipe(screen, false);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!tracking || swipeStartTab!=tab) return dragging;
                float dx=event.getX()-startX,dy=event.getY()-startY;
                if (!dragging) {
                    if (Math.abs(dy)>Ui.dp(context,22) && Math.abs(dy)>Math.abs(dx)) {
                        resetSwipe(screen, false);
                        return false;
                    }
                    int target=tab+(dx>0f ? 1 : -1);
                    if (Math.abs(dx)>Math.max(Ui.dp(context,48),
                            ViewConfiguration.get(context).getScaledTouchSlop()*3f)
                            && Math.abs(dx)>Math.abs(dy)*1.65f
                            && target>=HOME && target<=LAST_TAB) {
                        dragging = true;
                        MotionEvent cancel=MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        cancelChild.accept(cancel);
                        cancel.recycle();
                    }
                }
                if (dragging) {
                    float limit=Ui.dp(context,84);
                    View content = screen.findViewWithTag(PRIMARY_SCROLL_TAG);
                    if (content != null && animate()) {
                        content.animate().cancel();
                        content.setTranslationX(Math.max(-limit, Math.min(limit, dx * .26f)));
                    }
                    int preview = tab + (dx > 0f ? 1 : -1);
                    boolean valid = preview >= HOME && preview <= LAST_TAB;
                    swipeProgress = valid ? Math.max(0f, Math.min(1f,
                            (Math.abs(dx) - Ui.dp(context, 48)) / Ui.dp(context, 70))) : 0f;
                    NavigationVisualDecorator.followSwipe(originalTabs, tab,
                            valid ? preview : tab, swipeProgress);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    tracking = false;
                    return false;
                }
                int target=tab+(event.getX()>startX ? 1 : -1);
                float distance=Math.abs(event.getX()-startX);
                TextView destination = distance >= Ui.dp(context, 70)
                        && target >= HOME && target <= LAST_TAB ? originalTabs[target] : null;
                boolean committed = destination != null && destination.isAttachedToWindow();
                resetSwipe(screen, committed);
                if (committed) destination.performClick();
                return true;
            default:
                return dragging;
        }
    }
}
