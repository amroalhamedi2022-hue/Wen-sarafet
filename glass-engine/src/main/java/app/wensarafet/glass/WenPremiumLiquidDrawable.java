package app.wensarafet.glass;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

/**
 * Append-only premium glass palettes 25-29. This class paints pixels only:
 * no financial data, callbacks, interaction, state, backdrop capture or timers.
 * Draw is allocation-free, shaders are cached, low-power modes draw the base.
 */
public final class WenPremiumLiquidDrawable extends Drawable {
    public static final int ROYAL_DARK = 25;
    public static final int IMPERIAL_DARK = 26;
    public static final int CHAMPAGNE = 27;
    public static final int ORANGE_GREEN = 28;
    public static final int PURPLE_PINK = 29;

    // backdrop x3, card x2, header x2, navigation x2, FAB x2, specular shine.
    private static final int[][] PALETTES = {
            {0xFF0C1119,0xFF282319,0xFF111924, 0xFF242A32,0xFF454037,
             0xFF36332E,0xFFB69558, 0xFF212830,0xFF584A38,
             0xFF755B36,0xFFEAC583,0xFFFFEAC0},
            {0xFF10151F,0xFF2B2A32,0xFF121925, 0xFF252D3A,0xFF45454B,
             0xFF2C3543,0xFFADA18A, 0xFF222C3A,0xFF58534E,
             0xFF6F645B,0xFFE1CCAE,0xFFFFE7CB},
            {0xFFF8F6F0,0xFFF4E7D4,0xFFF8F8F2, 0xFFFFFFFF,0xFFFFF3E6,
             0xFFE9D8B4,0xFFFFF5DE, 0xFFFBF7EE,0xFFF3E6D5,
             0xFFC69E6A,0xFFF7DFAC,0xFFFFFFFF},
            {0xFFFFF5EF,0xFFFBE2D2,0xFFE4F1E5, 0xFFFFFFFF,0xFFF1F8F1,
             0xFFF2B58A,0xFFD0E7CF, 0xFFFFF5E9,0xFFEDF7ED,
             0xFFCC8D67,0xFFA2CCAA,0xFFFFF2D9},
            {0xFFF9F2FC,0xFFF0DAF1,0xFFE9E9FA, 0xFFFFFFFF,0xFFF9F0FC,
             0xFFD1ABE7,0xFFF1C5E7, 0xFFFCF2FE,0xFFF0EAFB,
             0xFF966FB5,0xFFF3BCE5,0xFFFFFFFF}
    };

    public static boolean supports(int index) {
        return index >= ROYAL_DARK && index <= PURPLE_PINK;
    }

    private final int index;
    private final WenReferenceGlassDrawable.Surface surface;
    private final boolean dark;
    private final boolean lowPower;
    private final float d;
    private final RectF box = new RectF(), inside = new RectF();
    private final Path silhouette = new Path();
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float radius;

    public WenPremiumLiquidDrawable(Context context, int theme,
            WenReferenceGlassDrawable.Surface kind, boolean darkMode) {
        if (!supports(theme)) throw new IllegalArgumentException("Unsupported visual palette");
        index = theme;
        surface = kind;
        dark = darkMode;
        d = context.getResources().getDisplayMetrics().density;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        lowPower = (power != null && power.isPowerSaveMode())
                || (manager != null && manager.isLowRamDevice());
        rim.setStyle(Paint.Style.STROKE);
        bevel.setStyle(Paint.Style.STROKE);
    }

    private static int blend(int a, int b, float t) {
        return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),
                Math.round(Color.green(a)*(1-t)+Color.green(b)*t),
                Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }

    private static int alpha(int c, int a) {
        return Color.argb(a,Color.red(c),Color.green(c),Color.blue(c));
    }

    private boolean backdrop() {
        return surface == WenReferenceGlassDrawable.Surface.BACKDROP;
    }
    private boolean chrome() {
        return surface == WenReferenceGlassDrawable.Surface.FAB
                || surface == WenReferenceGlassDrawable.Surface.NAVIGATION;
    }

    private int[] stops() {
        int[] p = PALETTES[index - ROYAL_DARK];
        switch (surface) {
            case BACKDROP: return new int[]{p[0],blend(p[0],p[1],.65f),p[1],p[2],p[0]};
            case HEADER: return new int[]{p[5],blend(p[5],p[6],.58f),p[6],p[5],blend(p[5],p[3],.32f)};
            case NAVIGATION: return new int[]{alpha(p[7],234),alpha(p[8],239),
                    alpha(blend(p[7],p[11],.23f),242),alpha(p[8],237),alpha(p[7],231)};
            case FAB: return new int[]{p[9],blend(p[9],p[10],.55f),
                    blend(p[10],p[11],.65f),p[10],p[9]};
            case ACCENT: return new int[]{p[9],p[10],p[11],p[10],p[9]};
            default: return new int[]{alpha(p[3],249),alpha(blend(p[3],p[4],.36f),250),
                    alpha(p[4],251),alpha(p[3],249),alpha(p[4],248)};
        }
    }

    @Override protected void onBoundsChange(Rect r) {
        super.onBoundsChange(r);
        box.set(r);
        silhouette.reset();
        if (r.isEmpty()) return;
        float w=box.width(), h=box.height();
        radius=backdrop()?0f:chrome()?h*.5f:Math.min(28f*d,Math.min(w,h)*.30f);
        silhouette.addRoundRect(box,radius,radius,Path.Direction.CW);
        base.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,
                stops(),new float[]{0f,.24f,.50f,.77f,1f},Shader.TileMode.CLAMP));
        if (!lowPower) {
            float span=Math.max(1f,Math.max(w,h)*(backdrop()?.71f:.92f));
            float glow=backdrop()?.25f:chrome()?.27f:.14f;
            int[] p=PALETTES[index-ROYAL_DARK];
            bloomA.setShader(new RadialGradient(box.left+w*.12f,box.top+h*.12f,span,
                    new int[]{alpha(p[11],Math.round(185f*glow)),
                    alpha(p[11],0)},null,Shader.TileMode.CLAMP));
            bloomB.setShader(new RadialGradient(box.left+w*.84f,box.top+h*.86f,span,
                    new int[]{alpha(p[10],Math.round(135f*glow)),
                    alpha(p[10],0)},null,Shader.TileMode.CLAMP));
            reflection.setShader(new LinearGradient(box.left+w*.1f,box.top,
                    box.right-w*.1f,box.top+h*.72f,
                    new int[]{0x49FFFFFF,0x24FFFFFF,0x00FFFFFF,0x0FFFFFFF},
                    new float[]{0f,.16f,.65f,1f},Shader.TileMode.CLAMP));
        } else {
            bloomA.setShader(null);
            bloomB.setShader(null);
            reflection.setShader(null);
        }
        float padding=Math.max(.8f,d);
        inside.set(box.left+padding,box.top+padding,box.right-padding,box.bottom-padding);
        rim.setStrokeWidth(Math.max(.8f,chrome()?1.45f*d:1.0f*d));
        bevel.setStrokeWidth(Math.max(.6f,.65f*d));
        rim.setColor(backdrop()?Color.TRANSPARENT:dark?0xB9E9D7AD:0xD9FFFFFF);
        bevel.setColor(backdrop()?Color.TRANSPARENT:dark?0x77FFFFFF:0xA8FFFFFF);
    }

    @Override public void draw(Canvas canvas) {
        if (box.isEmpty()) return;
        int saved=canvas.save();
        canvas.clipPath(silhouette);
        canvas.drawRoundRect(box,radius,radius,base);
        if (!lowPower) {
            canvas.drawRect(box,bloomA);
            canvas.drawRect(box,bloomB);
            if (!backdrop()) canvas.drawRect(box,reflection);
        }
        canvas.restoreToCount(saved);
        if (!backdrop()) {
            float innerRadius=Math.max(0f,radius-d);
            canvas.drawRoundRect(inside,innerRadius,innerRadius,rim);
            canvas.drawRoundRect(inside,innerRadius,innerRadius,bevel);
        }
    }

    @Override public void setAlpha(int a) {
        int opacity=Math.max(0,Math.min(255,a));
        base.setAlpha(opacity);
        bloomA.setAlpha(opacity);
        bloomB.setAlpha(opacity);
        reflection.setAlpha(opacity);
        rim.setAlpha(opacity);
        bevel.setAlpha(opacity);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        base.setColorFilter(filter);
        bloomA.setColorFilter(filter);
        bloomB.setColorFilter(filter);
        reflection.setColorFilter(filter);
        rim.setColorFilter(filter);
        bevel.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
