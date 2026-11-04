package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.text.InputType;
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
}
