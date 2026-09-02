package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wer da anruft — der Name statt der Nummer.
 *
 * Am Emulator gesehen: bei einem Anruf von einer Nummer, die im Adressbuch steht, stand
 * trotzdem die Ziffernfolge da. Der Grund war, dass nur genommen wurde, was das Netz
 * mitschickt (CNAP), und das kommt praktisch nie. Wer eine Nummer nicht auswendig kennt,
 * musste beim Klingeln raten, ob er drangehen will.
 */
class CallHeadlineTest {

    private fun view(number: String, name: String?) = CallView(
        status = CallStatus.RINGING,
        number = number,
        name = name,
        startedAtMillis = null,
    )

    @Test
    fun `der name geht der nummer vor`() {
        assertEquals("Oma", CallActions.headline(view("+4366412345", "Oma")))
    }

    // Ohne Namen bleibt die Nummer - in Bloecken, wie ueberall sonst.
    @Test
    fun `ohne namen steht die nummer in bloecken`() {
        assertEquals("+436 641 234 5", CallActions.headline(view("+4366412345", null)))
    }

    // Ein leerer Name ist kein Name. Sonst stuende auf dem Bildschirm gar nichts, und man
    // wuesste nicht einmal, dass ueberhaupt jemand anruft.
    @Test
    fun `ein leerer name faellt auf die nummer zurueck`() {
        assertEquals("+436 641 234 5", CallActions.headline(view("+4366412345", "  ")))
    }

    // Unterdrueckte Nummer: weder Name noch Ziffern. Ein Fragezeichen ist ehrlicher als
    // eine leere Zeile - es sagt "unbekannt", nicht "kaputt".
    @Test
    fun `ohne beides bleibt ein fragezeichen`() {
        assertEquals("?", CallActions.headline(view("", null)))
    }
}
