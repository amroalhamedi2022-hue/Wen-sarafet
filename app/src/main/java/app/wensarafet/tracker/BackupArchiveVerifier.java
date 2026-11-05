package app.wensarafet.tracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Verify a newly uploaded WSB archive by reading it back from its SAF provider.
 * First authenticate the entire AES-GCM ciphertext. Then require a complete ZIP
 * central directory and independently check each entry's uncompressed size/CRC.
 *
 * ZipInputStream alone accepts local entries even when the central directory or
 * end-of-central-directory record is missing, so it cannot approve publication.
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
            // ZipFile requires the central directory; ZipInputStream does not.
            try (ZipFile zip = new ZipFile(plain)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                int entryCount = 0;
                Set<String> seenNames = new HashSet<>();
                while (entries.hasMoreElements()) {
                    if (++entryCount > BackupArchiveLimits.MAX_ENTRIES)
                        throw new IllegalArgumentException("Too many backup entries");
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!seenNames.add(name))
                        throw new IllegalArgumentException("Duplicate backup ZIP entry: " + name);
                    if ("database.sqlite".equals(name)) {
                        if (database) throw new IllegalArgumentException("Duplicate backup database");
                        database = true;
                        inspect(zip, entry, BackupArchiveLimits.MAX_DATABASE_BYTES, true);
                    } else if ("settings.properties".equals(name)) {
                        if (settings) throw new IllegalArgumentException("Duplicate backup settings");
                        settings = true;
                        inspect(zip, entry, BackupArchiveLimits.MAX_SETTINGS_BYTES, false);
                    } else {
                        inspect(zip, entry, BackupArchiveLimits.MAX_EXTRA_BYTES, false);
                    }
                }
            }
            if (!database || !settings)
                throw new IllegalArgumentException("Incomplete encrypted backup archive");
        } finally {
            if (!plain.delete() && plain.exists()) plain.deleteOnExit();
        }
    }

    private static void inspect(ZipFile zip, ZipEntry entry, long limit, boolean sqlite)
            throws Exception {
        long declaredSize = entry.getSize();
        long declaredCrc = entry.getCrc();
        if (declaredSize < 0 || declaredSize > limit || declaredCrc < 0)
            throw new IllegalArgumentException("Invalid backup ZIP entry metadata");
        byte[] header = sqlite ? new byte[SQLITE_MAGIC.length] : null;
        int filled = 0;
        long actualSize = 0;
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[32 * 1024];
        try (InputStream input = zip.getInputStream(entry)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) continue;
                if (count > limit - actualSize)
                    throw new IllegalArgumentException("Backup ZIP entry exceeds size limit");
                actualSize += count;
                crc.update(buffer, 0, count);
                if (sqlite && filled < header.length) {
                    int copied = Math.min(count, header.length - filled);
                    System.arraycopy(buffer, 0, header, filled, copied);
                    filled += copied;
                }
            }
        }
        if (actualSize != declaredSize || crc.getValue() != declaredCrc)
            throw new IllegalArgumentException("Backup ZIP entry size/CRC mismatch");
        if (sqlite && (filled != header.length || !Arrays.equals(header, SQLITE_MAGIC)))
            throw new IllegalArgumentException("Invalid backup SQLite header");
    }
}
