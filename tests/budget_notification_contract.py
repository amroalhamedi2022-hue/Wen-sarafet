#!/usr/bin/env python3
"""Protect monthly budget alerts against denied notification channels and stale edit data."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
budget=(root/"app/src/main/java/app/wensarafet/tracker/BudgetAlertManager.java").read_text()
db=(root/"app/src/main/java/app/wensarafet/tracker/FinanceDbHelper.java").read_text()
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text()
assert 'if(notifyExceeded(context, currency, spent, budget))' in budget
assert 'preferences.edit().putString("notified_" + currency, monthKey).apply();' in budget
assert 'manager.areNotificationsEnabled()' in budget
assert 'channel.getImportance() == NotificationManager.IMPORTANCE_NONE' in budget
assert 'catch(SecurityException permissionRevoked)' in budget
assert 'return false;' in budget.split('catch(SecurityException permissionRevoked)',1)[1]
assert 'if(oldAmount==newAmountMinor && oldNote.equals(cleanNote))' in db
assert 'oldAmount!=expectedAmountMinor' in db
assert 'writable.setTransactionSuccessful();' in db
assert 'showSimpleManualTransactionEditDialog(tx)' in ui
assert 'BudgetAlertManager.checkAll(this);' in ui
print("BudgetNotificationContract PASS: denied channel/permission cannot consume monthly alert, no-op edit guard.")
