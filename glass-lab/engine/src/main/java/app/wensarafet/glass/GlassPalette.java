package app.wensarafet.glass;

import android.graphics.Color;

/** Pure visual tokens. Safe to reuse in other Android apps. */
public final class GlassPalette {
    public final String name;
    public final int brand, deep, soft, background, surface, ink, muted, accent;
    public final boolean dark;
    private GlassPalette(String name, int brand, int deep, int soft, int background,
                         int surface, int ink, int muted, int accent, boolean dark) {
        this.name = name;
        this.brand = brand;
        this.deep = deep;
        this.soft = soft;
        this.background = background;
        this.surface = surface;
        this.ink = ink;
        this.muted = muted;
        this.accent = accent;
        this.dark = dark;
    }
    public static final GlassPalette[] THEMES = {
        new GlassPalette("زجاج أزرق", 0xFF2965E5, 0xFF183B9F, 0xFFDDEBFF,
            0xFFF6F9FF, Color.WHITE, 0xFF192B4C, 0xFF71819B, 0xFF8DCFFF, false),
        new GlassPalette("فقاعات أكوا", 0xFF00A4BA, 0xFF006C86, 0xFFDFF9F6,
            0xFFF3FEFC, Color.WHITE, 0xFF123D48, 0xFF668E92, 0xFF76EFE3, false),
        new GlassPalette("أورورا ليلية", 0xFF8777FF, 0xFF22205D, 0xFF39355D,
            0xFF11152E, 0xFF202747, 0xFFF6F7FF, 0xFFAAB8D9, 0xFF73CAFF, true),
        new GlassPalette("لؤلؤ متجمّد", 0xFF7789CF, 0xFF465588, 0xFFF0F0FC,
            0xFFFAFBFF, Color.WHITE, 0xFF272E4D, 0xFF7C829D, 0xFFC4D8FF, false)
    };
    public static GlassPalette at(int index) {
        return THEMES[Math.max(0, Math.min(THEMES.length - 1, index))];
    }
}
