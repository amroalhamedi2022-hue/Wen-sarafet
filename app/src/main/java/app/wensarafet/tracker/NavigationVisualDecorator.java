package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.WeakHashMap;

import app.wensarafet.glass.GlassMotion;
import app.wensarafet.glass.WenNavigationLensDrawable;
import app.wensarafet.glass.WenNavigationTrack;

/**
 * Visual-only adapter for the four EXISTING Wen Sarafet 1.8.8 navigation tabs.
 *
 * The original MainActivity still creates every button and owns every
 * OnClickListener. This class only re-parents existing views for drawing and
 * contributes an inert, touch-through Liquid lens. No business, account,
 * database, receipt, permission, security or activation code is referenced.
 */
public final class NavigationVisualDecorator {
    // The Activity rebuilds its tabs on navigation. Retain only the optical
    // starting point across that handoff, without retaining its views.
    private static final WeakHashMap<Context, NavState> NAV_STATES = new WeakHashMap<>();
    private static final class NavState {
        int previous = -1;
        boolean heldNavigation;
        float swipeProgress;
    }
    private static final String LENS_TAG = "wen_navigation_lens";

    private NavigationVisualDecorator() {}

    public static void install(LinearLayout nav, TextView[] originalTabs, int selected) {
        if (nav == null || originalTabs == null || originalTabs.length != 4
                || !Ui.isUnifiedGlassTheme(nav.getContext())) return;
        final Context context = nav.getContext();
        NavState state = NAV_STATES.get(context);
        if (state == null) {
            state = new NavState();
            NAV_STATES.put(context, state);
        }
        final NavState navState = state;
        final WenNavigationTrack track = new WenNavigationTrack(context, Ui.DARK);
        // Visual-only premium selection; all four original callbacks survive.
        final int premiumIndex = Ui.themeIndex(context);
        final boolean premium = app.wensarafet.glass.WenPremiumLiquidDrawable
                .supports(premiumIndex);

        // Keep the original four TextViews and, more importantly, their
        // exact unmodified 1.8.8 OnClickListeners. Reparent before attachment.
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tabs.setTag("wen_glass_tab_content");

        // Unified optical contract for every selectable palette. The actual
        // floating dock owns the material; a second opaque tab plate would
        // conceal scrolling content and defeat native refraction.
        tabs.setBackground(null);

        nav.removeAllViews();
        nav.setPadding(Ui.dp(context, 5), Ui.dp(context, 5),
                Ui.dp(context, 5), Ui.dp(context, 5));
        String[] descriptions = new String[]{
                "تبويب الرئيسية", "تبويب العمليات", "تبويب الديون", "تبويب التحليل"
        };
        for (int i = 0; i < originalTabs.length; i++) {
            TextView item = originalTabs[i];
            item.setBackground(null); // One shared lens, never stacked pills.
            item.setSelected(i == selected);
            if (premium) {
                item.setTextColor(WenGoldGlassSurface.navigationInk(premiumIndex, i == selected));
            } else {
                item.setTextColor(i == selected ? Ui.TEXT : Ui.MUTED);
            }
            String original = item.getText().toString();
            int lineBreak = original.indexOf('\n');
            if (lineBreak > 0) {
                SpannableString label = new SpannableString(original);
                label.setSpan(new RelativeSizeSpan(1.2f), 0,
                        lineBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                item.setText(label);
            }
            item.setGravity(Gravity.CENTER);
            item.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            item.setTypeface(Typeface.create(
                    i == selected ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
            item.setAutoSizeTextTypeUniformWithConfiguration(10, 12, 1,
                    TypedValue.COMPLEX_UNIT_SP);
            item.setIncludeFontPadding(false);
            item.setLineSpacing(0f, 1f);
            item.setPadding(Ui.dp(context, 1), 0, Ui.dp(context, 1), 0);
            item.setMinHeight(Ui.dp(context, 48));
            item.setMaxLines(2);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.setContentDescription(descriptions[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tabs.addView(item, params);
        }

        // An ordinary non-clickable FrameLayout still dispatches events into
        // its shader child. The override is essential for genuine Hold+Drag.
        FrameLayout lens = new FrameLayout(context) {
            @Override public boolean dispatchTouchEvent(MotionEvent event) {
                return false;
            }
        };
        lens.setClickable(false);
        lens.setFocusable(false);
        lens.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        lens.setTag(LENS_TAG);
        lens.setBackground(new WenNavigationLensDrawable(context, true, true));
        track.addView(lens, new FrameLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        // If a native renderer cannot initialise on a device, retain the
        // tested Android-only fallback instead of affecting tab navigation.
        boolean attached = NavigationLiquidMaterial.attach(lens, track);
        if (attached) {
            // Keep a translucent neutral body even where the sampled page is empty.
            lens.setBackground(new WenNavigationLensDrawable(context, true, false));
            track.setExternalGlassActive(true);
        }
        track.addView(tabs, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < originalTabs.length; i++) {
            // The pre-existing tab's OnClickListener remains the ONLY
            // navigation action; the Runnable only remembers visual handoff.
            GlassMotion.bindNavigationTab(originalTabs[i], track, lens, originalTabs,
                    selected, i, Color.WHITE, () -> navState.heldNavigation = true);
        }
        nav.addView(track, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final boolean fromHeldGesture = navState.heldNavigation;
        final int initial = fromHeldGesture ? selected : navState.previous;
        navState.heldNavigation = false;
        GlassMotion.slideLens(track, lens, originalTabs, initial, selected,
                navState.swipeProgress);
        navState.swipeProgress = 0f;
        if (fromHeldGesture) GlassMotion.finishNavigationLensHandoff(track, lens);
        navState.previous = selected;
    }

    static void followSwipe(TextView[] tabs, int from, int to, float progress) {
        View parent = (View) tabs[from].getParent();
        if (parent == null || !(parent.getParent() instanceof WenNavigationTrack)) return;
        WenNavigationTrack track = (WenNavigationTrack) parent.getParent();
        View lens = track.findViewWithTag(LENS_TAG);
        if (lens == null || lens.getWidth() == 0) return;
        float start = tabs[from].getLeft();
        float destination = tabs[to].getLeft();
        lens.animate().cancel();
        lens.setTranslationX(start + (destination - start) * progress);
        track.invalidateLensPosition();
    }

    static void endSwipe(TextView[] tabs, int from, float progress, boolean committed) {
        if (committed) {
            NavState state = NAV_STATES.get(tabs[from].getContext());
            if (state != null) state.swipeProgress = progress;
            return;
        }
        View parent = (View) tabs[from].getParent();
        if (parent == null || !(parent.getParent() instanceof WenNavigationTrack)) return;
        WenNavigationTrack track = (WenNavigationTrack) parent.getParent();
        View lens = track.findViewWithTag(LENS_TAG);
        if (lens != null) {
            GlassMotion.settleLens(track, lens, tabs[from].getLeft());
        }
    }
}
