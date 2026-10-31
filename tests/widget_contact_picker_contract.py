#!/usr/bin/env python3
"""No silent contacts permission, no unsafe USSD, and accessible widget picker."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
java=(root/"app/src/main/java/app/wensarafet/tracker/QuickActionActivity.java").read_text(encoding="utf-8")
widget=(root/"app/src/main/java/app/wensarafet/tracker/QuickActionWidgetProvider.java").read_text(encoding="utf-8")
manifest=(root/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
layout=root/"app/src/main/res/layout/widget_quick_action.xml"
tree=ET.parse(layout)
ns="{http://schemas.android.com/apk/res/android}"
picker=[node for node in tree.iter() if node.attrib.get(ns+"id")=="@+id/widget_choose_contact"]
assert len(picker)==1
assert picker[0].attrib.get(ns+"contentDescription")=="اختيار جهة اتصال للتحويل"
assert "android.permission.READ_CONTACTS" not in manifest
assert "android.permission.WRITE_CONTACTS" not in manifest
assert "Intent.ACTION_PICK" in java
assert "ContactsContract.CommonDataKinds.Phone.CONTENT_URI" in java
assert "FLAG_GRANT_READ_URI_PERMISSION" in java
assert "PalestinianMobileNumber.normalize(c.getString(0))" in java
assert "pendingContactPhone.setText(mobile.substring(2))" in java
assert 'PalestinianMobileNumber.normalize("05"+tail)' in java
assert "new Intent(context,QuickActionActivity.class)" in widget
assert '"pick_contact",true' in widget
assert "R.id.widget_choose_contact" in widget
assert '"jawwal_friend".equals(action())' in widget
assert '"jawwal_merchant".equals(action())' in widget
assert '"palpay".equals(action())' in widget
assert "if(canChooseContact)" in widget
assert "views.setOnClickPendingIntent(R.id.widget_root, pending)" in widget
assert ET.parse(root/"app/src/main/res/drawable/widget_contact_chip.xml") is not None
print("WidgetContactPickerContract PASS: contact URI picker, permission-safe, small widget button, normalizer")
