package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Bildschirm, der eine Kachel bearbeitet, sagt **welche**.
 *
 * Der Kachel-Editor zeigte bis zum 04.09.2026 nur „Kachel bearbeiten" und „Belegt mit: …".
 * Wer eine von zwei leeren Kacheln antippte, sah nirgends, welche er erwischt hatte — und
 * der Fehler vom Vormittag, bei dem der Editor auf dem Startbildschirm statt im Ordner
 * aufging, wäre auf einen Blick sichtbar gewesen, wenn der Name des Screens dagestanden
 * hätte.
 *
 * Dasselbe in der Verschieben-Ansicht: dort heisst **jedes** Ziel „Zeile x, Spalte y". Ohne
 * eine Zeile darüber ist der ganze Bildschirm eine Liste abstrakter Plätze.
 *
 * Beide Sätze nennen Screen und Platz, und beide zählen ab eins — „Zeile 0" liest sich wie
 * ein Fehler.
 */
class WelcheKachelTest {

    private val quelle = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `der editor nennt screen und platz`() {
        val kopf = Quelltext.cut(quelle, from = "Mode.MENU -> MenuList(", to = "\n                        )")
        assertTrue(
            "Der Kachel-Editor sagt nicht, welche Kachel er bearbeitet:\n$kopf",
            "editor_where" in kopf,
        )
    }

    @Test
    fun `die verschieben-ansicht nennt die kachel`() {
        val kopf = Quelltext.cut(quelle, from = "Mode.MOVE -> MoveTargetList(", to = "\n                        )")
        assertTrue(
            "Die Verschieben-Ansicht sagt nicht, welche Kachel bewegt wird:\n$kopf",
            "move_which" in kopf,
        )
    }

    @Test
    fun `beide saetze zaehlen ab eins`() {
        listOf("editor_where", "move_which").forEach { name ->
            listOf("values-de", "values").forEach { sprache ->
                val text = Quelltext.textValue(name, sprache)
                assertTrue(
                    "$name braucht Screen, Zeile und Spalte (Sprache \"$sprache\"): $text",
                    "%1\$s" in text && "%2\$d" in text && "%3\$d" in text,
                )
            }
        }
        // Und im Quelltext steht ueberall die +1, nicht der Rasterindex.
        listOf("R.string.editor_where", "R.string.move_which").forEach { ruf ->
            val stelle = Quelltext.cut(quelle, from = ruf, atMost = 400)
            assertTrue(
                "Bei $ruf fehlt das Zaehlen ab eins - \"Zeile 0\" liest sich wie ein Fehler:\n$stelle",
                "y + 1" in stelle && "x + 1" in stelle,
            )
        }
    }
}
