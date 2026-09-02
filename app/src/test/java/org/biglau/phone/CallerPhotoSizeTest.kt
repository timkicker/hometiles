package org.biglau.phone

import org.biglau.data.CallerPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wie groß das Foto des Anrufers werden darf (`PLAN.md` 4.6).
 *
 * Der springende Punkt ist die Obergrenze. Auf dem Bildschirm, auf dem ein Fehler bedeutet,
 * dass jemand einen Anruf nicht annehmen kann, darf ein Foto niemals den Annehmen-Knopf
 * hinausschieben — auch nicht in der Stufe „so groß wie es passt".
 */
class CallerPhotoSizeTest {

    /** Nutzbare Höhe des Jelly 2 (PLAN.md 3.2). */
    private val jelly = 549f

    @Test
    fun `ohne Foto bleibt kein Platz belegt`() {
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.OFF, jelly), 0.01f)
    }

    @Test
    fun `die Stufen werden groesser`() {
        val klein = CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly)
        val halb = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly)
        val voll = CallerPhotoSize.heightDp(CallerPhoto.FULL, jelly)
        assertTrue("klein < halb", klein < halb)
        assertTrue("halb <= voll", halb <= voll)
    }

    @Test
    fun `auch die groesste Stufe laesst Platz fuer Name und Knoepfe`() {
        CallerPhoto.entries.forEach { stufe ->
            val hoehe = CallerPhotoSize.heightDp(stufe, jelly)
            assertTrue(
                "$stufe laesst nur ${jelly - hoehe} dp uebrig",
                jelly - hoehe >= CallerPhotoSize.RESERVED_DP,
            )
        }
    }

    @Test
    fun `auf einem sehr kurzen Bildschirm faellt das Foto ganz weg`() {
        // Weniger Platz als der Rest braucht: dann lieber kein Foto als kein Knopf.
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.FULL, 200f), 0.01f)
    }

    @Test
    fun `die Hoehe wird nie negativ`() {
        CallerPhoto.entries.forEach { stufe ->
            assertTrue(CallerPhotoSize.heightDp(stufe, 50f) >= 0f)
        }
    }

    @Test
    fun `klein bleibt auf dem Jelly 2 wirklich klein`() {
        // Knapp ein Fuenftel - genug, um ein Gesicht zu erkennen, ohne den Namen zu
        // verdraengen.
        assertEquals(98.8f, CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly), 1f)
    }

    // --- Foto, Initialen oder nichts ---

    @Test
    fun `mit Foto steht das Foto da`() {
        assertEquals(
            CallerPhotoSize.Image.PHOTO,
            CallerPhotoSize.imageFor(200f, "content://foto/1", "Anna Bauer"),
        )
    }

    /** Der Fund am Emulator: halbe Fläche reserviert, kein Foto, und alles blieb schwarz. */
    @Test
    fun `ohne Foto aber mit Namen die Initialen`() {
        assertEquals(CallerPhotoSize.Image.INITIALS, CallerPhotoSize.imageFor(200f, null, "Anna Bauer"))
    }

    /** Aus „+43" liesse sich kein Zeichen machen, das etwas bedeutet. */
    @Test
    fun `eine unbekannte Nummer bekommt nichts`() {
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(200f, null, null))
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(200f, null, "  "))
    }

    @Test
    fun `ohne reservierte Hoehe steht gar nichts da`() {
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(0f, "content://foto/1", "Anna Bauer"))
    }
}
