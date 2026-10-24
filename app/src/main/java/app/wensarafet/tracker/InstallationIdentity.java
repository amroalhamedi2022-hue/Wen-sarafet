package app.wensarafet.tracker;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

final class InstallationIdentity {
    private static final String PROVIDER = "AndroidKeyStore";
    private static final String ALIAS = "wen_sarafet_installation_identity_v2";
    private static final String REQUEST_PREFIX = "WSR2-";

    private InstallationIdentity() {}

    static String requestCode(Context context) throws Exception {
        return REQUEST_PREFIX + b64(keyPair().getPublic().getEncoded());
    }

    static String installationId(Context context) throws Exception {
        return b64(MessageDigest.getInstance("SHA-256").digest(keyPair().getPublic().getEncoded()));
    }

    static boolean provesPossession(Context context) {
        try {
            KeyPair pair = keyPair();
            byte[] challenge = MessageDigest.getInstance("SHA-256").digest(
                    (context.getPackageName() + "|" + b64(pair.getPublic().getEncoded())).getBytes(StandardCharsets.UTF_8));
            java.security.Signature signer = java.security.Signature.getInstance("SHA256withECDSA");
            signer.initSign(pair.getPrivate()); signer.update(challenge); byte[] proof = signer.sign();
            java.security.Signature verifier = java.security.Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(pair.getPublic()); verifier.update(challenge);
            boolean valid = verifier.verify(proof);
            if (!valid) deleteIdentity();
            return valid;
        } catch (Exception ignored) {
            try { deleteIdentity(); } catch (Exception ignoredAgain) {}
            return false;
        }
    }

    static String securityLabel(Context context) {
        try {
            PrivateKey privateKey = keyPair().getPrivate();
            KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), PROVIDER);
            KeyInfo info = factory.getKeySpec(privateKey, KeyInfo.class);
            int level = info.getSecurityLevel();
            if (level == KeyProperties.SECURITY_LEVEL_STRONGBOX) return "محمي داخل StrongBox";
            if (level == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT) return "محمي داخل عتاد الجهاز";
            return "محمي داخل Android Keystore";
        } catch (Exception ignored) { return "محمي داخل Android Keystore"; }
    }

    private static synchronized KeyPair keyPair() throws Exception {
        KeyStore store = KeyStore.getInstance(PROVIDER); store.load(null);
        if (store.containsAlias(ALIAS)) {
            PublicKey publicKey = store.getCertificate(ALIAS).getPublicKey();
            PrivateKey privateKey = (PrivateKey) store.getKey(ALIAS, null);
            if (publicKey != null && privateKey != null) return new KeyPair(publicKey, privateKey);
            store.deleteEntry(ALIAS);
        }
        try { return generate(true); }
        catch (StrongBoxUnavailableException unavailable) { return generate(false); }
        catch (java.security.InvalidAlgorithmParameterException unsupported) { return generate(false); }
        catch (java.security.ProviderException unavailable) { return generate(false); }
    }

    private static KeyPair generate(boolean strongBox) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER);
        KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false);
        if (strongBox) spec.setIsStrongBoxBacked(true);
        generator.initialize(spec.build());
        return generator.generateKeyPair();
    }

    private static void deleteIdentity() throws Exception {
        KeyStore store = KeyStore.getInstance(PROVIDER); store.load(null);
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS);
    }

    private static String b64(byte[] value) {
        return Base64.encodeToString(value, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
