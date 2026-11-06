package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Color;
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
                || !Ui.isLiquidTheme(nav.getContext())) return;
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

        GradientDrawable sampledPlate = new GradientDrawable();
        sampledPlate.setColor(Ui.DARK ? 0xFF30394C : 0xFFF8FBFF);
        sampledPlate.setCornerRadius(Ui.dp(context, 26));
        if (premium) {
            // Neutral smoked / ivory material, no coloured selected-tab pill.
            int[] glass = premiumIndex == 25
                    ? new int[]{0xE31D252E,0xE64A4032,0xE1222932}
                    : premiumIndex == 26
                    ? new int[]{0xE21E2A38,0xE5414347,0xE1242D39}
                    : premiumIndex == 27
                    ? new int[]{0xECFFF9EF,0xF0FAF3E6,0xEAFDF9F3}
                    : premiumIndex == 28
                    ? new int[]{0xDE234E40,0xD65C6546,0xDD234E40}
                    : new int[]{0xDE512B8B,0xD7683A9B,0xDA482585};
            GradientDrawable premiumPlate = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, glass);
            premiumPlate.setCornerRadius(Ui.dp(context, 26));
            // The nav host already draws the premium glass capsule and rim.
            // Painting another filled/stroked pill on this child creates the
            // three thick concentric outlines visible in real emulator PNGs.
            // Keep the child TRANSPARENT so the real moving lens samples the
            // scene directly; original four TextViews/clicks remain untouched.
            tabs.setBackground(null);
        } else if (Ui.isReferenceGlassTheme(context)) {
            // The old opaque #F8FBFF plate would completely conceal both
            // reference palettes under the native refractive lens.
            int reference = Ui.themeIndex(context);
            boolean prism = reference == 22;
            boolean royal = reference == 24;
            GradientDrawable palettePlate = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    prism ? new int[]{0xE24F246F, 0xE16C2E9B, 0xDF793BBA}
                            : royal ? new int[]{0xF51B1C23, 0xF0493923, 0xF31D2029}
                            : new int[]{0xF7FFE8CC, 0xF3FFE2BE, 0xF1F9DAC6});
            palettePlate.setCornerRadius(Ui.dp(context, 26));
            palettePlate.setStroke(Ui.dp(context, 1),
                    royal ? 0xC5D4A962 : prism ? 0x88FFFFFF : 0xD8FFFFFF);
            tabs.setBackground(palettePlate);
        } else {
            tabs.setBackground(sampledPlate);
        }

        nav.removeAllViews();
        String[] descriptions = new String[]{
                "تبويب الرئيسية", "تبويب العمليات", "تبويب الديون", "تبويب التحليل"
        };
        for (int i = 0; i < originalTabs.length; i++) {
            TextView item = originalTabs[i];
            item.setBackground(null); // One shared lens, never stacked pills.
            item.setSelected(i == selected);
            if (premium) {
                // Optical lens remains clear/neutral. Selected text is the
                // only per-tab colour cue, rather than an opaque colour bubble.
                int chosen = premiumIndex == 25 ? 0xFFFFE8B4
                        : premiumIndex == 26 ? 0xFFF6E6C9
                        : premiumIndex == 27 ? 0xFF644A2F
                        : premiumIndex == 28 ? 0xFFFFFFFF : 0xFFFFFFFF;
                int idle = premiumIndex == 25 ? 0xFFD4C5A9
                        : premiumIndex == 26 ? 0xFFCBC4BB
                        : premiumIndex == 27 ? 0xFF756B5B
                        : premiumIndex == 28 ? 0xFFEEEAD4 : 0xFFF4DDFC;
                item.setTextColor(i == selected ? chosen : idle);
                // Premium reference screenshots use fine, monochrome vector
                // glyphs. Android's four multi-colour emojis break the visual
                // hierarchy and resemble unstyled system defaults.
                // Replace glyph appearance ONLY; keep original tab TextView,
                // original navigation callback, order and accessibility label.
                String original = item.getText().toString();
                int lineBreak = original.lastIndexOf('\n');
                if (lineBreak >= 0) item.setText(original.substring(lineBreak + 1));
                WenPremiumTabIconDrawable glyph =
                        new WenPremiumTabIconDrawable(context, premiumIndex,
                                i, i == selected);
                int glyphSize=Ui.dp(context, 22);
                glyph.setBounds(0, 0, glyphSize, glyphSize);
                item.setCompoundDrawables(null, glyph, null, null);
                item.setCompoundDrawablePadding(Ui.dp(context, 4));
                item.setMaxLines(1);
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
            params.setMargins(Ui.dp(context, 4), 0, Ui.dp(context, 4), 0);
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
        lens.setBackground(new WenNavigationLensDrawable(context,
                premium ? 0x99FFFFFF : Ui.BRAND_SOFT,
                premium ? (Ui.DARK ? 0xB83D424A : 0xE5FFFFFF) : Ui.SURFACE,
                premium ? 0xC5FFFFFF : Ui.SKY, Ui.DARK));
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
            lens.bringToFront(); // Foreground optics ABOVE glyphs.
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
