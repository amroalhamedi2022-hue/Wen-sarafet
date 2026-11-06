package app.wensarafet.glass;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Owner reference-specific optical art direction, not interchangeable gradient
 * presets. Three genuinely different GOLD materials (two black / one ivory),
 * plus vivid peach-emerald and fuchsia-violet.
 *
 * A still light field: cached cubic lens folds, refracted gold highlights,
 * spherical glass bubbles and restrained radiating sunbeams. Every shader,
 * Path and Paint is created or updated ONLY when drawable bounds change.
 * No timers, allocations in draw(), RenderEffect churn or finance dependencies.
 */
final class WenPremiumCaustics {
    private final Path meniscus = new Path();
    private final Path secondary = new Path();
    private final Path rayA = new Path();
    private final Path rayB = new Path();
    private final Path rayC = new Path();
    private final Paint meniscusGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meniscusEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pearl = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pearlEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pearlX, pearlY, pearlRadius;
    private float secondaryPearlX, secondaryPearlY, secondaryPearlRadius;
    private boolean enabled, drawRays, showPearl, headerOnly, navigationOnly;

    WenPremiumCaustics() {
        meniscusGlow.setStyle(Paint.Style.STROKE);
        meniscusGlow.setStrokeCap(Paint.Cap.ROUND);
        meniscusGlow.setStrokeJoin(Paint.Join.ROUND);
        meniscusEdge.setStyle(Paint.Style.STROKE);
        meniscusEdge.setStrokeCap(Paint.Cap.ROUND);
        secondaryGlow.setStyle(Paint.Style.STROKE);
        secondaryGlow.setStrokeCap(Paint.Cap.ROUND);
        pearlEdge.setStyle(Paint.Style.STROKE);
        rayPaint.setStyle(Paint.Style.STROKE);
        rayPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private static int withAlpha(int rgb, int alpha) {
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    void configure(RectF box, int theme, WenReferenceGlassDrawable.Surface surface,
                   boolean lowPower, float density) {
        enabled = !lowPower && box.width() > 0 && box.height() > 0;
        meniscus.reset();
        secondary.reset();
        rayA.reset();
        rayB.reset();
        rayC.reset();
        if (!enabled) return;

        final boolean scene = surface == WenReferenceGlassDrawable.Surface.BACKDROP;
        final boolean header = surface == WenReferenceGlassDrawable.Surface.HEADER;
        final boolean navigation = surface == WenReferenceGlassDrawable.Surface.NAVIGATION;
        headerOnly = header;
        navigationOnly = navigation;
        final float l = box.left, t = box.top, w = box.width(), h = box.height();
        final int gold = theme == 25 ? 0xFFFCC86B
                : theme == 26 ? 0xFFFBE2A6
                : theme == 27 ? 0xFFFFEECA
                : theme == 28 ? 0xFFFFD39B : 0xFFFFBBF7;
        final int contrast = theme == 25 ? 0xFFB37129
                : theme == 26 ? 0xFFC89952
                : theme == 27 ? 0xFFE6B475
                : theme == 28 ? 0xFFFFAB66 : 0xFFD486FF;
        final int accent = theme == 25 ? 0xFFF1BC70
                : theme == 26 ? 0xFFF5D9A3
                : theme == 27 ? 0xFFFFFCF1
                : theme == 28 ? 0xFFFFECBB : 0xFFFFA4DE;

        // Light curves are anchored independently. No same-geometry recolours.
        if (scene) {
            switch (theme) {
                case 25: // Royal: upper-left auric light / lower-right reflected crescent.
                    meniscus.moveTo(l-w*.20f,t+h*.20f);
                    meniscus.cubicTo(l+w*.06f,t+h*.035f,l+w*.19f,t+h*.045f,l+w*.40f,t+h*.13f);
                    meniscus.cubicTo(l+w*.60f,t+h*.20f,l+w*.80f,t+h*.14f,l+w*1.12f,t+h*.09f);
                    secondary.moveTo(l+w*.42f,t+h*1.12f);
                    secondary.cubicTo(l+w*.62f,t+h*.82f,l+w*.88f,t+h*.98f,l+w*1.13f,t+h*.70f);
                    pearlX=l+w*.17f; pearlY=t+h*.21f; pearlRadius=w*.066f;
                    secondaryPearlX=l+w*.84f; secondaryPearlY=t+h*.82f; secondaryPearlRadius=w*.042f;
                    break;
                case 26: // Imperial: broad elliptical leaf on top-left; right edge gold caustic.
                    meniscus.moveTo(l-w*.12f,t+h*.14f);
                    meniscus.cubicTo(l+w*.13f,t+h*.29f,l+w*.24f,t+h*.06f,l+w*.41f,t-h*.04f);
                    meniscus.cubicTo(l+w*.23f,t+h*.27f,l+w*.09f,t+h*.29f,l-w*.07f,t+h*.31f);
                    secondary.moveTo(l+w*.66f,t+h*1.06f);
                    secondary.cubicTo(l+w*.78f,t+h*.77f,l+w*1.05f,t+h*.83f,l+w*1.13f,t+h*.54f);
                    pearlX=l+w*.19f; pearlY=t+h*.15f; pearlRadius=w*.087f;
                    secondaryPearlX=l+w*.87f; secondaryPearlY=t+h*.74f; secondaryPearlRadius=w*.051f;
                    break;
                case 27: // Champagne: ivory liquid waves top-left and champagne bottom shelf.
                    meniscus.moveTo(l-w*.16f,t+h*.22f);
                    meniscus.cubicTo(l+w*.09f,t+h*.01f,l+w*.22f,t+h*.19f,l+w*.37f,t+h*.07f);
                    meniscus.cubicTo(l+w*.57f,t-h*.04f,l+w*.71f,t+h*.07f,l+w*.80f,t+h*.01f);
                    secondary.moveTo(l-w*.10f,t+h*.90f);
                    secondary.cubicTo(l+w*.20f,t+h*.67f,l+w*.52f,t+h*.99f,l+w*1.10f,t+h*.78f);
                    pearlX=l+w*.92f; pearlY=t+h*.30f; pearlRadius=w*.069f;
                    secondaryPearlX=l+w*.10f; secondaryPearlY=t+h*.62f; secondaryPearlRadius=w*.041f;
                    break;
                case 28: // Peach-green: golden sunset from left, green pool at right.
                    meniscus.moveTo(l-w*.18f,t+h*.35f);
                    meniscus.cubicTo(l+w*.09f,t+h*.20f,l+w*.27f,t+h*.14f,l+w*.40f,t+h*.28f);
                    meniscus.cubicTo(l+w*.58f,t+h*.45f,l+w*.85f,t+h*.25f,l+w*1.10f,t+h*.12f);
                    secondary.moveTo(l+w*.32f,t+h*1.09f);
                    secondary.cubicTo(l+w*.60f,t+h*.74f,l+w*.92f,t+h*.94f,l+w*1.14f,t+h*.72f);
                    pearlX=l+w*.26f; pearlY=t+h*.18f; pearlRadius=w*.074f;
                    secondaryPearlX=l+w*.86f; secondaryPearlY=t+h*.78f; secondaryPearlRadius=w*.051f;
                    break;
                default: // Pink-violet: floating neon folds at bottom-left.
                    meniscus.moveTo(l-w*.16f,t+h*.67f);
                    meniscus.cubicTo(l+w*.14f,t+h*.44f,l+w*.27f,t+h*.80f,l+w*.53f,t+h*.52f);
                    meniscus.cubicTo(l+w*.69f,t+h*.37f,l+w*.91f,t+h*.59f,l+w*1.13f,t+h*.32f);
                    secondary.moveTo(l-w*.10f,t+h*.15f);
                    secondary.cubicTo(l+w*.24f,t+h*.35f,l+w*.52f,t-h*.05f,l+w*.95f,t+h*.09f);
                    pearlX=l+w*.76f; pearlY=t+h*.22f; pearlRadius=w*.074f;
                    secondaryPearlX=l+w*.21f; secondaryPearlY=t+h*.79f; secondaryPearlRadius=w*.048f;
                    break;
            }
        } else if (header || navigation) {
            // Small liquid fold acts like a refractive glass edge, not a flat shine.
            switch (theme) {
                case 25:
                    meniscus.moveTo(l-w*.10f,t+h*.78f);
                    meniscus.cubicTo(l+w*.16f,t-h*.20f,l+w*.31f,t+h*.62f,l+w*.44f,t+h*.08f);
                    secondary.moveTo(l+w*.60f,t+h*1.05f);
                    secondary.cubicTo(l+w*.83f,t+h*.45f,l+w*.88f,t+h*.98f,l+w*1.08f,t+h*.36f);
                    break;
                case 26:
                    meniscus.moveTo(l-w*.04f,t+h*.90f);
                    meniscus.cubicTo(l+w*.18f,t+h*.26f,l+w*.08f,t-h*.18f,l+w*.29f,t-h*.07f);
                    secondary.moveTo(l+w*.71f,t+h*1.12f);
                    secondary.cubicTo(l+w*.83f,t+h*.25f,l+w*.99f,t+h*.62f,l+w*1.12f,t+h*.16f);
                    break;
                case 27:
                    meniscus.moveTo(l-w*.15f,t+h*.92f);
                    meniscus.cubicTo(l+w*.13f,t+h*.10f,l+w*.25f,t+h*1.03f,l+w*.43f,t+h*.25f);
                    secondary.moveTo(l+w*.50f,t-h*.10f);
                    secondary.cubicTo(l+w*.65f,t+h*.35f,l+w*.81f,t-h*.03f,l+w*1.12f,t+h*.42f);
                    break;
                case 28:
                    meniscus.moveTo(l-w*.08f,t+h*.64f);
                    meniscus.cubicTo(l+w*.13f,t-h*.27f,l+w*.31f,t+h*.90f,l+w*.53f,t+h*.34f);
                    secondary.moveTo(l+w*.52f,t+h*1.14f);
                    secondary.cubicTo(l+w*.67f,t+h*.55f,l+w*.89f,t+h*.61f,l+w*1.07f,t+h*.24f);
                    break;
                default:
                    meniscus.moveTo(l-w*.08f,t+h*.81f);
                    meniscus.cubicTo(l+w*.27f,t-h*.23f,l+w*.38f,t+h*1.02f,l+w*.54f,t+h*.40f);
                    secondary.moveTo(l+w*.64f,t+h*1.08f);
                    secondary.cubicTo(l+w*.75f,t+h*.21f,l+w*.98f,t+h*.90f,l+w*1.08f,t+h*.18f);
                    break;
            }
            pearlX=l+w*(theme==27?.17f:theme==28?.23f:.12f);
            pearlY=t+h*.25f; pearlRadius=Math.min(w*.09f,h*.45f);
            secondaryPearlX=l+w*.88f; secondaryPearlY=t+h*.75f;
            secondaryPearlRadius=Math.min(w*.065f,h*.27f);
        } else {
            // Cards retain quiet readability: no artificial stripes across data.
            meniscus.moveTo(l+w*.06f,t+h*.06f);
            meniscus.cubicTo(l+w*.25f,t-h*.08f,l+w*.55f,t+h*.20f,l+w*.72f,t+h*.03f);
            secondary.moveTo(l+w*.65f,t+h*1.03f);
            secondary.cubicTo(l+w*.80f,t+h*.87f,l+w*.91f,t+h*.90f,l+w*1.03f,t+h*.67f);
            pearlX=l+w*.15f; pearlY=t+h*.10f; pearlRadius=Math.min(w*.14f,h*.50f);
            secondaryPearlX=l+w*.90f; secondaryPearlY=t+h*.90f;
            secondaryPearlRadius=Math.min(w*.07f,h*.29f);
        }

        float scale = Math.min(w,h);
        // Broad strokes created flat ribbons over text in real emulator shots.
        // Refracted highlights are now confined to the upper glass HEADER.
        // Dashboard cards and the backdrop use smooth radial light fields.
        meniscusGlow.setStrokeWidth(Math.max(.8f*density,header?h*.13f:scale*.014f));
        meniscusGlow.setShader(new LinearGradient(l,t+h*.08f,l+w,t+h*.84f,
                new int[]{withAlpha(accent,0),withAlpha(gold,header?48:12),
                        withAlpha(gold,header?82:16),withAlpha(contrast,9),
                        withAlpha(accent,0)},
                new float[]{0f,.20f,.45f,.76f,1f},Shader.TileMode.CLAMP));
        meniscusEdge.setStrokeWidth(Math.max(.6f*density,
                scene?1.0f*density:header?1.35f*density:.85f*density));
        meniscusEdge.setShader(new LinearGradient(l,t,l+w,t+h,
                new int[]{withAlpha(gold,0),withAlpha(accent,89),
                        withAlpha(accent,182),withAlpha(gold,85),withAlpha(gold,0)},
                new float[]{0f,.19f,.49f,.78f,1f},Shader.TileMode.CLAMP));
        secondaryGlow.setStrokeWidth(Math.max(.7f*density,
                header?h*.075f:navigation?h*.035f:scale*.008f));
        secondaryGlow.setShader(new LinearGradient(l+w*.18f,t,l+w*.94f,t+h,
                new int[]{withAlpha(contrast,0),withAlpha(contrast,25),
                        withAlpha(gold,42),withAlpha(accent,0)},
                new float[]{0f,.33f,.66f,1f},Shader.TileMode.CLAMP));

        float sunX=pearlX, sunY=pearlY;
        sun.setShader(new RadialGradient(sunX,sunY,Math.max(1f,pearlRadius*2.85f),
                new int[]{withAlpha(accent,scene?143:header?105:54),
                        withAlpha(gold,scene?62:30),withAlpha(gold,0)},
                new float[]{0f,.24f,1f},Shader.TileMode.CLAMP));
        pearl.setShader(new RadialGradient(secondaryPearlX,secondaryPearlY,
                Math.max(1f,secondaryPearlRadius),
                new int[]{withAlpha(accent,scene?79:63),withAlpha(gold,28),
                        withAlpha(gold,0)},
                new float[]{0f,.38f,1f},Shader.TileMode.CLAMP));
        pearlEdge.setColor(withAlpha(accent, scene?68:header?135:35));
        pearlEdge.setStrokeWidth(Math.max(.5f,density*.75f));
        showPearl=scene || header;

        // Sunlight is a continuous radial field: NEVER draw long hard rays
        // through cards. The original references have soft lens caustics, not
        // diagonal strokes across text, backgrounds or plots.
        drawRays = false;
        if (drawRays) {
            float startX=sunX, startY=sunY;
            rayA.moveTo(startX,startY);
            rayA.cubicTo(startX+w*.19f,startY+h*.03f,
                    startX+w*.27f,startY+h*.17f,startX+w*.48f,startY+h*.20f);
            rayB.moveTo(startX,startY);
            rayB.cubicTo(startX-w*.18f,startY+h*.07f,
                    startX-w*.14f,startY+h*.24f,startX-w*.26f,startY+h*.40f);
            rayC.moveTo(startX,startY);
            rayC.cubicTo(startX+w*.04f,startY+h*.18f,
                    startX+w*.17f,startY+h*.36f,startX+w*.27f,startY+h*.52f);
            rayPaint.setStrokeWidth(Math.max(density,w*.015f));
            rayPaint.setShader(new LinearGradient(l,t,l+w,t+h,
                    new int[]{withAlpha(gold,0),withAlpha(gold,33),
                            withAlpha(gold,15),withAlpha(gold,0)},
                    new float[]{0f,.28f,.66f,1f},Shader.TileMode.CLAMP));
        }
    }

    void draw(Canvas canvas) {
        if (!enabled) return;
        // Only a restrained pearlescent fold in the header, NEVER an opaque
        // stripe drawn over account cards, values or the 7-day chart.
        if (headerOnly) {
            canvas.drawPath(secondary,secondaryGlow);
            canvas.drawPath(meniscus,meniscusGlow);
            canvas.drawPath(meniscus,meniscusEdge);
        } else if (navigationOnly) {
            canvas.drawPath(meniscus,meniscusEdge);
        }
        if (showPearl) {
            canvas.drawCircle(pearlX,pearlY,pearlRadius*2.85f,sun);
            canvas.drawCircle(secondaryPearlX,secondaryPearlY,secondaryPearlRadius,pearl);
            // Soft-glass edge, not cartoon bubble outlines.
            canvas.drawCircle(secondaryPearlX,secondaryPearlY,
                    secondaryPearlRadius*.89f,pearlEdge);
        }
    }
}
