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
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF circle = new RectF();
    private final RectF glint = new RectF();
    private float press;
    private int alpha=255;

    public WenFabBubbleDrawable(Context context, int accentColor, boolean darkTheme) {
        density=context.getResources().getDisplayMetrics().density;
        dark=darkTheme;
        accent=accentColor;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(density*1.9f);
        rim.setColor(Color.argb(dark?200:240,255,255,255));
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(density*3.4f);
        halo.setColor(dark?0x80D5D8FF:0xD1F5F7FF);
        inner.setStyle(Paint.Style.STROKE);
        inner.setStrokeWidth(density*.95f);
        inner.setColor(dark?0x88FFFFFF:0xB5FFFFFF);
        highlight.setColor(0xA5FFFFFF);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        float side=Math.min(bounds.width(),bounds.height());
        circle.set(bounds.centerX()-side*.475f,bounds.centerY()-side*.475f,
                bounds.centerX()+side*.475f,bounds.centerY()+side*.475f);
        fill.setShader(new LinearGradient(circle.left,circle.top,circle.right,circle.bottom,
                new int[]{dark?0xD9908EEC:0xF38B9DEB, accent, dark?0xE13C488D:0xEF4759AE},
                new float[]{0f,.45f,1f},Shader.TileMode.CLAMP));
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
        glint.set(mx-radius*.68f,my-radius*.76f,mx+radius*.58f,my-radius*.04f);
        highlight.setAlpha((int)(alpha*(.66f+.15f*press)));
        c.drawOval(glint,highlight);
        rim.setAlpha((int)(alpha*(.88f+.12f*press)));
        c.drawCircle(mx,my,radius-density*.6f,rim);
        c.drawCircle(mx,my,radius-density*4f,inner);
        c.restore();
    }

    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){fill.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}