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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupFileManager {
    private static final String[] PREF_FILES = BackupSettingsCodec.PREF_FILES;

    private BackupFileManager() {}

    public static synchronized void create(Context context, OutputStream destination, char[] password) throws Exception {
        File database = context.getDatabasePath("wen_sarafet.db");
        if (!database.isFile()) throw new IllegalStateException("قاعدة البيانات غير موجودة");
        // A fixed cache filename lets concurrent manual/Drive backups delete or
        // overwrite one another. Reserve a unique name, then remove the empty
        // placeholder because SQLite VACUUM INTO requires a nonexistent target.
        File snapshot = File.createTempFile("wen-backup-", ".db", context.getCacheDir());
        try {
            if (!snapshot.delete()) throw new IllegalStateException("تعذر تجهيز ملف النسخة المؤقت");
            SQLiteDatabase live = SQLiteDatabase.openDatabase(database.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            try {
                live.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close();
                live.execSQL("VACUUM INTO " + DatabaseUtils.sqlEscapeString(snapshot.getAbsolutePath()));
            } finally { live.close(); }
            if (snapshot.length() > BackupArchiveLimits.MAX_DATABASE_BYTES)
                throw new IllegalStateException("قاعدة البيانات أكبر من الحد الآمن للنسخة الاحتياطية");
            try (ZipOutputStream zip = new ZipOutputStream(BackupCrypto.encryptingStream(destination, password))) {
            zip.putNextEntry(new ZipEntry("database.sqlite"));
            try (FileInputStream databaseInput = new FileInputStream(snapshot)) {
                BackupArchiveLimits.copy(databaseInput, zip, BackupArchiveLimits.MAX_DATABASE_BYTES);
            }
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("settings.properties"));
            Properties properties = collectPreferences(context);
            java.io.ByteArrayOutputStream settingsBytes = new java.io.ByteArrayOutputStream();
            properties.store(settingsBytes, "Wen Sarafet backup settings");
            if (settingsBytes.size() > BackupArchiveLimits.MAX_SETTINGS_BYTES)
                throw new IllegalStateException("إعدادات النسخة الاحتياطية أكبر من الحد الآمن");
            zip.write(settingsBytes.toByteArray());
            zip.closeEntry(); zip.finish();
            }
        } finally { snapshot.delete(); }
    }

    public static synchronized void restore(Context context, InputStream source, char[] password) throws Exception {
        // Serialize restores with backup snapshots in this process: otherwise
        // a backup can read a ledger while it is being replaced by restore.
        File temp = BackupRestoreStaging.createTemporaryDatabase(context.getCacheDir());
        Properties properties = new Properties(); boolean foundDatabase = false; boolean foundSettings = false;
        try (InputStream decrypted = BackupCrypto.decryptingStream(source, password);
             InputStream bounded = BackupArchiveLimits.bounded(decrypted,
                     BackupArchiveLimits.MAX_ARCHIVE_BYTES);
             ZipInputStream inputZip = new ZipInputStream(bounded)) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = inputZip.getNextEntry()) != null) {
                if (++entryCount > BackupArchiveLimits.MAX_ENTRIES)
                    throw new IllegalArgumentException("النسخة تحتوي ملفات أكثر من الحد الآمن");
                if ("database.sqlite".equals(entry.getName())) {
                    if (foundDatabase) throw new IllegalArgumentException("قاعدة بيانات مكررة في النسخة");
                    try (FileOutputStream databaseOutput = new FileOutputStream(temp)) {
                        BackupArchiveLimits.copy(inputZip, databaseOutput,
                                BackupArchiveLimits.MAX_DATABASE_BYTES);
                    }
                    foundDatabase = true;
                } else if ("settings.properties".equals(entry.getName())) {
                    if (foundSettings) throw new IllegalArgumentException("إعدادات مكررة في النسخة");
                    properties = BackupArchiveLimits.readSettings(inputZip);
                    foundSettings = true;
                } else {
                    // Legacy archives may contain optional metadata, but an
                    // unknown entry must not expand without a strict bound.
                    BackupArchiveLimits.copy(inputZip, null, BackupArchiveLimits.MAX_EXTRA_BYTES);
                }
                inputZip.closeEntry();
            }
            // ZipInputStream may stop before reading the encrypted GCM tag.
            // Authenticate the entire ciphertext before touching the live DB.
            BackupRestoreAuthentication.finish(bounded);
        } catch (Exception error) {
            temp.delete();
            throw new IllegalArgumentException("النسخة تالفة أو كلمة السر غير صحيحة", error);
        }
        if (!foundDatabase) {
            temp.delete();
            throw new IllegalArgumentException("النسخة لا تحتوي قاعدة بيانات");
        }
        // Never replace a working ledger on the strength of the SQLite magic header alone.
        // Validate the decrypted snapshot before touching the currently installed database.
        try { validateSqlite(temp); }
        catch (RuntimeException error) { temp.delete(); throw error; }
        // A legacy archive without settings must not erase the user's current
        // preferences. Decode malformed values before touching the live ledger.
        Map<String, Map<String, Object>> decodedSettings = null;
        if (foundSettings) {
            try { decodedSettings = BackupSettingsCodec.decode(properties); }
            catch (RuntimeException error) { temp.delete(); throw error; }
        }
        File target = context.getDatabasePath("wen_sarafet.db");
        boolean hadOldDatabase = target.exists();
        // A previous interrupted restore may have left a valuable rollback copy.
        // Do not delete or overwrite that copy on the next restore attempt.
        File old = BackupRestoreStaging.newRollbackDestination(target);
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
            try (FinanceDbHelper check = new FinanceDbHelper(context)) { check.getAccounts(); }
            restorePreferences(context, decodedSettings);
            old.delete();
        } catch (Exception error) {
            // A failed upgrade/open may have created WAL sidecars for the replacement.
            // Never replay those pages against the original database during rollback.
            new File(target.getPath() + "-wal").delete();
            new File(target.getPath() + "-shm").delete();
            new File(target.getPath() + "-journal").delete();
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

    private static void restorePreferences(Context context,
            Map<String, Map<String, Object>> decodedSettings) {
        if (decodedSettings == null) return;
        // SharedPreferences.apply() is asynchronous and silently ignores disk
        // failures. Commit each file and keep originals for best-effort rollback.
        Map<String, Map<String, ?>> originals = new LinkedHashMap<>();
        for (String file : PREF_FILES)
            originals.put(file, new HashMap<>(context.getSharedPreferences(
                    file, Context.MODE_PRIVATE).getAll()));
        try {
            for (String file : PREF_FILES) {
                SharedPreferences.Editor editor = context.getSharedPreferences(
                        file, Context.MODE_PRIVATE).edit().clear();
                for (Map.Entry<String, Object> entry : decodedSettings.get(file).entrySet())
                    putPreference(editor, entry.getKey(), entry.getValue());
                if (!editor.commit())
                    throw new IllegalStateException("تعذر حفظ إعدادات النسخة: " + file);
            }
        } catch (RuntimeException failure) {
            // If any preference file fails, restore all pre-restore values before
            // the caller rolls back the database. Never report partial success.
            for (String file : PREF_FILES) {
                try {
                    SharedPreferences.Editor editor = context.getSharedPreferences(
                            file, Context.MODE_PRIVATE).edit().clear();
                    for (Map.Entry<String, ?> entry : originals.get(file).entrySet())
                        putPreference(editor, entry.getKey(), entry.getValue());
                    if (!editor.commit())
                        throw new IllegalStateException("تعذر إرجاع الإعدادات: " + file);
                } catch (RuntimeException rollbackError) {
                    failure.addSuppressed(rollbackError);
                }
            }
            throw failure;
        }
    }

    @SuppressWarnings("unchecked")
    private static void putPreference(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Set<?>) editor.putStringSet(key, (Set<String>) value);
        else throw new IllegalArgumentException("نوع إعدادات غير مدعوم: " + key);
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
