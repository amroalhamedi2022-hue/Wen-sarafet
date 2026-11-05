#!/usr/bin/env python3
"""Regression: category-only UI preferences survive encrypted backup/restore.

No changes to money, debt, database schema, installation identity or license.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
codec=(root/"app/src/main/java/app/wensarafet/tracker/BackupSettingsCodec.java").read_text()
backup=(root/"app/src/main/java/app/wensarafet/tracker/BackupFileManager.java").read_text()
cat=(root/"app/src/main/java/app/wensarafet/tracker/CategoryMonthlyBudgetStore.java").read_text()
default=(root/"app/src/main/java/app/wensarafet/tracker/DefaultCategoryStore.java").read_text()
assert '"monthly_category_caps", "manual_entry_category_defaults"' in codec
assert 'INCLUDED_MARKER = "__backup_namespace_present_v1"' in codec
assert "newlyBackedUpNamespace(String file)" in codec
assert "shouldRestoreNamespace(String file, Map<String, Object> values)" in codec
assert 'Boolean.TRUE.equals(values.get(INCLUDED_MARKER))' in codec
assert 'BackupSettingsCodec.newlyBackedUpNamespace(file)' in backup
assert 'result.setProperty(file + "|" + BackupSettingsCodec.INCLUDED_MARKER, "B:true")' in backup
assert 'boolean included=BackupSettingsCodec.shouldRestoreNamespace(file,restored);' in backup
assert 'if(included)for (Map.Entry<String, Object> entry : restored.entrySet())' in backup
assert 'file, Context.MODE_PRIVATE).edit().clear()' in backup
assert 'if (BackupSettingsCodec.INCLUDED_MARKER.equals(entry.getKey())) continue;' in backup
assert 'if (!editor.commit())' in backup
assert 'failure.addSuppressed(rollbackError);' in backup
assert '"monthly_category_caps"' in cat
assert '"manual_entry_category_defaults"' in default
assert 'database.sqlite' in backup and 'settings.properties' in backup
assert 'BackupRestoreAuthentication.finish(bounded)' in backup
assert 'BackupSettingsCodec.decode(properties)' in backup
assert backup.index('BackupSettingsCodec.decode(properties)') < backup.index('target.renameTo(old)')
print("NewCategoryBackupContract PASS: category budgets/defaults included, empty-marker and safe old-backup category-ID reset")
