package org.biglau.phone

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aus dem Verlauf wird nicht sofort gewählt.
 *
 * `PLAN.md` 3.1, Leitsatz 5 nennt drei Dinge, die eine Rückfrage brauchen: Löschen,
 * **Anrufen aus dem Verlauf**, SOS. Löschen und SOS fragten, das Anrufen nicht — ein Tipp
 * auf eine Zeile wählte. In einer Liste, die jemand mit zittriger Hand durchsieht, ist ein
 * Tipp daneben damit ein Anruf bei einem Menschen. Gefunden beim Abgleich Plan gegen
 * Quelltext.
 *
 * Geprüft wird an der Stelle, an der es schiefging: die Zeile der Anrufliste darf nicht
 * unmittelbar wählen.
 */
class CallConfirmTest {

    private val quelle = Quelltext.datei("org/biglau/phone/DialerActivity.kt").readText()

    /** Die Zeile öffnet die Rückfrage, statt zu wählen. */
    @Test
    fun `die Zeile im Verlauf fragt erst`() {
        val zeile = quelle.substringAfter("items(groups, key =").substringBefore("onLongClick")
        assertTrue("Die Zeile ruft onAskCall auf: $zeile", "onAskCall(" in zeile)
        assertTrue("Die Zeile darf nicht unmittelbar waehlen: $zeile", "onCall(" !in zeile)
    }

    /** Es gibt beide Wege aus der Frage heraus - sonst wäre sie eine Sackgasse. */
    @Test
    fun `die Rueckfrage hat ein Ja und ein Nein`() {
        assertTrue("calllog_call_yes fehlt", "R.string.calllog_call_yes" in quelle)
        assertTrue("calllog_call_no fehlt", "R.string.calllog_call_no" in quelle)
        assertTrue("onCancelCall fehlt", "onCancelCall" in quelle)
        assertTrue("onConfirmCall fehlt", "onConfirmCall" in quelle)
    }

    /**
     * Der Notruf bleibt davon unberührt: er geht ohnehin an den System-Dialer und nicht
     * über diese Zeile. Siehe [PhoneNumbers.looksLikeEmergency] und `dial`.
     */
    @Test
    fun `der Notrufweg bleibt vor der Sperre`() {
        val dial = quelle.substringAfter("private fun dial(").substringBefore("\n    }")
        assertTrue(
            "Der Notruf muss vor der Nummernsperre geprüft werden",
            dial.indexOf("looksLikeEmergency") < dial.indexOf("CallBlocking.isBlocked"),
        )
    }
}
