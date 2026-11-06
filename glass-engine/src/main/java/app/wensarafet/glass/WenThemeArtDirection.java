package app.wensarafet.glass;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Five owner-approved art directions. Each scene places its light sources at
 * DIFFERENT positions with its own falloff, saturation, transparency and
 * specular orientation. No universal 'same shape, new palette' backdrop.
 *
 * Configure on bounds changes only; allocation-free Android draw() at 120 Hz.
 * No decorative lines, timers, runtime blur or financial application state.
 */
final class WenThemeArtDirection {
    private WenThemeArtDirection() {}

    static void configure(int index, RectF rect, boolean backdrop, boolean chrome,
                          boolean lowPower, Paint first, Paint second,
                          Paint focal, Paint specular) {
        if (lowPower) {
            first.setShader(null); second.setShader(null);
            focal.setShader(null); specular.setShader(null);
            return;
        }
        switch (index) {
            case 25: royal(rect, backdrop, chrome, first, second, focal, specular); break;
            case 26: imperial(rect, backdrop, chrome, first, second, focal, specular); break;
            case 27: champagne(rect, backdrop, chrome, first, second, focal, specular); break;
            case 28: emeraldAmber(rect, backdrop, chrome, first, second, focal, specular); break;
            case 29: violetFuchsia(rect, backdrop, chrome, first, second, focal, specular); break;
            default: throw new IllegalArgumentException("Unknown premium art direction");
        }
    }

    /** 25: deep obsidian, concentrated right-hand bronze caustic, left amber undertow. */
    private static void royal(RectF b, boolean scene, boolean chrome, Paint a,
                              Paint c, Paint focal, Paint reflection) {
        float w=b.width(),h=b.height();
        a.setShader(new RadialGradient(b.left+w*.85f,b.top+h*.24f,
                Math.max(1f,w*(scene?.79f:1.06f)),
                new int[]{0xA8CB9047,0x635A3820,0x18221513,0x00130D0B},
                new float[]{0f,.26f,.57f,1f},Shader.TileMode.CLAMP));
        c.setShader(new RadialGradient(b.left+w*.21f,b.top+h*.81f,
                Math.max(1f,w*(scene?1.06f:.84f)),
                new int[]{0x8757371D,0x393F2C1F,0x051A1816,0},
                new float[]{0f,.39f,.77f,1f},Shader.TileMode.CLAMP));
        focal.setShader(new RadialGradient(b.left+w*.72f,b.top+h*.09f,
                Math.max(1f,w*(chrome?.50f:.35f)),
                new int[]{0x8BFFE6AA,0x2FAE7A38,0},
                new float[]{0f,.33f,1f},Shader.TileMode.CLAMP));
        reflection.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,
                new int[]{0x75FFE8BC,0x18DAB782,0x04FFFFFF,0x32E4B374,0},
                new float[]{0f,.20f,.51f,.83f,1f},Shader.TileMode.CLAMP));
    }

    /** 26: blue-black graphite with opposite left upper and right lower gold light. */
    private static void imperial(RectF b, boolean scene, boolean chrome, Paint a,
                                 Paint c, Paint focal, Paint reflection) {
        float w=b.width(),h=b.height();
        a.setShader(new RadialGradient(b.left+w*.12f,b.top+h*.14f,
                Math.max(1f,w*(scene?.68f:.86f)),
                new int[]{0xB0C0A171,0x65594C3C,0x1A3D3937,0},
                new float[]{0f,.24f,.57f,1f},Shader.TileMode.CLAMP));
        c.setShader(new RadialGradient(b.left+w*.93f,b.top+h*.77f,
                Math.max(1f,w*(scene?.98f:.84f)),
                new int[]{0x996F593C,0x474F4033,0x09272C2B,0},
                new float[]{0f,.31f,.70f,1f},Shader.TileMode.CLAMP));
        focal.setShader(new RadialGradient(b.left+w*.67f,b.top+h*.35f,
                Math.max(1f,w*(chrome?.47f:.31f)),
                new int[]{0x91F9DEAB,0x1FCDA56D,0},
                new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
        reflection.setShader(new LinearGradient(b.left+w*.36f,b.top,
                b.right-w*.12f,b.bottom,new int[]{0x6FFFFFFF,0x1AD7C7AD,
                0x05FFFFFF,0x3DBDA078,0},new float[]{0f,.20f,.47f,.78f,1f},Shader.TileMode.CLAMP));
    }

    /** 27: soft pearlescent cream, reflected champagne rim and broad ivory refraction. */
    private static void champagne(RectF b, boolean scene, boolean chrome, Paint a,
                                  Paint c, Paint focal, Paint reflection) {
        float w=b.width(),h=b.height();
        a.setShader(new RadialGradient(b.left+w*.28f,b.top+h*.12f,
                Math.max(1f,w*(scene?.95f:1.05f)),
                new int[]{0xB1FFFFFF,0x78FFEBD7,0x26EDCEAC,0},
                new float[]{0f,.38f,.72f,1f},Shader.TileMode.CLAMP));
        c.setShader(new RadialGradient(b.left+w*.93f,b.top+h*.80f,
                Math.max(1f,w*(scene?1.15f:.91f)),
                new int[]{0xB3F1C3A2,0x65E9C5A9,0x16FFFFFF,0},
                new float[]{0f,.39f,.75f,1f},Shader.TileMode.CLAMP));
        focal.setShader(new RadialGradient(b.left+w*.64f,b.top+h*.30f,
                Math.max(1f,w*(chrome?.56f:.45f)),
                new int[]{0xA1FFFBEF,0x54FFE8BC,0x03FFFFFF},
                new float[]{0,.38f,1f},Shader.TileMode.CLAMP));
        reflection.setShader(new LinearGradient(b.left,b.top,
                b.left+w*.64f,b.bottom,
                new int[]{0xAEFFFFFF,0x46FFF9ED,0x08FFFFFF,0x6AF5D9B3,0},
                new float[]{0,.21f,.49f,.80f,1f},Shader.TileMode.CLAMP));
    }

    /** 28: true amber sunset at top left, translucent emerald folds at right/bottom. */
    private static void emeraldAmber(RectF b, boolean scene, boolean chrome, Paint a,
                                     Paint c, Paint focal, Paint reflection) {
        float w=b.width(),h=b.height();
        a.setShader(new RadialGradient(b.left+w*.17f,b.top+h*.18f,
                Math.max(1f,w*(scene?.89f:.95f)),
                new int[]{0xDFECA967,0xA9AF7242,0x4F536847,0},
                new float[]{0f,.33f,.70f,1f},Shader.TileMode.CLAMP));
        c.setShader(new RadialGradient(b.left+w*.84f,b.top+h*.73f,
                Math.max(1f,w*(scene?.94f:.78f)),
                new int[]{0xC05BAF86,0x76549374,0x16426E55,0},
                new float[]{0,.40f,.75f,1f},Shader.TileMode.CLAMP));
        focal.setShader(new RadialGradient(b.left+w*.78f,b.top+h*.09f,
                Math.max(1f,w*(chrome?.48f:.39f)),
                new int[]{0xB9FFE0AD,0x50FFC89F,0},new float[]{0,.36f,1f},
                Shader.TileMode.CLAMP));
        reflection.setShader(new LinearGradient(b.left+w*.15f,b.top,
                b.right,b.bottom,new int[]{0x7DFFF3D5,0x20E5FFDF,
                0x06FFFFFF,0x56D5F9D6,0},
                new float[]{0f,.22f,.50f,.79f,1f},Shader.TileMode.CLAMP));
    }

    /** 29: luminous magenta from upper left, electric indigo from lower right. */
    private static void violetFuchsia(RectF b, boolean scene, boolean chrome, Paint a,
                                      Paint c, Paint focal, Paint reflection) {
        float w=b.width(),h=b.height();
        a.setShader(new RadialGradient(b.left+w*.19f,b.top+h*.23f,
                Math.max(1f,w*(scene?.94f:.89f)),
                new int[]{0xDEF67AD8,0xA7DC4FAF,0x495C2BB1,0},
                new float[]{0,.33f,.65f,1f},Shader.TileMode.CLAMP));
        c.setShader(new RadialGradient(b.left+w*.87f,b.top+h*.79f,
                Math.max(1f,w*(scene?1.12f:.87f)),
                new int[]{0xCA6482FF,0x79594EE6,0x1E3A258D,0},
                new float[]{0,.37f,.76f,1f},Shader.TileMode.CLAMP));
        focal.setShader(new RadialGradient(b.left+w*.74f,b.top+h*.11f,
                Math.max(1f,w*(chrome?.51f:.40f)),
                new int[]{0xAEFFC6F7,0x43E59BE7,0},
                new float[]{0,.37f,1f},Shader.TileMode.CLAMP));
        reflection.setShader(new LinearGradient(b.left,b.top,
                b.right-w*.13f,b.bottom,new int[]{0xA6FFE4FF,0x20FEA8EE,
                0x06FFFFFF,0x45CBB1FF,0},new float[]{0f,.19f,.52f,.81f,1f},
                Shader.TileMode.CLAMP));
    }
}
