package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nach der ersten Nachricht ist die neue Unterhaltung eine richtige.
 *
 * Am 03.09.2026 am Geraet: die erste Nachricht an eine Nummer geschickt, das Feld leerte
 * sich, eine kurze Meldung kam - und der Bildschirm blieb **leer**. Die Nachricht war
 * geschrieben und stand in der Liste, aber dieser Bildschirm hing noch an der Nummer statt
 * an der Unterhaltung, die es inzwischen gab.
 *
 * Wer gerade etwas abgeschickt hat und danach vor einem leeren Bildschirm steht, schickt es
 * noch einmal. Bei einer SMS kostet das Geld und verwirrt den Empfaenger; bei einer
 * Nachricht, auf die es ankommt, kostet es Zeit, in der man glaubt, es sei nichts passiert.
 *
 * Dazu die zweite Haelfte derselben Stelle: die Zeile „Neue Nachricht an …" blieb stehen,
 * nachdem es die Unterhaltung gab - „New message to +43 650 7654321" ueber „Tim Kicker",
 * zwei Eintraege fuer dieselbe Person, einer mit Namen und einer mit Nummer.
 */
class NeueUnterhaltungTest {

    private val quelle = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `nach dem Senden wechselt der Bildschirm in die Unterhaltung`() {
        // Gefragt ist nicht, ob der Name irgendwo steht, sondern ob er **beim Senden**
        // gesetzt wird. Die erste Fassung dieser Regel liess sich nicht brechen: sie blieb
        // gruen, als genau diese Zuweisung entfernt wurde.
        val beimSenden = quelle.indexOf("send(newNumber")
        assertTrue("Das Senden in eine neue Unterhaltung gibt es nicht mehr", beimSenden > 0)
        val block = quelle.substring(beimSenden, minOf(quelle.length, beimSenden + 260))
        assertTrue(
            "Das Senden in eine neue Unterhaltung merkt sich die Nummer nicht - dann " +
                "bleibt der Bildschirm leer stehen, obwohl die Nachricht heraus ist: " +
                block,
            "justSentTo" in block,
        )
        val ab = quelle.indexOf("LaunchedEffect(threads, justSentTo)")
        assertTrue(
            "Niemand loest die gemerkte Nummer in eine Unterhaltung auf.",
            ab > 0,
        )
        val rumpf = quelle.substring(ab, minOf(quelle.length, ab + 500))
        assertTrue(
            "Der Effekt setzt die Unterhaltung nicht: $rumpf",
            "openThread =" in rumpf,
        )
    }

    @Test
    fun `die Zeile fuer eine neue Nachricht verschwindet mit der Unterhaltung`() {
        val ab = quelle.indexOf("prefilled =")
        assertTrue("Die Zeile gibt es nicht mehr", ab > 0)
        val rumpf = quelle.substring(ab, minOf(quelle.length, ab + 300))
        assertTrue(
            "Die Zeile haengt an nichts - dann steht sie auch dann da, wenn es die " +
                "Unterhaltung laengst gibt, und dieselbe Person erscheint zweimal: " +
                "einmal mit Namen, einmal mit Nummer.",
            "threads.none" in rumpf || "takeIf" in rumpf,
        )
    }
}
