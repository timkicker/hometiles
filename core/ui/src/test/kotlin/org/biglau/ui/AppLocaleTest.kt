package org.biglau.ui

import java.util.Locale
import org.biglau.data.Appearance
import org.biglau.data.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PLAN.md 4.9: Sprache System / Deutsch / Englisch.
 *
 * Der Grund ist nicht Bequemlichkeit. Viele Telefone stehen auf einer Sprache, die jemand
 * anderes eingestellt hat; wer sie umstellen wollte, müsste sich erst durch
 * Systemeinstellungen arbeiten, die er nicht lesen kann.
 */
class AppLocaleTest {

    @Test
    fun `die vorgabe folgt dem telefon`() {
        assertEquals(Language.SYSTEM, Appearance().language)
        assertNull(AppLocale.localeFor(Language.SYSTEM))
    }

    @Test
    fun `deutsch und englisch werden aufgeloest`() {
        assertEquals(Locale.GERMAN, AppLocale.localeFor(Language.GERMAN))
        assertEquals(Locale.ENGLISH, AppLocale.localeFor(Language.ENGLISH))
    }

    // Ein Bildschirm, der schon lief, haelt die alten Texte. Der Startbildschirm steht die
    // ganze Zeit im Hintergrund - ohne Neuaufbau bliebe ausgerechnet die Seite
    // fremdsprachig, auf der man nach dem Umstellen landet.
    @Test
    fun `nur eine geaenderte sprache baut neu auf`() {
        assertEquals(false, AppLocale.needsRecreate(Language.GERMAN, Language.GERMAN))
        assertEquals(true, AppLocale.needsRecreate(Language.GERMAN, Language.ENGLISH))
        assertEquals(true, AppLocale.needsRecreate(Language.SYSTEM, Language.GERMAN))
    }
}
