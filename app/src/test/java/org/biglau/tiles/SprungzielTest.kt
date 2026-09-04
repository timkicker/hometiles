package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Ordner ist kein Sprungziel.
 *
 * Ein Ordner gehört seiner Kachel und legt sich als Überlagerung darüber — mit Namen oben
 * und der Zeile „Ordner schliessen" unten. Als Ziel einer Sprungkachel wird er zum
 * gewöhnlichen Screen: ohne diese Zeile, ohne Eintrag in der Screen-Liste, und daneben steht
 * weiter die Kachel, die ihn als Überlagerung öffnet. Zwei Wege zu derselben Sache, die
 * verschieden aussehen.
 *
 * Am 04.09.2026 am Emulator erzeugt und angesehen: der Ordner stand als Screen da, sieben
 * leere Plätze und eine Kamera, mit Kopfzeile und ohne Ausgang ausser der Zurück-Geste.
 *
 * `SwipeChain` filtert Ordner seit jeher heraus, `ScreenEdits.unreachable` ebenfalls und
 * sagt sogar warum. Der Bildschirm, auf dem man das Ziel **wählt**, war die einzige Stelle,
 * die es nicht tat — die Regel war da, nur nicht überall.
 */
class SprungzielTest {

    @Test
    fun `die auswahl bietet keinen ordner an`() {
        val picker = Quelltext.ausschnitt(
            Quelltext.ohneKommentare("org/biglau/tiles/TileEditorActivity.kt"),
            von = "private fun ScreenPicker(",
            bis = "\n}",
        )
        assertTrue(
            "Der Screen-Waehler bietet auch Ordner an:\n$picker",
            "!it.isFolder" in picker,
        )
    }

    @Test
    fun `alle drei listen von screens filtern gleich`() {
        // Wo Screens fuer den Nutzer aufgezaehlt werden, gehoeren Ordner nicht dazu. Drei
        // Stellen gibt es: das Wischen, die Liste der unerreichbaren Screens und die
        // Auswahl eines Sprungziels.
        val stellen = mapOf(
            "org/biglau/tiles/SwipeChain.kt" to "das Wischen",
            "org/biglau/tiles/ScreenEdits.kt" to "die unerreichbaren Screens",
            "org/biglau/tiles/TileEditorActivity.kt" to "die Auswahl des Sprungziels",
        )
        val ohne = stellen.filterKeys { pfad ->
            "isFolder" !in Quelltext.ohneKommentare(pfad)
        }
        assertTrue(
            "Diese Aufzaehlung von Screens unterscheidet Ordner nicht: " +
                ohne.values.joinToString(", "),
            ohne.isEmpty(),
        )
    }
}
