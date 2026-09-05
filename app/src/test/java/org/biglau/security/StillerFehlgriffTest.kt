package org.biglau.security

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Warum die PIN nicht stimmte, wird auch gesagt, nicht nur gezeigt.
 *
 * BigLau macht auf diesem Bildschirm absichtlich keinen Ton. Die Punktreihe ist seit dem
 * 04.09.2026 eine `liveRegion` und sagt, wie viele Ziffern dastehen — nach einem
 * Fehlversuch also „Noch keine Ziffer eingegeben". Das ist die **Folge**. Der **Grund**
 * stand nur da: „Diese PIN stimmt nicht.", ein gewöhnlicher Text ohne `liveRegion`.
 *
 * Am Emulator nachgestellt: vier Ziffern, „Fertig", Ziffern weg, Meldung sichtbar — und für
 * jemanden, der nicht hinsieht, klang das genau wie ein verrutschter Finger. Wer sich fürs
 * Vertippen hält, tippt dieselbe falsche PIN noch einmal.
 *
 * Die Regel steht an der Fehlermeldung, nicht an der Datei: sie ist die einzige Stelle im
 * `PinGate`, die etwas mitteilt, was nicht schon woanders steht.
 */
class StillerFehlgriffTest {

    @Test
    fun `die fehlermeldung ist eine liveRegion`() {
        val gate = Quelltext.withoutComments("org/biglau/ui/PinGate.kt")
        val meldung = Quelltext.cut(
            gate,
            from = "wrong -> Text(",
            to = "\n                )",
        )
        assertTrue(
            "Die Meldung zur falschen PIN wird nicht angesagt: $meldung\n" +
                "Ohne liveRegion hoert man nur die Folge (die Punktreihe ist leer), nicht " +
                "den Grund - und haelt sich fuers Vertippen.",
            "liveRegion" in meldung,
        )
    }
}
