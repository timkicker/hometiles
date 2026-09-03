package org.biglau.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Zeile „nutzbar nach den Systemleisten" auf der Diagnoseseite.
 *
 * Sie meldete am Jelly 2 **565 dp**, nutzbar sind **581**. Der Fehler war die
 * Ausgangszahl: `resources.displayMetrics` gibt das Fenster schon ohne die Gestenleiste
 * (480 x 832 statt 480 x 854), und die Einblendungen zogen sie ein zweites Mal ab.
 *
 * Bemerkenswert daran ist, wie lange das gehalten hat: dieselben 565 dp standen in
 * `PLAN.md` 3.2 als „am Gerät gemessen". Zwei Stellen, derselbe Denkfehler - und weil sie
 * sich gegenseitig bestätigten, sah es nach einer geprüften Zahl aus.
 */
class DiagnosticsUsableTest {

    /** Das Jelly 2, mit den am 03.09.2026 gemessenen Leisten. */
    @Test
    fun `am Jelly 2 bleiben 349 auf 581 dp`() {
        assertEquals(
            349 to 581,
            Diagnostics.usableDp(480, 854, left = 0, top = 33, right = 0, bottom = 22, density = 1.375f),
        )
    }

    /**
     * Die Gegenprobe zum alten Fehler: gibt man die Fensterhöhe *ohne* Gestenleiste
     * hinein, kommen wieder 565 heraus. Die Rechnung ist richtig, die Eingabe war falsch.
     */
    @Test
    fun `mit der Fensterhoehe statt der Bildschirmhoehe kaeme wieder 565 heraus`() {
        assertEquals(
            349 to 565,
            Diagnostics.usableDp(480, 832, left = 0, top = 33, right = 0, bottom = 22, density = 1.375f),
        )
    }

    /** Im Querformat liegen die Leisten seitlich - dann geht dort etwas ab und oben wenig. */
    @Test
    fun `seitliche Leisten gehen von der Breite ab`() {
        assertEquals(
            332 to 186,
            Diagnostics.usableDp(854, 480, left = 0, top = 15, right = 22, bottom = 0, density = 2.5f),
        )
    }

    @Test
    fun `ohne Dichte gibt es keine Zahl statt einer geratenen`() {
        assertNull(Diagnostics.usableDp(480, 854, 0, 33, 0, 22, density = 0f))
    }

    /** Groessere Einblendungen als der Bildschirm ergeben null Flaeche, keine negative. */
    @Test
    fun `die Flaeche wird nie negativ`() {
        assertEquals(0 to 0, Diagnostics.usableDp(100, 100, 60, 60, 60, 60, density = 1f))
    }
}
