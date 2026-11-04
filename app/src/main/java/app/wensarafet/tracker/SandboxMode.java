package app.wensarafet.tracker;
import android.content.Context;
import android.content.pm.ApplicationInfo;
/** Independent application identity: never enables access for original production package. */
final class SandboxMode {
    private static final String ID = "app.wensarafet.tracker.sandbox";
    private SandboxMode() {}
    static boolean isSandbox(Context c) {
        return ID.equals(c.getPackageName())
          && (c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    static boolean canRun(Context c) {
        return isSandbox(c) || LicenseManager.isActivated(c);
    }
}
