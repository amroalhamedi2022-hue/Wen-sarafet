package app.wensarafet.tracker;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Owner-facing appearance catalogue: synthetic prefs only, no finance DB. */
@RunWith(AndroidJUnit4.class)
public final class ThemeCatalogueInstrumentedTest {
    @Test public void fiveApprovedThemesVisibleAndObsoleteAdditionsRetired() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("app.wensarafet.tracker.recovered.debug", c.getPackageName());
        ThemeCatalogue.restoreUserHidden(c);
        assertEquals(27, ThemeCatalogue.visibleCount(c));
        assertEquals("Frozen Pearl", ThemeCatalogue.displayName(21));
        assertFalse(ThemeCatalogue.isVisible(c, 22));
        assertFalse(ThemeCatalogue.isVisible(c, 23));
        assertFalse(ThemeCatalogue.isVisible(c, 24));
        for (int i = 25; i <= 29; i++)
            assertTrue("Owner-approved palette " + i + " must remain", ThemeCatalogue.isVisible(c, i));
        for (int i = 0; i < Ui.THEME_NAMES.length; i++)
            assertTrue("English display name expected at " + i,
                    ThemeCatalogue.displayName(i).matches("[a-zA-Z ]+"));
    }

    @Test public void hidingThemeDoesNotTouchSelectionAndCanBeRestored() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = c.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        int before = prefs.getInt("theme", 0);
        try {
            Ui.saveTheme(c, 25);
            ThemeCatalogue.restoreUserHidden(c);
            assertFalse("Active selection cannot be deleted", ThemeCatalogue.hide(c, 25));
            assertTrue(ThemeCatalogue.hide(c, 26));
            assertFalse(ThemeCatalogue.isVisible(c, 26));
            assertEquals(25, Ui.themeIndex(c));
            assertEquals(26, ThemeCatalogue.visibleCount(c));
            ThemeCatalogue.restoreUserHidden(c);
            assertTrue(ThemeCatalogue.isVisible(c, 26));
            assertEquals(27, ThemeCatalogue.visibleCount(c));
            assertEquals(25, Ui.themeIndex(c));
        } finally {
            ThemeCatalogue.restoreUserHidden(c);
            Ui.saveTheme(c, before);
        }
    }
}
