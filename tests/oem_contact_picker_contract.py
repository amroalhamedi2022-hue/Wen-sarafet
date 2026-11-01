#!/usr/bin/env python3
"""OEM contact picker regression for Android 13 and Xiaomi/HyperOS.

The UI only reads the one selected contact phone URI. No new account, contact,
database, pin, password, or finance permission should be introduced.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
source=(root/"app/src/main/java/app/wensarafet/tracker/QuickActionActivity.java").read_text(encoding="utf-8")
manifest=(root/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
number=(root/"app/src/main/java/app/wensarafet/tracker/PalestinianMobileNumber.java").read_text(encoding="utf-8")
widget=(root/"app/src/main/java/app/wensarafet/tracker/QuickActionWidgetProvider.java").read_text(encoding="utf-8")
pick=source.split("private void openContactPicker()",1)[1].split(
    "@Override protected void onActivityResult",1)[0]
result=source.split("@Override protected void onActivityResult",1)[1].split(
    "private EditText field(",1)[0]

assert "private static final int CONTACT_PICK_REQUEST=421;" in source
assert "if(getIntent().getBooleanExtra(\"pick_contact\",false))" in source
assert 'phone.post(this::openContactPicker)' in source
assert "new Intent(context,QuickActionActivity.class)" in widget
assert '"pick_contact",true' in widget
assert "startActivityForResult(choice,CONTACT_PICK_REQUEST)" in pick

# Android's recommended MIME-based ACTION_PICK FIRST, for nonstandard OEM
# contacts apps fall back to GET_CONTENT and URI-based pickers.
primary="new Intent(Intent.ACTION_PICK)\n                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)"
get_item="new Intent(Intent.ACTION_GET_CONTENT)\n                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)"
legacy="new Intent(Intent.ACTION_PICK,\n                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI)"
get_collection="new Intent(Intent.ACTION_GET_CONTENT)\n                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)"
for required in (primary,get_item,legacy,get_collection):
    assert required in pick,required
assert pick.index(primary)<pick.index(get_item)<pick.index(legacy)<pick.index(get_collection)
assert "startActivityForResult(choice,CONTACT_PICK_REQUEST)" in pick
assert "catch(RuntimeException error)" in pick
assert 'choice.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)' in pick
assert "new AlertDialog.Builder(this)" in pick, "clear no-handler error"
assert "لم يوفّر حاليًا نافذة متوافقة" in pick
assert "isFinishing()" in pick and "isDestroyed()" in pick
assert "if(!LicenseManager.isActivated(this))" in source

assert "getContentResolver().query(data.getData(),null,null,null,null)" in result
assert "c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)" in result
assert "if(numberIndex<0)" in result
assert "PalestinianMobileNumber.normalize(c.getString(numberIndex))" in result
assert "pendingContactPhone.setText(mobile.substring(2))" in result
assert "int nameIndex=c.getColumnIndex(" in result
assert "nameIndex>=0?c.getString(nameIndex):null" in result
assert "selectedContactLabel.setVisibility(View.VISIBLE)" in result
assert "selectedContactLabel.setVisibility(View.GONE)" in source
assert 'PalestinianMobileNumber.normalize("05"+tail)' in source
assert 'raw.matches("05[0-9]{8}")' in number
assert 'raw.startsWith("059") || raw.startsWith("056")' in number

android="{http://schemas.android.com/apk/res/android}"
element=ET.fromstring(manifest)
assert not any((p.attrib.get(android+"name") or "").endswith(
    ("READ_CONTACTS","WRITE_CONTACTS","QUERY_ALL_PACKAGES")) for p in element.findall("uses-permission"))
queries=element.find("queries")
assert queries is not None
choices={(i.find("action").attrib.get(android+"name"),
          i.find("data").attrib.get(android+"mimeType"))
         for i in queries.findall("intent")}
assert ("android.intent.action.PICK","vnd.android.cursor.dir/phone_v2") in choices
assert ("android.intent.action.GET_CONTENT","vnd.android.cursor.item/phone_v2") in choices
assert 'android:exported="false"' in manifest
assert 'android:authorities="app.wensarafet.tracker.notification-sounds"' in manifest
print("OEMContactPickerContract PASS: MIME picker, OEM fallback, selected-only read, number normalization, no broad contacts access")
