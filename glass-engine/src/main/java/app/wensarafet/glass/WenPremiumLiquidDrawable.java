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
            // Royal obsidian with warm metallic amber.
            {0xFF0B0C0D,0xFF1D1612,0xFF13110F, 0xFF24211E,0xFF40352A,
             0xFF302217,0xFFB28B4E, 0xFF241C17,0xFF493322,
             0xFF603D1C,0xFFE1AB50,0xFFFFE29A},
            // Imperial dark graphite, champagne-gold reflections.
            {0xFF0C1115,0xFF292622,0xFF181B20, 0xFF272B30,0xFF3E3832,
             0xFF312820,0xFFBE995B, 0xFF262525,0xFF534033,
             0xFF644823,0xFFD0A463,0xFFFFE4A7},
            // Champagne pearlescent ivory.
            {0xFFEAD9CB,0xFFE4C6B3,0xFFF4E6DA, 0xFFF9EAD9,0xFFFFF8EB,
             0xFFF2D5BB,0xFFFFF8DD, 0xFFF5E6D4,0xFFFFF6E9,
             0xFFC68A3C,0xFFF4C777,0xFFFFF1C2},
            // Bright amber pools suspended in deep green glass.
            {0xFF164A37,0xFF36794F,0xFF105843, 0xFF254A3B,0xFF6A5941,
             0xFF365F49,0xFFAB8653, 0xFF285240,0xFF655C45,
             0xFF286447,0xFFD1884B,0xFFFFDCA1},
            // Vibrant violet, magenta and electric blue.
            {0xFF300B6C,0xFF6E1487,0xFF210879, 0xFF532884,0xFF762DA6,
             0xFF5E2790,0xFFB54ABB, 0xFF44267F,0xFF7C39AD,
             0xFF7122AA,0xFFD83BA3,0xFFFFB3EA}
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
    private final Paint caustic = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimGlint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final WenPremiumCaustics ownerOptics = new WenPremiumCaustics();
    private final WenPremiumSunlight sunField;
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
        // The full-screen AGSL sunlight pass was duplicated over two cached
        // bloom gradients in the gold themes. Their static gradient art stays
        // intact while avoiding the extra fullscreen fragment shader cost.
        sunField = kind == WenReferenceGlassDrawable.Surface.BACKDROP
                && !(theme >= ROYAL_DARK && theme <= CHAMPAGNE)
                ? new WenPremiumSunlight(lowPower) : null;
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
        // Transparent glass, not a flat tinted plastic card. Gold stats stay
        // slightly denser to protect white text; champagne keeps dark ink.
        final int cardAlpha = index == CHAMPAGNE ? 138
                : index == IMPERIAL_DARK ? 122 : index == ROYAL_DARK ? 117
                : index == ORANGE_GREEN ? 165 : 168;
        final int headerAlpha = index == CHAMPAGNE ? 136
                : index == ORANGE_GREEN || index == PURPLE_PINK ? 191 : 128;
        final int navAlpha = index == CHAMPAGNE ? 116 : 98;
        switch (surface) {
            case BACKDROP:
                return new int[]{p[0], blend(p[0], p[1], .66f), p[1],
                        blend(p[2], p[1], .12f), p[2]};
            case HEADER:
                return new int[]{alpha(p[5],headerAlpha),
                        alpha(blend(p[5],p[6],index==CHAMPAGNE?.42f:.20f),headerAlpha+6),
                        alpha(blend(p[5],p[6],index==CHAMPAGNE?.57f:.31f),headerAlpha+3),
                        alpha(p[5],headerAlpha+2),alpha(p[5],headerAlpha)};
            case NAVIGATION:
                return new int[]{alpha(p[7],navAlpha),alpha(p[8],navAlpha+12),
                        alpha(blend(p[7],p[11],.15f),navAlpha+16),
                        alpha(p[8],navAlpha+9),alpha(p[7],navAlpha)};
            case FAB:
                return new int[]{p[9], blend(p[9], p[10], .67f),
                        blend(p[10], p[11], .77f), p[10], p[9]};
            case ACCENT:
                return new int[]{alpha(p[9], 244),
                        alpha(blend(p[9],p[10],.34f), 238),
                        alpha(blend(p[9],p[10],.47f), 234),
                        alpha(blend(p[9],p[10],.20f), 240), alpha(p[9],244)};
            default:
                return new int[]{alpha(p[3],cardAlpha),
                        alpha(blend(p[3],p[4],.26f),cardAlpha+10),
                        alpha(p[4],cardAlpha+7),alpha(p[3],cardAlpha+3),
                        alpha(p[3],cardAlpha)};
        }
    }

    @Override protected void onBoundsChange(Rect r) {
        super.onBoundsChange(r);
        box.set(r);
        silhouette.reset();
        if (r.isEmpty()) return;
        float w=box.width(), h=box.height();
        radius=backdrop()?0f:chrome()?h*.5f:Math.min(32f*d,Math.min(w,h)*.35f);
        silhouette.addRoundRect(box,radius,radius,Path.Direction.CW);
        base.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,
                stops(),new float[]{0f,.24f,.50f,.77f,1f},Shader.TileMode.CLAMP));
        // A distinct art director controls the position, falloff, hue,
        // translucency and specular orientation of EACH theme independently.
        WenThemeArtDirection.configure(index, box, backdrop(), chrome(), lowPower,
                bloomA, bloomB, caustic, reflection);
        ownerOptics.configure(box, index, surface, lowPower, d);
        if (sunField != null) sunField.configure(box, index);
        float padding=Math.max(.8f,d);
        inside.set(box.left+padding,box.top+padding,box.right-padding,box.bottom-padding);
        rim.setStrokeWidth(Math.max(.8f,chrome()?1.45f*d:1.0f*d));
        bevel.setStrokeWidth(Math.max(.6f,.65f*d));
        int[] p = PALETTES[index-ROYAL_DARK];
        rim.setColor(backdrop()?Color.TRANSPARENT:alpha(
                dark && index<=IMPERIAL_DARK ? p[11] : Color.WHITE,
                dark ? 191 : 218));
        bevel.setColor(backdrop()?Color.TRANSPARENT:alpha(
                dark && index<=IMPERIAL_DARK ? p[10] : Color.WHITE,
                dark ? 89 : 128));
        if (!backdrop()) {
            // Metallic highlights belong to the curved EDGE, not broad
            // diagonal plastic ribbons over balances and Arabic text.
            rim.setShader(new LinearGradient(box.left,box.top,
                    box.right,box.top+h*.57f,
                    new int[]{alpha(p[11],52),alpha(p[11],233),
                            alpha(p[10],87),alpha(p[11],215),alpha(p[11],49)},
                    new float[]{0f,.20f,.49f,.81f,1f},Shader.TileMode.CLAMP));
            float hotX=box.left+w*(index==IMPERIAL_DARK?.47f
                    : index==CHAMPAGNE?.28f:index==ORANGE_GREEN?.72f
                    : index==PURPLE_PINK?.86f:.78f);
            float hotY=box.bottom-h*.035f;
            rimGlint.setShader(new RadialGradient(hotX,hotY,
                    Math.max(1f,Math.min(w,h)*.87f),
                    new int[]{alpha(p[11],index==CHAMPAGNE?135:115),
                            alpha(p[10],22),alpha(p[11],0)},
                    new float[]{0f,.29f,1f},Shader.TileMode.CLAMP));
        } else {
            rim.setShader(null);
            rimGlint.setShader(null);
        }
    }

    @Override public void draw(Canvas canvas) {
        if (box.isEmpty()) return;
        int saved=canvas.save();
        canvas.clipPath(silhouette);
        canvas.drawRoundRect(box,radius,radius,base);
        if (!lowPower) {
            canvas.drawRect(box,bloomA);
            canvas.drawRect(box,bloomB);
            canvas.drawRect(box,caustic);
            if (!backdrop()) canvas.drawRect(box,reflection);
            // Each of the five scenes has its own refractive, broad sunlight
            // curves; rendered from cached Paths/Shaders without per-frame work.
            ownerOptics.draw(canvas);
            if (sunField != null) sunField.draw(canvas, box);
            if (!backdrop()) canvas.drawRect(box,rimGlint);
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
        caustic.setAlpha(opacity);
        rimGlint.setAlpha(opacity);
        rim.setAlpha(opacity);
        bevel.setAlpha(opacity);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        base.setColorFilter(filter);
        bloomA.setColorFilter(filter);
        bloomB.setColorFilter(filter);
        reflection.setColorFilter(filter);
        caustic.setColorFilter(filter);
        rimGlint.setColorFilter(filter);
        rim.setColorFilter(filter);
        bevel.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
