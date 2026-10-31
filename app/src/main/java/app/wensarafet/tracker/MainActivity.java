package app.wensarafet.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class MainActivity extends Activity {
    private FinanceDbHelper db;
    private boolean transactionsTab;
    private int currentTab;
    private static final int TAB_HOME = 0;
    private static final int TAB_TRANSACTIONS = 1;
    private static final int TAB_DEBTS = 2;
    private static final int TAB_ANALYTICS = 3;
    private boolean addScreenVisible;
    private boolean subScreenVisible;
    private String dashboardCurrency = "ILS";
    private boolean chartLineMode;
    private boolean authenticated;
    private boolean hasStarted;
    private boolean lockScreenVisible;
    private boolean biometricPromptVisible;
    private boolean activationScreenVisible;
    private boolean startupCheckRunning;
    private boolean startupResolved;
    private long backgroundAt;
    private char[] pendingBackupPassword;
    private char[] pendingDrivePassword;
    private String pendingNotificationType;
    private FrameLayout screenHost;
    private View activeScreen;
    private int screenTransitionGeneration;
    private static final int CREATE_BACKUP = 7301;
    private static final int OPEN_BACKUP = 7302;
    private static final int PICK_NOTIFICATION_SOUND = 7303;
    private static final int OPEN_DRIVE_FOLDER = 7304;

    /**
     * Keeps one stable Activity content host and transitions screens inside it.
     * This removes the refresh flash that occurred when every page immediately
     * replaced the complete Activity content.
     */
    @Override public void setContentView(View next) {
        if (next == null) return;
        next.setTag(Boolean.TRUE); // Ui.animateScreen avoids a duplicate full-screen animation.
        FrameLayout.LayoutParams match = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (screenHost == null) {
            screenHost = new FrameLayout(this);
            screenHost.setBackgroundColor(Ui.BACKGROUND);
            super.setContentView(screenHost);
            screenHost.addView(next, match);
            activeScreen = next;
            // A disabled system animator scale must never leave the first screen
            // transparent. In that mode, show the view without a transition.
            if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;
            next.setAlpha(0f);
            next.setTranslationX(Ui.dp(this, 14));
            next.animate().alpha(1f).translationX(0f).setDuration(260)
                    .setInterpolator(new android.view.animation.PathInterpolator(.2f, .8f, .2f, 1f)).start();
            return;
        }
        if (activeScreen == next) return;
        final int generation = ++screenTransitionGeneration;
        final View previous = activeScreen;
        for (int i = screenHost.getChildCount() - 1; i >= 0; i--) {
            View child = screenHost.getChildAt(i);
            if (child != previous) screenHost.removeViewAt(i);
        }
        screenHost.addView(next, match);
        activeScreen = next;
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            next.setAlpha(1f);
            next.setTranslationX(0f);
            if (previous != null && previous.getParent() == screenHost) {
                previous.animate().cancel();
                screenHost.removeView(previous);
            }
            return;
        }
        next.setAlpha(0f);
        next.setTranslationX(Ui.dp(this, 18));
        next.animate().alpha(1f).translationX(0f).setDuration(280)
                .setInterpolator(new android.view.animation.PathInterpolator(.2f, .8f, .2f, 1f)).start();
        if (previous != null) {
            previous.animate().cancel();
            previous.animate().alpha(0f).translationX(-Ui.dp(this, 10)).setDuration(180)
                    .setInterpolator(new android.view.animation.PathInterpolator(.4f, 0f, 1f, 1f))
                    .withEndAction(() -> {
                        if (generation == screenTransitionGeneration && previous.getParent() == screenHost)
                            screenHost.removeView(previous);
                    }).start();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        if (Ui.DARK) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BRAND_DARK);
        getWindow().setNavigationBarColor(Ui.SURFACE);
        // Some MIUI Android 13 builds throw inside Window#getInsetsController()
        // before the decor view has been attached. The decor flags path is stable
        // on API 33 and was already proven by the previous production build.
        View decor = getWindow().getDecorView();
        int lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decor.setSystemUiVisibility(Ui.DARK ? 0 : lightBars);
        decor.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        showStartupScreen();
        checkStartupAsync();
    }

    private void showStartupScreen() {
        LinearLayout page = Ui.column(this);
        page.setGravity(Gravity.CENTER);
        page.setPadding(Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28));
        page.setBackgroundColor(Ui.BACKGROUND);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher_final);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int markSize = Ui.iconDp(this, 82);
        page.addView(mark, new LinearLayout.LayoutParams(markSize, markSize));
        TextView title = Ui.text(this, "وين صرفت؟", 25, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        TextView status = Ui.text(this, "جاري فتح التطبيق…", 13, Ui.MUTED, false);
        status.setGravity(Gravity.CENTER);
        page.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 36)));
        setContentView(page);
    }

    private void checkStartupAsync() {
        if (startupCheckRunning) return;
        startupCheckRunning = true;
        new Thread(() -> {
            boolean trusted = false;
            boolean activated = false;
            String request = null;
            String securityLabel = null;
            String failure = null;
            try {
                trusted = AppIntegrity.isTrusted(this);
                if (trusted) {
                    activated = LicenseManager.isActivated(this);
                    if (!activated) {
                        securityLabel = LicenseManager.identitySecurityLabel(this);
                        request = LicenseManager.requestCode(this);
                    }
                }
            } catch (Throwable error) {
                failure = error.getClass().getSimpleName();
                android.util.Log.e("WenSarafet", "Startup security check failed", error);
            }
            final boolean finalTrusted = trusted;
            final boolean finalActivated = activated;
            final String finalRequest = request;
            final String finalSecurityLabel = securityLabel;
            final String finalFailure = failure;
            runOnUiThread(() -> {
                startupCheckRunning = false;
                startupResolved = true;
                if (isFinishing() || isDestroyed()) return;
                try {
                    if (!finalTrusted) showActivationScreen(null, null, null);
                    else if (finalActivated) initializeApp();
                    else showActivationScreen(finalRequest, finalSecurityLabel, finalFailure);
                } catch (Throwable error) {
                    android.util.Log.e("WenSarafet", "Startup UI failed", error);
                    showStartupFailure(error.getClass().getSimpleName());
                }
            });
        }, "wen-sarafet-startup").start();
    }

    private void showStartupFailure(String detail) {
        LinearLayout page = Ui.column(this);
        page.setGravity(Gravity.CENTER);
        page.setPadding(Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28));
        page.setBackgroundColor(Ui.BACKGROUND);
        TextView title = Ui.text(this, "تعذر فتح وين صرفت؟", 22, Ui.EXPENSE, true);
        title.setGravity(Gravity.CENTER); page.addView(title);
        TextView message = Ui.text(this, "بياناتك لم تُحذف. أعد المحاولة، وإذا تكرر الخطأ أرسل هذا الرمز إلى Amro: " +
                (detail == null ? "STARTUP" : detail), 14, Ui.TEXT, false);
        message.setGravity(Gravity.CENTER); message.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 18)); page.addView(message);
        Button retry = actionButton("إعادة المحاولة", true);
        retry.setOnClickListener(v -> { showStartupScreen(); startupResolved = false; checkStartupAsync(); });
        page.addView(retry, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));
        setContentView(page);
    }

    private void initializeApp() {
        activationScreenVisible = false;
        db = new FinanceDbHelper(this);
        db.getAccounts();
        chartLineMode = getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("chart_line", true);
        authenticated = !AppLockManager.enabled(this);
        if (authenticated) openInitialScreen(); else showLockScreen();
    }

    @Override protected void onStart() {
        super.onStart();
        if (startupCheckRunning || !startupResolved || activationScreenVisible) return;
        if (!LicenseManager.isActivated(this)) { showActivationScreen(); return; }
        if (!hasStarted) { hasStarted = true; return; }
        if (AppLockManager.enabled(this) && backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > 30_000L) {
            authenticated = false; showLockScreen();
        }
    }

    @Override protected void onStop() {
        super.onStop();
        backgroundAt = System.currentTimeMillis();
        if (authenticated && db != null && DriveBackupManager.linked(this))
            DriveBackupManager.syncAsync(this, false, null);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (startupCheckRunning || !startupResolved) return;
        if (!LicenseManager.isActivated(this)) { showActivationScreen(); return; }
        if (db == null) { initializeApp(); return; }
        if (AppLockManager.enabled(this) && backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > 30_000L) authenticated = false;
        if (authenticated) openInitialScreen(); else showLockScreen();
    }

    private void openInitialScreen() {
        if (getIntent().getBooleanExtra("open_capture_inbox", false)) { getIntent().removeExtra("open_capture_inbox"); showCaptureInbox(); }
        else if (getIntent().getBooleanExtra("open_transactions", false)) { getIntent().removeExtra("open_transactions"); showMain(true); }
        else if (getIntent().getBooleanExtra("open_analytics", false)) { getIntent().removeExtra("open_analytics"); showAnalyticsScreen(); }
        else showMain(false);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7201 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            BudgetAlertManager.checkAll(this);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NOTIFICATION_SOUND) {
            if (resultCode == RESULT_OK && pendingNotificationType != null && data != null) {
                Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                NotificationPreferences.saveSoundUri(this, pendingNotificationType, picked == null ? "" : picked.toString());
                NotificationPreferences.save(this, pendingNotificationType, picked != null,
                        NotificationPreferences.vibration(this, pendingNotificationType));
                showSettings();
            }
            pendingNotificationType = null;
            return;
        }
        if (requestCode == OPEN_DRIVE_FOLDER) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingDrivePassword != null) {
                try {
                    DriveBackupManager.link(this, data.getData(), new String(pendingDrivePassword));
                    Toast.makeText(this, "جاري مزامنة النسخة المشفّرة…", Toast.LENGTH_LONG).show();
                    DriveBackupManager.syncAsync(this, true, (ok, message) -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
                } catch (Exception error) {
                    Toast.makeText(this, "تعذرت المزامنة: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            if (pendingDrivePassword != null) Arrays.fill(pendingDrivePassword, '\0');
            pendingDrivePassword = null;
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) { clearPendingBackupPassword(); return; }
        Uri uri = data.getData();
        if (requestCode == CREATE_BACKUP && pendingBackupPassword != null) {
            try {
                if (db != null) db.close();
                OutputStream output = getContentResolver().openOutputStream(uri, "w");
                if (output == null) throw new IllegalStateException("تعذر فتح مكان الحفظ");
                BackupFileManager.create(this, output, pendingBackupPassword);
                Toast.makeText(this, "تم حفظ النسخة المشفّرة", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Toast.makeText(this, "تعذر إنشاء النسخة: " + error.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                db = new FinanceDbHelper(this); db.getAccounts(); clearPendingBackupPassword();
            }
        } else if (requestCode == OPEN_BACKUP) {
            showRestoreBackupDialog(uri);
        }
    }

    private void clearPendingBackupPassword() {
        if (pendingBackupPassword != null) Arrays.fill(pendingBackupPassword, '\0');
        pendingBackupPassword = null;
    }

    private void showCreateBackupDialog() {
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 8));
        EditText password = input("كلمة سر النسخة — 6 خانات أو أكثر"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirm = input("تأكيد كلمة السر"); confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password, fieldParams()); form.addView(confirm, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("نسخة احتياطية مشفّرة").setView(form)
                .setNegativeButton("إلغاء", null).setPositiveButton("اختيار مكان الحفظ", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String first = password.getText().toString();
            if (first.length() < 6) { password.setError("اكتب 6 خانات على الأقل"); return; }
            if (!first.equals(confirm.getText().toString())) { confirm.setError("كلمتا السر غير متطابقتين"); return; }
            pendingBackupPassword = first.toCharArray(); dialog.dismiss();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
            Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE, "Wen-Sarafet-" + date + ".wsb");
            startActivityForResult(create, CREATE_BACKUP);
        })); dialog.show();
    }

    private void showDriveLinkDialog() {
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 8));
        form.addView(Ui.text(this, "اختر كلمة سر للنسخة المشفّرة، ثم اختر مجلدًا داخل Google Drive من شاشة الملفات.", 12, Ui.MUTED, false));
        EditText password = input("كلمة سر النسخة — 6 خانات أو أكثر");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirm = input("تأكيد كلمة السر");
        confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password, fieldParams()); form.addView(confirm, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("ربط Google Drive").setView(form)
                .setNegativeButton("إلغاء", null).setPositiveButton("اختيار المجلد", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String first = password.getText().toString();
            if (first.length() < 6) { password.setError("اكتب 6 خانات على الأقل"); return; }
            if (!first.equals(confirm.getText().toString())) { confirm.setError("كلمتا السر غير متطابقتين"); return; }
            pendingDrivePassword = first.toCharArray(); dialog.dismiss();
            Intent tree = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(tree, OPEN_DRIVE_FOLDER);
        })); dialog.show();
    }

    private void showRestoreBackupDialog(Uri uri) {
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 8));
        form.addView(Ui.text(this, "الاستعادة ستستبدل الحسابات والعمليات والإعدادات الحالية.", 12, Ui.EXPENSE, true));
        EditText password = input("كلمة سر النسخة"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); form.addView(password, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("استعادة النسخة").setView(form)
                .setNegativeButton("إلغاء", null).setPositiveButton("استعادة", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (password.length() < 6) { password.setError("اكتب كلمة سر النسخة"); return; }
            char[] secret = password.getText().toString().toCharArray();
            try {
                if (db != null) db.close();
                InputStream input = getContentResolver().openInputStream(uri);
                if (input == null) throw new IllegalStateException("تعذر فتح ملف النسخة");
                BackupFileManager.restore(this, input, secret);
                db = new FinanceDbHelper(this); db.getAccounts(); ReminderScheduler.scheduleDaily(this);
                dialog.dismiss(); Toast.makeText(this, "تمت استعادة النسخة بنجاح", Toast.LENGTH_LONG).show();
                if (LicenseManager.isActivated(this)) recreate(); else showActivationScreen();
            } catch (Exception error) {
                db = new FinanceDbHelper(this); db.getAccounts();
                Toast.makeText(this, "فشلت الاستعادة: تأكد من الملف وكلمة السر", Toast.LENGTH_LONG).show();
            } finally { Arrays.fill(secret, '\0'); }
        })); dialog.show();
    }

    @Override public void onBackPressed() {
        if (AppLockManager.enabled(this) && !authenticated) {
            moveTaskToBack(true);
            return;
        }
        if (addScreenVisible || subScreenVisible || currentTab != TAB_HOME) {
            showMain(false);
            return;
        }
        finishAfterTransition();
    }

    private void showMain(boolean showTransactions) {
        if (AppLockManager.enabled(this) && !authenticated) { showLockScreen(); return; }
        // Recheck after saving, editing, recategorizing or deleting expenses and on
        // reopening the dashboard. A monthly alert is still emitted only once.
        if (BudgetAlertManager.enabled(this,"ILS") || BudgetAlertManager.enabled(this,"USD") ||
                BudgetAlertManager.enabled(this,"JOD")) BudgetAlertManager.checkAll(this);
        lockScreenVisible = false;
        addScreenVisible = false;
        subScreenVisible = false;
        transactionsTab = showTransactions;
        currentTab = showTransactions ? TAB_TRANSACTIONS : TAB_HOME;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout shell = Ui.column(this);
        root.addView(shell, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View topBar = mainTopBar();
        shell.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 112)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = showTransactions ? transactionsContent(1000) : homeContent();
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        View bottomNav = bottomNavigation(currentTab);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72), Gravity.BOTTOM);
        root.addView(bottomNav, navParams);

        int fabPercent = Ui.homeIconScalePercent(this, "fab");
        int fabSize = Math.max(Ui.dp(this, 46), Math.min(Ui.dp(this, 82), Ui.homeIconDp(this, "fab", 58)));
        TextView fab = Ui.text(this, "+", 32f * fabPercent / 100f, Color.WHITE, false);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.gradient(Ui.BRAND, Ui.VIOLET, 24, this));
        fab.setElevation(Ui.dp(this, 8));
        fab.setOnClickListener(v -> showAddTransaction());
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(fabSize, fabSize, Gravity.BOTTOM | (Ui.homeFabOnRight(this) ? Gravity.END : Gravity.START));
        fabParams.setMargins(Ui.dp(this, 18), 0, Ui.dp(this, 18), Ui.dp(this, 88));
        root.addView(fab, fabParams);
        final boolean[] chromeHidden = {false};
        final int[] upwardTravel = {0};
        final View topBarBackground = topBar.findViewWithTag("main_header_background");
        scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int delta = scrollY - oldScrollY;
            if (topBarBackground != null) {
                float progress = Math.min(1f, scrollY / (float) Ui.dp(this, 72));
                topBarBackground.setAlpha(1f - progress * .84f);
                topBarBackground.setElevation(progress > .02f ? 0f : Ui.dp(this, 7));
            }
            if (delta < 0) upwardTravel[0] += -delta; else if (delta > 0) upwardTravel[0] = 0;
            boolean hide = scrollY > Ui.dp(this, 48) && delta > Ui.dp(this, 1);
            boolean showAtTop = scrollY <= Ui.dp(this, 10);
            boolean showWhileAscending = delta < 0 && upwardTravel[0] >= Ui.dp(this, 18) && v.canScrollVertically(1);
            if (hide && !chromeHidden[0]) {
                chromeHidden[0] = true;
                if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    bottomNav.animate().cancel();
                    bottomNav.setTranslationY(Ui.dp(this, 76));
                    bottomNav.setAlpha(0f);
                    bottomNav.setVisibility(View.INVISIBLE);
                    fab.animate().cancel();
                    fab.setTranslationY(Ui.dp(this, 70));
                } else {
                    bottomNav.animate().translationY(Ui.dp(this, 76)).alpha(0f).setDuration(220)
                            .withEndAction(() -> { if (chromeHidden[0]) bottomNav.setVisibility(View.INVISIBLE); }).start();
                    fab.animate().translationY(Ui.dp(this, 70)).setDuration(220).start();
                }
            } else if ((showAtTop || showWhileAscending) && chromeHidden[0]) {
                chromeHidden[0] = false;
                upwardTravel[0] = 0;
                bottomNav.setVisibility(View.VISIBLE);
                if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    bottomNav.animate().cancel();
                    bottomNav.setAlpha(1f);
                    bottomNav.setTranslationY(0f);
                    fab.animate().cancel();
                    fab.setTranslationY(0f);
                } else {
                    bottomNav.setAlpha(0f);
                    bottomNav.setTranslationY(Ui.dp(this, 76));
                    bottomNav.animate().translationY(0f).alpha(1f).setDuration(220).start();
                    fab.animate().translationY(0f).setDuration(220).start();
                }
            }
        });
        setContentView(root);
        Ui.pressAnimation(fab);
        Ui.animateScreen(root);
        content.post(() -> Ui.animateChildren(content));
    }

    private View mainTopBar() {
        LinearLayout wrapper = Ui.column(this);
        wrapper.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        FrameLayout layers = new FrameLayout(this);
        View background = new View(this);
        background.setTag("main_header_background");
        background.setBackground(Ui.layeredBackground(this, 24));
        background.setElevation(Ui.dp(this, 7));
        layers.addView(background, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout bar = Ui.row(this);
        bar.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 10));
        bar.setElevation(Ui.dp(this, 8));

        LinearLayout titles = Ui.column(this);
        TextView title = Ui.text(this, "وين صرفت؟", 25, Color.WHITE, true);
        TextView subtitle = Ui.text(this, "أموالك واضحة، وخصوصيتك محفوظة", 11, Color.rgb(225, 239, 235), false);
        // Keep labels and controls inside the fixed header on compact RTL screens.
        title.setSingleLine(true);
        subtitle.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setAutoSizeTextTypeUniformWithConfiguration(17,25,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        titles.addView(title);
        titles.addView(subtitle);

        int headerPercent = Ui.homeIconScalePercent(this, "header");
        int headerIconSize = Math.max(Ui.dp(this, 32), Math.min(Ui.dp(this, 62), Ui.homeIconDp(this, "header", 42)));
        if(getResources().getConfiguration().screenWidthDp < 390)
            headerIconSize=Math.min(headerIconSize,Ui.dp(this,48));
        float headerIconText = Math.min(31f, 21f * headerPercent / 100f);
        TextView addAccount = Ui.text(this, "▦", headerIconText, Color.WHITE, true);
        addAccount.setGravity(Gravity.CENTER);
        addAccount.setContentDescription("إدارة الحسابات");
        addAccount.setBackground(Ui.rounded(Color.argb(42, 255, 255, 255), 20, this));
        addAccount.setOnClickListener(v -> showAccountManagement());
        TextView settings = Ui.text(this, "⚙", headerIconText, Color.WHITE, true);
        settings.setGravity(Gravity.CENTER);
        settings.setContentDescription("الإعدادات");
        settings.setBackground(Ui.rounded(Color.argb(42, 255, 255, 255), 20, this));
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(headerIconSize, headerIconSize);
        settingsParams.setMargins(Ui.dp(this, 8), 0, 0, 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        if (Ui.homeHeaderOnRight(this)) {
            bar.addView(addAccount, new LinearLayout.LayoutParams(headerIconSize, headerIconSize));
            bar.addView(settings, settingsParams);
            bar.addView(titles, titleParams);
        } else {
            bar.addView(titles, titleParams);
            bar.addView(addAccount, new LinearLayout.LayoutParams(headerIconSize, headerIconSize));
            bar.addView(settings, settingsParams);
        }
        Ui.pressAnimation(addAccount); Ui.pressAnimation(settings);
        layers.addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrapper.addView(layers, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 90)));
        return wrapper;
    }

    private LinearLayout bottomNavigation(int selectedTab) {
        LinearLayout nav = Ui.row(this);
        nav.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        nav.setBackgroundColor(Ui.SURFACE);
        nav.setElevation(Ui.dp(this, 12));

        TextView home = navButton("🏠\nالرئيسية", selectedTab == TAB_HOME);
        TextView tx = navButton("📋\nالعمليات", selectedTab == TAB_TRANSACTIONS);
        TextView debts = navButton("🤝\nالديون", selectedTab == TAB_DEBTS);
        TextView analytics = navButton("📊\nالتحليل", selectedTab == TAB_ANALYTICS);
        home.setOnClickListener(v -> showMain(false));
        tx.setOnClickListener(v -> showMain(true));
        debts.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));
        analytics.setOnClickListener(v -> showAnalyticsScreen());
        for(TextView item:new TextView[]{home,tx,debts,analytics}) {
            LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1);
            navParams.setMargins(Ui.dp(this,4),0,Ui.dp(this,4),0);
            nav.addView(item,navParams);
        }
        return nav;
    }

    private TextView navButton(String label, boolean selected) {
        TextView view = Ui.text(this, label, 11.5f, selected ? Ui.BRAND : Ui.MUTED, selected);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(Ui.dp(this,2),1f);
        view.setPadding(Ui.dp(this,4),Ui.dp(this,3),Ui.dp(this,4),Ui.dp(this,3));
        view.setMaxLines(2);
        if (selected) view.setBackground(Ui.rounded(Ui.BRAND_SOFT, 18, this));
        return view;
    }

    private LinearLayout homeContent() {
        LinearLayout content = Ui.column(this);
        content.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 110));
        content.addView(sectionTitle("إجمالي أرصدتك", "حسب العملة"));
        content.addView(balanceCards());
        content.addView(spendingDashboard());
        content.addView(sectionTitle("الحسابات", db.getAccounts().size() + " حسابات"));
        for (Models.Account account : db.getAccounts()) content.addView(accountCard(account));
        List<Models.Transaction> recent = db.getTransactions(5);
        content.addView(sectionTitle("آخر العمليات", recent.isEmpty() ? "لا توجد عمليات" : "أحدث 5"));
        if (recent.isEmpty()) content.addView(emptyCard());
        else for (Models.Transaction tx : recent) content.addView(transactionCard(tx));
        List<Models.Transaction> uncategorized = db.getUncategorizedTransactions();
        content.addView(sectionTitle("عمليات غير مصنفة", uncategorized.isEmpty() ? "كل العمليات مصنفة" : uncategorized.size() + " بحاجة للتصنيف"));
        if (uncategorized.isEmpty()) content.addView(messageCard("لا توجد عمليات غير مصنفة", "أي عملية جديدة تحتاج مراجعة ستظهر هنا تلقائيًا"));
        else for (Models.Transaction tx : uncategorized) content.addView(transactionCard(tx));
        return content;
    }

    private LinearLayout transactionsContent(int limit) {
        LinearLayout content=Ui.column(this);
        content.setPadding(Ui.dp(this,14),Ui.dp(this,8),Ui.dp(this,14),Ui.dp(this,110));
        content.addView(sectionTitle("كل العمليات","بحث محلي وفلاتر"));

        EditText search=input("🔎 ابحث بالملاحظة أو الحساب أو التصنيف أو المبلغ");
        search.setSingleLine(true);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        content.addView(search,fieldParams());

        LinearLayout filters=Ui.row(this);
        Spinner type=spinner(Arrays.asList("كل الأنواع","مصروف","دخل","تحويل"));
        Spinner currency=spinner(Arrays.asList("كل العملات","ILS","USD","JOD"));
        LinearLayout.LayoutParams typeParams=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1);
        typeParams.setMargins(Ui.dp(this,3),Ui.dp(this,5),Ui.dp(this,3),Ui.dp(this,5));
        LinearLayout.LayoutParams currencyParams=new LinearLayout.LayoutParams(0,Ui.dp(this,50),1);
        currencyParams.setMargins(Ui.dp(this,3),Ui.dp(this,5),Ui.dp(this,3),Ui.dp(this,5));
        filters.addView(type,typeParams);
        filters.addView(currency,currencyParams);
        content.addView(filters,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,62)));

        TextView status=Ui.text(this,"",12,Ui.MUTED,false);
        status.setPadding(Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8));
        content.addView(status);

        LinearLayout results=Ui.column(this);
        content.addView(results);
        final int pageSize=Math.max(20,Math.min(75,limit));
        final int[] offset={0};
        final Runnable[] loadNext={null};
        loadNext[0]=()->{
            int typeIndex=type.getSelectedItemPosition();
            String chosenType=typeIndex==1?FinanceRules.EXPENSE:
                    typeIndex==2?FinanceRules.INCOME:typeIndex==3?FinanceRules.TRANSFER:null;
            int currencyIndex=currency.getSelectedItemPosition();
            String chosenCurrency=currencyIndex==1?"ILS":currencyIndex==2?"USD":
                    currencyIndex==3?"JOD":null;
            List<Models.Transaction> page;
            try {
                // The extra result indicates whether a next page is available.
                page=db.searchTransactions(search.getText().toString(),chosenType,chosenCurrency,
                        pageSize+1,offset[0]);
            } catch(Exception error) {
                search.setError(error.getMessage()==null?"تعذّر البحث":error.getMessage());
                return;
            }
            int shown=Math.min(pageSize,page.size());
            for(int i=0;i<shown;i++)results.addView(transactionCard(page.get(i)));
            offset[0]+=shown;
            if(offset[0]==0)results.addView(emptyCard());
            status.setText(offset[0]==0?"لا توجد عمليات مطابقة":
                    "عُرضت "+offset[0]+" عملية"+(page.size()>pageSize?" — يوجد المزيد":""));
            if(page.size()>pageSize) {
                Button more=actionButton("عرض "+pageSize+" عملية أخرى",false);
                more.setOnClickListener(v->{
                    results.removeView(more);
                    loadNext[0].run();
                });
                results.addView(more,fieldParams());
            }
        };
        Runnable reset=()->{
            offset[0]=0;
            results.removeAllViews();
            loadNext[0].run();
        };
        // Debounce typing, rather than rebuilding up to 75 transaction cards every keystroke.
        android.os.Handler searchHandler=new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] pendingSearch={null};
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence text,int start,int before,int count) {
                if(pendingSearch[0]!=null)searchHandler.removeCallbacks(pendingSearch[0]);
                pendingSearch[0]=()->{
                    if(content.isAttachedToWindow())reset.run();
                };
                searchHandler.postDelayed(pendingSearch[0],180L);
            }
            @Override public void afterTextChanged(Editable text) {}
        });
        type.setOnItemSelectedListener(new SimpleItemSelected(reset));
        currency.setOnItemSelectedListener(new SimpleItemSelected(reset));
        reset.run();
        return content;
    }

    private View sectionTitle(String titleValue, String subtitleValue) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 10));
        TextView title = Ui.text(this, titleValue, 17, Ui.TEXT, true);
        TextView subtitle = Ui.text(this, subtitleValue, 12, Ui.MUTED, false);
        subtitle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1));
        row.addView(subtitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 36)));
        return row;
    }

    private View balanceCards() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 4));
        Map<String, Long> totals = db.getTotals();
        for (String currency : Arrays.asList("ILS", "USD", "JOD")) {
            LinearLayout card = Ui.column(this);
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            card.setBackground(Ui.layeredSoftBackground(this, 22));
            card.setElevation(Ui.dp(this, 2));
            card.addView(Ui.text(this, currency, 13, Ui.DARK ? Ui.TEXT : Ui.BRAND_DARK, true));
            TextView amount = Ui.text(this, Money.format(totals.get(currency), currency), 20, Ui.TEXT, true);
            LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            amountParams.topMargin = Ui.dp(this, 8);
            card.addView(amount, amountParams);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(Ui.dp(this, 178), Ui.dp(this, 98));
            cardParams.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
            row.addView(card, cardParams);
        }
        scroll.addView(row);
        return scroll;
    }

    private View spendingDashboard() {
        LinearLayout box = Ui.column(this);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, "نظرة على مصروفك", 18, Ui.TEXT, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        LinearLayout currencies = Ui.row(this);
        Map<TextView, String> currencyChips = new LinkedHashMap<>();
        for (String code : Arrays.asList("ILS", "USD", "JOD")) {
            boolean selected = code.equals(dashboardCurrency);
            TextView chip = Ui.text(this, code, 11, selected ? Color.WHITE : Ui.VIOLET, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(selected ? Ui.gradient(Ui.BRAND_DARK, Ui.BRAND, 14, this) : Ui.rounded(Ui.BRAND_SOFT, 14, this));
            currencyChips.put(chip, code);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 32));
            p.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            currencies.addView(chip, p);
        }
        header.addView(currencies);
        box.addView(header);
        TextView currencyHint = Ui.text(this, "عملة الرسم — اضغط ILS أو USD أو JOD للتبديل", 11, Ui.MUTED, false);
        currencyHint.setPadding(Ui.dp(this, 3), 0, Ui.dp(this, 3), Ui.dp(this, 8));
        box.addView(currencyHint);

        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0); day.set(Calendar.MINUTE, 0); day.set(Calendar.SECOND, 0); day.set(Calendar.MILLISECOND, 0);
        long[] starts = new long[8];
        day.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 8; i++) {
            starts[i] = day.getTimeInMillis();
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1); month.set(Calendar.HOUR_OF_DAY, 0); month.set(Calendar.MINUTE, 0); month.set(Calendar.SECOND, 0); month.set(Calendar.MILLISECOND, 0);
        LinearLayout stats = Ui.row(this);
        View todayCard = statCard("مصاريف اليوم", "");
        View weekCard = statCard("آخر 7 أيام", "");
        View monthCard = statCard("هذا الشهر", "");
        TextView todayAmount = (TextView) ((LinearLayout) todayCard).getChildAt(1);
        TextView weekAmount = (TextView) ((LinearLayout) weekCard).getChildAt(1);
        TextView monthAmount = (TextView) ((LinearLayout) monthCard).getChildAt(1);
        stats.addView(todayCard); stats.addView(weekCard); stats.addView(monthCard);
        todayCard.setOnClickListener(v -> showAnalyticsScreen());
        weekCard.setOnClickListener(v -> showAnalyticsScreen());
        monthCard.setOnClickListener(v -> showAnalyticsScreen());
        Ui.pressAnimation(todayCard); Ui.pressAnimation(weekCard); Ui.pressAnimation(monthCard);
        box.addView(stats);

        LinearLayout chartCard = Ui.column(this);
        chartCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 10));
        chartCard.setBackground(Ui.layeredSoftBackground(this, 22));
        chartCard.setElevation(Ui.dp(this, 2));
        chartCard.addView(Ui.text(this, "مصاريف الأيام السبعة", 15, Ui.TEXT, true));
        FrameLayout chartHost = new FrameLayout(this);
        chartCard.addView(chartHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 190)));
        chartCard.setOnClickListener(v -> showAnalyticsScreen()); Ui.pressAnimation(chartCard);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 240));
        chartParams.setMargins(0, Ui.dp(this, 10), 0, 0);
        box.addView(chartCard, chartParams);

        Runnable updateOverview = () -> {
            long end = System.currentTimeMillis() + 1;
            todayAmount.setText(Money.format(db.getExpenseTotal(dashboardCurrency, starts[6], end), dashboardCurrency));
            weekAmount.setText(Money.format(db.getExpenseTotal(dashboardCurrency, starts[0], end), dashboardCurrency));
            monthAmount.setText(Money.format(db.getExpenseTotal(dashboardCurrency, month.getTimeInMillis(), end), dashboardCurrency));
            long[] values = new long[7]; String[] labels = new String[7]; SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("ar"));
            for (int i = 0; i < 7; i++) { values[i] = db.getExpenseTotal(dashboardCurrency, starts[i], starts[i + 1]); labels[i] = dayFormat.format(new Date(starts[i])); }
            chartHost.removeAllViews(); ExpenseChartView chart = new ExpenseChartView(this, values, labels, chartLineMode); chart.setAlpha(0f);
            chartHost.addView(chart, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); chart.animate().alpha(1f).setDuration(220).start();
            for (Map.Entry<TextView, String> entry : currencyChips.entrySet()) {
                boolean active = entry.getValue().equals(dashboardCurrency); entry.getKey().setTextColor(active ? Color.WHITE : Ui.VIOLET);
                entry.getKey().setBackground(active ? Ui.gradient(Ui.BRAND_DARK, Ui.BRAND, 14, this) : Ui.rounded(Ui.BRAND_SOFT, 14, this));
            }
        };
        for (Map.Entry<TextView, String> entry : currencyChips.entrySet()) entry.getKey().setOnClickListener(v -> { dashboardCurrency = entry.getValue(); updateOverview.run(); });
        updateOverview.run();
        return box;
    }

    private View statCard(String label, String amount) {
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 7));
        card.setBackground(Ui.gradient(Ui.BRAND_DARK, Ui.BRAND, 20, this));
        card.setElevation(Ui.dp(this, 3));
        card.addView(Ui.text(this, label, 12, Color.rgb(239, 244, 255), false));
        TextView amountView = Ui.text(this, amount, 20, Color.WHITE, true);
        amountView.setAutoSizeTextTypeUniformWithConfiguration(13, 20, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        card.addView(amountView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 78), 1);
        params.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        card.setLayoutParams(params);
        return card;
    }

    private View accountIcon(Models.Account account) {
        int resource = 0;
        String name = account.name.toLowerCase(Locale.ROOT);
        if (name.contains("بنك فلسطين") || name.contains("bank of palestine")) resource = R.drawable.icon_bop_mark;
        else if (name.contains("jawwal")) resource = R.drawable.icon_jawwal_pay_mark;
        else if (name.contains("palpay")) resource = R.drawable.icon_palpay_mark;
        if (resource != 0) {
            ImageView image = new ImageView(this);
            image.setImageResource(resource);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconBackground = name.contains("بنك فلسطين") || name.contains("bank of palestine") ? 0xFFD40067 : name.contains("jawwal") ? 0xFF91CF32 : 0xFF9F2BE8;
            image.setBackground(Ui.rounded(iconBackground, 13, this));
            image.setClipToOutline(true);
            image.setElevation(Ui.dp(this, 2));
            return image;
        }
        TextView fallback = Ui.text(this, name.contains("نقد") ? "💵" : "◈", 20, Ui.BRAND, true);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(Ui.gradient(Color.rgb(216, 246, 235), Color.rgb(230, 224, 255), 16, this));
        return fallback;
    }

    private View accountCard(Models.Account account) {
        LinearLayout card = Ui.row(this);
        card.setPadding(Ui.dp(this, 15), Ui.dp(this, 13), Ui.dp(this, 15), Ui.dp(this, 13));
        card.setBackground(Ui.rounded(Ui.SURFACE, 18, this));
        card.setElevation(Ui.dp(this, 1));

        View icon = accountIcon(account);
        int iconSize = Ui.homeIconDp(this, "accounts", 44);
        if (Ui.iconsOnRight(this)) card.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout labels = Ui.column(this);
        labels.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        TextView accountName=Ui.text(this, account.name, 15, Ui.TEXT, true);
        accountName.setSingleLine(true);
        accountName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(accountName);
        labels.addView(Ui.text(this, account.currency, 12, Ui.MUTED, false));
        boolean compactAccountCard=getResources().getConfiguration().screenWidthDp < 390;
        TextView balance = Ui.text(this, Money.format(account.balanceMinor, account.currency), 15, Ui.TEXT, true);
        balance.setGravity((compactAccountCard?Gravity.START:Gravity.END) | Gravity.CENTER_VERTICAL);
        if(compactAccountCard){
            // Put the full balance on its own line instead of squeezing both labels on narrow displays.
            labels.addView(balance);
            card.addView(labels, new LinearLayout.LayoutParams(0, Ui.dp(this, 72), 1));
        }else{
            card.addView(labels, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
            card.addView(balance, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 48)));
        }
        if (!Ui.iconsOnRight(this)) card.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(Ui.dp(this, compactAccountCard?98:72), iconSize + Ui.dp(this, 26)));
        params.setMargins(Ui.dp(this, 16), Ui.dp(this, 5), Ui.dp(this, 16), Ui.dp(this, 5));
        card.setLayoutParams(params);
        card.setOnClickListener(v -> showAccountDetails(account));
        Ui.pressAnimation(card);
        return card;
    }

    private void showAccountDetails(Models.Account account) {
        Models.Account current = db.getAccount(account.id);
        if (current == null) { Toast.makeText(this, "الحساب غير موجود", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 22), Ui.dp(this, 6), Ui.dp(this, 22), Ui.dp(this, 8));
        LinearLayout identity = Ui.row(this);
        View accountImage = accountIcon(current); int detailIconSize = Ui.iconDp(this, 54);
        if (Ui.iconsOnRight(this)) identity.addView(accountImage, new LinearLayout.LayoutParams(detailIconSize, detailIconSize));
        LinearLayout text = Ui.column(this); text.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        text.addView(Ui.text(this, current.name, 18, Ui.TEXT, true));
        text.addView(Ui.text(this, current.currency, 12, Ui.MUTED, false));
        identity.addView(text, new LinearLayout.LayoutParams(0, Math.max(Ui.dp(this, 58), detailIconSize), 1));
        if (!Ui.iconsOnRight(this)) identity.addView(accountImage, new LinearLayout.LayoutParams(detailIconSize, detailIconSize));
        content.addView(identity);
        TextView balance = Ui.text(this, Money.format(current.balanceMinor, current.currency), 26, Ui.BRAND, true);
        balance.setGravity(Gravity.CENTER); balance.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        content.addView(balance, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)));
        TextView hint = Ui.text(this, "التعديل اليدوي يضبط الرصيد الفعلي مباشرة، ولا يُسجَّل كدخل أو مصروف.", 12, Ui.MUTED, false);
        hint.setGravity(Gravity.CENTER); content.addView(hint);
        Button edit = actionButton("تعديل الرصيد يدويًا", true);
        edit.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 16, this));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        editParams.topMargin = Ui.dp(this, 14); content.addView(edit, editParams);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تفاصيل الحساب")
                .setView(content).setPositiveButton("إغلاق", null).create();
        edit.setOnClickListener(v -> { dialog.dismiss(); showSetAccountBalanceDialog(current); });
        dialog.show();
    }

    private void showSetAccountBalanceDialog(Models.Account account) {
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 8));
        form.addView(Ui.text(this, account.name + " — " + account.currency, 14, Ui.MUTED, false));
        EditText amount = input("الرصيد الفعلي");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        amount.setText(String.format(Locale.ROOT, "%.2f", account.balanceMinor / 100.0));
        amount.setSelectAllOnFocus(true); form.addView(amount, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تعديل الرصيد يدويًا")
                .setView(form).setNegativeButton("إلغاء", null).setPositiveButton("حفظ الرصيد", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Long value = Money.parseMinor(amount.getText().toString());
            if (value == null) { amount.setError("أدخل رصيدًا صحيحًا"); return; }
            try {
                db.setAccountBalance(account.id, value);
                dialog.dismiss(); Toast.makeText(this, "تم تحديث رصيد " + account.name, Toast.LENGTH_SHORT).show();
                showMain(false);
            } catch (Exception error) { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
        }));
        dialog.show();
    }

    private View transactionCard(Models.Transaction tx) {
        LinearLayout card = Ui.row(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        card.setBackground(Ui.rounded(Ui.SURFACE, 18, this));

        int amountColor = tx.specialKind != null ? Ui.VIOLET : FinanceRules.EXPENSE.equals(tx.type) ? Ui.EXPENSE :
                FinanceRules.INCOME.equals(tx.type) ? Ui.INCOME : Ui.BRAND;
        String emoji = FinanceRules.SPECIAL_DEBT.equals(tx.specialKind) ? "▤" : FinanceRules.SPECIAL_DEBT_PAYMENT.equals(tx.specialKind) ? "✓" : tx.categoryEmoji != null ? tx.categoryEmoji : FinanceRules.TRANSFER.equals(tx.type) ? "↔" : "💰";
        TextView icon = Ui.text(this, emoji, 20, amountColor, false);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.rounded(Color.argb(28, Color.red(amountColor), Color.green(amountColor), Color.blue(amountColor)), 18, this));
        int transactionPercent = Ui.homeIconScalePercent(this, "transactions");
        int transactionIconSize = Ui.homeIconDp(this, "transactions", 44);
        icon.setTextSize(20f * transactionPercent / 100f);
        if (Ui.iconsOnRight(this)) card.addView(icon, new LinearLayout.LayoutParams(transactionIconSize, transactionIconSize));

        String titleValue;
        if (FinanceRules.SPECIAL_DEBT.equals(tx.specialKind)) titleValue = "دَين — " + (tx.debtPersonName == null ? "شخص" : tx.debtPersonName);
        else if (FinanceRules.SPECIAL_DEBT_PAYMENT.equals(tx.specialKind)) titleValue = "سداد دين — " + (tx.debtPersonName == null ? "شخص" : tx.debtPersonName);
        else if (tx.needsReview) titleValue = "بانتظار التصنيف";
        else if (FinanceRules.TRANSFER.equals(tx.type)) titleValue = "تحويل إلى " + (tx.destinationName == null ? "حساب آخر" : tx.destinationName);
        else if (tx.categoryName != null) titleValue = tx.categoryName;
        else if (tx.hasSplits) titleValue = "مصروف مقسّم";
        else titleValue = "غير مصنفة";

        LinearLayout info = Ui.column(this);
        info.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        TextView transactionTitle=Ui.text(this, titleValue, 15, Ui.TEXT, true);
        transactionTitle.setSingleLine(true);
        transactionTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(transactionTitle);
        String secondary = tx.accountName + (tx.affectsBalance ? "" : " • للتسجيل فقط") +
                (tx.note == null || tx.note.isEmpty() ? "" : " • " + tx.note);
        TextView transactionSubtitle=Ui.text(this, secondary, 11, Ui.MUTED, false);
        transactionSubtitle.setSingleLine(true);
        transactionSubtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(transactionSubtitle);
        String date = new SimpleDateFormat("dd/MM/yyyy  hh:mm a", new Locale("ar")).format(new Date(tx.createdAt));
        info.addView(Ui.text(this, date, 10, Ui.MUTED, false));
        boolean compactTransactionCard=getResources().getConfiguration().screenWidthDp < 390;

        String prefix = FinanceRules.SPECIAL_DEBT.equals(tx.specialKind) ? "• " : FinanceRules.EXPENSE.equals(tx.type) ? "−" : FinanceRules.INCOME.equals(tx.type) ? "+" : "↔ ";
        TextView amount = Ui.text(this, prefix + Money.format(tx.amountMinor, tx.currency), 14, amountColor, true);
        amount.setGravity((compactTransactionCard?Gravity.START:Gravity.END) | Gravity.CENTER_VERTICAL);
        if(compactTransactionCard) info.addView(amount);
        card.addView(info, new LinearLayout.LayoutParams(0, Ui.dp(this, compactTransactionCard?80:58), 1));
        if(!compactTransactionCard) card.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 58)));
        if (!Ui.iconsOnRight(this)) card.addView(icon, new LinearLayout.LayoutParams(transactionIconSize, transactionIconSize));
        card.setOnClickListener(v -> showTransactionDetails(tx));
        Ui.pressAnimation(card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(Ui.dp(this, compactTransactionCard?106:82), transactionIconSize + Ui.dp(this, 24)));
        params.setMargins(Ui.dp(this, 16), Ui.dp(this, 5), Ui.dp(this, 16), Ui.dp(this, 5));
        card.setLayoutParams(params);
        return card;
    }

    private void showTransactionDetails(Models.Transaction tx) {
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 22), Ui.dp(this, 8), Ui.dp(this, 22), Ui.dp(this, 8));
        int color = FinanceRules.EXPENSE.equals(tx.type) ? Ui.EXPENSE : FinanceRules.INCOME.equals(tx.type) ? Ui.INCOME : Ui.VIOLET;
        String kind = FinanceRules.EXPENSE.equals(tx.type) ? "مصروف" : FinanceRules.INCOME.equals(tx.type) ? "دخل" : "تحويل";
        TextView amount = Ui.text(this, Money.format(tx.amountMinor, tx.currency), 25, color, true);
        amount.setGravity(Gravity.CENTER);
        content.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)));
        content.addView(detailLine("نوع العملية", kind));
        content.addView(detailLine("الحساب", tx.accountName));
        content.addView(detailLine("يؤثر على الرصيد", tx.affectsBalance ? "نعم" : "لا — للتسجيل فقط"));
        if (tx.specialKind != null) {
            content.addView(detailLine("تصنيف الدين", FinanceRules.SPECIAL_DEBT.equals(tx.specialKind) ? "دَين" : "سداد دين"));
            content.addView(detailLine("الشخص", tx.debtPersonName == null ? "غير محدد" : tx.debtPersonName));
            content.addView(detailLine("اتجاه الدين", FinanceRules.DEBT_RECEIVABLE.equals(tx.debtDirection) ? "إلي" : "عليّ"));
        }
        if (tx.destinationName != null) content.addView(detailLine("إلى حساب", tx.destinationName));
        if (tx.destinationAmountMinor != null) {
            content.addView(detailLine("المبلغ المستلم",Money.format(tx.destinationAmountMinor,tx.destinationCurrency)));
            content.addView(detailLine("سعر الصرف المستخدم",tx.exchangeRate==null?"غير محدد":tx.exchangeRate));
            content.addView(detailLine("مصدر السعر", "MANUAL".equals(tx.exchangeRateSource)?"يدوي":tx.exchangeRateSource));
        }
        if (tx.categoryName != null) content.addView(detailLine("التصنيف", (tx.categoryEmoji == null ? "" : tx.categoryEmoji + " ") + tx.categoryName));

        List<Models.SplitDetail> splits = db.getSplitDetails(tx.id);
        if (!splits.isEmpty()) {
            TextView splitTitle = Ui.text(this, "تفاصيل التقسيم", 15, Ui.TEXT, true);
            splitTitle.setPadding(0, Ui.dp(this, 15), 0, Ui.dp(this, 6));
            content.addView(splitTitle);
            for (Models.SplitDetail split : splits) {
                content.addView(detailLine(split.displayName(), Money.format(split.amountMinor, tx.currency)));
            }
        }
        if (tx.note != null && !tx.note.isEmpty()) content.addView(detailLine("الملاحظة", tx.note));
        if (tx.rawText != null && !tx.rawText.isEmpty()) {
            TextView rawTitle = Ui.text(this, "نص الرسالة الأصلي", 15, Ui.TEXT, true);
            rawTitle.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
            content.addView(rawTitle);
            TextView raw = Ui.text(this, tx.rawText, 13, Ui.TEXT, false);
            raw.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            raw.setBackground(Ui.rounded(Ui.BRAND_SOFT, 12, this));
            content.addView(raw);
        }
        String date = new SimpleDateFormat("dd/MM/yyyy  hh:mm a", new Locale("ar")).format(new Date(tx.createdAt));
        content.addView(detailLine("التاريخ", date));

        LinearLayout actions = Ui.row(this);
        Button edit = actionButton(tx.specialKind != null ? "تعديل حركة الدين" : tx.hasSplits ? "تعديل التقسيم" : "تعديل / تقسيم", false);
        Button delete = actionButton("حذف", false); delete.setTextColor(Ui.EXPENSE);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1); editParams.setMargins(0, Ui.dp(this, 10), Ui.dp(this, 4), 0);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 48)); deleteParams.setMargins(Ui.dp(this, 4), Ui.dp(this, 10), 0, 0);
        actions.addView(edit, editParams); actions.addView(delete, deleteParams); content.addView(actions);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تفاصيل العملية")
                .setView(content)
                .setPositiveButton("إغلاق", null).create();
        edit.setOnClickListener(v -> { dialog.dismiss(); showTransactionEditMenu(tx); });
        delete.setOnClickListener(v -> { dialog.dismiss(); confirmDeleteTransaction(tx); });
        dialog.show();
    }

    private void showTransactionEditMenu(Models.Transaction tx) {
        if (tx.destinationAmountMinor != null) {
            Toast.makeText(this,"لتغيير تحويل عملتين: احذفه بعد المراجعة ثم سجّل التحويل الصحيح؛ الحذف يعكس الرصيدين",Toast.LENGTH_LONG).show();
            return;
        }
        if (tx.specialKind != null) { showEditDebtTransactionDialog(tx); return; }
        boolean simpleEditable=db.canEditSimpleManualTransaction(tx.id);
        String[] standardOptions = {"مصروف — اختيار تصنيف", "دخل — اختيار تصنيف",
                "تقسيم المصروف", "تحويل بين حساباتي", "دَين", "سداد دين"};
        String[] options;
        if(simpleEditable) {
            options=new String[standardOptions.length+1];
            options[0]="✏️ تعديل المبلغ والملاحظة";
            System.arraycopy(standardOptions,0,options,1,standardOptions.length);
        } else options=standardOptions;
        new AlertDialog.Builder(this).setTitle("تعديل العملية").setItems(options, (d, which) -> {
            if(simpleEditable && which==0) {
                showSimpleManualTransactionEditDialog(tx);
                return;
            }
            int action=simpleEditable?which-1:which;
            if (action == 0) showCategoryPicker(tx, FinanceRules.EXPENSE);
            else if (action == 1) showCategoryPicker(tx, FinanceRules.INCOME);
            else if (action == 2) showSplitTransactionDialog(tx);
            else if (action == 3) showTransactionTransferDialog(tx);
            else if (action == 4) showDebtClassificationDialog(tx, FinanceRules.SPECIAL_DEBT);
            else showDebtClassificationDialog(tx, FinanceRules.SPECIAL_DEBT_PAYMENT);
        }).setNegativeButton("إلغاء", null).show();
    }

    private void showSimpleManualTransactionEditDialog(Models.Transaction tx) {
        LinearLayout form=Ui.column(this);
        form.setPadding(Ui.dp(this,18),Ui.dp(this,8),Ui.dp(this,18),Ui.dp(this,8));
        form.addView(Ui.text(this,"سيُعدّل التطبيق الرصيد تلقائيًا بمقدار الفرق، مع الاحتفاظ بالتاريخ والتصنيف وسجل التدقيق.",12,Ui.MUTED,false));
        EditText amount=input("المبلغ");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(java.math.BigDecimal.valueOf(tx.amountMinor,2).toPlainString());
        amount.setSelectAllOnFocus(true);
        EditText note=input("الملاحظة");
        note.setText(tx.note==null?"":tx.note);
        note.setMinLines(2);
        form.addView(amount,fieldParams());
        form.addView(note,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("تعديل العملية اليدوية").setView(form)
                .setNegativeButton("إلغاء",null).setPositiveButton("حفظ التعديل",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            Long value=Money.parseMinor(amount.getText().toString());
            if(value==null || value<=0) { amount.setError("أدخل مبلغًا موجبًا صحيحًا"); return; }
            try {
                db.editSimpleManualTransaction(tx.id,tx.amountMinor,tx.note,value,note.getText().toString());
                dialog.dismiss();
                Toast.makeText(this,"تم تعديل العملية والرصيد بأمان",Toast.LENGTH_SHORT).show();
                showMain(transactionsTab);
            } catch(Exception error) {
                Toast.makeText(this,error.getMessage()==null?"تعذّر تعديل العملية":error.getMessage(),Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void showDebtClassificationDialog(Models.Transaction tx, String specialKind) {
        if (!FinanceRules.EXPENSE.equals(tx.type) && !FinanceRules.INCOME.equals(tx.type)) {
            Toast.makeText(this, "حوّل العملية أولًا إلى مصروف أو دخل", Toast.LENGTH_LONG).show(); return;
        }
        String inferredDirection = FinanceRules.INCOME.equals(tx.type) ? FinanceRules.DEBT_RECEIVABLE : FinanceRules.DEBT_PAYABLE;
        List<Models.DebtPerson> people = db.getDebtPeople();
        if (people.isEmpty()) {
            showQuickAddDebtPerson(() -> showDebtClassificationDialog(tx, specialKind)); return;
        }
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));
        List<String> names=new ArrayList<>();for(Models.DebtPerson p:people)names.add(p.name);Spinner person=spinner(names);
        form.addView(label("الدَين لمين؟"));form.addView(person,fieldParams());
        RadioGroup directionGroup=new RadioGroup(this);directionGroup.setOrientation(RadioGroup.HORIZONTAL);directionGroup.setGravity(Gravity.CENTER);
        RadioButton receivable=new RadioButton(this);receivable.setText("إلي");RadioButton payable=new RadioButton(this);payable.setText("عليّ");directionGroup.addView(receivable);directionGroup.addView(payable);
        if(FinanceRules.DEBT_RECEIVABLE.equals(inferredDirection))receivable.setChecked(true);else payable.setChecked(true);
        form.addView(label("اتجاه الدين"));form.addView(directionGroup,fieldParams());
        form.addView(Ui.text(this,"سيحدد التطبيق الحركة من اتجاه المال: الوارد من شخص مدين لك سداد، والصادر له زيادة دين. والعكس في ديون «عليّ».",11,Ui.MUTED,false));
        Button quick=actionButton("＋ إضافة شخص جديد",false);quick.setOnClickListener(v->showQuickAddDebtPerson(()->showDebtClassificationDialog(tx,specialKind)));form.addView(quick,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(FinanceRules.SPECIAL_DEBT.equals(specialKind)?"تصنيف دَين":"تصنيف سداد دين").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{String direction=receivable.isChecked()?FinanceRules.DEBT_RECEIVABLE:FinanceRules.DEBT_PAYABLE;String actual=debtKindForMoneyDirection(tx.type,direction);db.reclassifyTransactionAsDebt(tx.id,actual,people.get(person.getSelectedItemPosition()).id,direction);dialog.dismiss();Toast.makeText(this,FinanceRules.SPECIAL_DEBT_PAYMENT.equals(actual)?"تم تسجيلها كسداد دين":"تمت إضافتها إلى أصل الدين",Toast.LENGTH_SHORT).show();showMain(transactionsTab);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
        }));dialog.show();
    }

    private String debtKindForMoneyDirection(String type, String direction) {
        return DebtLedgerRules.movementKind(type, direction);
    }

    private void showEditDebtTransactionDialog(Models.Transaction tx) {
        List<Models.DebtPerson> people=db.getDebtPeople();
        boolean found=false;for(Models.DebtPerson p:people)if(tx.debtPersonId!=null&&p.id==tx.debtPersonId)found=true;
        if(!found&&tx.debtPersonId!=null){Models.DebtPerson current=new Models.DebtPerson();current.id=tx.debtPersonId;current.name=tx.debtPersonName;people.add(0,current);}
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));EditText amount=input("المبلغ");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setText(String.format(Locale.ROOT,"%.2f",tx.amountMinor/100.0));
        List<String> names=new ArrayList<>();for(Models.DebtPerson p:people)names.add(p.name);Spinner person=spinner(names);for(int i=0;i<people.size();i++)if(tx.debtPersonId!=null&&people.get(i).id==tx.debtPersonId)person.setSelection(i);
        form.addView(amount,fieldParams());form.addView(label("الشخص"));form.addView(person,fieldParams());
        RadioGroup directionGroup=new RadioGroup(this);directionGroup.setOrientation(RadioGroup.HORIZONTAL);directionGroup.setGravity(Gravity.CENTER);
        RadioButton receivable=new RadioButton(this);receivable.setText("إلي");RadioButton payable=new RadioButton(this);payable.setText("عليّ");directionGroup.addView(receivable);directionGroup.addView(payable);
        if(FinanceRules.DEBT_RECEIVABLE.equals(tx.debtDirection))receivable.setChecked(true);else payable.setChecked(true);
        form.addView(label("اتجاه الدين"));form.addView(directionGroup,fieldParams());
        form.addView(Ui.text(this,"يمكنك من القائمة التالية تغيير العملية إلى تصنيف عادي؛ وسيعكس التطبيق أثر الدين السابق تلقائيًا.",11,Ui.MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تعديل حركة الدين").setView(form).setNeutralButton("تغيير التصنيف",null).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{Long value=Money.parseMinor(amount.getText().toString());if(value==null||value<=0)throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");String direction=receivable.isChecked()?FinanceRules.DEBT_RECEIVABLE:FinanceRules.DEBT_PAYABLE;db.updateDebtTransaction(tx.id,value,people.get(person.getSelectedItemPosition()).id,direction);dialog.dismiss();showMain(transactionsTab);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}});
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dialog.dismiss();String[] options={"مصروف", "دخل", "تحويل بين حساباتي"};new AlertDialog.Builder(this).setTitle("تغيير التصنيف").setItems(options,(d,w)->{if(w==0)showCategoryPicker(tx,FinanceRules.EXPENSE);else if(w==1)showCategoryPicker(tx,FinanceRules.INCOME);else showTransactionTransferDialog(tx);}).show();});});dialog.show();
    }

    private void showCategoryPicker(Models.Transaction tx, String type) {
        List<Models.Category> categories = db.getCategories(type);
        String[] labels = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) labels[i] = categories.get(i).displayName();
        String title = FinanceRules.EXPENSE.equals(type) ? "اختر تصنيف المصروف" : "اختر تصنيف الدخل";
        new AlertDialog.Builder(this).setTitle(title).setItems(labels, (d, which) -> {
            try {
                db.reclassifyTransaction(tx.id, type, tx.accountId, null, categories.get(which).id, new ArrayList<>());
                Toast.makeText(this, "تم تحديث العملية والرصيد", Toast.LENGTH_SHORT).show(); showMain(transactionsTab);
            } catch (Exception error) { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
        }).setNegativeButton("إلغاء", null).show();
    }

    private void showSplitTransactionDialog(Models.Transaction tx) {
        if (!FinanceRules.EXPENSE.equals(tx.type)) {
            Toast.makeText(this, "تقسيم المصروف متاح للعمليات الصادرة فقط", Toast.LENGTH_LONG).show();
            return;
        }
        List<Models.Category> categories = db.getCategories(FinanceRules.EXPENSE);
        List<Models.DebtPerson> people = db.getDebtPeople();
        List<Models.SplitDetail> existing = db.getSplitDetails(tx.id);
        for (Models.SplitDetail split : existing) if (split.debtPersonId != null) {
            boolean found = false;
            for (Models.DebtPerson person : people) if (person.id == split.debtPersonId) found = true;
            if (!found) {
                Toast.makeText(this, "فعّل صاحب الدين من إدارة الأشخاص قبل تعديل التقسيم", Toast.LENGTH_LONG).show();
                return;
            }
        }
        List<SplitRowRef> rows = new ArrayList<>();
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 18), Ui.dp(this, 4), Ui.dp(this, 18), Ui.dp(this, 8));
        form.addView(Ui.text(this, "المبلغ الكامل: " + Money.format(tx.amountMinor, tx.currency) + " — يجب أن يساوي مجموع الأجزاء.", 12, Ui.MUTED, false));
        LinearLayout rowsBox = Ui.column(this); form.addView(rowsBox);
        if (existing.isEmpty()) {
            addDebtSplitRow(rowsBox, rows, categories, people); addDebtSplitRow(rowsBox, rows, categories, people);
        } else for (Models.SplitDetail split : existing) {
            addDebtSplitRow(rowsBox, rows, categories, people);
            SplitRowRef row = rows.get(rows.size() - 1);
            for (int i = 0; i < categories.size(); i++) if (categories.get(i).id == split.categoryId) { row.category.setSelection(i); break; }
            if (row.mode != null) row.mode.setSelection(
                    FinanceRules.SPECIAL_DEBT.equals(split.specialKind) ? 1 :
                    FinanceRules.SPECIAL_DEBT_PAYMENT.equals(split.specialKind) ? 2 : 0);
            if (row.person != null && split.debtPersonId != null) for(int i=0;i<people.size();i++)
                if (people.get(i).id==split.debtPersonId) { row.person.setSelection(i); break; }
            row.amount.setText(String.format(Locale.ROOT, "%.2f", split.amountMinor / 100.0));
        }
        Button add = actionButton("＋ إضافة جزء", false); add.setOnClickListener(v -> addDebtSplitRow(rowsBox, rows, categories, people)); form.addView(add, fieldParams());
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تقسيم العملية")
                .setView(scroll).setNegativeButton("إلغاء", null).setPositiveButton("حفظ التقسيم", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                if (rows.size() < 2) throw new IllegalArgumentException("التقسيم يحتاج جزأين على الأقل");
                List<Models.SplitInput> parts = new ArrayList<>(); List<Long> amounts = new ArrayList<>();
                for (SplitRowRef row : rows) {
                    Long value = Money.parseMinor(row.amount.getText().toString());
                    if (value == null || value <= 0) throw new IllegalArgumentException("أكمل مبلغ كل جزء");
                    int mode = row.mode == null ? 0 : row.mode.getSelectedItemPosition();
                    if (mode == 0) {
                        Models.Category category = categories.get(row.category.getSelectedItemPosition());
                        parts.add(new Models.SplitInput(category.id, value));
                    } else {
                        if (people.isEmpty() || row.person == null || row.person.getSelectedItemPosition() < 0)
                            throw new IllegalArgumentException("أضف الشخص من صفحة الديون أولًا");
                        Models.DebtPerson person = people.get(row.person.getSelectedItemPosition());
                        parts.add(Models.SplitInput.debt(value,
                                mode == 1 ? FinanceRules.SPECIAL_DEBT : FinanceRules.SPECIAL_DEBT_PAYMENT,
                                person.id, mode == 1 ? FinanceRules.DEBT_RECEIVABLE : FinanceRules.DEBT_PAYABLE));
                    }
                    amounts.add(value);
                }
                if (!FinanceRules.splitsMatch(tx.amountMinor, amounts)) throw new IllegalArgumentException("مجموع الأجزاء لا يساوي مبلغ العملية");
                db.reclassifyTransaction(tx.id, FinanceRules.EXPENSE, tx.accountId, null, null, parts);
                dialog.dismiss(); Toast.makeText(this, "تم حفظ التقسيم وتحديث العملية", Toast.LENGTH_SHORT).show(); showMain(transactionsTab);
            } catch (Exception error) { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
        }));
        dialog.show();
    }

    private void confirmDeleteTransaction(Models.Transaction tx) {
        String message = tx.affectsBalance
                ? "سيتم حذف العملية وإرجاع أثرها على الرصيد تلقائيًا."
                : "سيتم حذف العملية. لم تكن تؤثر على الرصيد.";
        new AlertDialog.Builder(this).setTitle("حذف العملية؟").setMessage(message)
                .setNegativeButton("إلغاء", null).setPositiveButton("حذف", (d, w) -> {
                    try { db.deleteTransaction(tx.id); Toast.makeText(this, "حُذفت العملية وتم تحديث الرصيد", Toast.LENGTH_SHORT).show(); showMain(transactionsTab); }
                    catch (Exception error) { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
                }).show();
    }

    private void showFxTransferDialog() { showFxTransferDialog(null,null,null); }

    private void showFxTransferDialog(Long preferredSourceId,Long preferredDestinationId,String initialAmount) {
        List<Models.Account> accounts=db.getAccounts();
        if(accounts.size()<2){Toast.makeText(this,"أضف حسابين مختلفين أولًا",Toast.LENGTH_LONG).show();return;}
        boolean hasDifferentCurrencies=false;
        for(Models.Account first:accounts){
            for(Models.Account second:accounts){
                if(!first.currency.equals(second.currency)){hasDifferentCurrencies=true;break;}
            }
            if(hasDifferentCurrencies)break;
        }
        if(!hasDifferentCurrencies){Toast.makeText(this,"التحويل بين عملتين يحتاج حسابين بعملتين مختلفتين",Toast.LENGTH_LONG).show();return;}
        List<String> options=new ArrayList<>();for(Models.Account a:accounts)options.add(a.displayName());
        LinearLayout form=Ui.column(this);
        form.setPadding(Ui.dp(this,18),Ui.dp(this,4),Ui.dp(this,18),Ui.dp(this,12));
        form.addView(label("من حساب"));Spinner source=spinner(options);form.addView(source,fieldParams());
        form.addView(label("إلى حساب"));Spinner destination=spinner(options);form.addView(destination,fieldParams());
        form.addView(label("المبلغ من الحساب المصدر"));EditText amount=input("المبلغ");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount,fieldParams());
        if(initialAmount!=null)amount.setText(initialAmount);
        if(preferredSourceId!=null)for(int i=0;i<accounts.size();i++)
            if(accounts.get(i).id==preferredSourceId){source.setSelection(i);break;}
        if(preferredDestinationId!=null)for(int i=0;i<accounts.size();i++)
            if(accounts.get(i).id==preferredDestinationId){destination.setSelection(i);break;}
        TextView rateGuide=Ui.text(this,"سعر الصرف: كم وحدة يستلم الحساب الثاني مقابل 1 من عملة الحساب الأول",12,Ui.MUTED,false);
        form.addView(rateGuide,fieldParams());
        EditText rate=input("سعر الصرف الفعلي");
        rate.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(rate,fieldParams());
        LinearLayout rateButtons=Ui.row(this);
        Button saveRate=miniButton("حفظ");
        Button refreshRate=miniButton("تحديث أونلاين");
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1);
        left.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        rateButtons.addView(saveRate,left);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,Ui.dp(this,44),1);
        right.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        rateButtons.addView(refreshRate,right);
        form.addView(rateButtons,fieldParams());
        TextView rateStatus=Ui.text(this,"السعر يدوي حتى تحميل تسعيرة إرشادية من بنك فلسطين",12,Ui.MUTED,false);
        form.addView(rateStatus,fieldParams());
        final boolean[] verifiedOnline={false};
        final long[] onlineTime={0L};
        final String[] onlineValue={""};
        final boolean[] loadingQuote={false};
        final boolean[] dialogVisible={true};
        final boolean[] autoAttemptedThisDialog={false};
        final Runnable[] onPairChanged={null};
        final android.content.SharedPreferences onlinePrefs=getSharedPreferences("fx_online_rates",MODE_PRIVATE);
        TextView preview=Ui.text(this,"",15,Ui.BRAND,true);
        form.addView(preview,fieldParams());
        TextView warning=Ui.text(this,"أدخل السعر الفعلي من البنك أو المحفظة. التسعيرة المنشورة إرشادية ولا تحلّ محل السعر المدخل دون موافقتك.",11,Ui.MUTED,false);
        form.addView(warning,fieldParams());
        Runnable recalc=()->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si<0 || di<0)return;
            Models.Account a=accounts.get(si),b=accounts.get(di);
            rateGuide.setText("كل 1 "+a.currency+" = ? "+b.currency);
            Long n=Money.parseMinor(amount.getText().toString());
            try {if(n==null)return;long converted=FxTransferMath.destinationMinor(n,rate.getText().toString());
                preview.setText("سيُخصم "+Money.format(n,a.currency)+" ويُضاف "+Money.format(converted,b.currency));
            } catch(Exception error){preview.setText("أدخل المبلغ والسعر الصحيحين");}
        };
        // Never overwrite a manually entered rate when accounts are reselected.
        Runnable loadSavedRate=()->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si<0 || di<0)return;
            String key=accounts.get(si).currency+"_"+accounts.get(di).currency;
            rate.setText(getSharedPreferences("fx_manual_rates",MODE_PRIVATE).getString(key,""));
            verifiedOnline[0]=false;onlineValue[0]="";onlineTime[0]=0L;
            if("USD_ILS".equals(key)){
                String oldQuote=onlinePrefs.getString("USD_ILS_rate","");
                long oldTime=onlinePrefs.getLong("USD_ILS_at",0L);
                if(!oldQuote.isEmpty() && oldTime>0){
                    rateStatus.setText("تسعيرة بنك فلسطين الإرشادية: "+oldQuote+" شيكل/دولار • "+
                        new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT).format(new Date(oldTime))+" • اضغط للمراجعة");
                }else rateStatus.setText("تسعيرة بنك فلسطين غير متوفرة؛ يمكنك إدخال السعر يدويًا");
            }else rateStatus.setText("سعر يدوي لهذا الزوج من العملات");
            recalc.run();
            if(onPairChanged[0]!=null)onPairChanged[0].run();
        };
        Runnable confirmCachedRate=()->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si<0 || di<0 || !"USD".equals(accounts.get(si).currency) || !"ILS".equals(accounts.get(di).currency))return;
            final String quote=onlinePrefs.getString("USD_ILS_rate","");
            final long checked=onlinePrefs.getLong("USD_ILS_at",0L);
            if(quote.isEmpty() || checked<=0)return;
            new AlertDialog.Builder(this).setTitle("تسعيرة بنك فلسطين (شراء الدولار)")
                .setMessage("كل 1 USD ≈ "+quote+" ILS (إرشادي). تاريخ الجلب: "+
                    new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT).format(new Date(checked))+". السعر الفعلي ورسوم التحويل قد تختلف.")
                .setNegativeButton("احتفظ بالسعر المكتوب",null)
                .setPositiveButton("استخدام السعر",(d,w)->{
                    rate.setText(quote);verifiedOnline[0]=true;onlineTime[0]=checked;onlineValue[0]=quote;
                    rateStatus.setText("تم اختيار السعر الإرشادي. تأكد من السعر الفعلي قبل تأكيد التحويل.");
                }).show();
        };
        rateStatus.setOnClickListener(v->confirmCachedRate.run());
        // true: explicit refresh opens the quote confirmation; false: silent daily refresh.
        java.util.function.Consumer<Boolean> downloadOnline=manual->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si<0 || di<0)return;
            Models.Account a=accounts.get(si),b=accounts.get(di);
            if(!"USD".equals(a.currency)||!"ILS".equals(b.currency)){
                if(manual)Toast.makeText(this,"سعر البنك الأونلاين متاح من USD إلى ILS فقط",Toast.LENGTH_LONG).show();
                return;
            }
            if(loadingQuote[0])return;
            loadingQuote[0]=true;refreshRate.setEnabled(false);
            rateStatus.setText("جارٍ جلب التسعيرة الإرشادية من بنك فلسطين...");
            new Thread(()->{
                try{
                    final String quote=BopRateFetcher.fetchBuyUsdIls();final long fetchedAt=System.currentTimeMillis();
                    runOnUiThread(()->{
                        loadingQuote[0]=false;refreshRate.setEnabled(true);
                        onlinePrefs.edit().putString("USD_ILS_rate",quote).putLong("USD_ILS_at",fetchedAt).apply();
                        if(!dialogVisible[0])return;
                        int activeSource=source.getSelectedItemPosition(),activeDestination=destination.getSelectedItemPosition();
                        if(activeSource<0 || activeDestination<0 || !"USD".equals(accounts.get(activeSource).currency)
                                || !"ILS".equals(accounts.get(activeDestination).currency))return;
                        rateStatus.setText("تسعيرة البنك الإرشادية: "+quote+" شيكل/دولار • اضغط للمراجعة");
                        if(manual)confirmCachedRate.run();
                    });
                }catch(Exception error){runOnUiThread(()->{
                    loadingQuote[0]=false;refreshRate.setEnabled(true);
                    if(dialogVisible[0])rateStatus.setText("لم تتوفر تسعيرة موثوقة من البنك؛ أدخل السعر الفعلي يدويًا");
                });}
            },"wen-bop-rate-fetch").start();
        };
        onPairChanged[0]=()->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si<0 || di<0 || !"USD".equals(accounts.get(si).currency) || !"ILS".equals(accounts.get(di).currency))return;
            if(autoAttemptedThisDialog[0] || loadingQuote[0])return;
            long lastFetched=onlinePrefs.getLong("USD_ILS_at",0L);
            if(FxRateRefreshPolicy.needsDailyRefresh(lastFetched,System.currentTimeMillis(),
                    java.util.TimeZone.getDefault())){
                autoAttemptedThisDialog[0]=true;downloadOnline.accept(false);
            }
        };
        refreshRate.setOnClickListener(v->downloadOnline.accept(true));
        saveRate.setOnClickListener(v->{
            try{
                int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
                if(si<0 || di<0)throw new IllegalArgumentException("اختر حسابين");
                String key=accounts.get(si).currency+"_"+accounts.get(di).currency;
                FxTransferMath.destinationMinor(100,rate.getText().toString());
                getSharedPreferences("fx_manual_rates",MODE_PRIVATE).edit().putString(key,rate.getText().toString().trim()).apply();
                Toast.makeText(this,"تم حفظ السعر المدخل لهذا الزوج",Toast.LENGTH_SHORT).show();
            }catch(Exception err){Toast.makeText(this,"أدخل سعر صرف صالحًا أولًا",Toast.LENGTH_SHORT).show();}
        });
        source.setOnItemSelectedListener(new SimpleItemSelected(loadSavedRate));
        destination.setOnItemSelectedListener(new SimpleItemSelected(loadSavedRate));
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){recalc.run();}
            public void afterTextChanged(Editable e){}};
        amount.addTextChangedListener(watcher);rate.addTextChangedListener(watcher);
        // Prevent submitting a same-currency pair from the FX-specific form.
        Runnable validatePair=()->{
            int si=source.getSelectedItemPosition(),di=destination.getSelectedItemPosition();
            if(si>=0 && di>=0 && accounts.get(si).currency.equals(accounts.get(di).currency))
                preview.setText("اختر حسابين بعملتين مختلفتين، أو استخدم التحويل العادي");
        };
        source.setOnItemSelectedListener(new SimpleItemSelected(()->{loadSavedRate.run();validatePair.run();}));
        destination.setOnItemSelectedListener(new SimpleItemSelected(()->{loadSavedRate.run();validatePair.run();}));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تحويل بين عملتين")
            .setView(scroll).setNegativeButton("إلغاء",null).setPositiveButton("تأكيد التحويل",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                Models.Account from=accounts.get(source.getSelectedItemPosition()),to=accounts.get(destination.getSelectedItemPosition());
                Long minor=Money.parseMinor(amount.getText().toString());
                if(minor==null || minor<=0)throw new IllegalArgumentException("أدخل المبلغ");
                boolean onlineSelected=verifiedOnline[0] && rate.getText().toString().trim().equals(onlineValue[0]);
                db.addFxTransfer(from.id,to.id,minor,rate.getText().toString(),
                        onlineSelected?"BOP_INDICATIVE_BUY_USER_CONFIRMED":"MANUAL",onlineSelected?onlineTime[0]:System.currentTimeMillis());
                dialog.dismiss();Toast.makeText(this,"تم التحويل بين الحسابين",Toast.LENGTH_SHORT).show();showMain(false);
            }catch(Exception error){Toast.makeText(this,error.getMessage()==null?"فشل التحويل":error.getMessage(),Toast.LENGTH_LONG).show();}
        }));
        dialog.setOnDismissListener(d->dialogVisible[0]=false);
        dialog.show();loadSavedRate.run();

    }

    private void showTransactionTransferDialog(Models.Transaction tx) {
        List<Models.Account> accounts = new ArrayList<>(); for (Models.Account account : db.getAccounts()) if (account.currency.equals(tx.currency)) accounts.add(account);
        if (accounts.size() < 2) { Toast.makeText(this, "تحتاج حسابين على الأقل بعملة " + tx.currency, Toast.LENGTH_LONG).show(); return; }
        List<String> labels = new ArrayList<>(); for (Models.Account account : accounts) labels.add(account.displayName());
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        form.addView(label("من حساب")); Spinner source = spinner(labels); form.addView(source, fieldParams());
        form.addView(label("إلى حساب")); Spinner destination = spinner(labels); form.addView(destination, fieldParams());
        int sourceIndex = 0, destinationIndex = Math.min(1, accounts.size() - 1);
        for (int i = 0; i < accounts.size(); i++) { if (accounts.get(i).id == tx.accountId) sourceIndex = i; if (tx.destinationAccountId != null && accounts.get(i).id == tx.destinationAccountId) destinationIndex = i; }
        source.setSelection(sourceIndex); destination.setSelection(destinationIndex);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تحويل بين حساباتي").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Models.Account from = accounts.get(source.getSelectedItemPosition()), to = accounts.get(destination.getSelectedItemPosition());
            if (from.id == to.id) { Toast.makeText(this, "اختر حسابين مختلفين", Toast.LENGTH_SHORT).show(); return; }
            try { db.convertTransactionToTransfer(tx.id, from.id, to.id); dialog.dismiss(); Toast.makeText(this, "تم تحويل العملية إلى تحويل بين حساباتك", Toast.LENGTH_SHORT).show(); showMain(transactionsTab); }
            catch (Exception error) { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
        })); dialog.show();
    }

    private View detailLine(String label, String value) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        row.setBackground(Ui.rounded(Ui.DARK ? Color.rgb(25, 45, 53) : Color.rgb(245, 247, 251), 12, this));
        row.addView(Ui.text(this, label, 13, Ui.MUTED, false), new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        TextView answer = Ui.text(this, value, 14, Ui.TEXT, true);
        answer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(answer, new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        p.setMargins(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        row.setLayoutParams(p);
        return row;
    }

    private View emptyCard() {
        return messageCard("ابدأ بأول عملية", "اضغط زر + وسجّل مصروفًا أو دخلًا أو تحويلًا");
    }

    private View messageCard(String titleValue, String subtitleValue) {
        LinearLayout card = Ui.column(this);
        card.setGravity(Gravity.CENTER);
        card.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24));
        card.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        TextView title = Ui.text(this, titleValue, 17, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView subtitle = Ui.text(this, subtitleValue, 13, Ui.MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        card.addView(title);
        card.addView(subtitle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 112));
        params.setMargins(Ui.dp(this, 16), Ui.dp(this, 5), Ui.dp(this, 16), Ui.dp(this, 5));
        card.setLayoutParams(params);
        return card;
    }

    private View pageHeader(String titleValue, Runnable backAction) {
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        top.setBackground(Ui.layeredBackground(this, 0));
        TextView back = Ui.text(this, "‹", 34, Color.WHITE, false);
        back.setGravity(Gravity.CENTER); back.setOnClickListener(v -> backAction.run());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 54)));
        top.addView(Ui.text(this, titleValue, 22, Color.WHITE, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        return top;
    }

    private void showSettings() {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("الإعدادات", () -> showMain(false)), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this); content.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 30));

        content.addView(sectionTitle("مظهر التطبيق", Ui.THEME_NAMES.length + " تنسيقًا • يتبع وضع الجوال"));
        HorizontalScrollView themeScroll = new HorizontalScrollView(this); themeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout themeRow = Ui.row(this);
        int currentTheme = Ui.themeIndex(this);
        for (int i = 0; i < Ui.THEME_NAMES.length; i++) {
            final int index = i;
            LinearLayout card = Ui.column(this); card.setGravity(Gravity.CENTER);
            card.setBackground(new AuroraDrawable(Ui.themePreviewColors(i), Ui.dp(this, 20)));
            TextView name = Ui.text(this, (i == currentTheme ? "✓ " : "") + Ui.THEME_NAMES[i], 14, Color.WHITE, true); name.setGravity(Gravity.CENTER);
            card.addView(name); card.setOnClickListener(v -> { Ui.saveTheme(this, index); recreate(); }); Ui.pressAnimation(card);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(this, 132), Ui.dp(this, 82)); p.setMargins(Ui.dp(this, 5), 0, Ui.dp(this, 5), 0); themeRow.addView(card, p);
        }
        themeScroll.addView(themeRow); content.addView(themeScroll);

        content.addView(sectionTitle("تخصيص الواجهة", "مكان واحد ومنظّم لإعدادات الصفحة الرئيسية"));
        LinearLayout layoutCard = Ui.row(this); layoutCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        layoutCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); layoutCard.setElevation(Ui.dp(this, 3));
        LinearLayout layoutText = Ui.column(this);
        layoutText.addView(Ui.text(this, "تخطيط الصفحة الرئيسية", 15, Ui.TEXT, true));
        layoutText.addView(Ui.text(this, "الحجم والموقع أسفل الشاشة", 11, Ui.MUTED, false));
        layoutCard.addView(layoutText, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        Button customize = actionButton("تخصيص", false); customize.setOnClickListener(v -> showHomeLayoutSettings());
        layoutCard.addView(customize, new LinearLayout.LayoutParams(Ui.dp(this, 94), Ui.dp(this, 48)));
        content.addView(layoutCard);

        Button journal=actionButton("🕘 سجل التعديلات",false);
        journal.setOnClickListener(v->showAuditHistory());content.addView(journal,fieldParams());
        Button manageCategories=actionButton("🏷️ إدارة التصنيفات والرموز",false);
        manageCategories.setOnClickListener(v->showCategoryManagement());content.addView(manageCategories,fieldParams());
        content.addView(sectionTitle("حماية التطبيق", AppLockManager.enabled(this) ? "مفعّلة" : "اختيارية"));
        LinearLayout lockCard = Ui.row(this); lockCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12)); lockCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); lockCard.setElevation(Ui.dp(this, 3));
        LinearLayout lockText = Ui.column(this); lockText.addView(Ui.text(this, "رمز دخول وبصمة", 15, Ui.TEXT, true)); lockText.addView(Ui.text(this, "احمِ الأرصدة والعمليات عند فتح التطبيق", 11, Ui.MUTED, false)); lockCard.addView(lockText, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        Button lockSetup = actionButton(AppLockManager.enabled(this) ? "تعديل" : "تفعيل", false); lockSetup.setOnClickListener(v -> showAppLockDialog()); lockCard.addView(lockSetup, new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 48))); content.addView(lockCard);

        content.addView(sectionTitle("تذكير المصاريف", "اختياري"));
        LinearLayout reminderCard = Ui.column(this); reminderCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        reminderCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); reminderCard.setElevation(Ui.dp(this, 3));
        android.content.SharedPreferences reminderPrefs = ReminderScheduler.prefs(this);
        CheckBox enabled = new CheckBox(this); enabled.setText("تذكيري يوميًا لتثبيت مصاريف اليوم"); enabled.setTextColor(Ui.TEXT); enabled.setChecked(reminderPrefs.getBoolean("enabled", false));
        reminderCard.addView(enabled, fieldParams());
        final int[] chosenTime = {reminderPrefs.getInt("hour", 20), reminderPrefs.getInt("minute", 0)};
        Button time = actionButton(String.format(Locale.ROOT, "وقت التذكير  %02d:%02d", chosenTime[0], chosenTime[1]), false);
        time.setOnClickListener(v -> new TimePickerDialog(this, (picker, hour, minute) -> { chosenTime[0] = hour; chosenTime[1] = minute; time.setText(String.format(Locale.ROOT, "وقت التذكير  %02d:%02d", hour, minute)); }, chosenTime[0], chosenTime[1], true).show());
        reminderCard.addView(time, fieldParams());
        CheckBox snooze = new CheckBox(this); snooze.setText("إظهار خيار «لاحقًا» في الإشعار"); snooze.setTextColor(Ui.TEXT); snooze.setChecked(reminderPrefs.getBoolean("snooze_enabled", true));
        reminderCard.addView(snooze, fieldParams());
        Spinner snoozeMinutes = spinner(Arrays.asList("بعد 15 دقيقة", "بعد 30 دقيقة", "بعد ساعة", "بعد ساعتين"));
        int oldMinutes = reminderPrefs.getInt("snooze_minutes", 30); snoozeMinutes.setSelection(oldMinutes == 15 ? 0 : oldMinutes == 60 ? 2 : oldMinutes == 120 ? 3 : 1);
        reminderCard.addView(snoozeMinutes, fieldParams());
        Button saveReminder = actionButton("حفظ التذكير", true); saveReminder.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 16, this));
        saveReminder.setOnClickListener(v -> {
            int[] choices = {15, 30, 60, 120};
            reminderPrefs.edit().putBoolean("enabled", enabled.isChecked()).putInt("hour", chosenTime[0]).putInt("minute", chosenTime[1])
                    .putBoolean("snooze_enabled", snooze.isChecked()).putInt("snooze_minutes", choices[snoozeMinutes.getSelectedItemPosition()]).apply();
            if (enabled.isChecked() && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7101);
            ReminderScheduler.scheduleDaily(this); Toast.makeText(this, "تم حفظ التذكير", Toast.LENGTH_SHORT).show();
            AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (enabled.isChecked() && !alarms.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this).setTitle("دقة وقت التذكير")
                        .setMessage("اسمح للتطبيق بالمنبّهات الدقيقة حتى يصل التذكير في الساعة التي اخترتها بدل أن يؤخره Android.")
                        .setNegativeButton("لاحقًا", null)
                        .setPositiveButton("السماح", (d, w) -> {
                            try { startActivity(new android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + getPackageName()))); }
                            catch (Exception ignored) {}
                        }).show();
            }
        });
        reminderCard.addView(saveReminder, fieldParams()); content.addView(reminderCard);

        content.addView(sectionTitle("صوت واهتزاز الإشعارات", "إعداد مستقل لكل نوع"));
        content.addView(notificationSettingsCard());

        content.addView(sectionTitle("النسخ الاحتياطي", "Google + ملف مشفّر"));
        LinearLayout backupCard = Ui.column(this); backupCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        backupCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); backupCard.setElevation(Ui.dp(this, 3));
        boolean driveLinked = DriveBackupManager.linked(this);
        backupCard.addView(Ui.text(this, driveLinked ? "Google Drive مربوط — تُحدّث نسخة مشفّرة تلقائيًا عند مغادرة التطبيق." :
                "اربط مجلدًا داخل Google Drive من شاشة الملفات لحفظ نسخة مشفّرة قابلة للاستعادة.", 12, Ui.MUTED, false));
        if (driveLinked) {
            Button syncDrive = actionButton("مزامنة Google Drive الآن", true);
            syncDrive.setOnClickListener(v -> { Toast.makeText(this,"جاري مزامنة النسخة المشفّرة…",Toast.LENGTH_SHORT).show(); DriveBackupManager.syncAsync(this,true,(ok,message)->Toast.makeText(this,message,Toast.LENGTH_LONG).show()); });
            backupCard.addView(syncDrive, fieldParams());
            Button unlinkDrive = actionButton("إلغاء ربط Google Drive", false);
            unlinkDrive.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("إلغاء ربط Google Drive؟")
                    .setMessage("لن تُحذف النسخة الموجودة في Drive، لكن التطبيق سيتوقف عن تحديثها تلقائيًا.")
                    .setNegativeButton("تراجع",null).setPositiveButton("إلغاء الربط",(d,w)->{DriveBackupManager.unlink(this);showSettings();}).show());
            backupCard.addView(unlinkDrive, fieldParams());
        } else {
            Button linkDrive = actionButton("ربط Google Drive", true); linkDrive.setOnClickListener(v -> showDriveLinkDialog());
            backupCard.addView(linkDrive, fieldParams());
        }
        Button createBackup = actionButton("إنشاء نسخة مشفّرة", true); createBackup.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 16, this));
        createBackup.setOnClickListener(v -> showCreateBackupDialog()); backupCard.addView(createBackup, fieldParams());
        Button restoreBackup = actionButton("استعادة نسخة", false); restoreBackup.setOnClickListener(v -> {
            Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream");
            startActivityForResult(open, OPEN_BACKUP);
        });
        backupCard.addView(restoreBackup, fieldParams());
        backupCard.addView(Ui.text(this, "لا تشمل النسخة PIN المحافظ أو رمز قفل التطبيق أو التفعيل. بعد حذف التطبيق أو تغيير الهاتف ستحتاج تفعيلًا جديدًا. احتفظ بكلمة سر النسخة؛ لا يمكن استعادتها بدونها.", 11, Ui.MUTED, false));
        content.addView(backupCard);

        content.addView(sectionTitle("الالتقاط التلقائي", db.getPendingCaptureCount() + " للمراجعة"));
        LinearLayout captureCard = Ui.column(this); captureCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12)); captureCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); captureCard.setElevation(Ui.dp(this, 3));
        captureCard.addView(Ui.text(this, "يقرأ فقط رسائل BOP وJawwal Pay وإشعارات بنك فلسطين وPalPay وجوال باي. النص الأصلي يبقى محفوظًا مع العملية.", 12, Ui.MUTED, false));
        Button smsAccess = actionButton(checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED ? "✓ قراءة رسائل SMS مفعّلة" : "السماح بقراءة رسائل BOP وجوال باي", false);
        smsAccess.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS}, 7102)); captureCard.addView(smsAccess, fieldParams());
        Button notificationAccess = actionButton("فتح صلاحية الوصول للإشعارات", false);
        notificationAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))); captureCard.addView(notificationAccess, fieldParams());
        Button inbox = actionButton("صندوق المراجعة (" + db.getPendingCaptureCount() + ")", true); inbox.setBackground(Ui.gradient(Ui.VIOLET, Ui.SKY, 16, this)); inbox.setOnClickListener(v -> showCaptureInbox()); captureCard.addView(inbox, fieldParams());
        Button ownNames = actionButton("الأسماء التي تعني تحويلًا بين حساباتي (" + TransferNameStore.get(this).size() + ")", false);
        ownNames.setOnClickListener(v -> showTransferNamesDialog()); captureCard.addView(ownNames, fieldParams());
        content.addView(captureCard);

        content.addView(sectionTitle("Widgets التحويل السريع", "PIN مشفّر"));
        LinearLayout ussdCard = Ui.column(this); ussdCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        ussdCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); ussdCard.setElevation(Ui.dp(this, 3));
        TextView info = Ui.text(this, "احفظ رموزك هنا، ثم أضف إحدى Widgets وين صرفت؟ من شاشة الهاتف الرئيسية. التطبيق يفتح الاتصال فقط ولا ينفذ الحوالة تلقائيًا.", 12, Ui.MUTED, false);
        ussdCard.addView(info);
        EditText jawwalPin = pinField(SecurePinStore.read(this, "jawwal_pin") == null ? "PIN جوال باي — 4 أرقام" : "PIN جوال باي محفوظ — اكتب لتغييره");
        EditText bopPin = pinField(SecurePinStore.read(this, "bop_pin") == null ? "PIN بنك فلسطين — 4 أرقام" : "PIN البنك محفوظ — اكتب لتغييره");
        ussdCard.addView(jawwalPin, fieldParams()); ussdCard.addView(bopPin, fieldParams());
        Button savePins = actionButton("حفظ الرموز بأمان", true); savePins.setBackground(Ui.gradient(Ui.VIOLET, Ui.CORAL, 16, this));
        savePins.setOnClickListener(v -> {
            try {
                String jp = jawwalPin.getText().toString().trim(), bp = bopPin.getText().toString().trim();
                if (!jp.isEmpty() && !jp.matches("[0-9]{4}")) { jawwalPin.setError("يجب أن يكون 4 أرقام"); return; }
                if (!bp.isEmpty() && !bp.matches("[0-9]{4}")) { bopPin.setError("يجب أن يكون 4 أرقام"); return; }
                if (!jp.isEmpty()) SecurePinStore.save(this, "jawwal_pin", jp);
                if (!bp.isEmpty()) SecurePinStore.save(this, "bop_pin", bp);
                jawwalPin.setText(""); bopPin.setText(""); Toast.makeText(this, "تم حفظ الرموز مشفّرة", Toast.LENGTH_SHORT).show();
            } catch (Exception error) { Toast.makeText(this, "تعذر تشفير الرموز", Toast.LENGTH_LONG).show(); }
        });
        ussdCard.addView(savePins, fieldParams()); content.addView(ussdCard);

        content.addView(brandingFooter());

        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page); Ui.animateScreen(page);
    }

    private void showHomeLayoutSettings() {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("تخطيط الصفحة الرئيسية", this::showSettings),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this); content.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 30));
        content.addView(sectionTitle("تخصيص الصفحة الرئيسية", "المقاسات والمواقع مستقلة ومنظمة"));
        SeekBar header = homeSizeControl(content, "حجم أزرار الشريط", "header");
        SeekBar accounts = homeSizeControl(content, "حجم رموز الحسابات", "accounts");
        SeekBar transactions = homeSizeControl(content, "حجم رموز العمليات", "transactions");
        SeekBar fab = homeSizeControl(content, "حجم زر الإضافة", "fab");

        content.addView(sectionTitle("المواقع", "اختر المكان الأنسب ليدك"));
        RadioGroup headerPosition = homePositionControl(content, "زر الحسابات وزر الإعدادات", Ui.homeHeaderOnRight(this));
        RadioGroup listPosition = homePositionControl(content, "الحسابات وآخر العمليات", Ui.iconsOnRight(this));
        RadioGroup fabPosition = homePositionControl(content, "زر الإضافة", Ui.homeFabOnRight(this));

        Button save = actionButton("حفظ التخصيص", true);
        save.setOnClickListener(v -> {
            getSharedPreferences("appearance", MODE_PRIVATE).edit()
                    .putInt("home_icon_header", 70 + header.getProgress())
                    .putInt("home_icon_accounts", 70 + accounts.getProgress())
                    .putInt("home_icon_transactions", 70 + transactions.getProgress())
                    .putInt("home_icon_fab", 70 + fab.getProgress())
                    .putBoolean("home_header_right", ((RadioButton) headerPosition.getChildAt(0)).isChecked())
                    .putBoolean("icons_on_right", ((RadioButton) listPosition.getChildAt(0)).isChecked())
                    .putBoolean("home_fab_right", ((RadioButton) fabPosition.getChildAt(0)).isChecked()).apply();
            Toast.makeText(this, "تم حفظ تخصيص الصفحة الرئيسية", Toast.LENGTH_SHORT).show();
            showMain(false);
        });
        content.addView(save, fieldParams());
        Button reset = actionButton("استعادة الافتراضي", false);
        reset.setOnClickListener(v -> {
            getSharedPreferences("appearance", MODE_PRIVATE).edit()
                    .remove("home_icon_header").remove("home_icon_accounts")
                    .remove("home_icon_transactions").remove("home_icon_fab")
                    .remove("home_header_right").remove("home_fab_right")
                    .remove("icons_on_right").remove("icon_scale").apply();
            Toast.makeText(this, "تمت استعادة المقاسات والمواقع الافتراضية", Toast.LENGTH_SHORT).show();
            showMain(false);
        });
        content.addView(reset, fieldParams());
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page); Ui.animateScreen(page);
    }

    private SeekBar homeSizeControl(LinearLayout parent, String title, String key) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        card.setBackground(Ui.rounded(Ui.SURFACE, 18, this)); card.setElevation(Ui.dp(this, 2));
        TextView value = Ui.text(this, title + ": " + Ui.homeIconScalePercent(this, key) + "%", 13, Ui.TEXT, true);
        SeekBar bar = new SeekBar(this); bar.setMax(75); bar.setProgress(Ui.homeIconScalePercent(this, key) - 70);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { value.setText(title + ": " + (70 + progress) + "%"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        card.addView(value, fieldParams()); card.addView(bar, fieldParams());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4)); parent.addView(card, params);
        return bar;
    }

    private RadioGroup homePositionControl(LinearLayout parent, String title, boolean rightSelected) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        card.setBackground(Ui.rounded(Ui.SURFACE, 18, this)); card.addView(Ui.text(this, title, 13, Ui.TEXT, true));
        RadioGroup group = new RadioGroup(this); group.setOrientation(LinearLayout.HORIZONTAL); group.setGravity(Gravity.CENTER);
        RadioButton right = radio("يمين", rightSelected); right.setId(View.generateViewId());
        RadioButton left = radio("يسار", !rightSelected); left.setId(View.generateViewId());
        group.addView(right, new RadioGroup.LayoutParams(0, Ui.dp(this, 48), 1));
        group.addView(left, new RadioGroup.LayoutParams(0, Ui.dp(this, 48), 1));
        card.addView(group); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4)); parent.addView(card, params); return group;
    }

    private View notificationSettingsCard() {
        LinearLayout card=Ui.column(this);card.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));card.setBackground(Ui.rounded(Ui.SURFACE,20,this));card.setElevation(Ui.dp(this,3));
        addNotificationSettingRow(card,"عمليات تحتاج مراجعة",NotificationPreferences.REVIEW);
        addNotificationSettingRow(card,"عمليات محفوظة تلقائيًا",NotificationPreferences.SAVED);
        addNotificationSettingRow(card,"تذكير مصاريف اليوم",NotificationPreferences.REMINDER);
        addNotificationSettingRow(card,"تجاوز الميزانية",NotificationPreferences.BUDGET);
        addNotificationSettingRow(card,"مزامنة النسخة الاحتياطية",NotificationPreferences.SYNC);
        card.addView(Ui.text(this,"ملاحظة: Android يحفظ كل إعداد كقناة إشعار مستقلة، ويُطبّق اختيارك على الإشعارات الجديدة.",11,Ui.MUTED,false));return card;
    }

    private void addNotificationSettingRow(LinearLayout parent,String title,String type){
        LinearLayout box=Ui.column(this);box.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8));box.setBackground(Ui.rounded(Ui.BRAND_SOFT,14,this));box.addView(Ui.text(this,title,14,Ui.TEXT,true));LinearLayout checks=Ui.row(this);
        CheckBox sound=new CheckBox(this);sound.setText("صوت");sound.setTextColor(Ui.TEXT);sound.setChecked(NotificationPreferences.sound(this,type));CheckBox vibration=new CheckBox(this);vibration.setText("اهتزاز");vibration.setTextColor(Ui.TEXT);vibration.setChecked(NotificationPreferences.vibration(this,type));
        android.widget.CompoundButton.OnCheckedChangeListener listener=(button,checked)->NotificationPreferences.save(this,type,sound.isChecked(),vibration.isChecked());sound.setOnCheckedChangeListener(listener);vibration.setOnCheckedChangeListener(listener);
        checks.addView(sound,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1));checks.addView(vibration,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1));box.addView(checks);
        Button chooseSound=actionButton(notificationSoundLabel(type),false);chooseSound.setOnClickListener(v->pickNotificationSound(type));box.addView(chooseSound,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,44)));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,4));parent.addView(box,params);
    }

    private String notificationSoundLabel(String type) {
        String stored = NotificationPreferences.soundUri(this, type);
        if (stored.isEmpty()) return "اختيار صوت — الافتراضي";
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(stored));
            return "اختيار صوت — " + (ringtone == null ? "مخصص" : ringtone.getTitle(this));
        } catch (Exception ignored) { return "اختيار صوت"; }
    }

    private void pickNotificationSound(String type) {
        pendingNotificationType = type;
        String stored = NotificationPreferences.soundUri(this, type);
        Intent picker = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        stored.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(stored));
        startActivityForResult(picker, PICK_NOTIFICATION_SOUND);
    }

    private void showTransferNamesDialog(){
        LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));EditText name=input("الاسم كما قد يظهر في الرسالة");Button add=actionButton("＋ إضافة الاسم",true);content.addView(name,fieldParams());content.addView(add,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("أسماء التحويل بين حساباتي").setView(content).setPositiveButton("إغلاق",null).create();add.setOnClickListener(v->{try{TransferNameStore.add(this,name.getText().toString());dialog.dismiss();showTransferNamesDialog();}catch(Exception error){name.setError(error.getMessage());}});
        for(String value:TransferNameStore.get(this)){LinearLayout row=Ui.row(this);TextView label=Ui.text(this,value,13,Ui.TEXT,true);label.setGravity(Gravity.CENTER_VERTICAL);Button remove=actionButton("حذف",false);remove.setTextColor(Ui.EXPENSE);remove.setOnClickListener(v->{TransferNameStore.remove(this,value);dialog.dismiss();showTransferNamesDialog();});row.addView(label,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));row.addView(remove,new LinearLayout.LayoutParams(Ui.dp(this,72),Ui.dp(this,44)));content.addView(row);}
        content.addView(Ui.text(this,"إذا ظهر أي اسم من هذه القائمة داخل الرسالة فستذهب العملية إلى مراجعة «تحويل بين حساباتي» تلقائيًا.",11,Ui.MUTED,false));dialog.show();
    }

    private EditText pinField(String hint) {
        EditText field = input(hint); field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); return field;
    }

    private void showAppLockDialog() {
        boolean active = AppLockManager.enabled(this);
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        CheckBox enabled = new CheckBox(this); enabled.setText("طلب رمز عند فتح التطبيق"); enabled.setTextColor(Ui.TEXT); enabled.setChecked(active); form.addView(enabled, fieldParams());
        EditText current = pinField("الرمز الحالي"); current.setVisibility(active ? View.VISIBLE : View.GONE); form.addView(current, fieldParams());
        EditText code = pinField(active ? "رمز جديد (اتركه فارغًا للإبقاء)" : "رمز جديد — 4 إلى 8 أرقام"); form.addView(code, fieldParams());
        EditText confirm = pinField("تأكيد الرمز الجديد"); form.addView(confirm, fieldParams());
        CheckBox biometric = new CheckBox(this); biometric.setText("السماح بالفتح بالبصمة"); biometric.setTextColor(Ui.TEXT); biometric.setChecked(AppLockManager.biometricEnabled(this)); biometric.setEnabled(biometricAvailable()); form.addView(biometric, fieldParams());
        if (!biometricAvailable()) form.addView(Ui.text(this, "البصمة غير متاحة أو غير مسجلة على هذا الجهاز.", 11, Ui.MUTED, false));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("قفل التطبيق").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (active && !AppLockManager.verify(this, current.getText().toString())) { current.setError("اكتب الرمز الحالي الصحيح"); return; }
            if (!enabled.isChecked()) { AppLockManager.disable(this); authenticated = true; dialog.dismiss(); Toast.makeText(this, "تم إلغاء قفل التطبيق", Toast.LENGTH_SHORT).show(); showSettings(); return; }
            String next = code.getText().toString();
            if (active && next.isEmpty()) { AppLockManager.setBiometric(this, biometric.isChecked()); dialog.dismiss(); Toast.makeText(this, "تم تحديث إعدادات القفل", Toast.LENGTH_SHORT).show(); showSettings(); return; }
            if (!next.matches("[0-9]{4,8}")) { code.setError("اكتب 4 إلى 8 أرقام"); return; }
            if (!next.equals(confirm.getText().toString())) { confirm.setError("الرمزان غير متطابقين"); return; }
            AppLockManager.setCode(this, next, biometric.isChecked()); authenticated = true; dialog.dismiss(); Toast.makeText(this, "تم تفعيل قفل التطبيق", Toast.LENGTH_SHORT).show(); showSettings();
        })); dialog.show();
    }

    private void showCaptureInbox() {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("صندوق المراجعة", () -> showSettings()), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this); LinearLayout content = Ui.column(this); content.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 34));
        List<Models.CaptureItem> items = db.getPendingCaptures();
        TextView info = Ui.text(this, "العمليات غير الواضحة لا تدخل المصروفات أو الدخل قبل اختيارك. منع التكرار يعمل بين SMS وإشعار التطبيق.", 12, Ui.MUTED, false); info.setPadding(Ui.dp(this, 3), 0, Ui.dp(this, 3), Ui.dp(this, 10)); content.addView(info);
        if (items.isEmpty()) { TextView empty = Ui.text(this, "لا توجد عمليات معلّقة للمراجعة ✓", 16, Ui.TEXT, true); empty.setGravity(Gravity.CENTER); empty.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); content.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120))); }
        else for (Models.CaptureItem item : items) content.addView(captureReviewCard(item));
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1)); setContentView(page); Ui.animateScreen(page); content.post(() -> Ui.animateChildren(content));
    }

    private View captureReviewCard(Models.CaptureItem item) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12)); card.setBackground(Ui.rounded(Ui.SURFACE, 20, this)); card.setElevation(Ui.dp(this, 2));
        String provider = "BOP".equals(item.provider) ? "بنك فلسطين" : "JAWAL".equals(item.provider) ? "Jawwal Pay" : "PalPay";
        LinearLayout head = Ui.row(this); head.addView(Ui.text(this, provider + " • " + ("SMS".equals(item.channel) ? "رسالة" : "إشعار"), 14, Ui.TEXT, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        String money = item.amountMinor == null ? "مبلغ غير مقروء" : Money.format(item.amountMinor, item.currency == null ? "" : item.currency); TextView amount = Ui.text(this, money, 15, Ui.VIOLET, true); amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); head.addView(amount); card.addView(head);
        TextView raw = Ui.text(this, item.rawText, 12, Ui.TEXT, false); raw.setPadding(Ui.dp(this, 10), Ui.dp(this, 9), Ui.dp(this, 10), Ui.dp(this, 9)); raw.setBackground(Ui.rounded(Ui.BRAND_SOFT, 12, this)); card.addView(raw);
        if (FinanceRules.TRANSFER.equals(item.inferredType)) { TextView detected = Ui.text(this, "↔ وُجد اسمك في الحوالة — اختر الحساب المصدر والمستلم", 12, Ui.BRAND, true); detected.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), 0); card.addView(detected); }
        if (item.amountMinor != null && item.currency != null) {
            LinearLayout types = Ui.row(this); Button expense = miniButton("مصروف"); Button income = miniButton("دخل"); Button transfer = miniButton("بين حساباتي");
            expense.setOnClickListener(v -> resolveCapture(item.id, FinanceRules.EXPENSE)); income.setOnClickListener(v -> resolveCapture(item.id, FinanceRules.INCOME)); transfer.setOnClickListener(v -> showCaptureTransferDialog(item));
            types.addView(expense); types.addView(income); types.addView(transfer); card.addView(types);
        }
        Button ignore = actionButton("تجاهل هذه الرسالة", false); ignore.setTextColor(Ui.EXPENSE); ignore.setOnClickListener(v -> { db.ignoreCapture(item.id); CaptureEngine.cancelNotification(this, item.id); showCaptureInbox(); }); card.addView(ignore, fieldParams());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, Ui.dp(this, 5), 0, Ui.dp(this, 7)); card.setLayoutParams(p); return card;
    }

    private void resolveCapture(long id, String type) {
        long tx = db.importCapture(id, type); CaptureEngine.cancelNotification(this, id);
        Toast.makeText(this, tx >= 0 ? "تم حفظ العملية — ستجدها بانتظار التصنيف" : "تعذر الحفظ؛ تأكد من وجود حساب بنفس العملة", Toast.LENGTH_LONG).show(); showCaptureInbox();
    }

    private void showCaptureTransferDialog(Models.CaptureItem item) {
        List<Models.Account> accounts = new ArrayList<>(); for (Models.Account account : db.getAccounts()) if (account.currency.equals(item.currency)) accounts.add(account);
        if (accounts.size() < 2) { Toast.makeText(this, "تحتاج حسابين على الأقل بعملة " + item.currency, Toast.LENGTH_LONG).show(); return; }
        List<String> labels = new ArrayList<>(); for (Models.Account account : accounts) labels.add(account.displayName());
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0); form.addView(label("من حساب")); Spinner source = spinner(labels); form.addView(source, fieldParams()); form.addView(label("إلى حساب")); Spinner destination = spinner(labels); destination.setSelection(1); form.addView(destination, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تحويل بين حساباتي").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Models.Account a = accounts.get(source.getSelectedItemPosition()), b = accounts.get(destination.getSelectedItemPosition()); if (a.id == b.id) { Toast.makeText(this, "اختر حسابين مختلفين", Toast.LENGTH_SHORT).show(); return; }
            long tx = db.importCaptureTransfer(item.id, a.id, b.id); if (tx < 0) { Toast.makeText(this, "تعذر حفظ التحويل", Toast.LENGTH_LONG).show(); return; }
            CaptureEngine.cancelNotification(this, item.id); dialog.dismiss(); showCaptureInbox();
        })); dialog.show();
    }

    private void showDebtsScreen(String direction) {
        addScreenVisible = false; subScreenVisible = false; currentTab = TAB_DEBTS;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("دفتر الديون", () -> showMain(false)), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this); LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 34));

        LinearLayout actions = Ui.row(this);
        actions.setPadding(Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,14));
        Button addDebt = actionButton("＋ إضافة دين", true); addDebt.setOnClickListener(v -> showManualDebtDialog(direction));
        Button people = actionButton("👥 الأشخاص", false); people.setOnClickListener(v -> showDebtPeopleManager(direction));
        LinearLayout.LayoutParams addDebtParams=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);
        addDebtParams.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        LinearLayout.LayoutParams peopleParams=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);
        peopleParams.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        actions.addView(addDebt,addDebtParams);
        actions.addView(people,peopleParams);
        content.addView(actions);

        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,16));
        TextView mine = debtTab("إليّ — لي عند الناس", FinanceRules.DEBT_RECEIVABLE.equals(direction));
        TextView owed = debtTab("عليّ — للناس عندي", FinanceRules.DEBT_PAYABLE.equals(direction));
        mine.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_RECEIVABLE));
        owed.setOnClickListener(v -> showDebtsScreen(FinanceRules.DEBT_PAYABLE));
        LinearLayout.LayoutParams mineParams=new LinearLayout.LayoutParams(0,Ui.dp(this,54),1);
        mineParams.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        LinearLayout.LayoutParams owedParams=new LinearLayout.LayoutParams(0,Ui.dp(this,54),1);
        owedParams.setMargins(Ui.dp(this,5),0,Ui.dp(this,5),0);
        tabs.addView(mine,mineParams);
        tabs.addView(owed,owedParams);content.addView(tabs);

        List<Models.DebtSummary> active = db.getDebtSummaries(direction, false);
        LinkedHashMap<Long,List<Models.DebtSummary>> activePeople = groupDebtsByPerson(active);
        content.addView(sectionTitle("الديون النشطة", active.isEmpty() ? "لا توجد ديون" : activePeople.size() + " أشخاص"));
        if (active.isEmpty()) content.addView(messageCard("دفتر الديون فارغ", "أضف دينًا يدويًا أو صنّف عملية باسم دَين"));
        else for (List<Models.DebtSummary> personDebts : activePeople.values()) content.addView(debtPersonCard(personDebts, false));

        List<Models.DebtSummary> closed = db.getDebtSummaries(direction, true);
        LinkedHashMap<Long,List<Models.DebtSummary>> closedPeople = groupDebtsByPerson(closed);
        content.addView(sectionTitle("المسددة والمغلقة", closed.isEmpty() ? "لا يوجد سجل بعد" : closedPeople.size() + " أشخاص"));
        for (List<Models.DebtSummary> personDebts : closedPeople.values()) content.addView(debtPersonCard(personDebts, true));
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        page.addView(bottomNavigation(TAB_DEBTS), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)));
        setContentView(page); Ui.animateScreen(page); content.post(() -> Ui.animateChildren(content));
    }

    private LinkedHashMap<Long,List<Models.DebtSummary>> groupDebtsByPerson(List<Models.DebtSummary> debts) {
        LinkedHashMap<Long,List<Models.DebtSummary>> grouped = new LinkedHashMap<>();
        for (Models.DebtSummary debt : debts) {
            List<Models.DebtSummary> values = grouped.get(debt.personId);
            if (values == null) { values = new ArrayList<>(); grouped.put(debt.personId, values); }
            values.add(debt);
        }
        return grouped;
    }

    private View debtPersonCard(List<Models.DebtSummary> debts, boolean closed) {
        Models.DebtSummary first = debts.get(0);
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));
        card.setBackground(Ui.layeredSoftBackground(this,20)); card.setElevation(Ui.dp(this,3));
        LinearLayout head=Ui.row(this); head.addView(Ui.text(this,"▤  "+first.personName+(closed?" ✓":""),17,closed?Ui.INCOME:Ui.TEXT,true),new LinearLayout.LayoutParams(0,Ui.dp(this,40),1));
        TextView arrow=Ui.text(this,"›",28,Ui.VIOLET,true);arrow.setGravity(Gravity.CENTER);head.addView(arrow,new LinearLayout.LayoutParams(Ui.dp(this,36),Ui.dp(this,40)));card.addView(head);
        long last=0; int openCount=0;
        for(Models.DebtSummary debt:debts){last=Math.max(last,debt.lastMovementAt);if(!debt.closed)openCount++;card.addView(Ui.text(this,(debt.closed?"✓ ":"")+debt.currency+"  •  المتبقي "+Money.format(debt.remainingMinor,debt.currency),12,debt.closed?Ui.MUTED:Ui.VIOLET,true));}
        if(last>0){String date=new SimpleDateFormat("dd/MM/yyyy  hh:mm a",new Locale("ar")).format(new Date(last));card.addView(Ui.text(this,"آخر حركة: "+date+(openCount>0?"  •  "+openCount+" نشط":""),11,Ui.MUTED,false));}
        card.setOnClickListener(v->showDebtPersonDetails(first.personId,first.personName,first.direction));Ui.pressAnimation(card);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,6));card.setLayoutParams(params);return card;
    }

    private void showDebtPersonDetails(long personId, String personName, String direction) {
        addScreenVisible=false;subScreenVisible=true;
        LinearLayout page=Ui.column(this);page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("ديون "+personName,()->showDebtsScreen(direction)),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,78)));
        ScrollView scroll=new ScrollView(this);LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,16),Ui.dp(this,10),Ui.dp(this,16),Ui.dp(this,34));
        Button add=actionButton("＋ إضافة دين جديد لنفس الشخص",true);add.setOnClickListener(v->showManualDebtDialog(direction,personId));content.addView(add,fieldParams());
        List<Models.DebtSummary> active=db.getDebtSummariesForPerson(personId,direction,false);
        content.addView(sectionTitle("الديون النشطة",active.isEmpty()?"✓ كل الديون مسددة":active.size()+" سجلات"));for(Models.DebtSummary debt:active)content.addView(debtCard(debt));
        List<Models.DebtSummary> closed=db.getDebtSummariesForPerson(personId,direction,true);
        content.addView(sectionTitle("المسددة والمغلقة",closed.isEmpty()?"لا يوجد سجل بعد":closed.size()+" سجلات"));for(Models.DebtSummary debt:closed)content.addView(debtCard(debt));
        scroll.addView(content);page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(page);Ui.animateScreen(page);content.post(()->Ui.animateChildren(content));
    }

    private TextView debtTab(String label, boolean selected) {
        TextView tab = Ui.text(this, label, 12, selected ? Color.WHITE : Ui.TEXT, true); tab.setGravity(Gravity.CENTER);
        tab.setBackground(selected ? Ui.gradient(Ui.BRAND, Ui.VIOLET, 16, this) : Ui.rounded(Ui.BRAND_SOFT, 16, this));
        tab.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        Ui.pressAnimation(tab); return tab;
    }

    private View debtCard(Models.DebtSummary debt) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        card.setBackground(Ui.layeredSoftBackground(this, 20)); card.setElevation(Ui.dp(this, 3));
        LinearLayout head = Ui.row(this); String check = debt.closed ? " ✓" : "";
        head.addView(Ui.text(this, "▤ " + debt.personName + check, 16, debt.closed ? Ui.INCOME : Ui.TEXT, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1));
        TextView remaining = Ui.text(this, Money.format(debt.remainingMinor, debt.currency), 16, debt.closed ? Ui.INCOME : Ui.VIOLET, true);
        remaining.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); head.addView(remaining); card.addView(head);
        card.addView(Ui.text(this, "الأصلي " + Money.format(debt.originalMinor, debt.currency) + "  •  المسدد " + Money.format(debt.paidMinor, debt.currency), 12, Ui.MUTED, false));
        String last = new SimpleDateFormat("dd/MM/yyyy  hh:mm a", new Locale("ar")).format(new Date(debt.lastMovementAt));
        card.addView(Ui.text(this, "آخر حركة: " + last, 11, Ui.MUTED, false));
        FrameLayout track = new FrameLayout(this); track.setBackground(Ui.rounded(Ui.BRAND_SOFT, 5, this));
        View fill = new View(this); fill.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 5, this));
        float paidRatio = debt.originalMinor <= 0 ? 0f : Math.min(1f, debt.paidMinor / (float) debt.originalMinor);
        track.addView(fill, new FrameLayout.LayoutParams(Math.max(Ui.dp(this, 8), (int) (Ui.dp(this, 270) * paidRatio)), Ui.dp(this, 8)));
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 8)); trackParams.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 8)); card.addView(track, trackParams);
        LinearLayout buttons = Ui.row(this); Button history = actionButton("سجل الحركات", false); history.setOnClickListener(v -> showDebtHistory(debt));
        buttons.addView(history, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
        if (!debt.closed && debt.remainingMinor > 0) {
            Button partial = actionButton("سداد جزئي", false); LinearLayout.LayoutParams partialParams = new LinearLayout.LayoutParams(0,Ui.dp(this,44),1);partialParams.setMargins(Ui.dp(this,8),0,0,0);buttons.addView(partial,partialParams);
            partial.setOnClickListener(v->showManualDebtPayment(debt));
            Button paid = actionButton("✓ تم السداد", true); LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1); pp.setMargins(Ui.dp(this, 8),0,0,0); buttons.addView(paid, pp);
            paid.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("تأكيد اكتمال السداد")
                    .setMessage("سيصبح المتبقي صفرًا ويُنقل الدين إلى قسم المسددة، من دون تغيير أي رصيد حساب.")
                    .setNegativeButton("إلغاء", null).setPositiveButton("تم السداد", (d,w) -> {
                        try { db.markDebtPaid(debt.id);
                            Toast.makeText(this,"تم تسجيل السداد وتحديث المتبقي",Toast.LENGTH_LONG).show();
                            showDebtPersonDetails(debt.personId,debt.personName,debt.direction);
                        }
                        catch (Exception error) { Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show(); }
                    }).show());
        }
        card.addView(buttons);
        Button deleteDebt=actionButton("حذف سجل الدين",false);deleteDebt.setTextColor(Ui.EXPENSE);
        deleteDebt.setOnClickListener(v->{
            int affected=db.countDebtLinkedFinancialTransactions(debt.id);
            String warning="سيُحذف سجل دين "+debt.personName+" ("+debt.currency+") وجميع حركاته. "+
                    (affected>0?"تحذير: هذا سيحذف أيضًا "+affected+" عملية مالية مرتبطة ويعكس أثرها من الأرصدة. ":
                    "هذا الدين يدوي ولا يغيّر الحذف أرصدة الحسابات. ")+
                    "الحذف نهائي، ولن تتم أي تغييرات إذا فشل التحقق المالي.";
            new AlertDialog.Builder(this).setTitle("تأكيد حذف سجل الدين نهائيًا")
                    .setMessage(warning).setNegativeButton("إلغاء",null)
                    .setPositiveButton("حذف نهائي",(d,w)->{
                        try {
                            db.deleteDebt(debt.id);
                            Toast.makeText(this,"تم حذف سجل الدين وتحديث الحسابات وسجل الحركات",Toast.LENGTH_LONG).show();
                            showDebtPersonDetails(debt.personId,debt.personName,debt.direction);
                        } catch(Exception error) {
                            Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }).show();
        });
        card.addView(deleteDebt,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,42)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); params.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6)); card.setLayoutParams(params);
        return card;
    }

    private void showDebtHistory(Models.DebtSummary debt) {
        LinearLayout content=Ui.column(this);
        content.setPadding(Ui.dp(this,18),Ui.dp(this,4),Ui.dp(this,18),Ui.dp(this,12));
        content.addView(Ui.text(this,
                "اضغط على الحركة لعرض تفاصيلها. أي حركة مرتبطة بعملية مالية تُعدّل من العملية الأصلية فقط.",
                12,Ui.MUTED,false));
        final AlertDialog[] historyDialog={null};
        List<Models.DebtMovement> movements=db.getDebtMovements(debt.id);
        if(movements.isEmpty())content.addView(Ui.text(this,"لا توجد حركات لهذا الدين",13,Ui.MUTED,false));
        for(Models.DebtMovement movement:movements) {
            String kind="OPEN".equals(movement.kind)?"إضافة دين":
                    "MANUAL_PAYMENT".equals(movement.kind)?"سداد يدوي":"سداد من عملية";
            String date=new SimpleDateFormat("dd/MM/yyyy  hh:mm a",new Locale("ar"))
                    .format(new Date(movement.createdAt));
            LinearLayout row=Ui.column(this);
            row.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12));
            row.setBackground(Ui.layeredSoftBackground(this,14));
            row.addView(Ui.text(this,kind+"  •  "+Money.format(movement.amountMinor,debt.currency),
                    14,Ui.TEXT,true));
            row.addView(Ui.text(this,date,11,Ui.MUTED,false));
            if(movement.fromSplitTransaction) {
                row.addView(Ui.text(this,
                        "جزء من عملية مالية مقسمة إلى "+movement.linkedSplitPartCount+" أجزاء",
                        12,Ui.VIOLET,true));
                if(movement.linkedTransactionAmountMinor!=null)
                    row.addView(Ui.text(this,"إجمالي العملية الأصلية: "+
                            Money.format(movement.linkedTransactionAmountMinor,debt.currency),
                            12,Ui.MUTED,false));
            }
            if(movement.linkedTransactionNote!=null && !movement.linkedTransactionNote.trim().isEmpty())
                row.addView(Ui.text(this,"ملاحظة العملية: "+movement.linkedTransactionNote,
                        12,Ui.MUTED,false));
            if(movement.note!=null && !movement.note.trim().isEmpty()
                    && !movement.note.equals(movement.linkedTransactionNote))
                row.addView(Ui.text(this,"ملاحظة الحركة: "+movement.note,12,Ui.MUTED,false));
            if(movement.transactionId!=null) {
                row.addView(Ui.text(this,"اضغط لعرض تفاصيل العملية الأصلية ←",
                        11,Ui.VIOLET,true));
                row.setOnClickListener(v->{
                    Models.Transaction original=db.getTransactionById(movement.transactionId);
                    if(original==null) {
                        Toast.makeText(this,"العملية المالية المرتبطة لم تعد موجودة",Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(historyDialog[0]!=null)historyDialog[0].dismiss();
                    showTransactionDetails(original);
                });
            } else {
                row.addView(Ui.text(this,"اضغط لتعديل أو حذف الحركة اليدوية",
                        11,Ui.VIOLET,false));
                row.setOnClickListener(v->{
                    if(historyDialog[0]!=null)historyDialog[0].dismiss();
                    showManualDebtMovementDialog(debt,movement);
                });
            }
            Ui.pressAnimation(row);
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0,Ui.dp(this,5),0,Ui.dp(this,5));
            content.addView(row,rowParams);
        }
        ScrollView scroll=new ScrollView(this);
        scroll.addView(content);
        historyDialog[0]=new AlertDialog.Builder(this)
                .setTitle("سجل دين "+debt.personName)
                .setView(scroll).setPositiveButton("إغلاق",null).create();
        historyDialog[0].show();
    }

    private void showManualDebtPayment(Models.DebtSummary debt){
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));
        form.addView(Ui.text(this,"المتبقي: "+Money.format(debt.remainingMinor,debt.currency)+"\nهذا تسجيل في دفتر الديون فقط ولا يغيّر رصيد أي حساب.",12,Ui.MUTED,false));
        EditText amount=input("المبلغ المسدد");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);EditText note=input("ملاحظة اختيارية");form.addView(amount,fieldParams());form.addView(note,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("سداد جزئي — "+debt.personName).setView(form).setNegativeButton("إلغاء",null).setPositiveButton("تسجيل",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{Long value=Money.parseMinor(amount.getText().toString());if(value==null)throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");db.addManualDebtPayment(debt.id,value,note.getText().toString());dialog.dismiss();showDebtPersonDetails(debt.personId,debt.personName,debt.direction);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
    }

    private void showManualDebtMovementDialog(Models.DebtSummary debt,Models.DebtMovement movement){
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));EditText amount=input("المبلغ");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setText(String.format(Locale.ROOT,"%.2f",movement.amountMinor/100.0));EditText note=input("ملاحظة");note.setText(movement.note);form.addView(amount,fieldParams());form.addView(note,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تعديل حركة يدوية").setView(form).setNegativeButton("حذف",null).setNeutralButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{Long value=Money.parseMinor(amount.getText().toString());if(value==null)throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");db.updateManualDebtMovement(movement.id,value,note.getText().toString());dialog.dismiss();showDebtPersonDetails(debt.personId,debt.personName,debt.direction);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}});dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Ui.EXPENSE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->
                    new AlertDialog.Builder(this).setTitle("تأكيد حذف حركة الدين")
                            .setMessage("الشخص: "+debt.personName+"\nالحركة: "+
                                    Money.format(movement.amountMinor,debt.currency)+
                                    "\nسيُعاد حساب أصل الدين والمتبقي. هذه حركة يدوية ولا تمس رصيد الحسابات المالية. لا يمكن التراجع.")
                            .setNegativeButton("إلغاء",null)
                            .setPositiveButton("حذف نهائي",(d,w)->{
                                try {
                                    db.deleteManualDebtMovement(movement.id);
                                    dialog.dismiss();
                                    Toast.makeText(this,"تم حذف الحركة وتحديث رصيد الدين فورًا",Toast.LENGTH_LONG).show();
                                    showDebtPersonDetails(debt.personId,debt.personName,debt.direction);
                                } catch(Exception error) {
                                    Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();
                                }
                            }).show());});dialog.show();
    }

    private void showManualDebtDialog(String direction) { showManualDebtDialog(direction, null); }

    private void showManualDebtDialog(String direction, Long selectedPersonId) {
        List<Models.DebtPerson> people = db.getDebtPeople();
        if (people.isEmpty()) { showQuickAddDebtPerson(() -> showManualDebtDialog(direction, selectedPersonId)); return; }
        LinearLayout form=Ui.column(this); form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));
        List<String> names=new ArrayList<>(); for(Models.DebtPerson p:people) names.add(p.name); Spinner person=spinner(names);
        if(selectedPersonId!=null)for(int i=0;i<people.size();i++)if(people.get(i).id==selectedPersonId){person.setSelection(i);break;}
        Spinner currency=spinner(Arrays.asList("ILS","USD","JOD")); EditText amount=input("المبلغ"); amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); EditText note=input("ملاحظة اختيارية");
        form.addView(label("الشخص")); form.addView(person,fieldParams()); Button quick=actionButton("＋ إضافة شخص جديد",false); quick.setOnClickListener(v->showQuickAddDebtPerson(() -> showManualDebtDialog(direction, selectedPersonId))); form.addView(quick,fieldParams());
        form.addView(label("العملة")); form.addView(currency,fieldParams()); form.addView(amount,fieldParams()); form.addView(note,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(FinanceRules.DEBT_RECEIVABLE.equals(direction)?"دين إلي":"دين عليّ").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try { Long value=Money.parseMinor(amount.getText().toString()); if(value==null||value<=0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
                db.createManualDebt(people.get(person.getSelectedItemPosition()).id,direction,currency.getSelectedItem().toString(),value,note.getText().toString()); dialog.dismiss(); showDebtsScreen(direction);
            } catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
        })); dialog.show();
    }

    private void showDebtPeopleManager(String returnDirection) {
        List<Models.DebtPerson> people=db.getDebtPeople(); LinearLayout content=Ui.column(this); content.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));
        Button add=actionButton("＋ إضافة شخص",true); content.addView(add,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("أشخاص الديون").setView(content).setPositiveButton("إغلاق",null).create();
        add.setOnClickListener(v->{dialog.dismiss();showQuickAddDebtPerson(()->showDebtPeopleManager(returnDirection));});
        for(Models.DebtPerson person:people){ TextView row=Ui.text(this,"👤  "+person.name+"   ›",15,Ui.TEXT,true); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0); row.setBackground(Ui.rounded(Ui.BRAND_SOFT,14,this));
            row.setOnClickListener(v->{dialog.dismiss();showEditDebtPerson(person,returnDirection);}); content.addView(row,fieldParams()); }
        dialog.show();
    }

    private void showQuickAddDebtPerson(Runnable after) {
        EditText name=input("اسم الشخص"); AlertDialog dialog=new AlertDialog.Builder(this).setTitle("إضافة شخص").setView(name).setNegativeButton("إلغاء",null).setPositiveButton("إضافة",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{db.addDebtPerson(name.getText().toString());dialog.dismiss();after.run();}catch(Exception error){name.setError(error.getMessage());}}));dialog.show();
    }

    private void showEditDebtPerson(Models.DebtPerson person,String returnDirection){
        EditText name=input("اسم الشخص");name.setText(person.name); AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تعديل الشخص").setView(name).setNegativeButton("حذف",null).setNeutralButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{db.renameDebtPerson(person.id,name.getText().toString());dialog.dismiss();showDebtPeopleManager(returnDirection);}catch(Exception error){name.setError(error.getMessage());}});
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Ui.EXPENSE);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{try{db.removeDebtPerson(person.id);dialog.dismiss();showDebtPeopleManager(returnDirection);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}});});dialog.show();
    }

    private void showAnalyticsScreen() {
        addScreenVisible = false; subScreenVisible = false; currentTab = TAB_ANALYTICS;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("تحليل المصروفات", () -> showMain(false)), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this); LinearLayout content = Ui.column(this); content.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 34));
        content.addView(Ui.text(this, "اختر عملة التحليل", 14, Ui.TEXT, true));
        LinearLayout currencyRow = Ui.row(this);
        for (String code : Arrays.asList("ILS", "USD", "JOD")) {
            boolean selected = code.equals(dashboardCurrency); TextView chip = Ui.text(this, code, 14, selected ? Color.WHITE : Ui.TEXT, true); chip.setGravity(Gravity.CENTER);
            chip.setBackground(selected ? Ui.gradient(Ui.VIOLET, Ui.SKY, 17, this) : Ui.rounded(Ui.BRAND_SOFT, 17, this));
            chip.setOnClickListener(v -> { dashboardCurrency = code; showAnalyticsScreen(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1); p.setMargins(Ui.dp(this, 4), Ui.dp(this, 5), Ui.dp(this, 4), Ui.dp(this, 8)); currencyRow.addView(chip, p);
        }
        content.addView(currencyRow);

        long now = System.currentTimeMillis() + 1; Calendar today = midnight(Calendar.getInstance());
        Calendar chartStart = (Calendar) today.clone(); chartStart.add(Calendar.DAY_OF_YEAR, -6);
        Calendar weekStart = (Calendar) today.clone();
        int daysSinceWeekStart = (7 + weekStart.get(Calendar.DAY_OF_WEEK) - weekStart.getFirstDayOfWeek()) % 7;
        weekStart.add(Calendar.DAY_OF_YEAR, -daysSinceWeekStart);
        Calendar previousWeek = (Calendar) weekStart.clone(); previousWeek.add(Calendar.DAY_OF_YEAR, -7);
        Calendar monthStart = (Calendar) today.clone(); monthStart.set(Calendar.DAY_OF_MONTH, 1);
        Calendar previousMonth = (Calendar) monthStart.clone(); previousMonth.add(Calendar.MONTH, -1);
        long week = db.getExpenseTotal(dashboardCurrency, weekStart.getTimeInMillis(), now);
        long todayTotal = db.getExpenseTotal(dashboardCurrency, today.getTimeInMillis(), now);
        long priorWeek = db.getExpenseTotal(dashboardCurrency, previousWeek.getTimeInMillis(), weekStart.getTimeInMillis());
        long month = db.getExpenseTotal(dashboardCurrency, monthStart.getTimeInMillis(), now);
        long priorMonth = db.getExpenseTotal(dashboardCurrency, previousMonth.getTimeInMillis(), monthStart.getTimeInMillis());
        LinearLayout todayCompare = Ui.row(this);
        LinearLayout compare = Ui.row(this);
        View todayCard = comparisonCard("مصاريف اليوم", todayTotal, -1);
        View weekCard = comparisonCard("هذا الأسبوع", week, priorWeek);
        View monthCard = comparisonCard("هذا الشهر", month, priorMonth);
        todayCard.setOnClickListener(v -> showExpensePeriodScreen("مصاريف اليوم", dashboardCurrency, today.getTimeInMillis(), now));
        weekCard.setOnClickListener(v -> showExpensePeriodScreen("مصاريف هذا الأسبوع", dashboardCurrency, weekStart.getTimeInMillis(), now));
        monthCard.setOnClickListener(v -> showExpensePeriodScreen("مصاريف هذا الشهر", dashboardCurrency, monthStart.getTimeInMillis(), now));
        Ui.pressAnimation(todayCard); Ui.pressAnimation(weekCard); Ui.pressAnimation(monthCard);
        todayCompare.addView(todayCard); content.addView(todayCompare);
        compare.addView(weekCard); compare.addView(monthCard); content.addView(compare);
        long incomeMonth=db.getIncomeTotal(dashboardCurrency,monthStart.getTimeInMillis(),now,false);
        long receivedMonth=db.getIncomeTotal(dashboardCurrency,monthStart.getTimeInMillis(),now,true);
        LinearLayout incomingRow=Ui.row(this);
        View earnedIncomeCard=comparisonCard("الدخل هذا الشهر",incomeMonth,-1);
        View receivedIncomeCard=comparisonCard("المستلم هذا الشهر",receivedMonth,-1);
        earnedIncomeCard.setOnClickListener(v -> showIncomePeriodScreen(
                "الدخل هذا الشهر",dashboardCurrency,monthStart.getTimeInMillis(),now,false));
        receivedIncomeCard.setOnClickListener(v -> showIncomePeriodScreen(
                "المستلم هذا الشهر",dashboardCurrency,monthStart.getTimeInMillis(),now,true));
        Ui.pressAnimation(earnedIncomeCard);
        Ui.pressAnimation(receivedIncomeCard);
        incomingRow.addView(earnedIncomeCard);
        incomingRow.addView(receivedIncomeCard);
        content.addView(incomingRow);
        content.addView(monthlyBudgetCard(dashboardCurrency, month));
        // Last completed month: a congratulation is meaningful only after the month closes.
        String previousMonthKey=MonthlyBudgetHistory.monthKey(previousMonth.getTimeInMillis(),TimeZone.getDefault());
        android.content.SharedPreferences budgetHistory=getSharedPreferences("monthly_budgets",MODE_PRIVATE);
        String completedKey="budget_"+dashboardCurrency+"_"+previousMonthKey;
        // A recorded zero means that month had no budget. Missing snapshots from
        // older versions inherit the rolling cap, instead of suppressing savings.
        Long completedSnapshot=budgetHistory.contains(completedKey)
                ? budgetHistory.getLong(completedKey,0L) : null;
        long priorCap=MonthlyBudgetHistory.completedMonthCap(
                budgetHistory.getLong("budget_"+dashboardCurrency,0L),completedSnapshot);
        MonthlySavingsRules.State finished=MonthlySavingsRules.calculate(priorCap,priorMonth,true);
        if(finished.configured && !finished.exceeded && finished.savedMinor>0) {
            content.addView(Ui.text(this,"🎉 Congratulations! أحسنت — الشهر الماضي بقيت تحت السقف بـ "
                    + Money.format(finished.savedMinor,dashboardCurrency),14,Ui.INCOME,true));
        }

        LinearLayout chartCard = Ui.column(this); chartCard.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 10)); chartCard.setBackground(Ui.rounded(Ui.SURFACE, 22, this)); chartCard.setElevation(Ui.dp(this, 3));
        LinearLayout chartHead = Ui.row(this); chartHead.addView(Ui.text(this, "آخر 7 أيام", 16, Ui.TEXT, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        TextView toggle = Ui.text(this, chartLineMode ? "▥ أعمدة" : "⌁ خط بياني", 12, Ui.VIOLET, true); toggle.setGravity(Gravity.CENTER); toggle.setBackground(Ui.rounded(Ui.BRAND_SOFT, 14, this));
        toggle.setOnClickListener(v -> { chartLineMode = !chartLineMode; getSharedPreferences("appearance", MODE_PRIVATE).edit().putBoolean("chart_line", chartLineMode).apply(); showAnalyticsScreen(); }); chartHead.addView(toggle, new LinearLayout.LayoutParams(Ui.dp(this, 105), Ui.dp(this, 36))); chartCard.addView(chartHead);
        long[] values = new long[7]; String[] labels = new String[7]; Calendar cursor = (Calendar) chartStart.clone(); SimpleDateFormat df = new SimpleDateFormat("EEE", new Locale("ar"));
        for (int i = 0; i < 7; i++) { long start = cursor.getTimeInMillis(); labels[i] = df.format(cursor.getTime()); cursor.add(Calendar.DAY_OF_YEAR, 1); values[i] = db.getExpenseTotal(dashboardCurrency, start, cursor.getTimeInMillis()); }
        chartCard.addView(new ExpenseChartView(this, values, labels, chartLineMode), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 190)));
        LinearLayout.LayoutParams chartP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 245)); chartP.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 10)); content.addView(chartCard, chartP);

        content.addView(Ui.text(this, "تصنيفات هذا الشهر", 17, Ui.TEXT, true));
        List<Models.CategoryTotal> totals = db.getCategoryExpenseTotals(dashboardCurrency, monthStart.getTimeInMillis(), now);
        long max = totals.isEmpty() ? 1 : totals.get(0).amountMinor;
        if (totals.isEmpty()) content.addView(Ui.text(this, "لا توجد مصروفات بهذه العملة هذا الشهر.", 13, Ui.MUTED, false));
        for (Models.CategoryTotal item : totals) content.addView(categoryAnalysisRow(item, max, monthStart.getTimeInMillis(), now));
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        page.addView(bottomNavigation(TAB_ANALYTICS), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)));
        setContentView(page); Ui.animateScreen(page);
    }

    private View monthlyBudgetCard(String currency, long spentMinor) {
        android.content.SharedPreferences budgets = getSharedPreferences("monthly_budgets", MODE_PRIVATE);
        long budgetMinor = budgets.getLong("budget_" + currency, 0L);
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        card.setBackground(Ui.layeredSoftBackground(this, 20));
        card.setElevation(Ui.dp(this, 3));

        LinearLayout head = Ui.row(this);
        head.addView(Ui.text(this, "ميزانية الشهر — " + currency, 16, Ui.TEXT, true),
                new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        TextView edit = Ui.text(this, budgetMinor > 0 ? "تعديل" : "تحديد", 13, Ui.BRAND, true);
        edit.setGravity(Gravity.CENTER);
        edit.setBackground(Ui.rounded(Ui.BRAND_SOFT, 14, this));
        edit.setOnClickListener(v -> showBudgetDialog(currency, budgetMinor));
        head.addView(edit, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 38)));
        card.addView(head);

        if (budgetMinor <= 0) {
            card.addView(Ui.text(this, "حدد سقفًا شهريًا لتعرف فورًا إن كنت ضمن الميزانية أو تجاوزتها.", 13, Ui.MUTED, false));
        } else {
            long difference = budgetMinor - spentMinor;
            boolean exceeded = difference < 0;
            String status = exceeded
                    ? "تجاوزت الميزانية بـ " + Money.format(-difference, currency)
                    : "المتبقي " + Money.format(difference, currency);
            card.addView(Ui.text(this, status, 16, exceeded ? Ui.EXPENSE : Ui.INCOME, true));
            card.addView(Ui.text(this, "صرفت " + Money.format(spentMinor, currency) + " من " + Money.format(budgetMinor, currency), 12, Ui.MUTED, false));
            MonthlySavingsRules.State savings = MonthlySavingsRules.calculate(budgetMinor,spentMinor,false);
            if (!savings.exceeded) {
                card.addView(Ui.text(this, "🌱 تحت سقف الصرف بـ " + Money.format(savings.remainderMinor,currency)
                        + " حتى الآن — المبلغ غير المصروف ليس ادخارًا مؤكدًا قبل نهاية الشهر",12,Ui.INCOME,true));
            }
            FrameLayout track = new FrameLayout(this);
            track.setBackground(Ui.rounded(Ui.BRAND_SOFT, 6, this));
            View fill = new View(this);
            fill.setBackground(Ui.rounded(exceeded ? Ui.EXPENSE : Ui.BRAND, 6, this));
            int maxWidth = Math.max(Ui.dp(this, 180), getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 64));
            int fillWidth = (int) Math.max(Ui.dp(this, 8), Math.min(maxWidth, maxWidth * (spentMinor / (double) budgetMinor)));
            track.addView(fill, new FrameLayout.LayoutParams(fillWidth, Ui.dp(this, 10)));
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 10));
            trackParams.topMargin = Ui.dp(this, 10);
            card.addView(track, trackParams);
        }
        boolean alertEnabled = BudgetAlertManager.enabled(this, currency);
        TextView alert = Ui.text(this, alertEnabled ? "🔔 تنبيه التجاوز مفعّل" : "🔔 إضافة تنبيه عند التجاوز", 13,
                alertEnabled ? Ui.INCOME : Ui.BRAND, true);
        alert.setGravity(Gravity.CENTER);
        alert.setBackground(Ui.rounded(Ui.BRAND_SOFT, 14, this));
        LinearLayout.LayoutParams alertParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44));
        alertParams.topMargin = Ui.dp(this, 12); card.addView(alert, alertParams);
        alert.setOnClickListener(v -> {
            boolean next = !BudgetAlertManager.enabled(this, currency);
            BudgetAlertManager.setEnabled(this, currency, next);
            if (next && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7201);
            Toast.makeText(this, next ? "سيرسل التطبيق تنبيهًا واحدًا فقط عند تجاوز ميزانية الشهر" : "تم إيقاف تنبيه الميزانية", Toast.LENGTH_LONG).show();
            showAnalyticsScreen();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 8));
        card.setLayoutParams(params);
        Ui.pressAnimation(edit);
        Ui.pressAnimation(alert);
        return card;
    }

    private void showBudgetDialog(String currency, long currentMinor) {
        EditText amount = input("الميزانية الشهرية بـ " + currency);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (currentMinor > 0) amount.setText(String.format(Locale.ROOT, "%.2f", currentMinor / 100.0));
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        form.addView(Ui.text(this, "تتجدد المقارنة تلقائيًا مع بداية كل شهر. اتركها صفرًا لحذف الميزانية.", 12, Ui.MUTED, false));
        form.addView(amount, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("ميزانية " + currency)
                .setView(form).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Long value = Money.parseMinor(amount.getText().toString());
            if (value == null || value < 0) { amount.setError("اكتب مبلغًا صحيحًا أو صفرًا للحذف"); return; }
            long nowMillis=System.currentTimeMillis();
            String currentMonthKey=MonthlyBudgetHistory.monthKey(nowMillis,TimeZone.getDefault());
            String previousMonthKey=MonthlyBudgetHistory.previousMonthKey(nowMillis,TimeZone.getDefault());
            android.content.SharedPreferences budgetHistory=getSharedPreferences("monthly_budgets",MODE_PRIVATE);
            String baseKey="budget_"+currency;
            String completedKey=baseKey+"_"+previousMonthKey;
            android.content.SharedPreferences.Editor edit=budgetHistory.edit();
            // Preserve the cap that applied to last month BEFORE editing this month's
            // rolling cap. Never overwrite an already recorded month snapshot.
            if(!budgetHistory.contains(completedKey)) edit.putLong(completedKey,currentMinor);
            edit.putLong(baseKey,value)
                    .putLong(baseKey+"_"+currentMonthKey,value).apply();
            BudgetAlertManager.checkAll(this);
            dialog.dismiss();
            showAnalyticsScreen();
        }));
        dialog.show();
    }

    private Calendar midnight(Calendar value) {
        value.set(Calendar.HOUR_OF_DAY, 0); value.set(Calendar.MINUTE, 0); value.set(Calendar.SECOND, 0); value.set(Calendar.MILLISECOND, 0); return value;
    }

    private View comparisonCard(String title, long current, long previous) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 10));
        card.setBackground(Ui.layeredBackground(this, 20)); card.setElevation(Ui.dp(this, 4));
        card.addView(Ui.text(this, title, 12, Color.WHITE, false)); card.addView(Ui.text(this, Money.format(current, dashboardCurrency), 17, Color.WHITE, true));
        String change; if (previous < 0) change = "اضغط لعرض التفاصيل"; else if (previous == 0) change = current == 0 ? "لا تغيير" : "لا توجد مقارنة سابقة"; else { double percent = (current - previous) * 100.0 / previous; change = (percent >= 0 ? "↑ " : "↓ ") + String.format(Locale.ROOT, "%.1f%%", Math.abs(percent)) + " عن السابق"; }
        card.addView(Ui.text(this, change, 11, Color.rgb(232, 239, 255), false));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 116), 1); p.setMargins(Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3), Ui.dp(this, 6)); card.setLayoutParams(p); return card;
    }

    /** C7: income and received remittances are separate analytical flows,
     * not all account credits (which include internal transfers and debts). */
    private void showIncomePeriodScreen(String title, String currency, long startInclusive,
                                        long endExclusive, boolean receivedOnly) {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this);
        page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader(title, this::showAnalyticsScreen),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this);
        content.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 34));
        long total = db.getIncomeTotal(currency, startInclusive, endExclusive, receivedOnly);
        TextView totalView = Ui.text(this, Money.format(total, currency), 25, Ui.INCOME, true);
        totalView.setGravity(Gravity.CENTER);
        totalView.setBackground(Ui.layeredSoftBackground(this, 20));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78));
        totalParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4),
                Ui.dp(this, 16), Ui.dp(this, 10));
        content.addView(totalView, totalParams);
        List<Models.Transaction> transactions =
                db.getIncomeTransactions(currency, startInclusive, endExclusive, receivedOnly);
        content.addView(sectionTitle(receivedOnly ? "الحوالات المستلمة" : "عمليات الدخل",
                transactions.isEmpty() ? "لا توجد عمليات" : transactions.size() + " عمليات"));
        if (transactions.isEmpty()) {
            content.addView(messageCard("لا توجد عمليات",
                    receivedOnly ? "لا توجد حوالات مستلمة بهذه العملة خلال الفترة"
                            : "لا توجد عمليات دخل بهذه العملة خلال الفترة"));
        } else {
            for (Models.Transaction tx : transactions) content.addView(transactionCard(tx));
        }
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page);
        Ui.animateScreen(page);
        content.post(() -> Ui.animateChildren(content));
    }

    private void showExpensePeriodScreen(String title, String currency, long startInclusive, long endExclusive) {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader(title, this::showAnalyticsScreen), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this); content.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 34));
        long total = db.getExpenseTotal(currency, startInclusive, endExclusive);
        TextView totalView = Ui.text(this, Money.format(total, currency), 25, Ui.EXPENSE, true);
        totalView.setGravity(Gravity.CENTER); totalView.setBackground(Ui.layeredSoftBackground(this, 20));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78));
        totalParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 10)); content.addView(totalView, totalParams);
        List<Models.Transaction> transactions = db.getExpenseTransactions(currency, startInclusive, endExclusive);
        content.addView(sectionTitle("تفاصيل المصروفات", transactions.isEmpty() ? "لا توجد عمليات" : transactions.size() + " عمليات"));
        if (transactions.isEmpty()) content.addView(messageCard("لا توجد مصروفات", "لا توجد عمليات محسوبة ضمن هذه الفترة والعملة"));
        else {
            content.addView(Ui.text(this, "العمليات المقسمة قد تتضمن أجزاء لا تدخل في مجموع المصروفات.", 11, Ui.MUTED, false),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 34)));
            for (Models.Transaction tx : transactions) content.addView(transactionCard(tx));
        }
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page); Ui.animateScreen(page); content.post(() -> Ui.animateChildren(content));
    }

    private View categoryAnalysisRow(Models.CategoryTotal item, long max, long startInclusive, long endExclusive) {
        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9)); card.setBackground(Ui.rounded(Ui.SURFACE, 16, this));
        LinearLayout text = Ui.row(this); text.addView(Ui.text(this, item.emoji + " " + item.name, 14, Ui.TEXT, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1));
        TextView amount = Ui.text(this, Money.format(item.amountMinor, dashboardCurrency), 13, Ui.VIOLET, true); amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); text.addView(amount); card.addView(text);
        FrameLayout track = new FrameLayout(this); track.setBackground(Ui.rounded(Ui.BRAND_SOFT, 5, this)); View fill = new View(this); fill.setBackground(Ui.gradient(Ui.VIOLET, Ui.SKY, 5, this));
        int width = Math.max(Ui.dp(this, 16), (int) (Ui.dp(this, 300) * (item.amountMinor / (float) max))); track.addView(fill, new FrameLayout.LayoutParams(width, Ui.dp(this, 8))); card.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 8)));
        card.setOnClickListener(v -> showCategoryExpenseScreen(item, dashboardCurrency, startInclusive, endExclusive));
        Ui.pressAnimation(card);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)); p.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4)); card.setLayoutParams(p); return card;
    }

    private void showCategoryExpenseScreen(Models.CategoryTotal category, String currency, long startInclusive, long endExclusive) {
        addScreenVisible = false; subScreenVisible = true;
        LinearLayout page = Ui.column(this); page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader(category.emoji + " " + category.name, this::showAnalyticsScreen),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this); content.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 34));
        TextView totalView = Ui.text(this, Money.format(category.amountMinor, currency), 25, Ui.EXPENSE, true);
        totalView.setGravity(Gravity.CENTER); totalView.setBackground(Ui.layeredSoftBackground(this, 20));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78));
        totalParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 10)); content.addView(totalView, totalParams);
        List<Models.Transaction> transactions = db.getExpenseTransactionsByCategory(currency, category.categoryId, startInclusive, endExclusive);
        content.addView(sectionTitle("عمليات هذا الشهر", transactions.size() + " عمليات"));
        if (transactions.isEmpty()) content.addView(messageCard("لا توجد عمليات", "لم نجد عمليات لهذا التصنيف"));
        else for (Models.Transaction tx : transactions) content.addView(transactionCard(tx));
        scroll.addView(content); page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page); Ui.animateScreen(page); content.post(() -> Ui.animateChildren(content));
    }

    private void showAccountManagement() {
        addScreenVisible = false;
        subScreenVisible = true;
        LinearLayout page = Ui.column(this);
        page.setBackgroundColor(Ui.BACKGROUND);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        top.setBackground(Ui.layeredBackground(this, 0));
        TextView back = Ui.text(this, "‹", 34, Color.WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> showMain(false));
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 54)));
        top.addView(Ui.text(this, "إدارة الحسابات", 22, Color.WHITE, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
        page.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 30));
        TextView hint = Ui.text(this, "رتّب حساباتك كما تريد. الحساب الذي لديه عمليات سابقة يُخفى بدل حذف تاريخه.", 13, Ui.MUTED, false);
        hint.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 12));
        content.addView(hint);
        Button add = actionButton("＋ إضافة حساب جديد", true);
        add.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 17, this));
        add.setOnClickListener(v -> showAddAccountDialog());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56));
        addParams.setMargins(0, 0, 0, Ui.dp(this, 12));
        content.addView(add, addParams);

        List<Models.Account> accounts = db.getAccounts();
        for (Models.Account account : accounts) content.addView(manageAccountCard(account));
        if (accounts.isEmpty()) content.addView(Ui.text(this, "لا توجد حسابات. أضف حسابًا للبدء.", 15, Ui.MUTED, false));
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(page);
        Ui.animateScreen(page);
    }

    private View manageAccountCard(Models.Account account) {
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        card.setBackground(Ui.gradient(Ui.SURFACE, Ui.DARK ? Color.rgb(30, 35, 55) : Color.rgb(244, 241, 255), 20, this));

        LinearLayout info = Ui.row(this);
        info.addView(accountIcon(account), new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        LinearLayout labels = Ui.column(this);
        labels.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        labels.addView(Ui.text(this, account.name, 16, Ui.TEXT, true));
        labels.addView(Ui.text(this, Money.format(account.balanceMinor, account.currency), 12, Ui.MUTED, false));
        info.addView(labels, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        info.setOnClickListener(v -> showAccountDetails(account));
        Ui.pressAnimation(info);
        card.addView(info);

        LinearLayout actions = Ui.row(this);
        Button up = miniButton("↑ للأعلى");
        Button down = miniButton("↓ للأسفل");
        Button delete = miniButton("حذف");
        delete.setTextColor(Ui.EXPENSE);
        up.setOnClickListener(v -> { db.moveAccount(account.id, -1); showAccountManagement(); });
        down.setOnClickListener(v -> { db.moveAccount(account.id, 1); showAccountManagement(); });
        delete.setOnClickListener(v -> confirmRemoveAccount(account));
        actions.addView(up);
        actions.addView(down);
        actions.addView(delete);
        card.addView(actions);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 128));
        p.setMargins(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        card.setLayoutParams(p);
        return card;
    }

    private Button miniButton(String text) {
        Button button = actionButton(text, false);
        button.setTextSize(12);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1);
        p.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        button.setLayoutParams(p);
        return button;
    }

    private void confirmRemoveAccount(Models.Account account) {
        new AlertDialog.Builder(this)
                .setTitle("حذف " + account.name + "؟")
                .setMessage(account.balanceMinor == 0 ? "لن تظهر هذه الخانة في الصفحة الرئيسية. العمليات القديمة، إن وجدت، ستبقى محفوظة." : "رصيد الحساب ليس صفرًا، لذلك لا يمكن حذفه الآن.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حذف", (dialog, which) -> {
                    try {
                        boolean archived = db.removeAccount(account.id);
                        Toast.makeText(this, archived ? "تم إخفاء الحساب مع الاحتفاظ بتاريخه" : "تم حذف الحساب", Toast.LENGTH_SHORT).show();
                        showAccountManagement();
                    } catch (Exception error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void showAddAccountDialog() {
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        EditText name = input("اسم الحساب");
        Spinner currency = spinner(Arrays.asList("ILS", "USD", "JOD"));
        form.addView(name, fieldParams());
        form.addView(label("العملة"));
        form.addView(currency, fieldParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("إضافة حساب")
                .setView(form)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إضافة", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) {
                name.setError("اكتب اسم الحساب");
                return;
            }
            db.addCustomAccount(name.getText().toString(), currency.getSelectedItem().toString());
            dialog.dismiss();
            showAccountManagement();
            Toast.makeText(this, "تمت إضافة الحساب", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private static final class SplitRowRef {
        Spinner category;
        Spinner mode;
        Spinner person;
        EditText amount;
    }

    private void showAddTransaction() {
        addScreenVisible = true;
        subScreenVisible = false;
        final String[] selectedType = {FinanceRules.EXPENSE};
        final List<Models.Account> filteredAccounts = new ArrayList<>();
        final List<Models.Category> filteredCategories = new ArrayList<>();
        final List<Models.DebtPerson> debtPeople = db.getDebtPeople();
        final List<SplitRowRef> splitRows = new ArrayList<>();

        LinearLayout page = Ui.column(this);
        page.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 8));
        top.setBackground(Ui.layeredBackground(this, 0));
        TextView back = Ui.text(this, "‹", 34, Color.WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> showMain(transactionsTab));
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 52)));
        top.addView(Ui.text(this, "إضافة عملية", 22, Color.WHITE, true), new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        page.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 18), Ui.dp(this, 6), Ui.dp(this, 18), Ui.dp(this, 40));
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        form.addView(label("نوع العملية"));
        RadioGroup typeGroup = new RadioGroup(this);
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        typeGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        RadioButton expense = radio("مصروف", true);
        RadioButton income = radio("دخل", false);
        RadioButton transfer = radio("تحويل", false);
        typeGroup.addView(expense, new RadioGroup.LayoutParams(0, Ui.dp(this, 48), 1));
        typeGroup.addView(income, new RadioGroup.LayoutParams(0, Ui.dp(this, 48), 1));
        typeGroup.addView(transfer, new RadioGroup.LayoutParams(0, Ui.dp(this, 48), 1));
        form.addView(typeGroup, fieldParams());

        EditText amount = input("المبلغ");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount, fieldParams());

        TextView currencyLabel=label("العملة");
        form.addView(currencyLabel);
        Spinner currency = spinner(Arrays.asList("ILS", "USD", "JOD"));
        form.addView(currency, fieldParams());

        form.addView(label("الحساب"));
        Spinner account = new Spinner(this);
        form.addView(account, fieldParams());

        LinearLayout destinationBox = Ui.column(this);
        destinationBox.addView(label("إلى حساب"));
        Spinner destination = new Spinner(this);
        destinationBox.addView(destination, fieldParams());
        Button multiCurrencyTransfer = miniButton("تحويل بين عملتين (USD ↔ ILS وغيرها)");
        multiCurrencyTransfer.setOnClickListener(v -> showFxTransferDialog());
        destinationBox.addView(multiCurrencyTransfer, fieldParams());
        destinationBox.setVisibility(View.GONE);
        form.addView(destinationBox);

        CheckBox affectsBalance = new CheckBox(this);
        affectsBalance.setText("تدخل العملية في حساب الرصيد");
        affectsBalance.setTextSize(15);
        affectsBalance.setTextColor(Ui.TEXT);
        affectsBalance.setChecked(true);
        form.addView(affectsBalance, fieldParams());
        TextView balanceHint = Ui.text(this, "أوقفها إذا أردت تسجيل العملية فقط دون خصم أو إضافة المبلغ إلى الحساب.", 11, Ui.MUTED, false);
        balanceHint.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 6));
        form.addView(balanceHint);

        LinearLayout categoryBox = Ui.column(this);
        categoryBox.addView(label("التصنيف"));
        Spinner category = new Spinner(this);
        categoryBox.addView(category, fieldParams());
        TextView smartHint = Ui.text(this, "", 12, Ui.BRAND, false);
        smartHint.setVisibility(View.GONE);
        Button applySmartHint = miniButton("تطبيق الاقتراح");
        applySmartHint.setVisibility(View.GONE);
        categoryBox.addView(smartHint, fieldParams());
        categoryBox.addView(applySmartHint, fieldParams());
        final long[] proposedCategory = {-1};
        Button addCategory = actionButton("＋ إضافة تصنيف جديد", false);
        categoryBox.addView(addCategory, fieldParams());
        form.addView(categoryBox);

        CheckBox splitCheck = new CheckBox(this);
        splitCheck.setText("تقسيم العملية على أكثر من تصنيف");
        splitCheck.setTextSize(15);
        splitCheck.setTextColor(Ui.TEXT);
        form.addView(splitCheck, fieldParams());

        LinearLayout splitBox = Ui.column(this);
        splitBox.setVisibility(View.GONE);
        LinearLayout splitRowsBox = Ui.column(this);
        splitBox.addView(splitRowsBox);
        Button addPart = actionButton("＋ إضافة جزء", false);
        splitBox.addView(addPart, fieldParams());
        form.addView(splitBox);

        EditText note = input("ملاحظة اختيارية");
        note.setMinLines(2);
        note.setGravity(Gravity.TOP | Gravity.START);
        form.addView(note, fieldParams());

        Button save = actionButton("حفظ العملية", true);
        save.setBackground(Ui.gradient(Ui.BRAND, Ui.VIOLET, 16, this));
        LinearLayout.LayoutParams saveParams = fieldParams();
        saveParams.height = Ui.dp(this, 56);
        saveParams.topMargin = Ui.dp(this, 14);
        form.addView(save, saveParams);

        Runnable refreshAccounts = () -> {
            String selectedCurrency = currency.getSelectedItem().toString();
            filteredAccounts.clear();
            for (Models.Account a : db.getAccounts())
                if (FinanceRules.TRANSFER.equals(selectedType[0]) || a.currency.equals(selectedCurrency))
                    filteredAccounts.add(a);
            List<String> labels = new ArrayList<>();
            for (Models.Account a : filteredAccounts) labels.add(a.displayName());
            account.setAdapter(adapter(labels));
            destination.setAdapter(adapter(labels));
            if (labels.size() > 1) destination.setSelection(1);
        };
        Runnable refreshCategories = () -> {
            String kind = FinanceRules.INCOME.equals(selectedType[0]) ? FinanceRules.INCOME : FinanceRules.EXPENSE;
            filteredCategories.clear(); filteredCategories.addAll(db.getCategories(kind));
            List<String> labels = new ArrayList<>();
            labels.add("▤ دَين — لا يؤثر على الرصيد أو التحليل");
            labels.add("✓ سداد دين — يؤثر على الرصيد فقط");
            for (Models.Category c : filteredCategories) labels.add(c.displayName());
            category.setAdapter(adapter(labels));
            List<String> splitLabels = new ArrayList<>(); for (Models.Category c : filteredCategories) splitLabels.add(c.displayName());
            for (SplitRowRef row : splitRows) row.category.setAdapter(adapter(splitLabels));
        };
        Runnable refreshSuggestion = () -> {
            proposedCategory[0]=-1;
            smartHint.setVisibility(View.GONE);
            applySmartHint.setVisibility(View.GONE);
            if (splitCheck.isChecked() || FinanceRules.TRANSFER.equals(selectedType[0]) || filteredAccounts.isEmpty()) return;
            int index = account.getSelectedItemPosition();
            if (index<0 || index>=filteredAccounts.size()) return;
            Long value = Money.parseMinor(amount.getText().toString());
            if (value==null || value<=0) return;
            String kind=selectedType[0], selectedCurrency=currency.getSelectedItem().toString();
            CategorySuggestionRules.Suggestion suggestion=db.suggestCategory(kind,selectedCurrency,
                    filteredAccounts.get(index).id,value,note.getText().toString());
            if(suggestion==null)return;
            for (Models.Category candidate:filteredCategories) if (candidate.id==suggestion.categoryId) {
                proposedCategory[0]=candidate.id;
                smartHint.setText("اقتراح: " + candidate.displayName()+ " — " + suggestion.reason);
                smartHint.setVisibility(View.VISIBLE);
                applySmartHint.setVisibility(View.VISIBLE);
                return;
            }
        };
        applySmartHint.setOnClickListener(v -> {
            if (proposedCategory[0] <= 0) return;
            for(int i=0;i<filteredCategories.size();i++)if(filteredCategories.get(i).id==proposedCategory[0]){
                category.setSelection(i+2); // Two special debt entries precede normal categories.
                break;
            }
        });
        Runnable refreshVisibility = () -> {
            boolean isTransfer = FinanceRules.TRANSFER.equals(selectedType[0]);
            boolean isExpense = FinanceRules.EXPENSE.equals(selectedType[0]);
            destinationBox.setVisibility(isTransfer ? View.VISIBLE : View.GONE);
            currency.setVisibility(isTransfer ? View.GONE : View.VISIBLE);
            currencyLabel.setVisibility(isTransfer ? View.GONE : View.VISIBLE);
            categoryBox.setVisibility(isTransfer || (isExpense && splitCheck.isChecked()) ? View.GONE : View.VISIBLE);
            splitCheck.setVisibility(isExpense ? View.VISIBLE : View.GONE);
            splitBox.setVisibility(isExpense && splitCheck.isChecked() ? View.VISIBLE : View.GONE);
        };
        addCategory.setOnClickListener(v -> showAddCategoryDialog(
                FinanceRules.INCOME.equals(selectedType[0]) ? FinanceRules.INCOME : FinanceRules.EXPENSE,
                refreshCategories));

        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == income.getId()) selectedType[0] = FinanceRules.INCOME;
            else if (checkedId == transfer.getId()) selectedType[0] = FinanceRules.TRANSFER;
            else selectedType[0] = FinanceRules.EXPENSE;
            if (!FinanceRules.EXPENSE.equals(selectedType[0])) splitCheck.setChecked(false);
            refreshCategories.run();
            refreshAccounts.run();
            refreshVisibility.run();
            refreshSuggestion.run();
        });
        currency.setOnItemSelectedListener(new SimpleItemSelected(() -> { refreshAccounts.run(); refreshSuggestion.run(); }));
        account.setOnItemSelectedListener(new SimpleItemSelected(refreshSuggestion));
        TextWatcher learningInputs = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence text,int start,int before,int count) { refreshSuggestion.run(); }
            @Override public void afterTextChanged(Editable text) {}
        };
        amount.addTextChangedListener(learningInputs);
        note.addTextChangedListener(learningInputs);
        splitCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && splitRows.isEmpty()) {
                addDebtSplitRow(splitRowsBox, splitRows, filteredCategories, debtPeople);
                addDebtSplitRow(splitRowsBox, splitRows, filteredCategories, debtPeople);
            }
            refreshVisibility.run();
            refreshSuggestion.run();
        });
        addPart.setOnClickListener(v -> addDebtSplitRow(splitRowsBox, splitRows, filteredCategories, debtPeople));

        refreshAccounts.run();
        refreshCategories.run();
        refreshVisibility.run();
        refreshSuggestion.run();

        save.setOnClickListener(v -> {
            try {
                Long amountMinor = Money.parseMinor(amount.getText().toString());
                if (amountMinor == null || amountMinor <= 0) throw new IllegalArgumentException("اكتب مبلغًا صحيحًا");
                if (filteredAccounts.isEmpty()) throw new IllegalArgumentException("لا يوجد حساب بهذه العملة");
                Models.Account source = filteredAccounts.get(account.getSelectedItemPosition());
                Long destinationId = null;
                Long categoryId = null;
                List<Models.SplitInput> splits = new ArrayList<>();
                if (FinanceRules.TRANSFER.equals(selectedType[0])) {
                    if (destination.getSelectedItemPosition() < 0) throw new IllegalArgumentException("اختر الحساب المستلم");
                    Models.Account dest = filteredAccounts.get(destination.getSelectedItemPosition());
                    if (dest.id == source.id) throw new IllegalArgumentException("اختر حسابًا مختلفًا للاستلام");
                    if(!dest.currency.equals(source.currency)) {
                        // Cross-currency transfer has its own dedicated rate-confirmation flow.
                        showFxTransferDialog(source.id,dest.id,amount.getText().toString());
                        return;
                    }
                    destinationId = dest.id;
                } else if (splitCheck.isChecked()) {
                    List<Long> parts = new ArrayList<>();
                    for (SplitRowRef row : splitRows) {
                        Long partAmount = Money.parseMinor(row.amount.getText().toString());
                        if (partAmount == null || partAmount <= 0) throw new IllegalArgumentException("أكمل مبالغ التقسيم");
                        int mode = row.mode == null ? 0 : row.mode.getSelectedItemPosition();
                        if (mode == 0) {
                            if (row.category.getSelectedItemPosition() < 0) throw new IllegalArgumentException("اختر تصنيف كل جزء");
                            Models.Category partCategory = filteredCategories.get(row.category.getSelectedItemPosition());
                            splits.add(new Models.SplitInput(partCategory.id, partAmount));
                        } else {
                            if (debtPeople.isEmpty() || row.person == null || row.person.getSelectedItemPosition() < 0)
                                throw new IllegalArgumentException("أضف صاحب الدين من صفحة الديون أولًا");
                            Models.DebtPerson person = debtPeople.get(row.person.getSelectedItemPosition());
                            splits.add(Models.SplitInput.debt(partAmount,
                                    mode == 1 ? FinanceRules.SPECIAL_DEBT : FinanceRules.SPECIAL_DEBT_PAYMENT,
                                    person.id, mode == 1 ? FinanceRules.DEBT_RECEIVABLE : FinanceRules.DEBT_PAYABLE));
                        }
                        parts.add(partAmount);
                    }
                    if (!FinanceRules.splitsMatch(amountMinor, parts)) throw new IllegalArgumentException("مجموع الأجزاء يجب أن يساوي المبلغ الكامل");
                } else {
                    if (category.getSelectedItemPosition() < 0) throw new IllegalArgumentException("اختر التصنيف");
                    int selectedCategory = category.getSelectedItemPosition();
                    if (selectedCategory < 2) {
                        showNewDebtTransactionDialog(selectedType[0], amountMinor, currency.getSelectedItem().toString(), source.id,
                                note.getText().toString(), affectsBalance.isChecked(), selectedCategory == 0 ? FinanceRules.SPECIAL_DEBT : FinanceRules.SPECIAL_DEBT_PAYMENT);
                        return;
                    }
                    categoryId = filteredCategories.get(selectedCategory - 2).id;
                }
                String transactionCurrency=FinanceRules.TRANSFER.equals(selectedType[0])
                        ? source.currency:currency.getSelectedItem().toString();
                db.addTransaction(selectedType[0], amountMinor, transactionCurrency, source.id,
                        destinationId, categoryId, note.getText().toString(), splits, affectsBalance.isChecked());
                Toast.makeText(this, "تم حفظ العملية", Toast.LENGTH_SHORT).show();
                showMain(false);
            } catch (Exception error) {
                Toast.makeText(this, error.getMessage() == null ? "تعذر حفظ العملية" : error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        setContentView(page);
        Ui.animateScreen(page);
    }

    private void showNewDebtTransactionDialog(String type,long amountMinor,String currency,long accountId,String note,boolean affectsBalance,String specialKind){
        String inferred=FinanceRules.SPECIAL_DEBT.equals(specialKind)
                ? (FinanceRules.EXPENSE.equals(type)?FinanceRules.DEBT_RECEIVABLE:FinanceRules.DEBT_PAYABLE)
                : (FinanceRules.EXPENSE.equals(type)?FinanceRules.DEBT_PAYABLE:FinanceRules.DEBT_RECEIVABLE);
        List<Models.DebtPerson> people=FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind)?db.getDebtPeopleWithOpenBalance(inferred,currency):db.getDebtPeople();
        if(people.isEmpty()){
            if(FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind)){Toast.makeText(this,"لا يوجد دين نشط لهذا الاتجاه والعملة",Toast.LENGTH_LONG).show();return;}
            showQuickAddDebtPerson(()->showNewDebtTransactionDialog(type,amountMinor,currency,accountId,note,affectsBalance,specialKind));return;
        }
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));List<String> names=new ArrayList<>();for(Models.DebtPerson p:people)names.add(p.name);Spinner person=spinner(names);form.addView(label("الدَين لمين؟"));form.addView(person,fieldParams());
        RadioGroup directions=new RadioGroup(this);directions.setOrientation(RadioGroup.HORIZONTAL);directions.setGravity(Gravity.CENTER);RadioButton mine=radio("إلي",FinanceRules.DEBT_RECEIVABLE.equals(inferred));RadioButton owed=radio("عليّ",FinanceRules.DEBT_PAYABLE.equals(inferred));directions.addView(mine);directions.addView(owed);if(FinanceRules.SPECIAL_DEBT_PAYMENT.equals(specialKind)){mine.setEnabled(false);owed.setEnabled(false);}form.addView(label("اتجاه الدين"));form.addView(directions,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(FinanceRules.SPECIAL_DEBT.equals(specialKind)?"إضافة دَين":"إضافة سداد دين").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{String direction=mine.isChecked()?FinanceRules.DEBT_RECEIVABLE:FinanceRules.DEBT_PAYABLE;db.addDebtTransaction(type,amountMinor,currency,accountId,note,affectsBalance,specialKind,people.get(person.getSelectedItemPosition()).id,direction);dialog.dismiss();Toast.makeText(this,"تم حفظ حركة الدين",Toast.LENGTH_SHORT).show();showMain(false);}catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
    }

    private void showAuditHistory(){
        addScreenVisible=false;subScreenVisible=true;
        LinearLayout page=Ui.column(this);page.setBackgroundColor(Ui.BACKGROUND);
        page.addView(pageHeader("سجل التعديلات",this::showSettings),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,78)));
        ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);
        body.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,32));
        body.addView(Ui.text(this,"سجل محفوظ على الجهاز: يبيّن الإضافة والتعديل والحذف والتقسيم. التراجع متاح لتغيير التصنيف العادي الأخير أو حذف عملية يدوية بسيطة حُذفت بعد هذا التحديث. لا يمكن استرجاع الديون أو التحويلات أو العمليات المقسّمة تلقائيًا، حفاظًا على الأرصدة.",12,Ui.MUTED,false));
        List<FinanceDbHelper.AuditEntry> events=db.getChangeHistory(200);
        if(events.isEmpty())body.addView(Ui.text(this,"لا توجد تعديلات مسجّلة بعد تحديث هذه النسخة.",14,Ui.MUTED,false));
        for(FinanceDbHelper.AuditEntry entry:events){
            LinearLayout card=Ui.column(this);card.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));
            card.setBackground(Ui.rounded(Ui.SURFACE,14,this));
            card.addView(Ui.text(this,entry.action+" · #"+entry.recordId+" · " + new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT).format(new Date(entry.createdAt)),12,Ui.TEXT,true));
            if(entry.before!=null)card.addView(Ui.text(this,"قبل: "+entry.before,12,Ui.MUTED,false));
            if(entry.after!=null)card.addView(Ui.text(this,"بعد: "+entry.after,12,Ui.MUTED,false));
            if(entry.undoable && "transactions".equals(entry.table)) {
                boolean deleted="DELETE".equals(entry.action);
                Button undo=miniButton(deleted?"↶ استرجاع العملية المحذوفة":"↶ تراجع عن تغيير التصنيف");
                undo.setOnClickListener(v->new AlertDialog.Builder(this)
                    .setTitle(deleted?"استرجاع العملية المحذوفة؟":"تراجع عن التصنيف؟")
                    .setMessage(deleted?"سيُعاد سجل العملية اليدوية ورصيد الحساب داخل معاملة واحدة. لن يُسترجع حذف الديون أو التحويلات.":
                            "سيتم إرجاع التصنيف السابق وإعادة احتساب الرصيد. لا يمكن التراجع عن تعديلات أقدم إذا تغيرت الحركة بعدها.")
                    .setNegativeButton("إلغاء",null).setPositiveButton("تراجع",(d,w)->{
                        try{
                            if(deleted)db.undoDeletedSimpleTransaction(entry.id);
                            else db.undoLatestSimpleCategoryChange(entry.id);
                            showAuditHistory();
                        }catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
                    }).show());
                card.addView(undo,fieldParams());
            }
            body.addView(card,fieldParams());
        }
        scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(page);Ui.animateScreen(page);
    }

    private void showCategoryManagement(){
        String[] kinds={"تصنيفات المصروف","تصنيفات الدخل"};
        new AlertDialog.Builder(this).setTitle("إدارة التصنيفات")
                .setItems(kinds,(dialog,kindIndex)->showCategoryManagementForKind(kindIndex==0?FinanceRules.EXPENSE:FinanceRules.INCOME))
                .setNegativeButton("إغلاق",null).show();
    }
    private void showCategoryManagementForKind(String kind){
        List<Models.Category> categories=db.getCategories(kind);
        if(categories.isEmpty()){Toast.makeText(this,"لا توجد تصنيفات",Toast.LENGTH_SHORT).show();return;}
        String[] labels=new String[categories.size()];boolean[] checked=new boolean[categories.size()];
        for(int i=0;i<labels.length;i++)labels[i]=categories.get(i).displayName();
        new AlertDialog.Builder(this).setTitle("اختر التصنيفات للحذف أو إدارة الاسم والرمز")
                .setMultiChoiceItems(labels,checked,(dialog,which,isChecked)->checked[which]=isChecked)
                .setNeutralButton("تعديل اسم / رمز",(dialog,unused)->showCategoryIconPicker(kind))
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف المحدد",(dialog,unused)->{
                    List<Long> ids=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])ids.add(categories.get(i).id);
                    if(ids.isEmpty())return;
                    new AlertDialog.Builder(this).setTitle("تأكيد إزالة " +ids.size()+" تصنيف؟")
                            .setMessage("التصنيف المستخدم سيُخفى من الإضافات الجديدة مع الاحتفاظ باسم التصنيف الأصلي في سجل عملياتك. التصنيف غير المستخدم سيُحذف. لن تتغير الأرصدة.")
                            .setNegativeButton("إلغاء",null).setPositiveButton("إزالة",(d,w)->{
                                try{int changed=db.removeCategoriesSafely(ids);Toast.makeText(this,"تمت إزالة "+changed+" تصنيفات",Toast.LENGTH_LONG).show();}
                                catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
                            }).show();
                }).show();
    }
    private void showCategoryIconPicker(String kind){
        List<Models.Category> categories=db.getCategories(kind);
        if(categories.isEmpty()){Toast.makeText(this,"لا توجد تصنيفات متاحة",Toast.LENGTH_SHORT).show();return;}
        String[] labels=new String[categories.size()];
        for(int i=0;i<labels.length;i++)labels[i]=categories.get(i).displayName();
        new AlertDialog.Builder(this).setTitle("اختر التصنيف لتعديل اسمه أو رمزه")
                .setItems(labels,(dialog,which)->{
                    Models.Category chosen=categories.get(which);
                    final String[] choices={
                            "✏️ تغيير اسم التصنيف",
                            "🚕","🚌","🚗","⛽","🛍️","🛒","🍽️","🥗","☕","🍞",
                            "💊","🩺","📚","🎓","💼","💰","🏠","🧾","💡","📱",
                            "🎁","🎮","🧸","👕","🧼","💳","🏦","🤝","🤲","✈️",
                            "🐾","🌱","✨","🏷️","📦","🪙","💸","🔧","🎨",
                            "✏️ رمز مخصص"
                    };
                    new AlertDialog.Builder(this).setTitle("ماذا تريد تغييرًا في "+chosen.name+"؟")
                            .setItems(choices,(d,index)->{
                                if(index==0){
                                    showRenameCategoryDialog(chosen,kind);
                                    return;
                                }
                                if(index==choices.length-1){
                                    showCustomCategoryIconDialog(chosen,kind);
                                    return;
                                }
                                try{
                                    db.setCategoryIcon(chosen.id,choices[index]);
                                    Toast.makeText(this,"تم تغيير الرمز",Toast.LENGTH_SHORT).show();
                                    showCategoryManagementForKind(kind);
                                }catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
                            }).setNegativeButton("إلغاء",null).show();
                }).setNegativeButton("إلغاء",null).show();
    }

    private void showRenameCategoryDialog(Models.Category chosen,String kind) {
        EditText nameInput=input("الاسم الجديد للتصنيف");
        nameInput.setSingleLine(true);
        nameInput.setText(chosen.name);
        nameInput.setSelectAllOnFocus(true);
        LinearLayout form=Ui.column(this);
        form.setPadding(Ui.dp(this,18),Ui.dp(this,8),Ui.dp(this,18),Ui.dp(this,8));
        form.addView(Ui.text(this,
                "سيظهر الاسم الجديد على عملياتك السابقة أيضًا، دون تغيير أي مبلغ أو رصيد.",
                12,Ui.MUTED,false));
        form.addView(nameInput,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("إعادة تسمية "+chosen.name)
                .setView(form).setNegativeButton("إلغاء",null)
                .setPositiveButton("حفظ الاسم",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                db.renameCategory(chosen.id,nameInput.getText().toString());
                dialog.dismiss();
                Toast.makeText(this,"تم تحديث اسم التصنيف",Toast.LENGTH_SHORT).show();
                showCategoryManagementForKind(kind);
            } catch(Exception error) {
                nameInput.setError(error.getMessage()==null?"تعذّر حفظ الاسم":error.getMessage());
            }
        }));
        dialog.show();
    }

    private void showCustomCategoryIconDialog(Models.Category chosen,String kind){
        EditText iconInput=input("أدخل رمزًا تعبيريًا مثل 🦋 أو 🪙");
        iconInput.setSingleLine(true);
        iconInput.setText(chosen.emoji==null?"":chosen.emoji);
        iconInput.setSelectAllOnFocus(true);
        LinearLayout form=Ui.column(this);
        form.setPadding(Ui.dp(this,18),Ui.dp(this,12),Ui.dp(this,18),Ui.dp(this,12));
        form.addView(iconInput,fieldParams());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تخصيص رمز "+chosen.name)
                .setView(form).setNegativeButton("إلغاء",null)
                .setPositiveButton("حفظ الرمز",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String symbol=iconInput.getText().toString().trim();
            if(symbol.isEmpty() || symbol.codePointCount(0,symbol.length())>8){
                iconInput.setError("اختر رمزًا من 1 إلى 8 أحرف Unicode");
                return;
            }
            try{
                db.setCategoryIcon(chosen.id,symbol);
                dialog.dismiss();
                Toast.makeText(this,"تم حفظ الرمز المخصص",Toast.LENGTH_SHORT).show();
                showCategoryManagementForKind(kind);
            }catch(Exception error){
                iconInput.setError(error.getMessage()==null?"تعذر حفظ الرمز":error.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddCategoryDialog(String kind, Runnable afterSave) {
        LinearLayout form = Ui.column(this); form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        EditText emoji = input("رمز مثل 🍕 أو 🚗"); EditText name = input("اسم التصنيف");
        form.addView(emoji, fieldParams()); form.addView(name, fieldParams());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("إضافة تصنيف")
                .setView(form).setNegativeButton("إلغاء", null).setPositiveButton("إضافة", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("اكتب اسم التصنيف"); return; }
            try {
                db.addCustomCategory(name.getText().toString(), kind, emoji.getText().toString());
                dialog.dismiss(); afterSave.run();
                Toast.makeText(this, "تمت إضافة التصنيف", Toast.LENGTH_SHORT).show();
            } catch(Exception error) {
                name.setError(error.getMessage()==null?"تعذر إضافة التصنيف":error.getMessage());
            }
        })); dialog.show();
    }

    private void addDebtSplitRow(LinearLayout container, List<SplitRowRef> rows,
                                 List<Models.Category> categories, List<Models.DebtPerson> people) {
        addSplitRow(container, rows, categories);
        SplitRowRef row = rows.get(rows.size()-1);
        // The original row's fields stay intact; insert compact debt controls above its amount.
        ViewGroup card = (ViewGroup) row.category.getParent();
        Spinner mode = spinner(java.util.Arrays.asList("مصروف عادي", "دَين — إلي", "سداد دَين — عليّ"));
        row.mode = mode;
        card.addView(mode, 0, fieldParams());
        List<String> names = new ArrayList<>();
        for (Models.DebtPerson person:people) names.add(person.name);
        Spinner person = spinner(names);
        row.person = person;
        card.addView(person, 1, fieldParams());
        mode.setOnItemSelectedListener(new SimpleItemSelected(() -> {
            boolean debt = mode.getSelectedItemPosition()!=0;
            person.setVisibility(debt ? View.VISIBLE : View.GONE);
            row.category.setVisibility(debt ? View.GONE : View.VISIBLE);
        }));
        person.setVisibility(View.GONE);
    }

    private void addSplitRow(LinearLayout container, List<SplitRowRef> rows, List<Models.Category> categories) {
        SplitRowRef ref = new SplitRowRef();
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        card.setBackground(Ui.rounded(Ui.BRAND_SOFT, 16, this));

        Spinner category = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (Models.Category c : categories) labels.add(c.displayName());
        category.setAdapter(adapter(labels));
        EditText amount = input("مبلغ الجزء");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Button remove = actionButton("حذف الجزء", false);
        remove.setTextColor(Ui.EXPENSE);

        card.addView(category, fieldParams());
        card.addView(amount, fieldParams());
        card.addView(remove, fieldParams());
        ref.category = category; ref.amount = amount;
        rows.add(ref);
        remove.setOnClickListener(v -> {
            if (rows.size() <= 2) {
                Toast.makeText(this, "التقسيم يحتاج جزأين على الأقل", Toast.LENGTH_SHORT).show();
                return;
            }
            rows.remove(ref);
            container.removeView(card);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        container.addView(card, params);
    }

    private TextView label(String text) {
        TextView label = Ui.text(this, text, 14, Ui.TEXT, true);
        label.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
        return label;
    }

    private EditText input(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(15);
        field.setTextColor(Ui.TEXT);
        field.setHintTextColor(Ui.MUTED);
        field.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        field.setBackground(Ui.outlined(Ui.SURFACE, Color.rgb(205, 216, 210), 14, this));
        field.setTextDirection(View.TEXT_DIRECTION_RTL);
        return field;
    }

    private Spinner spinner(List<String> values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter(values));
        spinner.setBackground(Ui.outlined(Ui.SURFACE, Color.rgb(205, 216, 210), 14, this));
        spinner.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        return spinner;
    }

    private ArrayAdapter<String> adapter(List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private RadioButton radio(String text, boolean checked) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTextColor(Ui.TEXT);
        button.setTextSize(14);
        button.setChecked(checked);
        return button;
    }

    private Button actionButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : Ui.BRAND);
        button.setBackground(Ui.rounded(primary ? Ui.BRAND : Ui.BRAND_SOFT, 16, this));
        Ui.pressAnimation(button);
        return button;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        params.setMargins(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        return params;
    }

    private void showActivationScreen() {
        showActivationScreen(null, null, null);
    }

    private void showActivationScreen(String preparedRequest, String preparedSecurityLabel, String preparationFailure) {
        activationScreenVisible = true;
        if (db != null) { db.close(); db = null; }
        lockScreenVisible = false; authenticated = false; addScreenVisible = false; subScreenVisible = false;
        ScrollView activationScroll = new ScrollView(this); activationScroll.setFillViewport(true);
        LinearLayout page = Ui.column(this); page.setGravity(Gravity.CENTER);
        page.setPadding(Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24)); page.setBackgroundColor(Ui.BACKGROUND);
        activationScroll.addView(page, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageView mark = new ImageView(this); mark.setImageResource(R.drawable.ic_launcher_final); mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int markSize = Ui.iconDp(this, 86); page.addView(mark, new LinearLayout.LayoutParams(markSize, markSize));
        TextView title = Ui.text(this, "تفعيل وين صرفت؟", 24, Ui.TEXT, true); title.setGravity(Gravity.CENTER); page.addView(title);
        if (!LicenseManager.integrityValid(this)) {
            title.setText("تعذر التحقق من نسخة التطبيق");
            TextView warning = Ui.text(this, "توقيع التطبيق غير معتمد أو تم تعديل ملف APK. ثبّت النسخة الأصلية الموقعة من Amro.", 14, Ui.EXPENSE, true);
            warning.setGravity(Gravity.CENTER); warning.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12)); page.addView(warning);
            page.addView(brandingFooter()); setContentView(activationScroll); Ui.animateScreen(page); return;
        }
        String label = preparedSecurityLabel != null ? preparedSecurityLabel : LicenseManager.identitySecurityLabel(this);
        TextView explanation = Ui.text(this, "هذا التفعيل مرتبط بمفتاح خاص داخل هذا التثبيت (" + label + "). أرسل رمز الطلب إلى Amro ثم أدخل شفرة التفعيل.", 13, Ui.MUTED, false);
        explanation.setGravity(Gravity.CENTER); explanation.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 14)); page.addView(explanation);
        String request = preparedRequest;
        try { if (request == null && preparationFailure == null) request = LicenseManager.requestCode(this); }
        catch (Exception error) {
            preparationFailure = error.getClass().getSimpleName();
        }
        if (request == null) {
            String code = preparationFailure == null ? "IDENTITY" : preparationFailure;
            TextView failure = Ui.text(this, "تعذر إنشاء هوية التثبيت. أعد تشغيل الجهاز وحاول مجددًا. رمز الخطأ: " + code, 14, Ui.EXPENSE, true);
            failure.setGravity(Gravity.CENTER); page.addView(failure); page.addView(brandingFooter()); setContentView(activationScroll); return;
        }
        final String requestCode = request;
        TextView requestView = Ui.text(this, request, 11, Ui.BRAND_DARK, true); requestView.setGravity(Gravity.CENTER); requestView.setTextDirection(View.TEXT_DIRECTION_LTR); requestView.setTextIsSelectable(true);
        requestView.setBackground(Ui.rounded(Ui.BRAND_SOFT, 16, this)); requestView.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        page.addView(requestView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 104)));
        LinearLayout requestActions = Ui.row(this); Button copy = actionButton("نسخ رمز الطلب", false); Button share = actionButton("إرسال إلى Amro", true);
        copy.setOnClickListener(v -> { ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Wen Sarafet request", requestCode)); Toast.makeText(this, "تم نسخ رمز الطلب", Toast.LENGTH_SHORT).show(); });
        share.setOnClickListener(v -> startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "رمز تفعيل وين صرفت؟\n" + requestCode), "إرسال رمز الطلب")));
        requestActions.addView(copy, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1)); LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1); shareParams.setMargins(Ui.dp(this, 8), 0, 0, 0); requestActions.addView(share, shareParams); page.addView(requestActions);
        EditText activation = input("الصق شفرة التفعيل هنا"); activation.setTextDirection(View.TEXT_DIRECTION_LTR); activation.setGravity(Gravity.CENTER); page.addView(activation, fieldParams());
        Button activate = actionButton("تفعيل هذا الجهاز", true); activate.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 17, this));
        activate.setOnClickListener(v -> {
            if (!LicenseManager.activate(this, activation.getText().toString())) { activation.setError("شفرة غير صحيحة أو تخص جهازًا آخر"); return; }
            Toast.makeText(this, "تم تفعيل الجهاز بنجاح", Toast.LENGTH_LONG).show(); initializeApp();
        });
        page.addView(activate, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));
        TextView reactivation = Ui.text(this, "بعد تحديث التطبيق يبقى التفعيل. بعد حذف التطبيق أو تغيير الهاتف تتغير هوية التثبيت وستحتاج إرسال رمز طلب جديد لإعادة التفعيل.", 11, Ui.MUTED, false);
        reactivation.setGravity(Gravity.CENTER); reactivation.setPadding(0, Ui.dp(this, 10), 0, 0); page.addView(reactivation);
        page.addView(brandingFooter()); setContentView(activationScroll); Ui.animateScreen(page);
    }

    private void showLockScreen() {
        authenticated = false;
        lockScreenVisible = true;
        addScreenVisible = false; subScreenVisible = false;
        LinearLayout page = Ui.column(this); page.setGravity(Gravity.CENTER); page.setPadding(Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28));
        page.setBackground(Ui.layeredBackground(this, 0));
        TextView mark = Ui.text(this, "◈", 48, Color.WHITE, true); mark.setGravity(Gravity.CENTER); page.addView(mark, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)));
        TextView title = Ui.text(this, "وين صرفت؟", 28, Color.WHITE, true); title.setGravity(Gravity.CENTER); page.addView(title);
        TextView hint = Ui.text(this, "أدخل رمز التطبيق لعرض حساباتك", 14, Color.rgb(225, 234, 244), false); hint.setGravity(Gravity.CENTER); hint.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 22)); page.addView(hint);
        EditText code = new EditText(this); code.setHint("رمز الدخول"); code.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); code.setGravity(Gravity.CENTER);
        code.setTextColor(Ui.TEXT); code.setHintTextColor(Ui.MUTED); code.setBackground(Ui.rounded(Ui.SURFACE, 18, this));
        page.addView(code, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));
        Button unlock = actionButton("فتح التطبيق", true); unlock.setBackground(Ui.gradient(Ui.BRAND, Ui.SKY, 17, this));
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)); up.topMargin = Ui.dp(this, 12); page.addView(unlock, up);
        unlock.setOnClickListener(v -> {
            if (AppLockManager.verify(this, code.getText().toString())) { authenticated = true; openInitialScreen(); }
            else { code.setError("رمز غير صحيح"); code.setText(""); }
        });
        if (AppLockManager.biometricEnabled(this) && biometricAvailable()) {
            Button biometric = actionButton("استخدام البصمة", false); biometric.setTextColor(Color.WHITE); biometric.setBackground(Ui.rounded(Color.argb(40, 255, 255, 255), 17, this));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)); bp.topMargin = Ui.dp(this, 9); page.addView(biometric, bp);
            biometric.setOnClickListener(v -> authenticateBiometric());
            page.postDelayed(this::authenticateBiometric, 260);
        }
        page.addView(brandingFooter());
        setContentView(page); Ui.animateScreen(page);
    }

    private boolean biometricAvailable() {
        try { BiometricManager manager = getSystemService(BiometricManager.class); return manager != null && manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS; }
        catch (Exception ignored) { return false; }
    }

    private void authenticateBiometric() {
        if (biometricPromptVisible || authenticated || !lockScreenVisible || !AppLockManager.biometricEnabled(this) || !biometricAvailable()) return;
        biometricPromptVisible = true;
        BiometricPrompt prompt = new BiometricPrompt.Builder(this).setTitle("فتح وين صرفت؟").setSubtitle("تحقق بالبصمة")
                .setNegativeButton("استخدام الرمز", getMainExecutor(), (d, which) -> {
                    biometricPromptVisible = false;
                    authenticated = false;
                }).build();
        prompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                biometricPromptVisible = false;
                authenticated = true;
                lockScreenVisible = false;
                openInitialScreen();
            }
            @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                biometricPromptVisible = false;
                authenticated = false;
                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED)
                    Toast.makeText(MainActivity.this, errString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View brandingFooter() {
        LinearLayout footer = Ui.column(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(Ui.dp(this, 8), Ui.dp(this, 22), Ui.dp(this, 8), Ui.dp(this, 12));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher_final);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int footerIconSize = Ui.iconDp(this, 42);
        footer.addView(icon, new LinearLayout.LayoutParams(footerIconSize, footerIconSize));
        TextView credit = Ui.text(this, "Powered by Amro Alhamedi", 12, lockScreenVisible ? Color.WHITE : Ui.MUTED, true);
        credit.setGravity(Gravity.CENTER);
        footer.addView(credit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28)));
        if (!lockScreenVisible) {
            TextView email = Ui.text(this, "amroalhamedi195@gmail.com", 11, Ui.BRAND, true);
            email.setGravity(Gravity.CENTER); email.setTextDirection(View.TEXT_DIRECTION_LTR);
            email.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:amroalhamedi195@gmail.com"))); }
                catch (Exception error) { Toast.makeText(this, "لا يوجد تطبيق بريد متاح", Toast.LENGTH_SHORT).show(); }
            });
            footer.addView(email, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28)));
        }
        TextView version = Ui.text(this, "وين صرفت؟ • " + versionName(), 10,
                lockScreenVisible ? Color.argb(190, 255, 255, 255) : Ui.MUTED, false);
        version.setGravity(Gravity.CENTER);
        footer.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));
        return footer;
    }

    private String versionName() {
        try { return "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception ignored) { return ""; }
    }

    private static final class SimpleItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;
        private SimpleItemSelected(Runnable callback) { this.callback = callback; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { callback.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
