package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AppLockManager {
    private static final String PREFS = "app_lock";
    private AppLockManager() {}

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static boolean enabled(Context context) { return prefs(context).getBoolean("enabled", false); }
    public static boolean biometricEnabled(Context context) { return enabled(context) && prefs(context).getBoolean("biometric", false); }

    public static void setCode(Context context, String code, boolean biometric) {
        byte[] salt = new byte[24]; new SecureRandom().nextBytes(salt);
        byte[] hash = derive(code, salt);
        prefs(context).edit().putBoolean("enabled", true).putBoolean("biometric", biometric)
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP)).apply();
    }

    public static void setBiometric(Context context, boolean enabled) { prefs(context).edit().putBoolean("biometric", enabled).apply(); }
    public static void disable(Context context) { prefs(context).edit().clear().apply(); }

    public static boolean verify(Context context, String code) {
        try {
            byte[] salt = Base64.decode(prefs(context).getString("salt", ""), Base64.NO_WRAP);
            byte[] expected = Base64.decode(prefs(context).getString("hash", ""), Base64.NO_WRAP);
            byte[] actual = derive(code, salt);
            if (expected.length != actual.length) return false;
            int diff = 0; for (int i = 0; i < expected.length; i++) diff |= expected[i] ^ actual[i];
            return diff == 0;
        } catch (Exception ignored) { return false; }
    }

    private static byte[] derive(String code, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, 120_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}
