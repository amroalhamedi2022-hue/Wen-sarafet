package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.text.InputType;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import app.wensarafet.glass.GlassMotion;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Real Android View/Editable/TextWatcher regression tests. Uses independent
 * synthetic TextViews and no MainActivity, login, account, transaction or DB.
 * The user's real 1.8.8 installation and money are never involved.
 */
@RunWith(AndroidJUnit4.class)
public final class SplitRemainderInstrumentedTest {
    private static EditText field(Context context, boolean splitAmount) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (splitAmount) input.setTag(SplitRemainderUi.AMOUNT_FIELD_TAG);
        return input;
    }

    private static LinearLayout card(Context context, EditText amount) {
        LinearLayout result = new LinearLayout(context);
        result.addView(amount);
        return result;
    }

    private static void runUi(Runnable task) {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { task.run(); } catch (Throwable error) { exception.set(error); }
        });
        if (exception.get() != null)
            throw new AssertionError("Android UI regression", exception.get());
    }

    @Test public void addEditDeleteSplitFieldsRecomputeWithoutSaving() {
        runUi(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout rows = new LinearLayout(context);
            EditText total = field(context, false);
            EditText first = field(context, true);
            EditText last = field(context, true);
            total.setText("100.00");
            LinearLayout firstCard = card(context, first);
            LinearLayout secondCard = card(context, last);
            rows.addView(firstCard);
            rows.addView(secondCard);

            SplitRemainderUi.bind(rows, () -> {
                Long parsed = Money.parseMinor(total.getText().toString());
                return parsed == null ? -1L : parsed;
            }, total);
            first.setText("40.00");
            assertEquals("60.00", last.getText().toString());
            assertNull("Calculated final field cannot accept manual digits", last.getKeyListener());
            assertFalse(last.isFocusable());

            first.setText("55.50");
            assertEquals("44.50", last.getText().toString());
            total.setText("120.00");
            assertEquals("64.50", last.getText().toString());

            EditText third = field(context, true);
            LinearLayout thirdCard = card(context, third);
            rows.addView(thirdCard);
            SplitRemainderUi.refresh(rows);
            assertEquals("A former automatic amount must not become a stale input", "", last.getText().toString());
            assertTrue(last.isFocusable());
            assertNotNull(last.getKeyListener());
            assertNull(third.getKeyListener());
            last.setText("20.00");
            assertEquals("44.50", third.getText().toString());

            first.setText("130.00");
            assertEquals("", third.getText().toString());
            assertNotNull("Over-allocation must be rejected visually", third.getError());
            first.setText("50.00");
            assertEquals("50.00", third.getText().toString());

            rows.removeView(thirdCard);
            SplitRemainderUi.refresh(rows);
            assertEquals("70.00", last.getText().toString());
            assertNull(last.getKeyListener());
            assertFalse(last.isFocusable());
        });
    }

    @Test public void existingTransactionSplitStartsCorrectAndRemainsExact() {
        runUi(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout rows = new LinearLayout(context);
            EditText first = field(context, true);
            EditText second = field(context, true);
            first.setText("33.33");
            second.setText("66.67");
            rows.addView(card(context, first));
            rows.addView(card(context, second));
            SplitRemainderUi.bind(rows, () -> 10000L, null);
            assertEquals("66.67", second.getText().toString());
            assertNull(second.getKeyListener());
            first.setText("33.34");
            assertEquals("66.66", second.getText().toString());
        });
    }

    private static void navTouch(TextView tab, int action, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
        try {
            assertTrue("Original navigation button should own the gesture", tab.dispatchTouchEvent(event));
        } finally {
            event.recycle();
        }
    }

    /** Isolated RTL navigation using the ORIGINAL clickable tab Views and handlers. */
    @Test public void rtlNavigationTapDragCancelPreservesExactlyOneOriginalCallback() {
        runUi(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            FrameLayout track = new FrameLayout(context);
            LinearLayout strip = new LinearLayout(context);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView[] tabs = new TextView[4];
            int[] clicks = new int[4];
            int[] handoffs = {0};
            for (int i = 0; i < tabs.length; i++) {
                final int index = i;
                tabs[i] = new TextView(context);
                tabs[i].setText("tab " + i);
                tabs[i].setContentDescription("original tab " + i);
                tabs[i].setOnClickListener(v -> clicks[index]++);
                strip.addView(tabs[i], new LinearLayout.LayoutParams(100, 80));
            }
            FrameLayout lens = new FrameLayout(context);
            track.addView(lens, new FrameLayout.LayoutParams(100, 80));
            track.addView(strip, new FrameLayout.LayoutParams(400, 80));
            track.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY));
            track.layout(0, 0, 400, 80);
            assertTrue("RTL must position first tab on right",
                    tabs[0].getLeft() > tabs[3].getLeft());
            for (int i = 0; i < tabs.length; i++) {
                GlassMotion.bindNavigationTab(tabs[i], track, lens, tabs,
                        0, i, 0, () -> handoffs[0]++);
            }
            navTouch(tabs[0], MotionEvent.ACTION_DOWN, 50f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_UP, 50f, 40f);
            assertEquals("Selected-tab refresh", 1, clicks[0]);
            assertEquals(0, handoffs[0]);

            navTouch(tabs[0], MotionEvent.ACTION_DOWN, 50f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_MOVE, -250f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_UP, -250f, 40f);
            assertEquals("Only destination fires", 1, clicks[3]);
            assertEquals("One handoff", 1, handoffs[0]);
            assertEquals("No redundant selected refresh", 1, clicks[0]);

            navTouch(tabs[0], MotionEvent.ACTION_DOWN, 50f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_MOVE, -250f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_MOVE, 50f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_UP, 50f, 40f);
            assertEquals("Return drag must not refresh", 1, clicks[0]);
            assertEquals("Return drag must not navigate", 1, handoffs[0]);

            navTouch(tabs[0], MotionEvent.ACTION_DOWN, 50f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_MOVE, -250f, 40f);
            navTouch(tabs[0], MotionEvent.ACTION_CANCEL, -250f, 40f);
            assertEquals("Cancellation must not navigate", 1, handoffs[0]);
            assertEquals("Cancellation must not click", 1, clicks[3]);

            navTouch(tabs[3], MotionEvent.ACTION_DOWN, 50f, 40f);
            navTouch(tabs[3], MotionEvent.ACTION_UP, 50f, 40f);
            assertEquals(2, clicks[3]);
            assertEquals(2, handoffs[0]);
            assertEquals(0, clicks[1]);
            assertEquals(0, clicks[2]);
        });
    }

    @Test public void invalidSplitInputClearsAndRecoversCalculatedLastPart() {
        runUi(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout rows = new LinearLayout(context);
            EditText total = field(context, false);
            EditText first = field(context, true);
            EditText remainder = field(context, true);
            total.setText("100.01");
            rows.addView(card(context, first));
            rows.addView(card(context, remainder));
            SplitRemainderUi.bind(rows, () -> {
                Long value = Money.parseMinor(total.getText().toString());
                return value == null ? -1L : value;
            }, total);
            first.setText("40.02");
            assertEquals("59.99", remainder.getText().toString());
            first.setText("101");
            assertEquals("", remainder.getText().toString());
            assertNotNull(remainder.getError());
            first.setText("40.02");
            assertEquals("59.99", remainder.getText().toString());
            assertNull(remainder.getError());
            total.setText("");
            assertEquals("", remainder.getText().toString());
            assertNotNull(remainder.getError());
            total.setText("100.01");
            assertEquals("59.99", remainder.getText().toString());
            assertNull(remainder.getError());
            assertFalse(remainder.isFocusable());
        });
    }
}
