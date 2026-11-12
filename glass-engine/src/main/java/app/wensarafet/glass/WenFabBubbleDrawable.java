package app.wensarafet.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Lightweight live-through FAB lens. Its transparent center is composited
 * directly over the CURRENT pixels behind the button on every frame.
 * No stored backdrop, frozen image, blur layer, internal glare or gradient.
 * The optically soft ~1dp edge is anti-aliased; not a 5dp Gaussian blur.
 * The original press/getPress contract remains unchanged.
 */
public final class WenFabBubbleDrawable extends Drawable {
    private final Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final float glassOpacity;
    private final float edgeOpacity;
    private float cx,cy,radius;
    private float press;
    private int alpha=255;

    public WenFabBubbleDrawable(Context context, boolean dark) {
        density=context.getResources().getDisplayMetrics().density;
        // Low-opacity neutral tint: the underlying text/cards remain LIVE.
        // Never baked into a screenshot, and no colored theme paint is added.
        glass.setColor(dark?0xFFF0F5F8:0xFFFFFFFF);
        edge.setColor(dark?0xFFE2EDF3:0xFFFFFFFF);
        glassOpacity=dark?.17f:.15f;
        edgeOpacity=dark?.46f:.52f;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(1f,.85f*density));
    }

    @Override protected void onBoundsChange(Rect b) {
        cx=b.exactCenterX();
        cy=b.exactCenterY();
        radius=Math.max(0f,Math.min(b.width(),b.height())*.5f-1.2f*density);
    }

    public float getPress(){return press;}
    public void setPress(float fraction) {
        float next=Math.max(0f,Math.min(1f,fraction));
        if(Math.abs(next-press)<.005f)return;
        press=next;invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if(radius<=0f)return;
        // Press changes alpha uniformly. No localized white halo or lens bump.
        glass.setAlpha(Math.round(alpha*glassOpacity*(1f-.05f*press)));
        edge.setAlpha(Math.round(alpha*edgeOpacity*(.96f+.04f*press)));
        canvas.drawCircle(cx,cy,radius,glass);
        canvas.drawCircle(cx,cy,radius,edge);
    }
    @Override public void setAlpha(int value) {
        alpha=Math.max(0,Math.min(255,value));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {
        glass.setColorFilter(filter);
        edge.setColorFilter(filter);
        invalidateSelf();
    }
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
