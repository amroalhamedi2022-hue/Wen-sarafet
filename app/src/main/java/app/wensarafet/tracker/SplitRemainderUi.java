package app.wensarafet.tracker;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * Only controls category split amount text fields; never changes a saved amount.
 * Existing 1.8.8 validation and the database transaction remain untouched.
 */
public final class SplitRemainderUi {
    static final String AMOUNT_FIELD_TAG = "wen_auto_split_amount";
    private SplitRemainderUi() {}

    static void bind(LinearLayout rows, LongSupplier total, EditText editableTotal) {
        if (rows == null || total == null) return;
        Controller controller = new Controller(rows, total);
        rows.setTag(controller); // existing row container has no other tag
        if (editableTotal != null) {
            editableTotal.addTextChangedListener(controller.watcher());
        }
        controller.refresh();
    }

    static void refresh(LinearLayout rows) {
        if (rows == null) return;
        Object owner = rows.getTag();
        if (owner instanceof Controller) ((Controller) owner).refresh();
    }

    private static final class Controller {
        private final LinearLayout rows;
        private final LongSupplier total;
        private final List<EditText> amountFields = new ArrayList<>();
        private final IdentityHashMap<EditText, TextWatcher> watched = new IdentityHashMap<>();
        private EditText derived;
        private KeyListener originalListener;
        private boolean restoring;

        Controller(LinearLayout rows, LongSupplier total) {
            this.rows = rows;
            this.total = total;
        }

        private TextWatcher watcher() {
            return new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!restoring) update();
                }
                @Override public void afterTextChanged(Editable s) {}
            };
        }

        void refresh() {
            amountFields.clear();
            collect(rows);
            for (EditText field : amountFields) {
                if (!watched.containsKey(field)) {
                    TextWatcher observer = watcher();
                    watched.put(field, observer);
                    field.addTextChangedListener(observer);
                }
            }
            EditText next = amountFields.size() >= 2
                    ? amountFields.get(amountFields.size() - 1) : null;
            if (derived != next) {
                if (derived != null) {
                    derived.setKeyListener(originalListener);
                    derived.setFocusable(true);
                    derived.setFocusableInTouchMode(true);
                    derived.setCursorVisible(true);
                    derived.setHint("مبلغ الجزء");
                    derived.setError(null);
                }
                derived = next;
                originalListener = derived == null ? null : derived.getKeyListener();
                if (derived != null) {
                    derived.setKeyListener(null);
                    derived.setFocusable(false);
                    derived.setFocusableInTouchMode(false);
                    derived.setCursorVisible(false);
                    derived.setHint("المتبقي يُحسب تلقائيًا");
                    derived.setContentDescription("مبلغ الجزء الأخير يُحسب تلقائيًا من المبلغ المتبقي");
                }
            }
            update();
        }

        private void collect(View root) {
            if (root instanceof EditText && AMOUNT_FIELD_TAG.equals(root.getTag())) {
                amountFields.add((EditText) root);
            } else if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i));
            }
        }

        private void update() {
            if (restoring || derived == null) return;
            String calculated = "";
            String problem = null;
            try {
                long totalMinor = total.getAsLong();
                List<Long> preceding = new ArrayList<>();
                for (int i = 0; i < amountFields.size() - 1; i++)
                    preceding.add(Money.parseMinor(amountFields.get(i).getText().toString()));
                long remaining = SplitRemainderRules.lastPart(totalMinor, preceding);
                calculated = String.format(Locale.ROOT, "%d.%02d", remaining / 100, remaining % 100);
            } catch (IllegalArgumentException | ArithmeticException invalid) {
                problem = invalid.getMessage();
            }
            restoring = true;
            try {
                if (!calculated.contentEquals(derived.getText())) derived.setText(calculated);
                derived.setError(problem);
            } finally {
                restoring = false;
            }
        }
    }
}
