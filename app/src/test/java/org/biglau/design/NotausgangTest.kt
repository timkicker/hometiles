package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Notausgang fuehrt aus BigLau hinaus, nicht wieder hinein.
 *
 * Der Notmodus erscheint, wenn der Startbildschirm zweimal hintereinander nicht
 * durchgekommen ist. Er sagt von sich: „This view works without your settings, so that you
 * can get back." Darunter stehen Telefon und Kontakte - damit man auch dann jemanden
 * erreicht, wenn die eigene Oberflaeche nicht mehr geht.
 *
 * `openDialer` schickte dafuer ein nacktes `ACTION_DIAL`, und darueber stand der Satz „Die
 * Waehltastatur **des Systems**". Ein nacktes `ACTION_DIAL` geht aber an die
 * Standard-Telefon-App - und seit dem 03.09.2026 ist das BigLau selbst. Der Notausgang
 * haette in die App zurueckgefuehrt, die gerade zweimal abgestuerzt ist.
 *
 * Am Geraet fiel es nicht auf: das System nahm an dem Tag seinen eigenen Dialer. Eine
 * Beobachtung, die zufaellig richtig ausgeht, ist kein Beweis.
 */
class NotausgangTest {

    private val intents = Quelltext.ohneKommentare("org/biglau/actions/Intents.kt")
    private val notmodus = Quelltext.ohneKommentare("org/biglau/safety/EmergencyScreen.kt")

    @Test
    fun `der Notausgang sucht ausdruecklich eine fremde App`() {
        val ab = intents.indexOf("fun openDialer(")
        assertTrue("openDialer gibt es nicht mehr", ab > 0)
        val rumpf = intents.substring(ab, minOf(intents.length, ab + 600))
        assertTrue(
            "openDialer schickt ein nacktes ACTION_DIAL. Das geht an die " +
                "Standard-Telefon-App - und wenn BigLau die ist, fuehrt der Notausgang " +
                "zurueck in die abgestuerzte App.",
            "packageName != context.packageName" in rumpf,
        )
    }

    @Test
    fun `der Notmodus bietet Telefon und Kontakte an`() {
        listOf("openDialer", "openContacts").forEach { weg ->
            assertTrue(
                "Der Notmodus bietet $weg nicht mehr an - dann steht dort nur noch " +
                    "Reparatur, und wer Hilfe braucht, erreicht niemanden.",
                weg in notmodus,
            )
        }
    }

    /**
     * Und er waehlt nichts von selbst.
     *
     * Ein Bildschirm, der nach zwei Abstuerzen erscheint, ist der letzte Ort, an dem etwas
     * ungefragt hinausgehen darf.
     */
    @Test
    fun `der Notmodus waehlt nichts`() {
        val waehlt = notmodus.lines().withIndex()
            .filter { (_, z) -> "ACTION_CALL" in z || "Intents.call(" in z }
            .map { it.index + 1 }
        assertEquals(
            "Der Notmodus waehlt. Er soll oeffnen, nicht anrufen.",
            emptyList<Int>(),
            waehlt,
        )
    }
}
