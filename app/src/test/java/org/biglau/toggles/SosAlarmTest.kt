package org.biglau.toggles

import org.biglau.Quelltext
import java.io.File
import org.biglau.data.SosConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Alarmton und Blinklicht während des Notrufs, `PLAN.md` 4.8.
 *
 * Der Punkt stand seit dem ersten Tag im Plan und war nirgends gebaut — er stand nicht
 * einmal unter den offenen Sachen. Gefunden beim Abgleich Plan gegen Quelltext.
 */
class SosAlarmTest {

    private val sosQuelle = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `beides aus heisst nichts tun`() {
        assertFalse(SosAlarm.active(SosConfig()))
        assertTrue(SosAlarm.active(SosConfig(alarmSound = true)))
        assertTrue(SosAlarm.active(SosConfig(alarmFlash = true)))
    }

    /**
     * Erst nach dem Countdown, nicht währenddessen.
     *
     * Wer im Supermarkt versehentlich auf den Knopf kommt und ihn wegdrückt, soll keine
     * Sirene ausgelöst haben — sonst schaltet er den Notruf danach ganz ab. Geprüft an der
     * Reihenfolge im Quelltext: der Start steht hinter dem Countdown und direkt beim Senden.
     */
    @Test
    fun `der Alarm beginnt erst mit dem Senden`() {
        val start = sosQuelle.indexOf("SosAlarm.start")
        val countdown = sosQuelle.indexOf("if (remaining == 0) break")
        assertTrue("SosAlarm.start fehlt in SosActivity", start > 0)
        assertTrue("Der Alarm darf nicht vor dem Ende des Countdowns beginnen", start > countdown)
    }

    /** Ein Ton, den man nur durch Neustart losgeworden wäre, macht aus dem Notruf ein Ärgernis. */
    @Test
    fun `der Alarm hoert mit dem Bildschirm auf`() {
        assertTrue("SosAlarm.stop fehlt in SosActivity", "SosAlarm.stop" in sosQuelle)
        // Nach dem Inhalt gefragt, nicht nach der Schreibweise: im selben Aufraeumen steht
        // inzwischen auch das Abmelden der Ortung, und daran soll diese Regel nicht
        // zerbrechen.
        val aufraeumen = Quelltext.cut(sosQuelle, "onDispose {", "}")
        assertTrue("Es fehlt das onDispose dazu: $aufraeumen", "SosAlarm.stop" in aufraeumen)
    }
}
