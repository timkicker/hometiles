package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Farben eines Bildschirms kommen von seinem Thema, nicht vom Vorgabewert.
 *
 * `LocalBigPalette` ist ein `staticCompositionLocalOf { Dark }`. Wer ihn **vor** dem eigenen
 * `BigLauTheme`-Aufruf liest, bekommt nicht das eingestellte Thema, sondern diese Vorgabe —
 * und zwar lautlos: im dunklen Thema, das ohnehin Standard ist, sieht es richtig aus.
 *
 * Sechs Bildschirme machten genau das: Nachrichten, Notruf, Assistent, App-Liste, Einlesen
 * und **der Anrufbildschirm**. Aufgefallen ist es erst, als ich den Emulator auf „hell"
 * gestellt und die Bildschirmfotos ausgemessen habe: die Nachrichtenliste stand auf
 * #0A0A0A, während der Rest der App #E8EAEC trug, und ihre Überschrift war in der dunklen
 * Tinte des hellen Themas praktisch unsichtbar. Wer das Kontrast-Thema braucht — also
 * derjenige, der es am nötigsten hat —, bekam auf diesen sechs Bildschirmen etwas anderes,
 * als er eingestellt hatte.
 */
class PaletteScopeTest {

    private val quellen: List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    /** Zeile des ersten Aufrufs von [name] in dieser Datei, oder null. */
    private fun ersteZeile(zeilen: List<String>, treffer: (String) -> Boolean): Int? =
        zeilen.indexOfFirst(treffer).takeIf { it >= 0 }

    @Test
    fun `keine Palette wird vor ihrem Thema gelesen`() {
        val zuFrueh = mutableListOf<String>()
        quellen.forEach { datei ->
            val zeilen = datei.readLines()
            val thema = ersteZeile(zeilen) {
                it.contains("BigLauTheme(") && !it.contains("fun BigLauTheme")
            } ?: return@forEach
            val palette = ersteZeile(zeilen) { it.contains("LocalBigPalette.current") }
                ?: return@forEach
            if (palette < thema) {
                zuFrueh += "${datei.name}:${palette + 1} (Thema erst in Zeile ${thema + 1})"
            }
        }
        assertEquals(
            "Hier steht die Vorgabepalette statt des eingestellten Themas: $zuFrueh",
            emptyList<String>(),
            zuFrueh,
        )
    }

    @Test
    fun `jeder Bildschirm setzt ueberhaupt ein Thema`() {
        // Eine Activity ohne BigLauTheme malt durchgehend in der Vorgabe - derselbe Fehler,
        // nur vollstaendig.
        val ohne = quellen
            .filter { it.name.endsWith("Activity.kt") }
            .filter { it.readText().contains("setContent") }
            .filterNot { it.readText().contains("BigLauTheme(") }
            .map { it.name }
        assertEquals("Diese Bildschirme malen ohne Thema: $ohne", emptyList<String>(), ohne)
    }

    @Test
    fun `die Regel wuerde den alten Zustand finden`() {
        // Gegenprobe an einer erfundenen Datei.
        val kaputt = listOf(
            "val palette = LocalBigPalette.current",
            "BigLauTheme(theme) {",
        )
        val thema = kaputt.indexOfFirst { it.contains("BigLauTheme(") }
        val palette = kaputt.indexOfFirst { it.contains("LocalBigPalette.current") }
        assertTrue("die Reihenfolge muss auffallen", palette < thema)
    }
}
