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
    private val jelly = 581f

    /** Es klingelt: Annehmen und Ablehnen. */
    private val klingelt = 2

    /** Das Gespräch läuft: Auflegen, Stumm, Lautsprecher, Halten, Tastenfeld. */
    private val gespraech = 5

    @Test
    fun `ohne Foto bleibt kein Platz belegt`() {
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.OFF, jelly, klingelt), 0.01f)
    }

    @Test
    fun `die Stufen werden groesser`() {
        val klein = CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly, klingelt)
        val halb = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, klingelt)
        val voll = CallerPhotoSize.heightDp(CallerPhoto.FULL, jelly, klingelt)
        assertTrue("klein < halb", klein < halb)
        assertTrue("halb <= voll", halb <= voll)
    }

    @Test
    fun `auch die groesste Stufe laesst Platz fuer Name und Knoepfe`() {
        CallerPhoto.entries.forEach { stufe ->
            val hoehe = CallerPhotoSize.heightDp(stufe, jelly, klingelt)
            assertTrue(
                "$stufe laesst nur ${jelly - hoehe} dp uebrig",
                jelly - hoehe >= CallerPhotoSize.reservedDp(klingelt),
            )
        }
    }

    @Test
    fun `auf einem sehr kurzen Bildschirm faellt das Foto ganz weg`() {
        // Weniger Platz als der Rest braucht: dann lieber kein Foto als kein Knopf.
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.FULL, 200f, klingelt), 0.01f)
    }

    @Test
    fun `die Hoehe wird nie negativ`() {
        CallerPhoto.entries.forEach { stufe ->
            assertTrue(CallerPhotoSize.heightDp(stufe, 50f, klingelt) >= 0f)
        }
    }

    @Test
    fun `klein bleibt auf dem Jelly 2 wirklich klein`() {
        // Knapp ein Fuenftel - genug, um ein Gesicht zu erkennen, ohne den Namen zu
        // verdraengen.
        assertEquals(104.6f, CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly, klingelt), 1f)
    }

    // --- Foto, Initialen oder nichts ---

    /**
     * Der Fund vom 02.09.2026: mit „halbes Display" standen nach dem Annehmen nur noch
     * zwei von fünf Knöpfen im Bild. Der Platz für das Foto muss von der Zahl der Knöpfe
     * abhängen, nicht von einer festen Zahl.
     */
    @Test
    fun `waehrend des Gespraechs bleiben alle fuenf Knoepfe im Bild`() {
        CallerPhoto.entries.forEach { stufe ->
            val hoehe = CallerPhotoSize.heightDp(stufe, jelly, gespraech)
            val rest = jelly - hoehe
            assertTrue(
                "$stufe laesst nur $rest dp fuer fuenf Knoepfe",
                rest >= CallerPhotoSize.reservedDp(gespraech) ||
                    // Oder das Foto faellt ganz weg - dann ist ohnehin alles frei.
                    hoehe == 0f,
            )
        }
    }

    @Test
    fun `mehr Knoepfe lassen dem Foto weniger Platz`() {
        val beiZwei = CallerPhotoSize.heightDp(CallerPhoto.FULL, 900f, klingelt)
        val beiFuenf = CallerPhotoSize.heightDp(CallerPhoto.FULL, 900f, gespraech)
        assertTrue("$beiFuenf < $beiZwei", beiFuenf < beiZwei)
    }

    @Test
    fun `auf drei Zoll weicht das Foto im Gespraech ganz`() {
        // 581 dp minus Kopfzeile, fuenf Knoepfe und der Leerraum gegen das Ohr: es bleibt
        // kein Streifen uebrig, der ein Gesicht zeigen koennte.
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.FULL, jelly, gespraech), 0.01f)
    }

    @Test
    fun `der Leerraum gegen das Ohr steckt in der Reservierung`() {
        val ohne = CallerPhotoSize.HEADER_DP + gespraech * CallerPhotoSize.ROW_DP +
            (gespraech + 1) * CallerPhotoSize.ROW_GAP_DP
        assertEquals(
            CallerPhotoSize.EAR_GAP_DP,
            CallerPhotoSize.reservedDp(gespraech) - ohne,
            0.01f,
        )
    }

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

    @Test
    fun `die Hinweiszeile beim zweiten Anruf nimmt dem Foto Platz`() {
        val ohne = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, klingelt, notice = false)
        val mit = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, klingelt, notice = true)
        assertTrue("$mit < $ohne", mit < ohne)
        assertEquals(
            CallerPhotoSize.NOTICE_DP,
            CallerPhotoSize.reservedDp(klingelt, notice = true) -
                CallerPhotoSize.reservedDp(klingelt),
            0.01f,
        )
    }
}
