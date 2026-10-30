package app.wensarafet.tracker;

import android.content.Context;
public final class LicenseManager {
    private static final String PREFS = "license_state";
    private static final String TOKEN = "activation_token";
    private static final String PUBLIC_KEY_B64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGVnutmCujTtBpHTZiQdqvIXjSwrW/antkIFVVqu0VcZzsDqFtc4LHYVrQkWSKhDOV7LQCNQvu+V2cp3GMA6OWg==";

    private LicenseManager() {}

    public static String requestCode(Context context) {
        try {
            if (!AppIntegrity.isTrusted(context)) throw new IllegalStateException("تعذر التحقق من سلامة نسخة التطبيق");
            if (!InstallationIdentity.provesPossession(context) && !InstallationIdentity.provesPossession(context))
                throw new IllegalStateException("تعذر استخدام مفتاح هوية التثبيت");
            return InstallationIdentity.requestCode(context);
        } catch (Exception error) { throw new IllegalStateException("تعذر إنشاء هوية التثبيت", error); }
    }

    public static boolean isActivated(Context context) {
        if (!AppIntegrity.isTrusted(context) || !InstallationIdentity.provesPossession(context)) return false;
        String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TOKEN, null);
        return token != null && verify(context, token);
    }

    public static boolean activate(Context context, String token) {
        String clean = token == null ? "" : token.trim().replace("\n", "").replace("\r", "").replace(" ", "");
        if (!AppIntegrity.isTrusted(context) || !InstallationIdentity.provesPossession(context) || !verify(context, clean)) return false;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(TOKEN, clean).apply();
        return true;
    }

    private static boolean verify(Context context, String token) {
        try { return LicenseProtocol.verify(token, InstallationIdentity.installationId(context), PUBLIC_KEY_B64); }
        catch (Exception ignored) { return false; }
    }

    public static boolean integrityValid(Context context) { return AppIntegrity.isTrusted(context); }

    public static String identitySecurityLabel(Context context) {
        return InstallationIdentity.securityLabel(context);
    }
}
