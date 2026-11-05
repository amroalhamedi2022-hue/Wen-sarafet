package app.wensarafet.tracker;

import java.io.IOException;
import java.io.InputStream;

/**
 * ZipInputStream stops at the ZIP central directory, which can be before the
 * AES-GCM authentication tag. Drain the underlying decrypted stream to EOF
 * before accepting the restored database. Never infer authentication from a
 * successful ZIP parse or a stream close.
 */
final class BackupRestoreAuthentication {
    private BackupRestoreAuthentication() {}

    static void finish(InputStream decrypted) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        while (decrypted.read(buffer) != -1) {
            // Intentionally discard ZIP central-directory bytes; reaching EOF
            // forces CipherInputStream to authenticate the GCM tag.
        }
    }
}
