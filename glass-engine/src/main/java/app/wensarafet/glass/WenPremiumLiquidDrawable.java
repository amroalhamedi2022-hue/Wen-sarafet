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
            {0xFF100C0A,0xFF2D1D11,0xFF140E0B, 0xFF28221C,0xFF4A3928,
             0xFF302217,0xFFB28B4E, 0xFF241C17,0xFF493322,
             0xFF603D1C,0xFFE1AB50,0xFFFFE29A},
            // Imperial dark graphite, champagne-gold reflections.
            {0xFF101415,0xFF33241C,0xFF16191B, 0xFF292725,0xFF4B3B31,
             0xFF312820,0xFFBE995B, 0xFF262525,0xFF534033,
             0xFF644823,0xFFD0A463,0xFFFFE4A7},
            // Champagne pearlescent ivory.
            {0xFFF4E7DA,0xFFE9CAB1,0xFFFDF3E8, 0xFFF9EAD9,0xFFFFF8EB,
             0xFFF2D5BB,0xFFFFF8DD, 0xFFF5E6D4,0xFFFFF6E9,
             0xFFC68A3C,0xFFF4C777,0xFFFFF1C2},
            // Bright amber pools suspended in deep green glass.
            {0xFF0E3529,0xFF386948,0xFF103E34, 0xFF254A3B,0xFF6A5941,
             0xFF365F49,0xFFAB8653, 0xFF285240,0xFF655C45,
             0xFF286447,0xFFD1884B,0xFFFFDCA1},
            // Vibrant violet, magenta and electric blue.
            {0xFF3C0E76,0xFF891B8D,0xFF26108B, 0xFF532884,0xFF762DA6,
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
    private final Paint ribbonGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ribbonEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ribbon = new Path();
    private final Path secondRibbon = new Path();
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
        ribbonGlow.setStyle(Paint.Style.STROKE);
        ribbonEdge.setStyle(Paint.Style.STROKE);
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
            case BACKDROP:
                return new int[]{p[0], blend(p[0], p[1], .66f), p[1],
                        blend(p[2], p[1], .12f), p[2]};
            case HEADER:
                return new int[]{alpha(p[5], 226),
                        alpha(blend(p[5], p[6], index==CHAMPAGNE?.42f:.20f), 220),
                        alpha(blend(p[5], p[6], index==CHAMPAGNE?.57f:.31f), 217),
                        alpha(p[5], 227), alpha(p[5], 222)};
            case NAVIGATION:
                return new int[]{alpha(p[7], 220), alpha(p[8], 226),
                        alpha(blend(p[7], p[11], .19f), 225),
                        alpha(p[8], 225), alpha(p[7], 220)};
            case FAB:
                return new int[]{p[9], blend(p[9], p[10], .67f),
                        blend(p[10], p[11], .77f), p[10], p[9]};
            case ACCENT:
                return new int[]{alpha(p[9], 244),
                        alpha(blend(p[9],p[10],.34f), 238),
                        alpha(blend(p[9],p[10],.47f), 234),
                        alpha(blend(p[9],p[10],.20f), 240), alpha(p[9],244)};
            default:
                return new int[]{alpha(p[3], 228), alpha(blend(p[3], p[4], .35f), 232),
                        alpha(p[4], 230), alpha(p[3], 228), alpha(p[3], 225)};
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
        ribbon.reset();
        secondRibbon.reset();
        ribbon.moveTo(box.left-w*.12f, box.top+h*(backdrop()?.30f:.90f));
        ribbon.cubicTo(box.left+w*.22f, box.top+h*.04f,
                box.left+w*.36f, box.top+h*.86f,
                box.left+w*.66f, box.top+h*.40f);
        ribbon.cubicTo(box.left+w*.88f, box.top+h*.18f,
                box.right+w*.04f, box.top+h*.72f,
                box.right+w*.16f, box.top+h*.46f);
        secondRibbon.moveTo(box.left-w*.10f, box.top+h*.80f);
        secondRibbon.cubicTo(box.left+w*.32f, box.top+h*.58f,
                box.left+w*.56f, box.top+h*1.14f,
                box.right+w*.15f, box.top+h*.62f);
        if (!lowPower) {
            int[] p = PALETTES[index-ROYAL_DARK];
            float span = Math.max(1f, backdrop() ? Math.min(w,h)*.84f
                    : Math.max(w,h)*(chrome()?.62f:.73f));
            int strength = backdrop()
                    ? (index==ORANGE_GREEN?178:index==PURPLE_PINK?162
                    : index==CHAMPAGNE?124:138)
                    : chrome()?115:72;
            bloomA.setShader(new RadialGradient(box.left+w*.14f,box.top+h*.19f,
                    span,new int[]{alpha(p[11],strength),
                    alpha(p[10],Math.round(strength*.50f)),alpha(p[10],0)},
                    new float[]{0f,.30f,1f},Shader.TileMode.CLAMP));
            bloomB.setShader(new RadialGradient(box.left+w*.83f,box.top+h*.76f,
                    span*.85f,new int[]{alpha(p[10],Math.round(strength*.82f)),
                    alpha(p[10],Math.round(strength*.24f)),alpha(p[10],0)},
                    new float[]{0f,.40f,1f},Shader.TileMode.CLAMP));
            caustic.setShader(new RadialGradient(box.left+w*.70f,box.top+h*.07f,
                    Math.max(1f,Math.max(w,h)*.34f),
                    new int[]{alpha(p[11],backdrop()?116:chrome()?122:74),
                    alpha(p[11],0)},null,Shader.TileMode.CLAMP));
            reflection.setShader(new LinearGradient(box.left,box.top,
                    box.left+w*.65f,box.top+h*.82f,
                    new int[]{0x64FFFFFF,0x12FFFFFF,0x00FFFFFF,
                    0x33FFFFFF,0x00FFFFFF},
                    new float[]{0f,.18f,.44f,.65f,1f},Shader.TileMode.CLAMP));
            ribbonGlow.setStrokeWidth((backdrop()?19f:chrome()?9f:7f)*d);
            ribbonGlow.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,
                    new int[]{alpha(p[10],0),alpha(p[11],backdrop()?105:83),
                    alpha(p[10],35),alpha(p[11],92),alpha(p[11],0)},
                    null,Shader.TileMode.CLAMP));
            ribbonEdge.setStrokeWidth(Math.max(.65f,(backdrop()?1.5f:1f)*d));
            ribbonEdge.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,
                    new int[]{alpha(p[11],0),alpha(p[11],132),
                    alpha(p[11],52),alpha(p[11],158),alpha(p[11],0)},
                    null,Shader.TileMode.CLAMP));
        } else {
            bloomA.setShader(null);
            bloomB.setShader(null);
            caustic.setShader(null);
            reflection.setShader(null);
            ribbonGlow.setShader(null);
            ribbonEdge.setShader(null);
        }

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
            // Do not draw ornamental S-curves across every metric/account.
            // In the references caustic ribbons belong to the backdrop/header;
            // the content stays sharp and legible above the refractive material.
            if (backdrop() || surface==WenReferenceGlassDrawable.Surface.HEADER) {
                canvas.drawPath(ribbon,ribbonGlow);
                canvas.drawPath(ribbon,ribbonEdge);
                canvas.drawPath(secondRibbon,ribbonGlow);
            }
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
        ribbonGlow.setAlpha(opacity);
        ribbonEdge.setAlpha(opacity);
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
        ribbonGlow.setColorFilter(filter);
        ribbonEdge.setColorFilter(filter);
        rim.setColorFilter(filter);
        bevel.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
