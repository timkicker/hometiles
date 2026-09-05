package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was eine Kachel **sagt**, haengt nicht daran, was sie **zeigt**.
 *
 * Zwei Einstellungen blenden die Beschriftung weg: „Beschriftung aus" und „abgeschnittene
 * Beschriftungen weglassen". Beide sind fuer Augen gemacht - ein abgeschnittenes Wort ist
 * schwerer zu lesen als keines. Fuer einen Screenreader wird nichts abgeschnitten; wuerde
 * der gesprochene Text mit der sichtbaren Beschriftung verschwinden, waere die Kachel fuer
 * einen blinden Menschen namenlos.
 *
 * Und das trifft genau zusammen: wer die Beschriftungen wegblendet, tut es meist, weil er
 * schlecht sieht. Dieselbe Einstellung darf ihm nicht den Namen nehmen.
 *
 * Am 04.09.2026 im Quelltext geprueft und am Geraet **nicht** nachweisbar: auf dem Jelly 2
 * passen alle Beschriftungen des Nutzers, es wird also nie eine abgeschnitten. Die
 * Bedingung liesse sich nur herstellen, indem man seine Textgroesse oder eine
 * Kachelbeschriftung aendert. Deshalb steht die Sache hier als Regel statt als Beobachtung.
 */
class GesprochenTest {

    private val kachel = Quelltext.withoutComments("org/biglau/ui/BigTile.kt")

    @Test
    fun `der gesprochene Text kennt die Sichtbarkeit nicht`() {
        val ab = kachel.indexOf("val gesprochen =")
        assertTrue("Der gesprochene Text wird nicht mehr gebaut", ab > 0)
        val rumpf = kachel.substring(ab, minOf(kachel.length, ab + 400))
        listOf("zeigeLabel", "LocalHideCutLabels", "LabelPosition.HIDDEN").forEach { sicht ->
            assertTrue(
                "Der gesprochene Text haengt an $sicht - dann nimmt eine Einstellung fuer " +
                    "Augen dem Screenreader den Namen der Kachel.",
                sicht !in rumpf,
            )
        }
    }

    /**
     * Und er wird an **einer** Stelle gebaut, damit keine Kachel vergessen wird.
     *
     * Die Regel stand zuerst auf dem Namen `val gesprochen` und fiel am 04.09.2026 um, als
     * `BigRow` eine Variable gleichen Namens fuer etwas ganz anderes bekam. Beim Nachsehen
     * zeigte sich, dass sie ohnehin das Falsche zaehlte: `TileSpeech.describe` steht an
     * zwei Stellen, und das ist richtig so - der Startbildschirm legt den Zustand der
     * Kachel dazu (Empfang, Ladestand), die Kachel selbst den Zaehler an der Ecke.
     *
     * Worum es der Regel wirklich geht, ist der **Zaehler**: den darf kein Aufrufer
     * anhaengen muessen, sonst vergisst ihn einer. Also wird gezaehlt, wer `badge`
     * uebergibt.
     */
    @Test
    fun `nur die Kachel selbst baut ihn`() {
        val stellen = Quelltext.files()
            .filter { datei ->
                val text = datei.readText()
                val ab = text.indexOf("TileSpeech.describe(")
                ab >= 0 && "badge =" in text.substring(ab, minOf(text.length, ab + 300))
            }
            .map { it.name }
        assertEquals(
            "Der Zaehler an der Ecke wird an mehr als einer Stelle angehaengt. Dann gibt " +
                "es eine Kachel, die ihn vergisst.",
            listOf("BigTile.kt"),
            stellen,
        )
    }

    /** Die Sichtbarkeit haengt umgekehrt sehr wohl an den beiden Einstellungen. */
    @Test
    fun `die Sichtbarkeit haengt an beiden Einstellungen`() {
        val ab = kachel.indexOf("val zeigeLabel =")
        assertTrue("zeigeLabel gibt es nicht mehr", ab > 0)
        val rumpf = kachel.substring(ab, minOf(kachel.length, ab + 200))
        assertTrue(
            "Die Beschriftung richtet sich nicht mehr nach beiden Einstellungen: $rumpf",
            "LabelPosition.HIDDEN" in rumpf && "LocalHideCutLabels" in rumpf,
        )
    }
}
