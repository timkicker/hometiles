package org.biglau.safety

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aus dem Notmodus kommt man ans Telefon.
 *
 * Der Notfall-Auffang erscheint, wenn BigLau **zweimal hintereinander** nicht bis zum
 * Zeichnen kommt. Bis zum 3.9.2026 bot er vier Wege an: noch einmal versuchen, Einstellungen,
 * anderen Startbildschirm wählen, Kacheln zurücksetzen. Alle vier reparieren den Launcher -
 * keiner half dem Menschen, der in diesem Moment **telefonieren** will.
 *
 * Das README versprach dagegen seit jeher „ein einfacher Bildschirm mit Telefon, Kontakten
 * und Einstellungen". Zwei der drei gab es nicht. Aufgefallen beim Nachlesen der eigenen
 * Zusagen, nicht durch einen Fehler - eine falsche Zusage fällt nie von selbst auf.
 *
 * Bewusst die Apps des **Systems**: die eigene Wähltastatur ist genau das, worauf man sich
 * nicht verlassen sollte, wenn die eigene App gerade zweimal abgestürzt ist.
 */
class NotmodusReachTest {

    private val quelle = Quelltext.file("org/biglau/safety/EmergencyScreen.kt").readText()

    @Test
    fun `der Notmodus fuehrt ans Telefon und zu den Kontakten`() {
        assertTrue("kein Weg zur Wähltastatur", "Intents.openDialer(" in quelle)
        assertTrue("kein Weg zu den Kontakten", "Intents.openContacts(" in quelle)
    }

    /**
     * Und zwar **vor** den Knöpfen, die den Launcher reparieren. Wer telefonieren will,
     * soll nicht erst an „anderen Startbildschirm wählen" vorbeilesen.
     */
    @Test
    fun `das Telefon steht vor der Reparatur`() {
        val telefon = quelle.indexOf("R.string.emergency_phone")
        val nochmal = quelle.indexOf("R.string.emergency_retry")
        assertTrue("emergency_phone fehlt", telefon >= 0)
        assertTrue("emergency_retry fehlt", nochmal >= 0)
        assertTrue("der Telefonknopf steht hinter den Reparaturknöpfen", telefon < nochmal)
    }

    /**
     * Der Notmodus lädt die Konfiguration **nicht** - sie könnte gerade das Problem sein.
     * Deshalb hat er feste Farben, und deshalb darf hier auch kein `ConfigStore` auftauchen.
     */
    @Test
    fun `der Notmodus liest die Konfiguration nicht`() {
        assertTrue("ConfigStore im Notmodus", "ConfigStore" !in quelle)
    }
}
