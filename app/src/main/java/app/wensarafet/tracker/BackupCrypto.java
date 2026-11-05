package app.wensarafet.tracker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class BackupCrypto {
    private static final byte[] MAGIC = new byte[]{'W','S','B','1'};
    private static final int ITERATIONS = 180_000;

    private BackupCrypto() {}

    static OutputStream encryptingStream(OutputStream destination, char[] password) throws Exception {
        byte[] salt = new byte[16], iv = new byte[12];
        SecureRandom random = new SecureRandom(); random.nextBytes(salt); random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
        DataOutputStream header = new DataOutputStream(new BufferedOutputStream(destination));
        header.write(MAGIC); header.write(salt); header.write(iv); header.flush();
        return new CipherOutputStream(header, cipher);
    }

    static InputStream decryptingStream(InputStream source, char[] password) throws Exception {
        DataInputStream header = new DataInputStream(new BufferedInputStream(source));
        byte[] magic = new byte[4]; header.readFully(magic);
        if (!Arrays.equals(MAGIC, magic)) throw new IllegalArgumentException("هذا الملف ليس نسخة وين صرفت؟");
        byte[] salt = new byte[16], iv = new byte[12]; header.readFully(salt); header.readFully(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), new GCMParameterSpec(128, iv));
        return new CipherInputStream(header, cipher);
    }

    private static SecretKey derive(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(key, "AES");
        } finally { spec.clearPassword(); }
    }
}
