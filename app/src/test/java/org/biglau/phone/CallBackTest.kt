package org.biglau.phone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine unterdrückte Nummer lässt sich nicht zurückrufen.
 *
 * Am Emulator gefunden: ein Eintrag ohne Nummer stand in der Anrufliste als „?" da, und
 * beim Antippen fragte BigLau **„„" jetzt anrufen?"** — mit leeren Anführungszeichen. Ein
 * „Ja, anrufen" hätte nichts gewählt; die einzige Rückmeldung wäre gewesen, dass nichts
 * geschieht.
 *
 * `PhoneNumbers.isDialable` gab es längst und wurde an drei anderen Stellen benutzt — nur
 * an der einen, an der aus einem Tipp ein Anruf wird, nicht.
 */
class CallBackTest {

    @Test
    fun `eine unterdrueckte Nummer ist nicht waehlbar`() {
        assertFalse(PhoneNumbers.isDialable(""))
        assertFalse(PhoneNumbers.isDialable("   "))
        // So kommen unterdrueckte Nummern bei manchen Netzen an.
        assertFalse(PhoneNumbers.isDialable("unknown"))
        assertFalse(PhoneNumbers.isDialable("-"))
    }

    @Test
    fun `eine gewoehnliche Nummer bleibt waehlbar`() {
        assertTrue(PhoneNumbers.isDialable("+43664111001"))
        assertTrue(PhoneNumbers.isDialable("0664 111 001"))
    }

    /** Ohne Nummer bleibt nur der Ersatztext - die Zeile darf nicht leer sein. */
    @Test
    fun `ohne Nummer und ohne Namen steht der Ersatztext da`() {
        val ohne = CallView(
            status = CallStatus.RINGING,
            number = "",
            name = null,
            startedAtMillis = null,
        )
        org.junit.Assert.assertEquals("Unbekannt", CallActions.headline(ohne, "Unbekannt"))
    }
}
