# Dark Crystal Widget Rebuild 2.2 — Porting boundary
- **Approved visual reference:** first Dark Crystal screenshot only.
- **Production source:** PR #50 at commit `1d724a9ec6f8d2eaef4043f925ee07407d5c32d6` — equivalent app UI/assets to signed v2.2 payload, with release manifest versionCode 30 and versionName 2.2 in actual signed APK.
- **Widget-only commit:** `92736ec827d09ccfa0ad6603084d43b9a7f57e8d` (and parent `678a9f78680539a581a0f88073f4ffdf29331d22`). It modifies only 9 widget visuals and QuickActionWidgetProvider (RemoteViews drawable selection).
- **Sandbox only:** this commit modifies build identity, manifest provider authority, test-only unlocked licensing and demo balances. **NEVER MERGE OR CHERRY-PICK SANDBOX COMMIT INTO OWNER'S PRODUCTION**.
- The following original source files MUST remain byte-identical when later porting widgets: MainActivity, Ui, NavigationVisualDecorator, NavigationLiquidMaterial, WenScreenVisualMotion, WenNavigationTrack, WenFabBubbleDrawable, all finance logic.
- Production package remains `app.wensarafet.tracker`. The test package is `app.wensarafet.tracker.sandbox` signed by Android debug key, not the original owner key. Android requires a signature even for sandbox installs.
- Debug-specific `WidgetPreviewActivity` provides test only screenshots with mock amounts. Original widget routes remain in existing app.
- Real-time launcher wallpaper blur cannot be reproduced by RemoteViews; transparent and specular optics approximate the **visual** effect.
- Source validation and Android Lint plus actual RemoteViews emulator screenshot are required prior to claiming visual acceptance.
