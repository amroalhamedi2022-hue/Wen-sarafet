package app.wensarafet.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.security.MessageDigest;

final class AppIntegrity {
    private static final String PACKAGE_NAME = "app.wensarafet.tracker";
    private static final byte[] RELEASE_CERT_SHA256 = hex(
            "6B8268E5B647B12A2AA055D7A22F7B8EA888E40FB863E74409938AAD4EC789C9");

    private AppIntegrity() {}

    static boolean isTrusted(Context context) {
        try {
            if (!PACKAGE_NAME.equals(context.getPackageName())) return false;
            ApplicationInfo app = context.getApplicationInfo();
            if ((app.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) return false;
            android.content.pm.Signature[] signatures = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo.getApkContentsSigners();
            if (signatures == null || signatures.length != 1) return false;
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            return MessageDigest.isEqual(RELEASE_CERT_SHA256, actual);
        } catch (Exception ignored) { return false; }
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++)
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return result;
    }
}
