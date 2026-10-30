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
            zip.write("SQLite format 3\\u0000".replace("\\u0000", "\u0000")
                    .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
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
                zip.write("appearance|mode=S:ZGFyaw==\\n".replace("\\n", "\n")
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
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
            rejects(dir, archive(false, true, false, false), PASSWORD);
            rejects(dir, archive(true, false, false, false), PASSWORD);
            rejects(dir, archive(true, true, false, true), PASSWORD);
            rejects(dir, oversizedEntry("settings.properties"), PASSWORD);
            rejects(dir, oversizedEntry("unrecognized-extra.bin"), PASSWORD);
            rejects(dir, good, "wrong-password".toCharArray());
            rejects(dir, Arrays.copyOf(good, good.length - 12), PASSWORD);
            byte[] tampered = good.clone();
            tampered[tampered.length - 1] ^= 1;
            rejects(dir, tampered, PASSWORD);
            System.out.println("BackupArchiveVerifierTest passed: authenticated read-back, required entries, cleanup");
        } finally {
            if (!dir.delete()) throw new AssertionError("Verifier temp directory not cleaned");
        }
    }
}
