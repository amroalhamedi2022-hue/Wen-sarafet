package app.wensarafet.tracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Verify a newly uploaded WSB archive by reading it back from its SAF provider.
 * Drain the entire GCM stream (including its authentication tag) before parsing
 * the ZIP and checking every entry's CRC. Never publish unverified ciphertext.
 */
final class BackupArchiveVerifier {
    private static final byte[] SQLITE_MAGIC =
            "SQLite format 3\u0000".getBytes(StandardCharsets.ISO_8859_1);

    private BackupArchiveVerifier() {}

    static void verify(InputStream encrypted, char[] password, File cacheDir) throws Exception {
        File plain = File.createTempFile("wen-verify-", ".zip", cacheDir);
        try {
            try (InputStream decrypted = BackupCrypto.decryptingStream(encrypted, password);
                 FileOutputStream output = new FileOutputStream(plain)) {
                BackupArchiveLimits.copy(decrypted, output, BackupArchiveLimits.MAX_ARCHIVE_BYTES);
                output.getFD().sync();
            }
            boolean database = false, settings = false;
            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(plain))) {
                ZipEntry entry;
                byte[] buffer = new byte[32 * 1024];
                int entryCount = 0;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entryCount > BackupArchiveLimits.MAX_ENTRIES)
                        throw new IllegalArgumentException("Too many backup entries");
                    String name = entry.getName();
                    if ("database.sqlite".equals(name)) {
                        if (database) throw new IllegalArgumentException("Duplicate backup database");
                        database = true;
                        byte[] header = new byte[SQLITE_MAGIC.length];
                        int filled = 0, count;
                        long databaseBytes = 0;
                        while ((count = zip.read(buffer)) != -1) {
                            if (count > BackupArchiveLimits.MAX_DATABASE_BYTES - databaseBytes)
                                throw new IllegalArgumentException("Backup database exceeds size limit");
                            databaseBytes += count;
                            int copied = Math.min(count, header.length - filled);
                            if (copied > 0) {
                                System.arraycopy(buffer, 0, header, filled, copied);
                                filled += copied;
                            }
                        }
                        if (filled != header.length || !Arrays.equals(header, SQLITE_MAGIC))
                            throw new IllegalArgumentException("Invalid backup SQLite header");
                    } else {
                        if ("settings.properties".equals(name)) {
                            if (settings) throw new IllegalArgumentException("Duplicate backup settings");
                            settings = true;
                            BackupArchiveLimits.copy(zip, null, BackupArchiveLimits.MAX_SETTINGS_BYTES);
                        } else {
                            BackupArchiveLimits.copy(zip, null, BackupArchiveLimits.MAX_EXTRA_BYTES);
                        }
                    }
                    zip.closeEntry();
                }
            }
            if (!database || !settings)
                throw new IllegalArgumentException("Incomplete encrypted backup archive");
        } finally {
            if (!plain.delete() && plain.exists()) plain.deleteOnExit();
        }
    }
}
