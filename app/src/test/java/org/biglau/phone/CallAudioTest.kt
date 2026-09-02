package org.biglau.phone

import org.biglau.data.AudioRoute
import org.biglau.data.PhoneConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Wohin der Ton beim Verbinden geht (`PLAN.md` 4.6).
 *
 * Zwei Zusagen treffen sich hier, und die Reihenfolge zwischen ihnen ist die eigentliche
 * Entscheidung: „Lautsprecher bei abgehenden Anrufen" ist der speziellere Fall und schlägt
 * die allgemeine Standard-Ausgabe.
 */
class CallAudioTest {

    @Test
    fun `ohne Einstellung wird nichts umgestellt`() {
        // Hoermuschel ist ohnehin die Vorgabe des Systems - dann ist der ehrlichste Eingriff
        // gar keiner.
        assertNull(CallAudio.routeOnConnect(PhoneConfig(), outgoing = false))
        assertNull(CallAudio.routeOnConnect(PhoneConfig(), outgoing = true))
    }

    @Test
    fun `die Standard-Ausgabe gilt fuer beide Richtungen`() {
        val config = PhoneConfig(audioRoute = AudioRoute.BLUETOOTH)
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = false))
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = true))
    }

    @Test
    fun `Lautsprecher beim Waehlen gilt nur fuer eigene Anrufe`() {
        val config = PhoneConfig(speakerOnOutgoing = true)
        assertEquals(AudioRoute.SPEAKER, CallAudio.routeOnConnect(config, outgoing = true))
        assertNull(CallAudio.routeOnConnect(config, outgoing = false))
    }

    @Test
    fun `beim Waehlen schlaegt der Lautsprecher die Standard-Ausgabe`() {
        // Wer selbst waehlt, haelt das Telefon oft noch in der Hand und schaut darauf.
        val config = PhoneConfig(audioRoute = AudioRoute.BLUETOOTH, speakerOnOutgoing = true)
        assertEquals(AudioRoute.SPEAKER, CallAudio.routeOnConnect(config, outgoing = true))
        assertEquals(AudioRoute.BLUETOOTH, CallAudio.routeOnConnect(config, outgoing = false))
    }

    @Test
    fun `Hoermuschel ausdruecklich gewaehlt bleibt ein Nicht-Eingriff`() {
        val config = PhoneConfig(audioRoute = AudioRoute.EARPIECE, speakerOnOutgoing = false)
        assertNull(CallAudio.routeOnConnect(config, outgoing = true))
    }
}
