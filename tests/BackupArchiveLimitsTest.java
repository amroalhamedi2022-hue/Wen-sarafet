package app.wensarafet.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class BackupArchiveLimitsTest {
    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private static void rejects(ByteArrayInputStream input, long max) throws Exception {
        try {
            BackupArchiveLimits.copy(input, null, max);
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Oversized entry accepted");
    }

    public static void main(String[] args) throws Exception {
        byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream copied = new ByteArrayOutputStream();
        check(BackupArchiveLimits.copy(new ByteArrayInputStream(data), copied, 10) == 10,
                "Exact limit must be accepted");
        check(java.util.Arrays.equals(data, copied.toByteArray()), "Bytes changed");
        rejects(new ByteArrayInputStream(data), 9);
        check(BackupArchiveLimits.copy(new ByteArrayInputStream(new byte[0]), null, 0) == 0,
                "Empty entry rejected");
        ByteArrayInputStream exact = new ByteArrayInputStream(data);
        java.io.InputStream bounded = BackupArchiveLimits.bounded(exact, 10);
        ByteArrayOutputStream boundedResult = new ByteArrayOutputStream();
        check(BackupArchiveLimits.copy(bounded, boundedResult, 10) == 10,
                "Exact archive cap must be accepted");
        check(java.util.Arrays.equals(data, boundedResult.toByteArray()),
                "Bounded archive bytes changed");
        try {
            BackupArchiveLimits.copy(BackupArchiveLimits.bounded(
                    new ByteArrayInputStream(data), 9), null, 10);
            throw new AssertionError("Oversized whole archive accepted");
        } catch (IOException expected) { }
        Properties p = BackupArchiveLimits.readSettings(new ByteArrayInputStream(
                "appearance|mode=S:ZGFyaw==\n".getBytes(StandardCharsets.US_ASCII)));
        check("S:ZGFyaw==".equals(p.getProperty("appearance|mode")), "Settings changed");
        try {
            BackupArchiveLimits.readSettings(new ByteArrayInputStream(
                    new byte[(int) BackupArchiveLimits.MAX_SETTINGS_BYTES + 1]));
            throw new AssertionError("Oversized settings accepted");
        } catch (IOException expected) {
            // Reject before Properties.load can allocate unbounded memory.
        }
        System.out.println("BackupArchiveLimitsTest passed: exact boundary, overflow, settings cap");
    }
}
