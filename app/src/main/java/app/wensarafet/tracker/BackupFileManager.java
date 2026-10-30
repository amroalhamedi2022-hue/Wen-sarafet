package app.wensarafet.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupFileManager {
    private static final String[] PREF_FILES = {"appearance", "daily_reminder", "monthly_budgets", "transfer_names", "notification_preferences"};

    private BackupFileManager() {}

    public static void create(Context context, OutputStream destination, char[] password) throws Exception {
        File database = context.getDatabasePath("wen_sarafet.db");
        if (!database.isFile()) throw new IllegalStateException("قاعدة البيانات غير موجودة");
        File snapshot = new File(context.getCacheDir(), "backup-snapshot.db");
        snapshot.delete();
        SQLiteDatabase live = SQLiteDatabase.openDatabase(database.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        try {
            live.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close();
            live.execSQL("VACUUM INTO " + DatabaseUtils.sqlEscapeString(snapshot.getAbsolutePath()));
        } finally { live.close(); }
        try (ZipOutputStream zip = new ZipOutputStream(BackupCrypto.encryptingStream(destination, password))) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            try (FileInputStream databaseInput = new FileInputStream(snapshot)) { copy(databaseInput, zip); }
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("settings.properties"));
            Properties properties = collectPreferences(context);
            properties.store(zip, "Wen Sarafet backup settings");
            zip.closeEntry(); zip.finish();
        } finally { snapshot.delete(); }
    }

    public static void restore(Context context, InputStream source, char[] password) throws Exception {
        ZipInputStream zip = new ZipInputStream(BackupCrypto.decryptingStream(source, password));
        File temp = new File(context.getCacheDir(), "restore-" + System.currentTimeMillis() + ".db");
        Properties properties = new Properties(); boolean foundDatabase = false;
        try (ZipInputStream inputZip = zip) {
            ZipEntry entry;
            while ((entry = inputZip.getNextEntry()) != null) {
                if ("database.sqlite".equals(entry.getName())) {
                    FileOutputStream databaseOutput = new FileOutputStream(temp);
                    try { copy(inputZip, databaseOutput); } finally { databaseOutput.close(); }
                    foundDatabase = true;
                } else if ("settings.properties".equals(entry.getName())) {
                    properties.load(inputZip);
                }
                inputZip.closeEntry();
            }
        } catch (Exception error) {
            temp.delete();
            throw new IllegalArgumentException("النسخة تالفة أو كلمة السر غير صحيحة", error);
        }
        // Never replace a working ledger on the strength of the SQLite magic header alone.
        // Validate the decrypted snapshot before touching the currently installed database.
        try { validateSqlite(temp); }
        catch (RuntimeException error) { temp.delete(); throw error; }
        File target = context.getDatabasePath("wen_sarafet.db");
        boolean hadOldDatabase = target.exists();
        // A previous interrupted restore may have left a valuable rollback copy.
        // Do not delete or overwrite that copy on the next restore attempt.
        File old = new File(target.getParentFile(),
                "wen_sarafet.pre_restore." + System.currentTimeMillis() + ".db");
        if (old.exists()) { temp.delete(); throw new IllegalStateException("ملف الاستعادة الاحتياطي موجود بالفعل"); }
        try {
            if (hadOldDatabase) checkpointLiveDatabase(target);
        } catch (RuntimeException error) { temp.delete(); throw error; }
        if (hadOldDatabase && !target.renameTo(old)) { temp.delete(); throw new IllegalStateException("تعذر تجهيز قاعدة البيانات للاستعادة"); }
        try {
            if (!temp.renameTo(target)) {
                FileInputStream input = new FileInputStream(temp); FileOutputStream output = new FileOutputStream(target);
                try { copy(input, output); } finally { input.close(); output.close(); }
            }
            new File(target.getPath() + "-wal").delete(); new File(target.getPath() + "-shm").delete(); new File(target.getPath() + "-journal").delete();
            FinanceDbHelper check = new FinanceDbHelper(context); check.getAccounts(); check.close();
            restorePreferences(context, properties);
            old.delete();
        } catch (Exception error) {
            target.delete();
            if (hadOldDatabase && !old.renameTo(target))
                throw new IllegalStateException("فشلت الاستعادة وتعذر إرجاع قاعدة البيانات السابقة", error);
            throw error;
        } finally { temp.delete(); }
    }

    private static Properties collectPreferences(Context context) {
        Properties result = new Properties();
        for (String file : PREF_FILES) for (Map.Entry<String, ?> item : context.getSharedPreferences(file, Context.MODE_PRIVATE).getAll().entrySet()) {
            Object value = item.getValue(); String encoded;
            if (value instanceof Boolean) encoded = "B:" + value;
            else if (value instanceof Integer) encoded = "I:" + value;
            else if (value instanceof Long) encoded = "L:" + value;
            else if (value instanceof Float) encoded = "F:" + value;
            else if (value instanceof String) encoded = "S:" + Base64.encodeToString(((String) value).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            else continue;
            result.setProperty(file + "|" + item.getKey(), encoded);
        }
        return result;
    }

    private static void restorePreferences(Context context, Properties properties) {
        for (String file : PREF_FILES) context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().apply();
        for (String joined : properties.stringPropertyNames()) {
            int split = joined.indexOf('|'); if (split <= 0) continue;
            String file = joined.substring(0, split), key = joined.substring(split + 1), value = properties.getProperty(joined);
            if (!Arrays.asList(PREF_FILES).contains(file) || value == null || value.length() < 2) continue;
            SharedPreferences.Editor editor = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit();
            String raw = value.substring(2);
            try {
                if (value.startsWith("B:")) editor.putBoolean(key, Boolean.parseBoolean(raw));
                else if (value.startsWith("I:")) editor.putInt(key, Integer.parseInt(raw));
                else if (value.startsWith("L:")) editor.putLong(key, Long.parseLong(raw));
                else if (value.startsWith("F:")) editor.putFloat(key, Float.parseFloat(raw));
                else if (value.startsWith("S:")) editor.putString(key, new String(Base64.decode(raw, Base64.NO_WRAP), StandardCharsets.UTF_8));
                editor.apply();
            } catch (Exception ignored) {}
        }
    }

    private static void validateSqlite(File file) {
        if (!validSqlite(file)) throw new IllegalArgumentException("النسخة لا تحتوي قاعدة SQLite سليمة");
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
            int version = database.getVersion();
            if (version < 1 || version > FinanceDbHelper.DB_VERSION)
                throw new IllegalArgumentException("إصدار قاعدة البيانات غير مدعوم للاستعادة: " + version);
            try (Cursor integrity = database.rawQuery("PRAGMA integrity_check", null)) {
                if (!integrity.moveToFirst() || !"ok".equalsIgnoreCase(integrity.getString(0))
                        || integrity.moveToNext())
                    throw new IllegalArgumentException("قاعدة البيانات في النسخة الاحتياطية تالفة");
            }
            try (Cursor foreignKeys = database.rawQuery("PRAGMA foreign_key_check", null)) {
                if (foreignKeys.moveToFirst())
                    throw new IllegalArgumentException("النسخة الاحتياطية تحتوي مراجع مالية غير سليمة");
            }
            try (Cursor tables = database.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' " +
                            "AND name IN ('accounts','categories','transactions','transaction_splits')", null)) {
                if (!tables.moveToFirst() || tables.getInt(0) != 4)
                    throw new IllegalArgumentException("النسخة لا تحتوي جداول التطبيق الأساسية");
            }
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("تعذر فحص قاعدة البيانات في النسخة الاحتياطية", error);
        }
    }

    private static void checkpointLiveDatabase(File target) {
        // Closing the Activity's helper normally checkpoints WAL, but verify any
        // remaining committed WAL pages are durable before moving the live .db.
        if (!new File(target.getPath() + "-wal").exists()) return;
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(
                target.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
             Cursor checkpoint = database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
            if (!checkpoint.moveToFirst() || checkpoint.getInt(0) != 0)
                throw new IllegalStateException("قاعدة البيانات قيد الاستخدام؛ تعذرت الاستعادة بأمان");
        }
    }

    private static boolean validSqlite(File file) {
        try {
            byte[] head = new byte[16]; FileInputStream input = new FileInputStream(file); int read = input.read(head); input.close();
            return read == 16 && "SQLite format 3\u0000".equals(new String(head, StandardCharsets.ISO_8859_1));
        } catch (Exception ignored) { return false; }
    }

    private static void copy(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[32 * 1024]; int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        output.flush();
    }
}
