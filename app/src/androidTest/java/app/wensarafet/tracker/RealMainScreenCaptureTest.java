package app.wensarafet.tracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.PixelCopy;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Screenshot the REAL protected MainActivity with its ORIGINAL layout, assets,
 * account cards, navigation, stats, headers and FAB. Unlike VisualPreviewActivity
 * this does not fabricate Android views or synthetic copycat screen layouts.
 *
 * Licence is NOT modified or forged: the dedicated .recovered.debug
 * instrumentation test waits for the normal startup check to finish, then
 * temporarily injects only an isolated fresh debug-package DB into this
 * Activity instance and calls existing private screen methods on UI thread.
 * No production login bypass, financial code change or user device access.
 */
@RunWith(AndroidJUnit4.class)
public final class RealMainScreenCaptureTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    private static void set(Object target, String field, Object value) throws Exception {
        Field f=target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target,value);
    }

    private static boolean getBoolean(Object target, String field) throws Exception {
        Field f=target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getBoolean(target);
    }

    private static void invoke(Object target, String method, Class<?>[] types,
                               Object... args) throws Exception {
        Method m=target.getClass().getDeclaredMethod(method,types);
        m.setAccessible(true);
        m.invoke(target,args);
    }

    private static boolean contains(View view, String value) {
        if(view instanceof TextView &&
                ((TextView)view).getText().toString().contains(value))return true;
        if(!(view instanceof ViewGroup))return false;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++)
            if(contains(group.getChildAt(i),value))return true;
        return false;
    }

    private static ScrollView findScroll(View view) {
        if(view instanceof ScrollView)return (ScrollView)view;
        if(!(view instanceof ViewGroup))return null;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++) {
            ScrollView match=findScroll(group.getChildAt(i));
            if(match!=null)return match;
        }
        return null;
    }

    private static void waitForStartup(ActivityScenario<MainActivity> scenario)
            throws Exception {
        boolean resolved=false;
        for(int attempt=0;attempt<70;attempt++) {
            AtomicBoolean ready=new AtomicBoolean();
            scenario.onActivity(activity-> {
                try {ready.set(getBoolean(activity,"startupResolved"));}
                catch(Exception e){throw new AssertionError(e);}
            });
            if(ready.get()){resolved=true;break;}
            SystemClock.sleep(160);
        }
        assertTrue("Standard debug startup must complete before test-only screen reflection",
                resolved);
    }

    private void capture(ActivityScenario<MainActivity> scenario, int theme,
                         String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(600);
        final AtomicReference<MainActivity> active = new AtomicReference<>();
        scenario.onActivity(active::set);
        MainActivity activity=active.get();
        assertNotNull(activity);
        View decor=activity.getWindow().getDecorView();
        int width=decor.getWidth(),height=decor.getHeight();
        assertTrue("MainActivity Window must be laid out",width>=720 && height>=1280);
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        CountDownLatch copyDone=new CountDownLatch(1);
        AtomicInteger result=new AtomicInteger(PixelCopy.ERROR_UNKNOWN);
        // Copy the ACTUAL rendered Android window, not a mock View tree or
        // a generative image. This avoids UiAutomation screenshot service
        // disconnects observed on GitHub's API35 headless emulator.
        PixelCopy.request(activity.getWindow(),bitmap,code-> {
            result.set(code);
            copyDone.countDown();
        },new Handler(Looper.getMainLooper()));
        assertTrue("Android PixelCopy did not finish",copyDone.await(12,TimeUnit.SECONDS));
        assertEquals("Real rendered window copy failed",PixelCopy.SUCCESS,result.get());
        File root=new File(context.getExternalFilesDir(null),"owner-real-screens");
        assertTrue(root.isDirectory()||root.mkdirs());
        File output=new File(root,"real-theme-"+theme+"-"+name+".png");
        try(FileOutputStream out=new FileOutputStream(output)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));
        } finally {bitmap.recycle();}
        assertTrue(output.exists()&&output.length()>100000);
        android.util.Log.i("WenRealPreview","CAPTURED "+output.getAbsolutePath());
    }

    @Test public void actualMainActivityScreensUseOriginalIconsAndSandboxChrome()
            throws Exception {
        assertEquals("Test-only package: never change real app",
                "app.wensarafet.tracker.recovered.debug",context.getPackageName());
        for(int theme=25;theme<=29;theme++) {
            Ui.saveTheme(context,theme);
            try(ActivityScenario<MainActivity> scenario =
                    ActivityScenario.launch(MainActivity.class)) {
                waitForStartup(scenario);
                scenario.onActivity(activity-> {
                    try {
                        FinanceDbHelper db=new FinanceDbHelper(activity);
                        // Existing unchanged source seeds the ORIGINAL Bank of
                        // Palestine, Jawwal Pay, PalPay and cash accounts/icons.
                        assertTrue(db.getAccounts().size()>=7);
                        set(activity,"db",db);
                        set(activity,"authenticated",true);
                        set(activity,"activationScreenVisible",false);
                        set(activity,"startupResolved",true);
                        invoke(activity,"showMain",new Class<?>[]{boolean.class},false);
                        assertTrue(contains(activity.getWindow().getDecorView(),
                                "إجمالي أرصدتك"));
                        assertTrue(contains(activity.getWindow().getDecorView(),
                                "الرئيسية"));
                    } catch(Exception error){throw new AssertionError(error);}
                });
                capture(scenario,theme,"home");

                scenario.onActivity(activity-> {
                    ScrollView scroll=findScroll(activity.getWindow().getDecorView());
                    assertNotNull(scroll);
                    scroll.scrollTo(0,Ui.dp(activity,360));
                });
                capture(scenario,theme,"original-account-icons");

                scenario.onActivity(activity-> {
                    try {
                        invoke(activity,"showAnalyticsScreen",new Class<?>[]{});
                        assertTrue(contains(activity.getWindow().getDecorView(),
                                "التحليلات"));
                    }catch(Exception error){throw new AssertionError(error);}
                });
                capture(scenario,theme,"analytics");

                scenario.onActivity(activity-> {
                    try {
                        invoke(activity,"showSettings",new Class<?>[]{});
                        assertTrue(contains(activity.getWindow().getDecorView(),
                                "الإعدادات"));
                    }catch(Exception error){throw new AssertionError(error);}
                });
                capture(scenario,theme,"settings");
            }
        }
    }
}
