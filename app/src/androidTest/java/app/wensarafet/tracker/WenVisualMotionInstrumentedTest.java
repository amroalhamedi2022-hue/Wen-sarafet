package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import app.wensarafet.glass.WenFabBubbleDrawable;
import app.wensarafet.glass.WenFabOpticalCrownDrawable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Visual-only migration regression. These run on the disposable debug package
 * at final 2.0 QA; they NEVER open production financial data or build an APK.
 */
@RunWith(AndroidJUnit4.class)
public final class WenVisualMotionInstrumentedTest {
    private static void ui(Runnable body) {
        AtomicReference<Throwable> issue = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { body.run(); } catch (Throwable failure) { issue.set(failure); }
        });
        if (issue.get() != null) throw new AssertionError("Visual-only Android regression", issue.get());
    }

    private static void theme(int index, Runnable check) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("Tests must not run in the owner's production package",
                "app.wensarafet.tracker.recovered.debug", context.getPackageName());
        SharedPreferences prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        int old = prefs.getInt("theme", 0);
        try {
            Ui.saveTheme(context, index);
            Ui.applyTheme(context);
            check.run();
        } finally {
            Ui.saveTheme(context, old);
            Ui.applyTheme(context);
        }
    }

    private static MotionEvent touch(int action, float x, float y) {
        return MotionEvent.obtain(0L, 10L, action, x, y, 0);
    }

    private static FrameLayout host(Context context) {
        FrameLayout view = new FrameLayout(context);
        view.layout(0, 0, 1080, 1920);
        return view;
    }

    @Test public void fabOpticalCrownHasFinitePressAndTransparentCircularBounds() {
        ui(() -> theme(16, () -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TextView fab = new TextView(context);
            fab.setTag("wen_intro_fab");
            fab.setText("+");
            fab.setBackground(Ui.glassAccentBackground(context, 24));
            assertTrue(fab.getBackground() instanceof WenFabBubbleDrawable);
            AtomicInteger click = new AtomicInteger();
            fab.setOnClickListener(v -> click.incrementAndGet());
            fab.layout(0, 0, 160, 160);
            Ui.pressAnimation(fab);
            assertTrue("Optical FAB must keep its original click handler", fab.performClick());
            assertEquals(1, click.get());

            WenFabOpticalCrownDrawable crown = new WenFabOpticalCrownDrawable(
                    context.getResources().getDisplayMetrics().density);
            crown.setBounds(0, 0, 160, 160);
            Bitmap before = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
            crown.draw(new Canvas(before));
            crown.setPress(1f);
            Bitmap pressed = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
            crown.draw(new Canvas(pressed));
            assertEquals("Crown outside circle must be transparent", 0,
                    Color.alpha(before.getPixel(0, 0)));
            int changed = 0;
            int painted = 0;
            for (int y = 0; y < 160; y += 3) {
                for (int x = 0; x < 160; x += 3) {
                    if (before.getPixel(x, y) != 0) painted++;
                    if (before.getPixel(x, y) != pressed.getPixel(x, y)) changed++;
                }
            }
            assertTrue("Caustic must paint above the glyph", painted > 15);
            assertTrue("Press must visibly change only optical material", changed > 15);
            before.recycle();
            pressed.recycle();
        }));
    }

    @Test public void legacyThemeDoesNotBecomeCircularLiquidFab() {
        ui(() -> theme(0, () -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertFalse(Ui.isLiquidTheme(context));
            assertFalse(Ui.glassAccentBackground(context, 24)
                    instanceof WenFabBubbleDrawable);
        }));
    }

    @Test public void protectedFinancialScreensNeverAcceptHorizontalNavigation() {
        ui(() -> theme(16, () -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            WenScreenVisualMotion visual = new WenScreenVisualMotion(context);
            FrameLayout host = host(context);
            FrameLayout screen = host(context);
            host.addView(screen, new FrameLayout.LayoutParams(1080, 1920));
            screen.layout(0, 0, 1080, 1920);
            TextView[] tabs = {new TextView(context), new TextView(context),
                    new TextView(context), new TextView(context)};
            visual.captureOriginalTabs(tabs[0], tabs[1], tabs[2], tabs[3]);
            AtomicInteger cancelled = new AtomicInteger();
            MotionEvent down = touch(MotionEvent.ACTION_DOWN, 400f, 600f);
            MotionEvent move = touch(MotionEvent.ACTION_MOVE, 750f, 610f);
            try {
                assertFalse(visual.interceptSwipe(down, host, screen,
                        false, 1, ignored -> cancelled.incrementAndGet()));
                assertFalse(visual.interceptSwipe(move, host, screen,
                        false, 1, ignored -> cancelled.incrementAndGet()));
                assertEquals("Protected financial actions must not be cancelled",
                        0, cancelled.get());
                assertEquals(0f, screen.getTranslationX(), 0f);
            } finally {
                down.recycle();
                move.recycle();
            }
        }));
    }

    @Test public void validLiquidSwipeCancelsTheOldChildOnlyOnce() {
        ui(() -> theme(16, () -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            WenScreenVisualMotion visual = new WenScreenVisualMotion(context);
            FrameLayout host = host(context);
            FrameLayout screen = host(context);
            host.addView(screen, new FrameLayout.LayoutParams(1080, 1920));
            screen.layout(0, 0, 1080, 1920);
            TextView[] tabs = {new TextView(context), new TextView(context),
                    new TextView(context), new TextView(context)};
            visual.captureOriginalTabs(tabs[0], tabs[1], tabs[2], tabs[3]);
            AtomicInteger cancelled = new AtomicInteger();
            MotionEvent down = touch(MotionEvent.ACTION_DOWN, 390f, 540f);
            MotionEvent move = touch(MotionEvent.ACTION_MOVE, 780f, 540f);
            MotionEvent moveAgain = touch(MotionEvent.ACTION_MOVE, 815f, 544f);
            MotionEvent up = touch(MotionEvent.ACTION_UP, 830f, 545f);
            try {
                assertFalse(visual.interceptSwipe(down, host, screen, true, 1,
                        ignored -> cancelled.incrementAndGet()));
                assertTrue("Liquid horizontal intent must be recognized",
                        visual.interceptSwipe(move, host, screen, true, 1,
                                ignored -> cancelled.incrementAndGet()));
                assertTrue(visual.interceptSwipe(moveAgain, host, screen, true, 1,
                        ignored -> cancelled.incrementAndGet()));
                assertTrue(visual.interceptSwipe(up, host, screen, true, 1,
                        ignored -> cancelled.incrementAndGet()));
                assertEquals("No duplicate child cancellation/action", 1, cancelled.get());
            } finally {
                down.recycle();
                move.recycle();
                moveAgain.recycle();
                up.recycle();
            }
        }));
    }

    @Test public void gesturesOverButtonsOrEditableRowsNeverStealTheirClick() {
        ui(() -> theme(16, () -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            WenScreenVisualMotion visual = new WenScreenVisualMotion(context);
            FrameLayout host = host(context);
            FrameLayout screen = host(context);
            host.addView(screen, new FrameLayout.LayoutParams(1080, 1920));
            screen.layout(0, 0, 1080, 1920);
            EditText input = new EditText(context);
            // Full upper input surface has the same local/global hit test
            // even when the synthetic view is not attached to a Window.
            screen.addView(input, new FrameLayout.LayoutParams(900, 800));
            input.layout(0, 0, 900, 800);
            TextView[] tabs = {new TextView(context), new TextView(context),
                    new TextView(context), new TextView(context)};
            visual.captureOriginalTabs(tabs[0], tabs[1], tabs[2], tabs[3]);
            AtomicInteger cancelled = new AtomicInteger();
            MotionEvent down = touch(MotionEvent.ACTION_DOWN, 300f, 550f);
            MotionEvent move = touch(MotionEvent.ACTION_MOVE, 650f, 550f);
            try {
                assertFalse(visual.interceptSwipe(down, host, screen,
                        true, 1, ignored -> cancelled.incrementAndGet()));
                assertFalse("Swipe over an EditText must leave financial input alone",
                        visual.interceptSwipe(move, host, screen,
                                true, 1, ignored -> cancelled.incrementAndGet()));
                assertEquals(0, cancelled.get());
            } finally {
                down.recycle();
                move.recycle();
            }
        }));
    }
}
