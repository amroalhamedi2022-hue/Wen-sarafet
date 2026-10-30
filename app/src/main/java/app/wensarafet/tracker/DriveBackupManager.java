package app.wensarafet.tracker;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encrypted folder backup through Android's Storage Access Framework.
 * A Google Drive account is used when the user selects a Drive folder in the
 * system picker; this backup path itself never sends data over HTTP.
 */
public final class DriveBackupManager {
    private static final String PREFS = "drive_backup";
    private static final String TREE_URI = "tree_uri";
    private static final String SECRET_NAME = "drive_backup_password";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    public interface Callback { void finished(boolean success, String message); }

    private DriveBackupManager() {}

    public static boolean linked(Context context) {
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TREE_URI, "").isEmpty()
                && SecurePinStore.read(context, SECRET_NAME) != null;
    }

    public static void link(Context context, Uri tree, String password) throws Exception {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        context.getContentResolver().takePersistableUriPermission(tree, flags);
        SecurePinStore.save(context, SECRET_NAME, password);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(TREE_URI, tree.toString()).apply();
    }

    public static void unlink(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TREE_URI, "");
        if (!raw.isEmpty()) {
            try {
                context.getContentResolver().releasePersistableUriPermission(Uri.parse(raw),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {}
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        try { SecurePinStore.save(context, SECRET_NAME, ""); } catch (Exception ignored) {}
    }

    public static void syncAsync(Context context, boolean notifyOnSuccess, Callback callback) {
        Context app = context.getApplicationContext();
        if (!linked(app) || !RUNNING.compareAndSet(false, true)) {
            if (callback != null) callback.finished(false, linked(app) ? "المزامنة تعمل الآن" : "Google Drive غير مربوط");
            return;
        }
        new Thread(() -> {
            boolean success = false;
            String message;
            try {
                sync(app);
                success = true;
                message = "تمت مزامنة Google Drive بنجاح";
            } catch (Exception error) {
                message = "تعذرت المزامنة: " + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
                notifyFailure(app, message);
            } finally { RUNNING.set(false); }
            final boolean result = success;
            final String finalMessage = message;
            new android.os.Handler(app.getMainLooper()).post(() -> {
                if (callback != null) callback.finished(result, finalMessage);
                else if (notifyOnSuccess && result)
                    android.widget.Toast.makeText(app, finalMessage, android.widget.Toast.LENGTH_LONG).show();
            });
        }, "wen-sarafet-drive-sync").start();
    }

    private static void sync(Context context) throws Exception {
        String rawTree = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TREE_URI, "");
        String password = SecurePinStore.read(context, SECRET_NAME);
        if (rawTree.isEmpty() || password == null || password.length() < 6)
            throw new IllegalStateException("إعداد النسخة غير مكتمل");
        Uri tree = Uri.parse(rawTree);
        Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        char[] secret = password.toCharArray();
        try {
            DriveBackupPublication.publish(new DriveBackupPublication.Store<Uri>() {
                @Override public Uri find(String name) throws Exception {
                    return findChild(context, tree, name);
                }
                @Override public Uri create(String name) throws Exception {
                    return DocumentsContract.createDocument(context.getContentResolver(), root,
                            "application/octet-stream", name);
                }
                @Override public void write(Uri document) throws Exception {
                    try (OutputStream output = context.getContentResolver().openOutputStream(document, "wt")) {
                        if (output == null) throw new IllegalStateException("تعذر فتح ملف النسخة");
                        BackupFileManager.create(context, output, secret);
                    }
                }
                @Override public void verify(Uri document) throws Exception {
                    try (InputStream input = context.getContentResolver().openInputStream(document)) {
                        if (input == null) throw new IllegalStateException("تعذر قراءة النسخة بعد رفعها");
                        BackupArchiveVerifier.verify(input, secret, context.getCacheDir());
                    }
                }
                @Override public Uri rename(Uri document, String name) throws Exception {
                    return DocumentsContract.renameDocument(context.getContentResolver(), document, name);
                }
                @Override public String name(Uri document) throws Exception {
                    String[] columns = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
                    try (Cursor cursor = context.getContentResolver().query(document, columns, null, null, null)) {
                        if (cursor == null || !cursor.moveToFirst())
                            throw new IllegalStateException("تعذر التحقق من اسم ملف النسخة");
                        return cursor.getString(0);
                    }
                }
                @Override public void delete(Uri document) throws Exception {
                    if (!DocumentsContract.deleteDocument(context.getContentResolver(), document))
                        throw new IllegalStateException("تعذر تنظيف ملف النسخة غير المكتمل");
                }
            }, java.util.UUID.randomUUID().toString());
        } finally {
            java.util.Arrays.fill(secret, '\\0');
        }
    }

    private static Uri findChild(Context context, Uri tree, String name) throws Exception {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME};
        Uri found = null;
        try (Cursor cursor = context.getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) throw new IllegalStateException("تعذر قراءة مجلد النسخ الاحتياطية");
            while (cursor.moveToNext()) {
                if (name.equals(cursor.getString(1))) {
                    if (found != null)
                        throw new IllegalStateException("أكثر من ملف بنفس اسم النسخة؛ أوقف المزامنة لحماية البيانات");
                    found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0));
                }
            }
        }
        return found;
    }

    private static void notifyFailure(Context context, String message) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String channel = NotificationPreferences.ensureChannel(context, NotificationPreferences.SYNC);
        android.app.Notification notification = new android.app.Notification.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("مشكلة مزامنة النسخة الاحتياطية")
                .setContentText(message).setAutoCancel(true).build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(18801, notification);
    }
}
