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
    // Theme-free neutral glass: original constructor signature preserved.
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
        // The add button is NEUTRAL liquid glass across EVERY theme. It
        // borrows the brightness of its surroundings, never the palette hue.
        // Only white/grey transmittance, optical depth and specular geometry
        // belong to this passive drawable; hitbox and press logic stay intact.
        fill.setShader(new LinearGradient(circle.left,circle.top,
                circle.right,circle.bottom,
                dark
                    ? new int[]{0x4CFFFFFF,0x1FFFFFFF,0x38FFFFFF}
                    : new int[]{0x94FFFFFF,0x38FFFFFF,0x75FFFFFF},
                new float[]{0f,.50f,1f},Shader.TileMode.CLAMP));
        opticalDepth.setShader(new RadialGradient(
                circle.centerX()-circle.width()*.24f,
                circle.centerY()-circle.height()*.28f,
                Math.max(1f,circle.width()*.78f),
                dark
                    ? new int[]{0x65FFFFFF,0x10FFFFFF,0x36000000}
                    : new int[]{0x8FFFFFFF,0x0AFFFFFF,0x22909090},
                new float[]{0f,.43f,1f},Shader.TileMode.CLAMP));
        rim.setColor(dark ? 0xCCFFFFFF : 0xE8FFFFFF);
        halo.setColor(dark ? 0x55FFFFFF : 0x64FFFFFF);
        inner.setColor(dark ? 0x75FFFFFF : 0xB0FFFFFF);
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