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
            assertEquals(24, Ui.THEME_NAMES.length);
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
}
