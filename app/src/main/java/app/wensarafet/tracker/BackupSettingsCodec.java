package app.wensarafet.tracker;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Decode all backed-up preferences before replacing the live finance database.
 * No Android dependency: malformed settings are covered by ordinary JVM tests.
 */
final class BackupSettingsCodec {
    static final String[] PREF_FILES = {
            "appearance", "daily_reminder", "monthly_budgets",
            "transfer_names", "notification_preferences"
    };

    private BackupSettingsCodec() {}

    static Map<String, Map<String, Object>> decode(Properties properties) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String file : PREF_FILES) result.put(file, new LinkedHashMap<>());

        for (String joined : properties.stringPropertyNames()) {
            int separator = joined.indexOf('|');
            if (separator <= 0) continue;
            String file = joined.substring(0, separator);
            Map<String, Object> values = result.get(file);
            // Future/unknown preference namespaces must not overwrite local data.
            if (values == null) continue;
            String key = joined.substring(separator + 1);
            String encoded = properties.getProperty(joined);
            if (key.isEmpty() || encoded == null || encoded.length() < 2
                    || encoded.charAt(1) != ':')
                throw new IllegalArgumentException("Invalid backup preference: " + joined);
            try {
                String raw = encoded.substring(2);
                Object decoded;
                switch (encoded.charAt(0)) {
                    case 'B':
                        if (!"true".equals(raw) && !"false".equals(raw))
                            throw new IllegalArgumentException("Invalid boolean");
                        decoded = Boolean.parseBoolean(raw);
                        break;
                    case 'I': decoded = Integer.parseInt(raw); break;
                    case 'L': decoded = Long.parseLong(raw); break;
                    case 'F':
                        float number = Float.parseFloat(raw);
                        if (Float.isNaN(number) || Float.isInfinite(number))
                            throw new IllegalArgumentException("Non-finite float");
                        decoded = number;
                        break;
                    case 'S':
                        decoded = new String(Base64.getDecoder().decode(raw),
                                StandardCharsets.UTF_8);
                        break;
                    default: throw new IllegalArgumentException("Unsupported preference type");
                }
                values.put(key, decoded);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Invalid backup preference: " + joined, error);
            }
        }
        return result;
    }
}
