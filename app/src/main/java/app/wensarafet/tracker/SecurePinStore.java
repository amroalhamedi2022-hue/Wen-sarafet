package app.wensarafet.tracker;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecurePinStore {
    private static final String KEY_ALIAS = "wen_sarafet_ussd_pins";
    private static final String PREFS = "secure_ussd";

    private SecurePinStore() {}

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            generator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public static void save(Context context, String name, String pin) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
        String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(name, value).apply();
    }

    public static String read(Context context, String name) {
        try {
            String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(name, null);
            if (value == null) return null;
            String[] parts = value.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }
}
