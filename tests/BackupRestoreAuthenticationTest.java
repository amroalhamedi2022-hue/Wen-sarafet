package app.wensarafet.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Regression: parsing a ZIP is not proof that the full AES-GCM tag was read. */
public final class BackupRestoreAuthenticationTest {
    private static final char[] PASSWORD = "test-only-backup-password".toCharArray();

    private static byte[] encryptedZip() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(
                BackupCrypto.encryptingStream(output, PASSWORD))) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            zip.write("SQLite format 3\u0000".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("settings.properties"));
            zip.write("appearance|mode=S:ZGFyaw==\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static void verify(byte[] encrypted) throws Exception {
        try (InputStream decrypted = BackupCrypto.decryptingStream(
                    new ByteArrayInputStream(encrypted), PASSWORD);
             ZipInputStream zip = new ZipInputStream(decrypted)) {
            int entries = 0;
            byte[] buffer = new byte[512];
            while (zip.getNextEntry() != null) {
                while (zip.read(buffer) != -1) { }
                zip.closeEntry();
                entries++;
            }
            if (entries != 2) throw new AssertionError("Unexpected ZIP entries");
            BackupRestoreAuthentication.finish(decrypted);
        }
    }

    private static void rejects(byte[] encrypted) throws Exception {
        try { verify(encrypted); }
        catch (Exception expected) { return; }
        throw new AssertionError("Tampered or truncated GCM archive was accepted");
    }

    public static void main(String[] args) throws Exception {
        byte[] good = encryptedZip();
        verify(good);
        byte[] tampered = good.clone();
        tampered[tampered.length - 1] ^= 0x40;
        rejects(tampered);
        rejects(Arrays.copyOf(good, good.length - 8));
        System.out.println("BackupRestoreAuthenticationTest passed: complete ZIP, GCM tag and truncation");
    }
}
