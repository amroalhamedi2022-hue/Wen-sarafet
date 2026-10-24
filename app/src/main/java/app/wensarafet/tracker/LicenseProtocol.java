package app.wensarafet.tracker;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class LicenseProtocol {
    private LicenseProtocol() {}

    static boolean verify(String token, String expectedFingerprint, String publicKeyBase64) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) return false;
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[1]);
            String text = new String(payload, StandardCharsets.UTF_8);
            String[] fields = text.split("\\|", -1);
            if (fields.length != 6 || !"WSL2".equals(fields[0]) || !expectedFingerprint.equals(fields[1]) ||
                    !"PERMANENT".equals(fields[2])) return false;
            long issuedAt = Long.parseLong(fields[3]);
            if (issuedAt <= 0 || fields[4].length() < 6) return false;
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
            java.security.Signature verifier = java.security.Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key); verifier.update(payload);
            return verifier.verify(signatureBytes);
        } catch (Exception ignored) { return false; }
    }
}
