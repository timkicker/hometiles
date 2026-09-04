package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ein Screenwechsel aus einem Ordner heraus schliesst den Ordner.
 *
 * Ein Ordner ist eine Überlagerung: er wechselt den Screen nicht, er legt sich darüber.
 * Damit gehört ihm auch der Bildschirm. Wechselt jemand von einer Kachel **im** Ordner aus
 * den Screen, passiert das sonst hinter der Überlagerung — und für den Nutzer passiert gar
 * nichts.
 *
 * Am 04.09.2026 am Jelly 2 nachgestellt: eine Kachel „zu Screen 2" auf dem freien Platz im
 * Ordner „Mehr", angetippt. Der Ordner blieb offen, die fünf Kacheln standen weiter da,
 * nichts rührte sich. Erst wer den Ordner schloss, stand plötzlich auf Screen 2 — mit
 * fünfmal „Zum Belegen tippen" vor sich. Wer stattdessen ein zweites Mal tippt, wechselt
 * ein zweites Mal.
 *
 * Es traf vier Wege zugleich: die Kachel „zu Screen", und die eingebauten
 * „Startbildschirm", „nächster Screen" und „voriger Screen". Deshalb steht die Regel nicht
 * an einem der vier, sondern an der einen Stelle, durch die alle vier gehen.
 */
class OrdnerWechselTest {

    private val activate = Quelltext.ausschnitt(
        Quelltext.ohneKommentare("org/biglau/MainActivity.kt"),
        von = "private fun activate(",
        bis = "\n    private fun ",
    )

    @Test
    fun `activate wechselt den screen nur ueber eine stelle`() {
        val direkt = Regex("goToScreen\\(").findAll(activate).count()
        assertEquals(
            "In activate() steht `goToScreen(` $direkt-mal. Genau einer dieser Aufrufe darf " +
                "es sein — der in `wechseln`, das vorher den Ordner schliesst. Jeder weitere " +
                "wechselt den Screen hinter einer offenen Überlagerung, und der Nutzer sieht " +
                "nichts davon.",
            1,
            direkt,
        )
    }

    @Test
    fun `die eine stelle schliesst den ordner`() {
        val wechseln = Quelltext.ausschnitt(
            activate,
            von = "val wechseln: (String) -> Unit = {",
            bis = "}",
        )
        assertEquals(
            "`wechseln` schliesst den Ordner nicht: $wechseln",
            1,
            Regex("openFolder\\.value = null").findAll(wechseln).count(),
        )
    }
}
