package app.wensarafet.tracker;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public final class LicenseProtocolTest {
    public static void main(String[] args) throws Exception {
        KeyPair licenseAuthority = installationKey();
        String publicKey = Base64.getEncoder().encodeToString(licenseAuthority.getPublic().getEncoded());
        KeyPair installationA = installationKey();
        String idA = installationId(installationA);
        String valid = issue(idA, "Amro", licenseAuthority.getPrivate());
        check(LicenseProtocol.verify(valid, idA, publicKey), "valid license rejected");

        check(!LicenseProtocol.verify("not-a-license", idA, publicKey), "random code accepted");
        check(!LicenseProtocol.verify(tamperPayload(valid), idA, publicKey), "tampered payload accepted");
        check(!LicenseProtocol.verify(tamperSignature(valid), idA, publicKey), "tampered signature accepted");

        KeyPair installationB = installationKey();
        String idB = installationId(installationB);
        check(!LicenseProtocol.verify(valid, idB, publicKey), "license copied to another device accepted");
        check(LicenseProtocol.verify(valid, installationId(installationA), publicKey), "license failed after restart simulation");

        KeyPair reinstall = installationKey();
        check(!LicenseProtocol.verify(valid, installationId(reinstall), publicKey), "license survived reinstall identity change");
        System.out.println("LicenseProtocolTest: all checks passed");
    }

    private static KeyPair installationKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return generator.generateKeyPair();
    }

    private static String installationId(KeyPair key) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(key.getPublic().getEncoded()));
    }

    private static String issue(String installationId, String customer, PrivateKey key) throws Exception {
        String name = Base64.getUrlEncoder().withoutPadding().encodeToString(customer.getBytes(StandardCharsets.UTF_8));
        String payloadText = "WSL2|" + installationId + "|PERMANENT|1760000000000|ABCDEF12|" + name;
        byte[] payload = payloadText.getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withECDSA"); signer.initSign(key); signer.update(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "." +
                Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    }

    private static String tamperPayload(String token) {
        String[] parts = token.split("\\.", 2); byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
        payload[payload.length - 1] ^= 1;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "." + parts[1];
    }

    private static String tamperSignature(String token) {
        String[] parts = token.split("\\.", 2); byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
        signature[signature.length - 1] ^= 1;
        return parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
