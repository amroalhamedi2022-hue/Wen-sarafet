package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

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
            assertEquals(255, Color.alpha(gold));
            assertTrue("The Royal Gold button should actually look gold",
                    Color.red(gold) > Color.blue(gold) && Color.green(gold) > Color.blue(gold));
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
            assertTrue(Ui.glassScreenBackground(ctx) instanceof android.graphics.drawable.ColorDrawable);
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
                assertTrue("Navigation glass is readable", Color.alpha(
                        nav.getPixel(220, 44)) > 200);
                assertTrue("Balance tiles keep a readable fill", Color.alpha(
                        card.getPixel(190, 75)) > 220);
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

}
