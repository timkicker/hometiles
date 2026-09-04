package org.biglau.ui

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Auf einer Kontaktkachel ohne Foto stehen die Initialen.
 *
 * `PLAN.md` 3.4 sagt das ausdrücklich zu: „ohne Foto die Initialen auf der Kachelfarbe."
 * Gezeichnet wurde stattdessen auf **jeder** Kontaktkachel dasselbe Personensymbol — drei
 * Kontakte nebeneinander sahen damit gleich aus, und das Symbol sagte nichts, was die
 * Beschriftung nicht schon sagte. Am Emulator gesehen und dort auch nachgeprüft: „AB" auf
 * Türkis, „FM" auf Magenta.
 */
class ContactTileInitialsTest {

    private val zweig = Quelltext.datei("org/biglau/ui/HomeScreenView.kt")
        .readText()
        .let { Quelltext.ausschnitt(it, "is ButtonAction.Contact -> BigTile(", "is ButtonAction.Shortcut") }

    @Test
    fun `die Kontaktkachel reicht Initialen weiter`() {
        assertTrue("initials fehlt an der Kontaktkachel", "initials =" in zweig)
        assertTrue("tileInitials fehlt", "tileInitials(" in zweig)
    }

    @Test
    fun `kein allgemeines Personensymbol mehr`() {
        assertTrue(
            "Ein Personensymbol auf jeder Kontaktkachel trägt nichts: $zweig",
            "Builtin.CONTACTS.icon()" !in zweig,
        )
    }

    /** Zwei Buchstaben, und aus einem einteiligen Namen einer. Siehe [initialsOf]. */
    @Test
    fun `die Initialen kommen aus dem Namen`() {
        assertEquals("AB", tileInitials("Anna Bauer"))
        assertEquals("FM", tileInitials("Franz Müller"))
        assertEquals("O", tileInitials("Oma"))
    }

    /**
     * Am Emulator gesehen: die Kachel „055 501 00" trug die Initialen „00". Zwei Nullen
     * sagen nichts und sehen nach Fehler aus; auf der Kachel ist leer besser als falsch.
     */
    @Test
    fun `eine Nummer bekommt keine Initialen`() {
        assertNull(tileInitials("055 501 00"))
        assertNull(tileInitials("+43 664 111 001"))
        assertNull(tileInitials(""))
    }
}
