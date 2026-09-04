package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Rand pulst langsam genug, um nicht zu drängen.
 *
 * Ein Hinweis darf auffallen. Er darf nicht hetzen. Bis zum 04.09.2026 lief der Puls in
 * 500 ms von dünn nach dick und in 500 ms zurück, mit gleichmässigem Verlauf, also gut eine
 * Sekunde für den ganzen Weg. Der Nutzer hat es am Jelly 2 als ablenkend beschrieben, und
 * das ist der Massstab: die Kachel soll melden, dass etwas da ist, nicht daran erinnern,
 * dass sie blinkt.
 *
 * Der lineare Verlauf war der zweite Teil davon. Er kehrt an beiden Enden abrupt um, und
 * genau dieses Umschlagen macht das Zucken aus; ein weicher Verlauf hält an den Enden kurz
 * inne.
 *
 * Die Zahl steht hier, damit sie nicht beim nächsten Aufräumen wieder auf einen runden
 * kleinen Wert fällt.
 */
class PulsTest {

    private val kachel = Quelltext.ohneKommentare("org/biglau/ui/BigTile.kt")

    @Test
    fun `ein halber Puls dauert mindestens eine Sekunde`() {
        val zahl = Regex("""PULSDAUER_MS = (\d+)""").find(kachel)
            ?: throw AssertionError("PULSDAUER_MS steht nicht mehr im Quelltext")
        val ms = zahl.groupValues[1].toInt()
        assertTrue("Der Puls braucht nur $ms ms und wirkt damit hektisch", ms >= 1000)
    }

    @Test
    fun `der Verlauf ist weich`() {
        assertTrue(
            "Der Puls laeuft wieder gleichmaessig - dann schlaegt er an beiden Enden um",
            "FastOutSlowInEasing" in kachel && "LinearEasing" !in kachel,
        )
    }
}
