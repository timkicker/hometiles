package org.biglau.ui

import java.util.Locale
import org.biglau.data.Appearance
import org.biglau.data.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PLAN.md 4.9: "Sprache System / Deutsch / Englisch", quoted in the plan's german.
 *
 * the reason is not convenience. many phones stand in a language somebody else set; whoever
 * wanted to change it would first have to work through system settings they cannot read.
 */
class AppLocaleTest {

    @Test
    fun `the default follows the phone`() {
        assertEquals(Language.SYSTEM, Appearance().language)
        assertNull(AppLocale.localeFor(Language.SYSTEM))
    }

    @Test
    fun `german and english resolve`() {
        assertEquals(Locale.GERMAN, AppLocale.localeFor(Language.GERMAN))
        assertEquals(Locale.ENGLISH, AppLocale.localeFor(Language.ENGLISH))
    }

    // a screen already running keeps the old texts. the home screen stands in the background
    // the whole time - without a rebuild the very page one lands on after switching would
    // stay in the foreign language.
    @Test
    fun `only a changed language rebuilds`() {
        assertEquals(false, AppLocale.needsRecreate(Language.GERMAN, Language.GERMAN))
        assertEquals(true, AppLocale.needsRecreate(Language.GERMAN, Language.ENGLISH))
        assertEquals(true, AppLocale.needsRecreate(Language.SYSTEM, Language.GERMAN))
    }
}
