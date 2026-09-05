package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Weg aus einer Meldung führt an die Stelle, die gemeint war.
 *
 * Tippt jemand auf eine Kachel, deren App nicht mehr da ist, sagt BigLau das und macht den
 * Kachel-Editor auf — der Weg statt der Wegbeschreibung. Nur muss der Editor dann auch auf
 * **dieser** Kachel stehen.
 *
 * Am 04.09.2026 am Jelly 2 nachgestellt: eine Kachel auf ein Paket, das es nicht gibt, im
 * Ordner „Mehr" auf Feld (1,2). Der Editor ging auf — und sagte „Belegt mit: Mehr", also
 * Feld (1,2) des **Startbildschirms**: die Kachel, die den Ordner aufmacht. Ein Ordner legt
 * sich über den Startbildschirm, ohne den Screen zu wechseln, und `currentScreenId()` weiss
 * davon nichts. Wer der Einladung folgte, hätte seinen Ordner überschrieben statt der
 * kaputten Kachel — eine Meldung, die einen Schaden anrichtet, wo sie einen beheben wollte.
 *
 * Die Zellen im Editor kommen alle aus derselben Ecke: `zeigeKachel` bekommt den gezeigten
 * Screen als `gezeigt` herein und ist einmal für den Screen und einmal für den Ordner
 * darüber im Einsatz. Wer von dort aus eine Kachel benennt, muss `gezeigt.id` nehmen.
 */
class KachelWegTest {

    private val haupt = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    @Test
    fun `kein editor auf dem screen der gerade nicht zu sehen ist`() {
        val falsch = Regex("TileEditorActivity\\.intent\\([^)]*currentScreenId\\(\\)")
            .findAll(haupt).map { it.value }.toList()
        assertEquals(
            "Der Kachel-Editor wird mit currentScreenId() aufgemacht. Ein offener Ordner " +
                "wechselt den Screen nicht — die Kachel liegt dann im Ordner, der Editor " +
                "aber auf dem Startbildschirm. Der gezeigte Screen heisst in der " +
                "Komposition `gezeigt.id`.",
            emptyList<String>(),
            falsch,
        )
    }

    @Test
    fun `wer eine kachel merkt merkt auch ihren screen`() {
        val tipp = Quelltext.cut(
            haupt,
            from = "private data class GesperrterTipp(",
            to = ")",
        )
        assertTrue(
            "GesperrterTipp haelt x und y, aber nicht den Screen. Nach der PIN weiss " +
                "niemand mehr, auf welchem Screen die Kachel lag: $tipp",
            "screenId" in tipp,
        )
    }

    @Test
    fun `starten bekommt den screen gesagt`() {
        val zeichen = Quelltext.cut(haupt, from = "private fun starten(", to = "{")
        assertTrue(
            "starten() nimmt keinen screenId — dann muss es sich den Screen selbst " +
                "ausdenken, und das ging schon einmal schief: $zeichen",
            "screenId: String" in zeichen,
        )
    }
}
