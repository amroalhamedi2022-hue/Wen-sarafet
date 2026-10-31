#!/usr/bin/env python3
"""Sound provider exposes only original tones, preserving user toggles and protected data."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
provider=(root/"app/src/main/java/app/wensarafet/tracker/NotificationSoundProvider.java").read_text(encoding="utf-8")
prefs=(root/"app/src/main/java/app/wensarafet/tracker/NotificationPreferences.java").read_text(encoding="utf-8")
ui=(root/"app/src/main/java/app/wensarafet/tracker/MainActivity.java").read_text(encoding="utf-8")
manifest=ET.parse(root/"app/src/main/AndroidManifest.xml")
android="{http://schemas.android.com/apk/res/android}"
entry=[p for p in manifest.iter("provider") if p.attrib.get(android+"name")==".NotificationSoundProvider"]
assert len(entry)==1
assert entry[0].attrib.get(android+"authorities")=="app.wensarafet.tracker.notification-sounds"
assert entry[0].attrib.get(android+"exported")=="true"
assert entry[0].attrib.get(android+"grantUriPermissions")=="true"
assert 'if(!"r".equals(mode) || !isOurSound(uri))' in provider
assert 'ParcelFileDescriptor.MODE_READ_ONLY' in provider
assert 'AUTHORITY.equals(uri.getAuthority())' in provider
assert "valid(uri.getLastPathSegment())" in provider
assert '"review".equals(type)' in provider
assert '"saved".equals(type)' in provider
assert '"reminder".equals(type)' in provider
assert '"budget".equals(type)' in provider
assert '"sync".equals(type)' in provider
for forbidden in ("FinanceDbHelper","SharedPreferences","getDatabasePath","getCacheDir","getExternalStorage",
                  "SecurePinStore","LicenseManager","getContacts","READ_CONTACTS"):
    assert forbidden not in provider,forbidden
assert "useDistinctiveSounds(Context context)" in prefs
assert "NotificationSoundProvider.ensureSound(context,type)" in prefs
assert 'edit.putString(type+"_sound_uri"' in prefs
assert "if (sound) {" in prefs
assert "if(NotificationSoundProvider.isOurSound(selected))" in prefs
assert "channel.setSound(null, null)" in prefs
assert 'actionButton("🎵 تفعيل 5 أصوات هادئة ومميزة"' in ui
assert "NotificationPreferences.useDistinctiveSounds(this)" in ui
assert "لن تُفعّل الأصوات التي عطّلتها يدويًا تلقائيًا" in ui
print("DistinctiveNotificationSoundContract PASS: explicit opt-in, sound-only provider, user mute preserved")
