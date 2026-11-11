package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import app.wensarafet.glass.WenReferenceGlassDrawable;

import java.util.concurrent.atomic.AtomicReference;

/** Render and select the two opt-in glass themes without launching a finance account. */
@RunWith(AndroidJUnit4.class)
public final class ReferenceThemesInstrumentedTest {
    private static void onUi(Runnable task) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { task.run(); } catch (Throwable error) { failure.set(error); }
        });
        if (failure.get() != null) throw new AssertionError("Real Android theme regression", failure.get());
    }

    private static void underTheme(int index, Runnable work) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // All test resources run in isolated .recovered.debug (not the owner's install).
        assertEquals("app.wensarafet.tracker.recovered.debug", context.getPackageName());
        SharedPreferences preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        int original = preferences.getInt("theme", 0);
        try {
            Ui.saveTheme(context, index);
            assertEquals(index, Ui.themeIndex(context));
            Ui.applyTheme(context);
            work.run();
        } finally {
            Ui.saveTheme(context, original);
        }
    }

    private static Bitmap draw(Drawable material, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        material.setBounds(0, 0, width, height);
        material.draw(new Canvas(bitmap));
        return bitmap;
    }

    @Test public void prismGlowHasFloatingVioletBarAndKeepsLiquidInteractions() {
        onUi(() -> underTheme(WenReferenceGlassDrawable.PRISM, () -> {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals(30, Ui.THEME_NAMES.length);
            assertTrue(Ui.THEME_NAMES[22].contains("Prism Glow"));
            assertTrue(Ui.isLiquidTheme(ctx));
            assertTrue(Ui.isReferenceGlassTheme(ctx));
            assertTrue(Ui.glassScreenBackground(ctx) instanceof WenReferenceGlassDrawable);
            Bitmap nav = draw(Ui.glassNavigationBackground(ctx, 26), 480, 90);
            assertTrue(Color.alpha(nav.getPixel(240, 45)) > 190);
            assertNotEquals("Optical violet nav needs real colour", Color.WHITE,
                    nav.getPixel(240, 45));
            Bitmap bg = draw(Ui.glassScreenBackground(ctx), 480, 800);
            assertNotEquals("Magenta/violet atmosphere cannot be flat white",
                    bg.getPixel(70, 150), bg.getPixel(410, 680));
            nav.recycle();
            bg.recycle();
        }));
    }

    @Test public void goldenBloomProducesCircularGlassAddControlAndSoftBackdrop() {
        onUi(() -> underTheme(WenReferenceGlassDrawable.BLOOM, () -> {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertTrue(Ui.THEME_NAMES[23].contains("Golden Bloom"));
            assertTrue(Ui.isLiquidTheme(ctx));
            assertTrue(Ui.isReferenceGlassTheme(ctx));
            Bitmap circle = draw(Ui.glassFloatingAddBackground(ctx), 160, 160);
            assertEquals("Outside the round plus button must remain transparent",
                    0, Color.alpha(circle.getPixel(0, 0)));
            assertEquals(255, Color.alpha(circle.getPixel(80, 80)));
            Bitmap bg = draw(Ui.glassScreenBackground(ctx), 480, 800);
            assertTrue("Golden Bloom uses a warm colour palette",
                    Color.red(bg.getPixel(80, 60)) > Color.blue(bg.getPixel(80, 60)));
            assertNotEquals(bg.getPixel(80, 60), bg.getPixel(400, 720));
            circle.recycle();
            bg.recycle();
        }));
    }

    @Test public void royalGoldRendersMetallicObsidianWithoutChangingNativeLiquid() {
        onUi(() -> underTheme(WenReferenceGlassDrawable.ROYAL, () -> {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertEquals(30, Ui.THEME_NAMES.length);
            assertTrue(Ui.THEME_NAMES[24].contains("Royal Gold"));
            assertEquals(24, Ui.themeIndex(ctx));
            assertTrue("Royal Gold must have dark premium contrast", Ui.DARK);
            assertTrue("Keep the original Liquid engine opt-in", Ui.isLiquidTheme(ctx));
            assertTrue(Ui.isReferenceGlassTheme(ctx));
            Bitmap backdrop = draw(Ui.glassScreenBackground(ctx), 480, 800);
            int darkPixel = backdrop.getPixel(410, 660);
            assertTrue("Obsidian background cannot be pastel",
                    Color.red(darkPixel) < 100 && Color.blue(darkPixel) < 100);
            assertNotEquals("Royal depth requires multiple metallic shadows",
                    backdrop.getPixel(70, 50), backdrop.getPixel(410, 660));
            Bitmap nav = draw(Ui.glassNavigationBackground(ctx, 26), 480, 90);
            assertTrue("Royal nav must stay nontransparent beneath native lens",
                    Color.alpha(nav.getPixel(240, 45)) > 200);
            assertNotEquals("A metallic nav requires gold and dark contrast",
                    nav.getPixel(100, 20), nav.getPixel(380, 65));
            Bitmap fab = draw(Ui.glassFloatingAddBackground(ctx), 160, 160);
            assertEquals("Round royal plus remains transparent outside the rim",
                    0, Color.alpha(fab.getPixel(0, 0)));
            int gold = fab.getPixel(80, 80);
            assertTrue("The neutral FAB remains readable but translucent",
                    Color.alpha(gold) > 100);
            assertTrue("Neutral FAB must not reintroduce a gold/orange tint",
                    Math.abs(Color.red(gold) - Color.blue(gold)) < 18
                            && Math.abs(Color.green(gold) - Color.blue(gold)) < 18);
            backdrop.recycle();
            nav.recycle();
            fab.recycle();
        }));
    }

    @Test public void oldThemesRemainSelectedExactlyAsBeforeAndUseLegacyDrawables() {
        onUi(() -> underTheme(0, () -> {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertFalse(Ui.isReferenceGlassTheme(ctx));
            assertFalse(Ui.isLiquidTheme(ctx));
            assertEquals("زمرد نعناعي", Ui.THEME_NAMES[0]);
            assertTrue(Ui.isUnifiedGlassTheme(ctx));
            assertTrue(Ui.glassScreenBackground(ctx)
                    instanceof app.wensarafet.glass.WenGlassBackdropDrawable);
            assertTrue(Ui.glassHeaderBackground(ctx,24)
                    instanceof app.wensarafet.glass.WenGlassDrawable);
            assertTrue(Ui.glassNavigationBackground(ctx,26)
                    instanceof app.wensarafet.glass.WenGlassDrawable);
        }));
        onUi(() -> underTheme(20, () -> {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertFalse(Ui.isReferenceGlassTheme(ctx));
            assertTrue(Ui.isLiquidTheme(ctx));
        }));
    }
    @Test public void fiveOwnerApprovedPremiumPalettesRenderWithoutTouchingAccounts() {
        for (int index = 25; index <= 29; index++) {
            final int theme = index;
            onUi(() -> underTheme(theme, () -> {
                Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
                assertEquals(30, Ui.THEME_NAMES.length);
                assertTrue(Ui.isReferenceGlassTheme(ctx));
                assertTrue(Ui.isLiquidTheme(ctx));
                assertEquals("Gold dark and the two vivid chromatic materials use dark ink mode",
                        theme != 27, Ui.DARK);
                assertTrue(Ui.glassScreenBackground(ctx)
                        instanceof app.wensarafet.glass.WenPremiumLiquidDrawable);
                Bitmap background = draw(Ui.glassScreenBackground(ctx), 480, 800);
                Bitmap card = draw(Ui.glassBalanceBackground(ctx, 22), 380, 150);
                Bitmap nav = draw(Ui.glassNavigationBackground(ctx, 26), 440, 88);
                Bitmap plus = draw(Ui.glassFloatingAddBackground(ctx), 140, 140);
                assertEquals("Floating add remains a true circle", 0,
                        Color.alpha(plus.getPixel(0, 0)));
                // The exact original Sandbox glass bubble is genuinely
                // translucent. It must remain readable, not opaque plastic.
                assertTrue("Original circular optical bubble must remain legible",
                        Color.alpha(plus.getPixel(70, 70)) > 215);
                assertTrue("Floating nav remains visible without an opaque block",
                        Color.alpha(nav.getPixel(220, 44)) > 60);
                assertTrue("Balance tiles keep readable translucent material",
                        Color.alpha(card.getPixel(190, 75)) > 75);
                assertNotEquals("Ambient backdrop must have depth",
                        background.getPixel(65, 75), background.getPixel(400, 730));
                assertNotEquals("Shimmering glass must not be flat",
                        nav.getPixel(55, 22), nav.getPixel(380, 67));
                background.recycle();
                card.recycle();
                nav.recycle();
                plus.recycle();
            }));
        }
    }

    /** Regression for actual View geometry and callbacks, not a mocked screen PNG. */
    @Test public void threeGoldDocksUseLegibleUnifiedIconsAndKeepNativeTabClicks() {
        for (int theme = 25; theme <= 27; theme++) {
            final int selectedTheme = theme;
            onUi(() -> underTheme(selectedTheme, () -> {
                Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
                int expectedHeight = Ui.dp(ctx, WenPremiumLayout.compact(ctx) ? 68 : 72);
                assertEquals("Consistent floating dock height on all primary screens",
                        expectedHeight, WenPremiumLayout.navigationHeight(ctx));
                assertEquals(16, WenPremiumLayout.navigationSideInsetDp(ctx));
                assertEquals(10, WenPremiumLayout.navigationBottomInsetDp(ctx));

                LinearLayout dock = new LinearLayout(ctx);
                dock.setOrientation(LinearLayout.HORIZONTAL);
                dock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                String[] labels = {"الرئيسية", "العمليات", "الديون", "التحليل"};
                String[] emojis = {"🏠", "📋", "🤝", "📊"};
                TextView[] tabs = new TextView[4];
                int[] taps = new int[4];
                for (int i = 0; i < 4; i++) {
                    final int clickIndex = i;
                    TextView tab = new TextView(ctx);
                    tab.setText(emojis[i] + "\n" + labels[i]);
                    tab.setGravity(Gravity.CENTER);
                    tab.setOnClickListener(v -> taps[clickIndex]++);
                    tabs[i] = tab;
                    dock.addView(tab, new LinearLayout.LayoutParams(
                            0, expectedHeight, 1f));
                }
                NavigationVisualDecorator.install(dock, tabs, 0);
                assertEquals("Only one shared glass track", 1, dock.getChildCount());
                for (int i = 0; i < tabs.length; i++) {
                    TextView tab = tabs[i];
                    assertEquals("Original familiar icon and Arabic label preserved",
                            emojis[i] + "\n" + labels[i], tab.getText().toString());
                    assertEquals("Icon/label typography has one scene-specific ink",
                            WenGoldGlassSurface.navigationInk(selectedTheme, i == 0),
                            tab.getCurrentTextColor());
                    assertNull("No replacement vector icon hides the original",
                            tab.getCompoundDrawables()[1]);
                    assertTrue("Touchable tab too short", tab.getMinHeight() >= Ui.dp(ctx, 48));
                    assertEquals("Original two-line symbol + label", 2, tab.getMaxLines());
                }
                tabs[2].performClick();
                assertArrayEquals("Original navigation listeners remain attached",
                        new int[]{0, 0, 1, 0}, taps);

                FrameLayout screen = new FrameLayout(ctx);
                screen.setBackground(Ui.glassScreenBackground(ctx));
                WenGoldGlassSurface.attachGoldBackdrop(screen);
                assertEquals("One shared authentic optical source", 1,
                        screen.getChildCount());
                assertEquals(WenGoldGlassSurface.BACKDROP_TAG,
                        screen.getChildAt(0).getTag());
            }));
        }
    }


    @Test public void allOriginalAndGoldThemesReturnToPickerWithoutChangingIds() {
        onUi(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ThemeCatalogue.restoreUserHidden(context);
            assertEquals(30, Ui.THEME_NAMES.length);
            for (int i = 0; i <= 27; i++) {
                assertTrue("Theme " + i + " should remain selectable",
                        ThemeCatalogue.isVisible(context, i));
            }
            assertEquals(28, ThemeCatalogue.visibleCount(context));
            assertFalse("Retired orange/green remains hidden",
                    ThemeCatalogue.isVisible(context, 28));
            assertFalse("Retired violet/pink remains hidden",
                    ThemeCatalogue.isVisible(context, 29));
        });
    }
}
