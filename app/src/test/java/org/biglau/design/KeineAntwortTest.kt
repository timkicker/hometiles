package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keine Antwort ist keine leere Antwort.
 *
 * `ShortcutRepository` schrieb es sich selbst in den Kopf: eine leere Liste zeigen sei
 * falsch, weil niemand ihr ansieht, ob die App keine Verknuepfungen hat oder wir nicht
 * fragen durften. Fuer die fehlende Berechtigung stimmte das auch. Eine Zeile weiter
 * unten wurde ein fehlgeschlagenes `getShortcuts` dann doch zu `emptyList()` - und der
 * Bildschirm sagte „Diese App bietet keine Verknuepfungen an", ohne dass jemand sie
 * gefragt hatte. Die Sorgfalt stand an der einen Tuer und fehlte an der anderen.
 *
 * Die Regel haelt beide Haelften fest: der Unterschied darf im Typ nicht verschwinden,
 * und er muss in der Oberflaeche als zwei verschiedene Saetze ankommen.
 */
class KeineAntwortTest {

    private val repository = Quelltext.datei("org/biglau/shortcuts/ShortcutRepository.kt")
    private val editor = Quelltext.datei("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `forPackage unterscheidet Fehlschlag und leere Liste im Typ`() {
        val zeile = repository.readLines().firstOrNull { "fun forPackage(" in it }
        assertTrue("forPackage gibt es nicht mehr - wandert die Regel mit?", zeile != null)
        assertTrue(
            "forPackage liefert wieder eine nackte Liste: $zeile - dann ist ein Fehlschlag " +
                "von einer leeren Antwort nicht mehr zu unterscheiden.",
            "ShortcutAnswer" in zeile!!,
        )
    }

    @Test
    fun `ein fehlgeschlagener Aufruf wird nicht zur leeren Liste`() {
        val verschmolzen = repository.readLines().withIndex()
            .filter { (_, z) -> "getShortcuts" in z && !z.trim().startsWith("*") }
            .filter { (_, z) -> ".orEmpty()" in z || "getOrDefault(emptyList" in z }
            .map { it.index + 1 }
        assertEquals(
            "Hier wird ein fehlgeschlagenes getShortcuts zu einer leeren Liste. Danach ist " +
                "nicht mehr zu sehen, ob die App keine Verknuepfungen hat oder ob wir " +
                "keine Antwort bekommen haben.",
            emptyList<Int>(),
            verschmolzen,
        )
    }

    @Test
    fun `die Oberflaeche sagt fuer beide Zustaende etwas anderes`() {
        val quelle = editor.readText()
        val anfang = quelle.indexOf("private fun ShortcutList(")
        assertTrue("ShortcutList gibt es nicht mehr", anfang > 0)
        val rumpf = quelle.substring(anfang).substringBefore("\n@Composable")

        assertTrue(
            "ShortcutList kennt den Fehlschlag nicht mehr - dann trifft der Satz ueber die " +
                "App auch dann zu, wenn niemand sie gefragt hat.",
            "ShortcutAnswer.Failed" in rumpf,
        )
        assertTrue(
            "Beide Zustaende zeigen denselben Satz.",
            "R.string.shortcut_none" in rumpf && "R.string.shortcut_unreadable" in rumpf,
        )
        // Ein Satz, der ein Problem nennt, braucht einen Ausweg - und der steht hier im
        // selben Zweig, damit er auch beim Fehlschlag mitkommt.
        assertTrue(
            "Der Fehlschlag endet in einer Sackgasse: kein Knopf zu einer anderen App.",
            rumpf.indexOf("R.string.shortcut_other_app") > rumpf.indexOf("R.string.shortcut_unreadable"),
        )
    }

    @Test
    fun `beide Saetze gibt es in beiden Sprachen`() {
        // Je Sprache genuegt eine Datei, die den Satz fuehrt - welches Modul das ist,
        // geht die Regel nichts an, umziehen darf er.
        val fehlt = listOf("shortcut_none", "shortcut_unreadable").flatMap { name ->
            listOf("values", "values-de").filterNot { sprache ->
                Quelltext.texte(sprache).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertEquals(
            "Ein Satz fehlt in einer Sprache. Deutsch ist hier die Sprache des Geraets, " +
                "nicht die Ausweichfassung.",
            emptyList<String>(),
            fehlt,
        )
    }
}
