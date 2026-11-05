package app.wensarafet.tracker;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class CaptureEngine {
    public static final String ACTION_RESOLVE = "app.wensarafet.tracker.RESOLVE_CAPTURE";
    private CaptureEngine() {}

    public static void handle(Context context, String provider, String channel, String sender, String rawText, long receivedAt) {
        String clean = TransactionParser.normalize(rawText);
        if (clean.isEmpty() || TransactionParser.isVerificationMessage(clean)) return;
        TransactionParser.Result parsed = TransactionParser.parse(provider, clean, TransferNameStore.get(context));
        FinanceDbHelper db = new FinanceDbHelper(context);
        try {
            if (db.hasMatchingCapture(provider, channel, parsed.amountMinor, parsed.currency, parsed.type, parsed.counterparty, parsed.balanceMinor, receivedAt)) return;
            long id = db.insertCapture(provider, channel, sender, clean, parsed, fingerprint(provider + "|" + clean.toLowerCase(Locale.ROOT)), receivedAt);
            if (id < 0) return;
            if (parsed.amountMinor != null && parsed.currency != null && parsed.type != null && !FinanceRules.TRANSFER.equals(parsed.type) && parsed.confidence >= 90) {
                long txId = db.importCapture(id, parsed.type);
                if (txId >= 0) notifySaved(context, id, parsed, db, txId, clean); else notifyReview(context, id, parsed);
            } else notifyReview(context, id, parsed);
        } finally { db.close(); }
    }

    private static boolean canNotify(Context context) {
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static PendingIntent openInbox(Context context, long id) {
        Intent intent = new Intent(context, MainActivity.class).putExtra("open_capture_inbox", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 13000 + (int) (id % 1000), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openTransactions(Context context, long id) {
        Intent intent = new Intent(context, MainActivity.class).putExtra("open_transactions", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 17000 + (int) (id % 1000), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void notifyReview(Context context, long id, TransactionParser.Result parsed) {
        if (!canNotify(context)) return;
        String channelId = NotificationPreferences.ensureChannel(context, NotificationPreferences.REVIEW);
        String amount = parsed.amountMinor == null ? "تعذر قراءة المبلغ" : Money.format(parsed.amountMinor, parsed.currency == null ? "" : parsed.currency);
        boolean internalTransfer = FinanceRules.TRANSFER.equals(parsed.type);
        android.app.Notification.Builder builder = new android.app.Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification).setColor(Color.rgb(104, 76, 190))
                .setContentTitle(internalTransfer ? "تحويل بين حساباتك" : "عملية جديدة تحتاج تحديدًا")
                .setContentText(internalTransfer ? amount + " — اختر من أي حساب وإلى أي حساب" : amount + " — اختر مصروف أو دخل")
                .setStyle(new android.app.Notification.BigTextStyle().bigText(internalTransfer ? amount + " — وجدنا اسمك في الحوالة. افتح صندوق المراجعة وحدد الحساب المصدر والمستلم." : amount + " — لم نستطع تحديد اتجاه العملية بأمان. اختر الآن أو راجعها لاحقًا من صندوق المراجعة."))
                .setContentIntent(openInbox(context, id)).setAutoCancel(true);
        if (!internalTransfer) {
            builder.addAction(action(context, id, FinanceRules.EXPENSE, "مصروف", 1));
            builder.addAction(action(context, id, FinanceRules.INCOME, "دخل", 2));
        }
        builder.addAction(new android.app.Notification.Action.Builder(null, "لاحقًا", openInbox(context, id)).build());
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(14000 + (int) (id % 1000), builder.build());
    }

    private static android.app.Notification.Action action(Context context, long id, String type, String title, int suffix) {
        Intent intent = new Intent(context, CaptureActionReceiver.class).setAction(ACTION_RESOLVE).putExtra("capture_id", id).putExtra("type", type);
        PendingIntent pending = PendingIntent.getBroadcast(context, 15000 + (int) (id % 1000) * 10 + suffix, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new android.app.Notification.Action.Builder(null, title, pending).build();
    }

    private static void notifySaved(Context context, long id, TransactionParser.Result parsed,
                                    FinanceDbHelper db,long transactionId,String originalMessage) {
        if (!canNotify(context)) return;
        String channelId = NotificationPreferences.ensureChannel(context, NotificationPreferences.SAVED);
        String kind = FinanceRules.EXPENSE.equals(parsed.type) ? "مصروف" : "دخل";
        String amount = Money.format(parsed.amountMinor, parsed.currency);
        String merchantSuggestion=null;
        try {
            merchantSuggestion=CaptureSuggestionPolicy.suggest(context,db,transactionId,
                    parsed,originalMessage);
        } catch(Exception ignored) {
            // A heuristic or its cache must never block a confirmed financial receipt.
        }
        String text=merchantSuggestion==null
                ?amount+" — اضغط لاختيار التصنيف"
                :amount+" — اقتراح حسب المحل: "+merchantSuggestion;
        String extended=merchantSuggestion==null?text:
                text+"\nهذا اقتراح فقط، ولم يُغيّر التطبيق تصنيف العملية تلقائيًا.";
        android.app.Notification notification = new android.app.Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification).setColor(Color.rgb(11, 107, 87))
                .setContentTitle("تم حفظ " + kind + " تلقائيًا")
                .setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(extended))
                .setContentIntent(openTransactions(context, id)).setAutoCancel(true).build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(16000 + (int) (id % 1000), notification);
    }

    public static void cancelNotification(Context context, long id) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(14000 + (int) (id % 1000));
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception ignored) { return String.valueOf(value.hashCode()); }
    }
}
