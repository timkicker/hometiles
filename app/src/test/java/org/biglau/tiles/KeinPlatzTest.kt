package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wo nichts geht, steht wenigstens, wie es doch geht.
 *
 * „Grösse ändern" auf einer Kachel, deren Nachbarn alle belegt sind, könnte eine leere Liste
 * zeigen — ein Bildschirm, auf dem nichts angeboten wird und niemand sagt warum. Er tut es
 * nicht, und das ist gut genug, um festgehalten zu werden: er nennt den Grund („die Kachel
 * ist schon ein Feld gross, und ringsum ist alles belegt") und **zwei** Auswege — einen
 * Nachbarn frei machen oder dem Screen mehr Felder geben.
 *
 * Am 04.09.2026 am Emulator gesehen, mit der Kontakte-Kachel mitten im vollen Raster. Diese
 * Regel hat keinen Fehler gefunden; sie hält fest, was schon richtig ist. Ein Satz, der
 * einen Weg nennt, verliert ihn beim nächsten Kürzen leicht.
 */
class KeinPlatzTest {

    @Test
    fun `die meldung nennt einen ausweg`() {
        listOf(
            "values-de" to listOf("Nachbarn", "Einstellungen"),
            "values" to listOf("neighbour", "settings"),
        ).forEach { (sprache, worte) ->
            val text = Quelltext.textValue("resize_no_room", sprache).lowercase()
            worte.forEach { wort ->
                assertTrue(
                    "Die Meldung sagt nicht, wie es doch ginge - \"$wort\" fehlt " +
                        "(Sprache \"$sprache\"): $text",
                    wort.lowercase() in text,
                )
            }
        }
    }

    @Test
    fun `die meldung steht dort wo nichts mehr geht`() {
        val quelle = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")
        assertTrue(
            "resize_no_room wird nirgends gezeigt - dann ist der Bildschirm bei vollem " +
                "Raster leer",
            "resize_no_room" in quelle,
        )
    }
}
