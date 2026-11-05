package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class TransferNameStore {
    private static final String PREFS = "transfer_names";
    private static final String KEY = "names";
    private static final List<String> DEFAULTS = Arrays.asList(
            "amro ibrahim sewilm alhamidi", "amro ibrahim sewilm alhmidi",
            "عمرو ابراهيم سويلم الحميدي", "عمرو إبراهيم سويلم الحميدي");

    private TransferNameStore() {}

    public static List<String> get(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY, null);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (stored == null) values.addAll(DEFAULTS);
        else for (String line : stored.split("\\n")) if (!line.trim().isEmpty()) values.add(line.trim());
        return new ArrayList<>(values);
    }

    public static void add(Context context, String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.length() < 3) throw new IllegalArgumentException("اكتب الاسم كاملًا");
        List<String> values = get(context); if (!values.contains(clean)) values.add(clean); save(context, values);
    }

    public static void remove(Context context, String name) {
        List<String> values = get(context); values.remove(name); save(context, values);
    }

    private static void save(Context context, List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) { if (joined.length() > 0) joined.append('\n'); joined.append(value); }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, joined.toString()).apply();
    }
}
