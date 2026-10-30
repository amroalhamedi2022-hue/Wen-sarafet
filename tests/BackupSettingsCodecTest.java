package app.wensarafet.tracker;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

public final class BackupSettingsCodecTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        try {
            BackupSettingsCodec.decode(properties);
            throw new AssertionError("Invalid setting was accepted: " + key + "=" + value);
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains(key), "Missing failing key in error");
        }
    }

    public static void main(String[] args) {
        Properties settings = new Properties();
        String arabic = "لي ٢٥ شيكل";
        settings.setProperty("appearance|theme", "S:" + Base64.getEncoder()
                .encodeToString(arabic.getBytes(StandardCharsets.UTF_8)));
        settings.setProperty("appearance|dark", "B:true");
        settings.setProperty("daily_reminder|hour", "I:17");
        settings.setProperty("monthly_budgets|limit", "L:9223372036854775807");
        settings.setProperty("notification_preferences|volume", "F:0.75");
        settings.setProperty("future_namespace|future", "X:unrecognized");
        Map<String, Map<String, Object>> decoded = BackupSettingsCodec.decode(settings);
        check(arabic.equals(decoded.get("appearance").get("theme")), "UTF-8 string");
        check(Boolean.TRUE.equals(decoded.get("appearance").get("dark")), "Boolean");
        check(Integer.valueOf(17).equals(decoded.get("daily_reminder").get("hour")), "Integer");
        check(Long.valueOf(Long.MAX_VALUE).equals(decoded.get("monthly_budgets").get("limit")), "Long");
        check(Float.valueOf(0.75f).equals(decoded.get("notification_preferences").get("volume")), "Float");
        check(decoded.size() == BackupSettingsCodec.PREF_FILES.length, "Known namespaces only");
        check(decoded.get("appearance").size() == 2, "No cross-file setting leakage");

        rejects("appearance|dark", "B:maybe");
        rejects("appearance|dark", "B:");
        rejects("daily_reminder|hour", "I:99999999999999");
        rejects("monthly_budgets|limit", "L:not-a-number");
        rejects("notification_preferences|volume", "F:NaN");
        rejects("notification_preferences|volume", "F:Infinity");
        rejects("appearance|theme", "S:not base64!!");
        rejects("appearance|theme", "Q:unexpected");
        rejects("appearance|", "S:YWJj");
        rejects("appearance|theme", "missing-prefix");

        check(BackupSettingsCodec.decode(new Properties()).get("appearance").isEmpty(),
                "Empty but present settings should decode safely");
        System.out.println("BackupSettingsCodecTest passed: typed roundtrip, Arabic, unknown namespace, malformed values");
    }
}
