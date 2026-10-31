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
        // New UI-only budget and preferred-category settings must round-trip,
        // but legacy backups without their namespace markers must preserve
        // newer preferences rather than wiping them.
        Properties ownerBackup = new Properties();
        ownerBackup.setProperty("monthly_category_caps|category_7_ILS", "L:125000");
        ownerBackup.setProperty("monthly_category_caps|" + BackupSettingsCodec.INCLUDED_MARKER, "B:true");
        ownerBackup.setProperty("manual_entry_category_defaults|default_EXPENSE", "L:7");
        ownerBackup.setProperty("manual_entry_category_defaults|" + BackupSettingsCodec.INCLUDED_MARKER, "B:true");
        Map<String, Map<String, Object>> restored = BackupSettingsCodec.decode(ownerBackup);
        check(Long.valueOf(125000).equals(restored.get("monthly_category_caps").get("category_7_ILS")),
                "Saved category budget amount lost");
        check(Long.valueOf(7).equals(restored.get("manual_entry_category_defaults").get("default_EXPENSE")),
                "Saved category preference lost");
        check(BackupSettingsCodec.shouldRestoreNamespace("monthly_category_caps",
                restored.get("monthly_category_caps")), "new budget archive should restore");
        check(BackupSettingsCodec.shouldRestoreNamespace("manual_entry_category_defaults",
                restored.get("manual_entry_category_defaults")), "new default archive should restore");
        Properties emptyNewBackup = new Properties();
        emptyNewBackup.setProperty("monthly_category_caps|" + BackupSettingsCodec.INCLUDED_MARKER,
                "B:true");
        Map<String, Map<String, Object>> decodedEmpty = BackupSettingsCodec.decode(emptyNewBackup);
        check(BackupSettingsCodec.shouldRestoreNamespace("monthly_category_caps",
                decodedEmpty.get("monthly_category_caps")), "explicit empty budget should clear on restore");
        Map<String, Map<String, Object>> legacy = BackupSettingsCodec.decode(new Properties());
        check(!BackupSettingsCodec.shouldRestoreNamespace("monthly_category_caps",
                legacy.get("monthly_category_caps")), "old backup must be treated as having no category budgets");
        check(!BackupSettingsCodec.shouldRestoreNamespace("manual_entry_category_defaults",
                legacy.get("manual_entry_category_defaults")), "old backup must be treated as having no default category");
        check(BackupSettingsCodec.shouldRestoreNamespace("appearance",
                legacy.get("appearance")), "legacy original settings remain compatible");
        System.out.println("BackupSettingsCodecTest passed: typed roundtrip, Arabic, unknown namespace, malformed values");
    }
}
