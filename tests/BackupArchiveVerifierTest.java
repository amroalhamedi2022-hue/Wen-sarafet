package app.wensarafet.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupArchiveVerifierTest {
    private static final char[] PASSWORD = "private-test-password".toCharArray();

    private static byte[] archive(boolean db, boolean settings, boolean duplicate, boolean invalidHeader)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(BackupCrypto.encryptingStream(bytes, PASSWORD))) {
            if (db) {
                zip.putNextEntry(new ZipEntry("database.sqlite"));
                zip.write((invalidHeader ? "not a database" : "SQLite format 3\u0000")
                        .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                zip.write(new byte[]{1, 2, 3});
                zip.closeEntry();
                if (duplicate) {
                    // Java ZipOutputStream forbids duplicate names; the verifier
                    // also guards this case for provider-supplied ZIPs.
                    zip.putNextEntry(new ZipEntry("extra.txt"));
                    zip.write(1);
                    zip.closeEntry();
                }
            }
            if (settings) {
                zip.putNextEntry(new ZipEntry("settings.properties"));
                zip.write("appearance|mode=S:ZGFyaw==\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] oversizedEntry(String name) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(BackupCrypto.encryptingStream(bytes, PASSWORD))) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            zip.write("SQLite format 3".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            zip.write(0);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(name));
            byte[] block = new byte[8192];
            long remaining = BackupArchiveLimits.MAX_SETTINGS_BYTES + 1;
            while (remaining > 0) {
                int count = (int) Math.min(block.length, remaining);
                zip.write(block, 0, count);
                remaining -= count;
            }
            zip.closeEntry();
            if (!"settings.properties".equals(name)) {
                zip.putNextEntry(new ZipEntry("settings.properties"));
                zip.write("appearance|mode=S:ZGFyaw==\\n"
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    // A ZIP with valid local file records but no end-of-central-directory
    // was previously accepted by ZipInputStream. It is not a restorable WSB.
    private static byte[] encryptedIncompleteCentralDirectory() throws Exception {
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(plain)) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            zip.write("SQLite format 3\u0000".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("settings.properties"));
            zip.write("appearance|mode=S:ZGFyaw==\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        // Standard ZIP with no archive comment ends in the 22-byte EOCD.
        byte[] valid = plain.toByteArray();
        if (valid.length < 22 || valid[valid.length - 22] != 0x50
                || valid[valid.length - 21] != 0x4b
                || valid[valid.length - 20] != 0x05
                || valid[valid.length - 19] != 0x06)
            throw new AssertionError("Expected ZIP end-of-central-directory");
        return encryptPlain(Arrays.copyOf(valid, valid.length - 22));
    }

    // Tamper only with the central-directory CRC, keeping compressed payload
    // and AES-GCM valid. ZipFile InputStream does not itself enforce entry CRC.
    private static byte[] encryptedCentralDirectoryCrcMismatch() throws Exception {
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(plain)) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            zip.write("SQLite format 3\u0000".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("settings.properties"));
            zip.write("appearance|mode=S:ZGFyaw==\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        byte[] bytes = plain.toByteArray();
        boolean changed = false;
        for (int i = 0; i + 20 < bytes.length; i++) {
            if (bytes[i] == 0x50 && bytes[i + 1] == 0x4b
                    && bytes[i + 2] == 0x01 && bytes[i + 3] == 0x02) {
                bytes[i + 16] ^= 1; // CRC32 in central directory header
                changed = true;
                break;
            }
        }
        if (!changed) throw new AssertionError("Missing ZIP central directory");
        return encryptPlain(bytes);
    }

    // A malicious archive can repeat an optional filename; reject ambiguous ZIPs.
    private static byte[] encryptedDuplicateOptionalName() throws Exception {
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(plain)) {
            for (String name : new String[]{"database.sqlite", "settings.properties", "extra-a.txt", "extra-b.txt"}) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(name.equals("database.sqlite")
                        ? "SQLite format 3\u0000".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)
                        : new byte[]{1, 2, 3});
                zip.closeEntry();
            }
        }
        byte[] bytes = plain.toByteArray();
        byte[] oldName = "extra-b.txt".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] newName = "extra-a.txt".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int matches = 0;
        for (int i = 0; i <= bytes.length - oldName.length; i++) {
            boolean equal = true;
            for (int j = 0; j < oldName.length; j++) {
                if (bytes[i + j] != oldName[j]) { equal = false; break; }
            }
            if (equal) {
                System.arraycopy(newName, 0, bytes, i, newName.length);
                matches++;
            }
        }
        if (matches != 2) throw new AssertionError("Expected local and central ZIP filenames");
        return encryptPlain(bytes);
    }

    private static byte[] encryptPlain(byte[] plain) throws Exception {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (java.io.OutputStream output = BackupCrypto.encryptingStream(encrypted, PASSWORD)) {
            output.write(plain);
        }
        return encrypted.toByteArray();
    }

    private static void passes(File dir, byte[] bytes) throws Exception {
        BackupArchiveVerifier.verify(new ByteArrayInputStream(bytes), PASSWORD, dir);
        try (java.util.stream.Stream<java.nio.file.Path> paths = Files.list(dir.toPath())) {
            if (paths.findAny().isPresent())
                throw new AssertionError("Plaintext verification cache must be removed");
        }
    }

    private static void rejects(File dir, byte[] bytes, char[] password) throws Exception {
        try {
            BackupArchiveVerifier.verify(new ByteArrayInputStream(bytes), password, dir);
        } catch (Exception expected) {
            try (java.util.stream.Stream<java.nio.file.Path> paths = Files.list(dir.toPath())) {
                if (paths.findAny().isPresent())
                    throw new AssertionError("Failed verification left plaintext cache");
            }
            return;
        }
        throw new AssertionError("Expected corrupt backup rejection");
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("wen-archive-verifier-test").toFile();
        try {
            byte[] good = archive(true, true, false, false);
            passes(dir, good);
            // Optional entries with distinct names must remain valid; the
            // duplicate-name guard must not reject ordinary extra files.
            passes(dir, archive(true, true, true, false));
            rejects(dir, archive(false, true, false, false), PASSWORD);
            rejects(dir, archive(true, false, false, false), PASSWORD);
            rejects(dir, archive(true, true, false, true), PASSWORD);
            rejects(dir, oversizedEntry("settings.properties"), PASSWORD);
            rejects(dir, oversizedEntry("unrecognized-extra.bin"), PASSWORD);
            rejects(dir, encryptedIncompleteCentralDirectory(), PASSWORD);
            rejects(dir, encryptedCentralDirectoryCrcMismatch(), PASSWORD);
            rejects(dir, encryptedDuplicateOptionalName(), PASSWORD);
            rejects(dir, good, "wrong-password".toCharArray());
            rejects(dir, Arrays.copyOf(good, good.length - 12), PASSWORD);
            byte[] tampered = good.clone();
            tampered[tampered.length - 1] ^= 1;
            rejects(dir, tampered, PASSWORD);
            System.out.println("BackupArchiveVerifierTest passed: authenticated read-back, complete ZIP directory, entry CRC, cleanup");
        } finally {
            if (!dir.delete()) throw new AssertionError("Verifier temp directory not cleaned");
        }
    }
}
