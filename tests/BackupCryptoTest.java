package app.wensarafet.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BackupCryptoTest {
    public static void main(String[] args) throws Exception {
        byte[] original = "بيانات مالية تجريبية — Wen Sarafet".getBytes(StandardCharsets.UTF_8);
        char[] password = "correct-horse-2026".toCharArray();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (OutputStream output = BackupCrypto.encryptingStream(encrypted, password)) { output.write(original); }

        byte[] decoded;
        try (InputStream input = BackupCrypto.decryptingStream(new ByteArrayInputStream(encrypted.toByteArray()), password)) {
            decoded = readAll(input);
        }
        check(Arrays.equals(original, decoded), "backup roundtrip mismatch");
        check(fails(encrypted.toByteArray(), "wrong-password".toCharArray()), "wrong password was accepted");

        byte[] modified = encrypted.toByteArray().clone(); modified[modified.length - 1] ^= 1;
        check(fails(modified, password), "tampered backup was accepted");
        System.out.println("BackupCryptoTest: all checks passed");
    }

    private static boolean fails(byte[] value, char[] password) {
        try (InputStream input = BackupCrypto.decryptingStream(new ByteArrayInputStream(value), password)) {
            readAll(input); return false;
        } catch (Exception expected) { return true; }
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[1024]; int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
