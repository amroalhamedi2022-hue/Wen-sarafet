#!/usr/bin/env python3
"""C4: distinguish user snooze requests from the alarm's eventual delivery."""
from pathlib import Path
root = Path(__file__).resolve().parents[1] / "app/src/main/java/app/wensarafet/tracker"
receiver = (root / "ReminderReceiver.java").read_text()
scheduler = (root / "ReminderScheduler.java").read_text()

assert 'ACTION_SNOOZE_REQUEST = ' in scheduler
assert "setAction(ReminderScheduler.ACTION_SNOOZE_REQUEST)" in receiver
on_receive = receiver.split("void onReceive(Context context, Intent intent)", 1)[1].split(
    "private void show(Context context)", 1)[0]
request_branch = on_receive.split(
    "if (ReminderScheduler.ACTION_SNOOZE_REQUEST.equals(action)) {", 1)[1].split(
    "\n            return;\n        }", 1)[0]
assert "ReminderScheduler.scheduleSnooze(context);" in request_branch
assert "getPendingExpenseClassificationCount() > 0" in request_branch
assert "notifications.cancel(8301)" in request_branch
assert "show(context)" not in request_branch
assert "show(context);" in on_receive.split(
    "if (!ReminderScheduler.ACTION_DAILY.equals(action)", 1)[1]
assert 'prefs(context).getBoolean("enabled", false)' in on_receive
assert 'ACTION_DAILY.equals(action)' in on_receive
assert "if (pending != null) {" in scheduler
assert "manager.cancel(pending);" in scheduler
assert "pending.cancel();" in scheduler
assert "notifications.cancel(8301);" in scheduler
assert "Math.max(1, Math.min(1440" in scheduler
assert "new String[]{ACTION_DAILY, ACTION_SNOOZE}" in scheduler
print("ReminderSnoozeContract passed: deferred action, disabled state, both alarms and stale notification.")
