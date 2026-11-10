package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Tiny, allocation-free-per-frame pearly glass bubble for Add Transaction. */
public final class WenFabBubbleDrawable extends Drawable {
    private final float density;
    private final boolean dark;
    private final int accent;
    private final int premiumTheme;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainbow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint opticalDepth = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caustic = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF reflection = new RectF();
    private final RectF circle = new RectF();
    private final RectF glint = new RectF();
    private float press;
    private int alpha=255;

    public WenFabBubbleDrawable(Context context, int accentColor, boolean darkTheme) {
        this(context, accentColor, darkTheme, -1);
    }

    /** Exact Sandbox optics and geometry, theme-specific spectral materials. */
    public WenFabBubbleDrawable(Context context, int accentColor, boolean darkTheme,
                                int premiumTheme) {
        this.premiumTheme = premiumTheme;
        density=context.getResources().getDisplayMetrics().density;
        dark=darkTheme;
        accent=accentColor;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(density*1.9f);
        rim.setColor(Color.argb(dark?200:240,255,255,255));
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(density*3.4f);
        halo.setColor(dark?0x7AFFFFFF:0xC3FFFFFF);
        inner.setStyle(Paint.Style.STROKE);
        inner.setStrokeWidth(density*.95f);
        inner.setColor(dark?0x88FFFFFF:0xB5FFFFFF);
        highlight.setColor(0xA5FFFFFF);
        rainbow.setStyle(Paint.Style.STROKE);
        rainbow.setStrokeWidth(density * 1.65f);
        caustic.setStyle(Paint.Style.STROKE);
        caustic.setStrokeWidth(density * 2.6f);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        float side=Math.min(bounds.width(),bounds.height());
        circle.set(bounds.centerX()-side*.475f,bounds.centerY()-side*.475f,
                bounds.centerX()+side*.475f,bounds.centerY()+side*.475f);
        // Neutral, light-transmitting body. The old nearly opaque blue gradient
        // looked like a plastic action button instead of a curved glass lens.
        // Five distinct glass bodies: keep the original Sandbox bubble's
        // precise round silhouette, hold morph, rim and highlight geometry.
        // Only the colour/refraction spectrum changes per approved scene.
        if (premiumTheme >= 25 && premiumTheme <= 29) {
            final int[] material;
            final int glow;
            final int depth;
            final int edge;
            switch (premiumTheme) {
                case 25: // Royal Dark Gold
                    material = new int[]{0xE18A5A2B,0xD9FFC66B,0xDF8A5428};
                    glow=0xAEFFE0A1; depth=0x666A401B; edge=0xEEFFE3AE; break;
                case 26: // Imperial Dark Gold
                    material = new int[]{0xD96F5E48,0xDFF5CF8F,0xE07A5C35};
                    glow=0xB3FFE1B8; depth=0x624D402B; edge=0xEEFFEDC7; break;
                case 27: // Champagne Gold
                    material = new int[]{0xEFFFF4DF,0xF2F0C58C,0xE6E9BB7F};
                    glow=0xA9FFFFFF; depth=0x65B78855; edge=0xEEFFFCF2; break;
                case 28: // Orange-Green Glass
                    material = new int[]{0xD579B896,0xE0FFD08A,0xC6579974};
                    glow=0xB3FFF0BC; depth=0x653A704E; edge=0xE4FFFFFF; break;
                default: // Pink-Violet Glass
                    material = new int[]{0xE7AD59DC,0xE9FF7DC7,0xD65A42C4};
                    glow=0xBEFFBDF1; depth=0x644A277B; edge=0xE9FFFFFF; break;
            }
            fill.setShader(new LinearGradient(circle.left,circle.top,
                    circle.right,circle.bottom,material,
                    new float[]{0f,.51f,1f},Shader.TileMode.CLAMP));
            opticalDepth.setShader(new RadialGradient(
                    circle.centerX()-circle.width()*.24f,
                    circle.centerY()-circle.height()*.28f,
                    Math.max(1f,circle.width()*.78f),
                    new int[]{glow,0x28FFFFFF,depth},
                    new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
            rim.setColor(edge);
            halo.setColor(premiumTheme==25||premiumTheme==26||premiumTheme==27
                    ? 0xA8FFDB98 : premiumTheme==28 ? 0xA4FFE2A8 : 0xA8FFD5FB);
            inner.setColor(premiumTheme<=27 ? 0xA4FFEEC3 : 0xB8FFFFFF);
        } else {
        fill.setShader(new LinearGradient(circle.left, circle.top,
                circle.right, circle.bottom,
                new int[]{dark ? 0xC65E638E : 0xD9FFFFFF,
                          dark ? 0x865D76B9 : 0x8BD5EFFF,
                          dark ? 0xBA33427E : 0xB0A4BCEB},
                new float[]{0f, .51f, 1f}, Shader.TileMode.CLAMP));
        opticalDepth.setShader(new RadialGradient(
                circle.centerX() - circle.width() * .22f,
                circle.centerY() - circle.height() * .28f,
                Math.max(1f, circle.width() * .77f),
                new int[]{0xA2FFFFFF, 0x2BFFFFFF,
                        dark ? 0x42233169 : 0x3C5276A9},
                new float[]{0f, .43f, 1f}, Shader.TileMode.CLAMP));
        }
        highlight.setShader(new LinearGradient(circle.left,circle.top,
                circle.left,circle.bottom,
                new int[]{0xDFFFFFFF, 0x46FFFFFF, 0x00FFFFFF},
                new float[]{0f, .38f, 1f}, Shader.TileMode.CLAMP));
        caustic.setShader(new LinearGradient(circle.left, circle.top,
                circle.right, circle.bottom,
                new int[]{0xF3FFFFFF, 0x85FFFFFF, 0xA2FFFFFF,
                        0x96FFFFFF, 0xE8FFFFFF}, null, Shader.TileMode.CLAMP));
        rainbow.setShader(new LinearGradient(circle.left, circle.top,
                circle.right, circle.bottom,
                new int[]{0x8BFFFFFF, 0xB6FFFFFF, 0xA0FFFFFF, 0x92FFFFFF, 0xACFFFFFF},
                null, Shader.TileMode.CLAMP));
    }

    public float getPress() { return press; }
    public void setPress(float fraction) {
        float next=Math.max(0f,Math.min(1f,fraction));
        if(Math.abs(next-press)<.005f)return;
        press=next;
        invalidateSelf();
    }

    @Override public void draw(Canvas c) {
        float mx=circle.centerX(),my=circle.centerY();
        float radius=circle.width()*.5f;
        float expansion=1f+.085f*press;
        c.save();
        c.scale(expansion,expansion,mx,my);
        halo.setAlpha((int)((.22f+.62f*press)*alpha));
        c.drawCircle(mx,my,radius+ density*(1.8f+press*2.8f),halo);
        fill.setAlpha((int)(alpha*(1f-.08f*press)));
        c.drawCircle(mx,my,radius,fill);
        opticalDepth.setAlpha((int)(alpha*(.84f+.16f*press)));
        c.drawCircle(mx,my,radius, opticalDepth);
        glint.set(mx-radius*.68f,my-radius*.76f,mx+radius*.58f,my-radius*.04f);
        highlight.setAlpha((int)(alpha*(.66f+.15f*press)));
        c.drawOval(glint,highlight);
        reflection.set(mx-radius*.85f, my-radius*.85f,
                mx+radius*.85f, my+radius*.85f);
        caustic.setAlpha((int)(alpha*(.70f+.30f*press)));
        c.drawOval(reflection,caustic);
        rim.setAlpha((int)(alpha*(.88f+.12f*press)));
        c.drawCircle(mx,my,radius-density*.6f,rim);
        rainbow.setAlpha((int)(alpha*(.72f+.28f*press)));
        c.drawCircle(mx,my,radius-density*2.1f,rainbow);
        inner.setAlpha((int)(alpha*(.65f+.28f*press)));
        c.drawCircle(mx,my,radius-density*4f,inner);
        c.restore();
    }

    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){fill.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}