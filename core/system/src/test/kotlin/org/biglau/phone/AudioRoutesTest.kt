package org.biglau.phone

import android.telecom.CallAudioState
import org.biglau.data.AudioRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Zuordnung zwischen Einstellung und Telecom stand zweimal im Quelltext - und beim
 * zweiten Mal nur zur Hälfte: „Lautsprecher an" gab es, „Lautsprecher aus" nicht.
 */
class AudioRoutesTest {

    @Test
    fun `jeder Weg hat seine Entsprechung`() {
        assertEquals(CallAudioState.ROUTE_EARPIECE, AudioRoutes.toTelecom(AudioRoute.EARPIECE))
        assertEquals(CallAudioState.ROUTE_SPEAKER, AudioRoutes.toTelecom(AudioRoute.SPEAKER))
        assertEquals(CallAudioState.ROUTE_BLUETOOTH, AudioRoutes.toTelecom(AudioRoute.BLUETOOTH))
    }

    @Test
    fun `der Rueckweg stimmt mit dem Hinweg ueberein`() {
        AudioRoute.entries.forEach { weg ->
            assertEquals(weg, AudioRoutes.fromTelecom(AudioRoutes.toTelecom(weg)))
        }
    }

    @Test
    fun `unbekanntes zaehlt als Hoermuschel`() {
        // Ein Weg, den diese App nicht kennt (Kabelhoerer etwa), darf die Anzeige nicht
        // auf "Lautsprecher" stellen - das waere eine Auskunft, die nicht stimmt.
        assertEquals(AudioRoute.EARPIECE, AudioRoutes.fromTelecom(null))
        assertEquals(AudioRoute.EARPIECE, AudioRoutes.fromTelecom(CallAudioState.ROUTE_WIRED_HEADSET))
    }

    @Test
    fun `Bluetooth zaehlt nur, wenn das Geraet es anbietet`() {
        assertFalse(AudioRoutes.bluetoothAvailable(null))
        assertFalse(
            AudioRoutes.bluetoothAvailable(
                CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
            ),
        )
        assertTrue(
            AudioRoutes.bluetoothAvailable(
                CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_BLUETOOTH,
            ),
        )
    }
}
