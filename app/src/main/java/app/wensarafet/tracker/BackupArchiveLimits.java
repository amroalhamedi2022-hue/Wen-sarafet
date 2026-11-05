package app.wensarafet.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Bounds untrusted decrypted backup content before it can fill device storage
 * or exhaust heap. Limits are deliberately generous for ordinary finance data;
 * a larger database must be handled explicitly, never silently truncated.
 */
final class BackupArchiveLimits {
    static final long MAX_DATABASE_BYTES = 1024L * 1024 * 1024;
    static final long MAX_ARCHIVE_BYTES = 1280L * 1024 * 1024;
    static final long MAX_SETTINGS_BYTES = 2L * 1024 * 1024;
    static final long MAX_EXTRA_BYTES = 2L * 1024 * 1024;
    static final int MAX_ENTRIES = 16;

    private BackupArchiveLimits() {}

    static long copy(InputStream source, OutputStream destination, long maxBytes)
            throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("Negative archive size limit");
        byte[] buffer = new byte[32 * 1024];
        long total = 0;
        int count;
        while ((count = source.read(buffer)) != -1) {
            if (count == 0) continue;
            if (count > maxBytes - total)
                throw new IOException("Backup content exceeds safe size limit");
            if (destination != null) destination.write(buffer, 0, count);
            total += count;
        }
        return total;
    }

    static InputStream bounded(InputStream source, long maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("Negative archive size limit");
        return new FilterInputStream(source) {
            private long remaining = maxBytes;

            @Override public int read() throws IOException {
                int value = super.read();
                if (value != -1) {
                    if (remaining == 0)
                        throw new IOException("Backup archive exceeds safe size limit");
                    remaining--;
                }
                return value;
            }

            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                if (length == 0) return 0;
                if (remaining == 0) {
                    if (super.read() == -1) return -1;
                    throw new IOException("Backup archive exceeds safe size limit");
                }
                int count = super.read(bytes, offset, (int) Math.min(length, remaining));
                if (count > 0) remaining -= count;
                return count;
            }
        };
    }

    static Properties readSettings(InputStream source) throws IOException {
        // Properties.load(InputStream) has no size limit; never feed it an
        // unbounded ZIP entry controlled by an imported backup.
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        copy(source, data, MAX_SETTINGS_BYTES);
        Properties result = new Properties();
        result.load(new ByteArrayInputStream(data.toByteArray()));
        return result;
    }
}
