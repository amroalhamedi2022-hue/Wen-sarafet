package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.graphics.drawable.GradientDrawable;
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
    // A new Activity (including rotation/recreation) starts with its own lens.
    // Global tab state used to animate across two unrelated Activity windows.
    // Weak keys avoid retaining an Activity after its views are destroyed.
    private static final WeakHashMap<Context, NavState> NAV_STATES = new WeakHashMap<>();
    private static final class NavState {
        int previous = -1;
        boolean heldNavigation;
    }
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
        if (premium) {
            // 48dp+ touch targets, optically centred in one floating capsule.
            // Every tab keeps its original listener and accessibility identity.
            nav.setPadding(Ui.dp(context, 8), Ui.dp(context, 6),
                    Ui.dp(context, 8), Ui.dp(context, 6));
            nav.setElevation(Ui.dp(context, 6));
        }
        String[] descriptions = new String[]{
                "تبويب الرئيسية", "تبويب العمليات", "تبويب الديون", "تبويب التحليل"
        };
        for (int i = 0; i < originalTabs.length; i++) {
            TextView item = originalTabs[i];
            item.setBackground(null); // One shared lens, never stacked pills.
            item.setSelected(i == selected);
            if (!premium) {
                // Keep original Unicode symbols for every legacy/reference
                // theme; only enlarge the glyph slightly within its own line.
                String original = item.getText().toString();
                int lineBreak = original.indexOf('\n');
                if (lineBreak > 0) {
                    SpannableString originalLabel = new SpannableString(original);
                    originalLabel.setSpan(new RelativeSizeSpan(1.20f), 0,
                            lineBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    item.setText(originalLabel);
                }
            }
            if (premium) {
                // One calibrated palette from screen header through nav label
                // and vector stroke: no Android emoji or unrelated blue pill.
                int ink = premiumIndex <= 27
                        ? WenGoldGlassSurface.navigationInk(premiumIndex, i == selected)
                        : (i == selected ? Color.WHITE : 0xFFF6E6FF);
                item.setTextColor(ink);
                // Restore the ORIGINAL 1.8.8 emoji symbols and Arabic tab names.
                // A span enlarges only the symbol, not the whole dock or hitbox.
                String original = item.getText().toString();
                int lineBreak = original.indexOf('\n');
                if (lineBreak > 0) {
                    SpannableString originalLabel = new SpannableString(original);
                    originalLabel.setSpan(new RelativeSizeSpan(1.30f), 0,
                            lineBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    item.setText(originalLabel);
                }
                item.setGravity(Gravity.CENTER);
                item.setTextDirection(View.TEXT_DIRECTION_LOCALE);
                item.setTypeface(Typeface.create(
                        i == selected ? "sans-serif-medium" : "sans-serif",
                        Typeface.NORMAL));
                item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                item.setAutoSizeTextTypeUniformWithConfiguration(
                        12, 14, 1, TypedValue.COMPLEX_UNIT_SP);
                item.setIncludeFontPadding(false);
                item.setLineSpacing(0f, 1f);
                item.setPadding(Ui.dp(context, 2), Ui.dp(context, 3),
                        Ui.dp(context, 2), Ui.dp(context, 3));
                item.setMinHeight(Ui.dp(context, 48));
                // The newer vector set replaced the familiar tab symbols.
                // Reuse their existing original emoji TextViews instead.
                item.setCompoundDrawables(null, null, null, null);
                item.setCompoundDrawablePadding(0);
                item.setSingleLine(false);
                item.setMaxLines(2);
                item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } else if (Ui.isReferenceGlassTheme(context)) {
                // Only appearance changes; the original navigation actions,
                // accessibility labels and held-drag lens remain untouched.
                int style = Ui.themeIndex(context);
                item.setTextColor(style == 22
                        ? (i == selected ? 0xFFFFFFFF : 0xFFF3DFFB)
                        : style == 24
                        ? (i == selected ? 0xFFFFE8B0 : 0xFFD5BF91)
                        : (i == selected ? 0xFF63391F : 0xFF725749));
            }
            item.setContentDescription(descriptions[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            params.setMargins(Ui.dp(context, premium ? 2 : 4), 0,
                    Ui.dp(context, premium ? 2 : 4), 0);
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
        // Neutral refractive lens at every theme index: no coloured halo or
        // opaque circle over the saved account/tab palette.
        lens.setBackground(new WenNavigationLensDrawable(context,
                0x99FFFFFF, Ui.DARK ? 0xB83D424A : 0xD8FFFFFF,
                0xC5FFFFFF, Ui.DARK));
        track.addView(lens, new FrameLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        // If a native renderer cannot initialise on a device, retain the
        // tested Android-only fallback instead of affecting tab navigation.
        boolean attached = NavigationLiquidMaterial.attach(lens, track);
        if (attached) {
            lens.setBackground(null);
            track.setExternalGlassActive(true);
        }
        track.addView(tabs, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (attached) {
            // The liquid lens now refracts the actual SCROLL BACKDROP, so
            // tab icons/Arabic labels must sit above the glass to remain crisp.
            // Legacy materials retain their prior original draw order.
            tabs.bringToFront();
        } else if (track.getOptic() != null) {
            track.getOptic().bringToFront();
        }

        for (int i = 0; i < originalTabs.length; i++) {
            // The pre-existing tab's OnClickListener remains the ONLY
            // navigation action; the Runnable only remembers visual handoff.
            GlassMotion.bindNavigationTab(originalTabs[i], track, lens, originalTabs,
                    selected, i, Ui.SKY, () -> navState.heldNavigation = true);
        }
        nav.addView(track, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final boolean fromHeldGesture = navState.heldNavigation;
        final int initial = fromHeldGesture ? selected : navState.previous;
        navState.heldNavigation = false;
        GlassMotion.slideLens(track, lens, originalTabs, initial, selected);
        if (fromHeldGesture) GlassMotion.finishNavigationLensHandoff(track, lens);
        navState.previous = selected;
    }
}
