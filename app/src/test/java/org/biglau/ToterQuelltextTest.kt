package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was niemand ruft, wird gelöscht.
 *
 * Am 3.9.2026 fanden sich drei solche Stellen: `WidgetHostController.infoFor`,
 * `dpToPx` (mit einem KDoc, das behauptete, es werde „an einer Stelle" gebraucht — es wurde
 * an keiner) und `SosCountdown.DEFAULT_SECONDS`, eine benannte Zahl neben derselben Zahl
 * ohne Namen im Modell. Die dritte war die schlimmste: sie sah wie die eine Wahrheit aus
 * und war nur ein Kommentar mit Typ.
 *
 * Toter Quelltext ist nicht bloss Ballast. Er wird gelesen, bei jeder Suche mitgefunden und
 * beim Umbauen mitgeschleppt — und wer ihn ändert, prüft nichts, weil ihn nichts benutzt.
 *
 * **Was diese Regel nicht sieht** (und bewusst nicht): `private` und `internal` (das meldet
 * der Übersetzer selbst), `override`, `@Composable` (Vorschauen rufen sie nicht sichtbar
 * auf) und Namen unter vier Zeichen (zu viele Zufallstreffer).
 */
class ToterQuelltextTest {

    /** Name → warum es ihn trotzdem geben darf. */
    private val darfBleiben = mapOf<String, String>()

    private fun deklarationen(): Map<String, List<File>> {
        val treffer = mutableMapOf<String, MutableList<File>>()
        Quelltext.files().forEach { datei ->
            datei.readLines().forEach { zeile ->
                if (zeile.trimStart().startsWith("private ") ||
                    zeile.trimStart().startsWith("internal ") ||
                    zeile.trimStart().startsWith("override ")
                ) {
                    return@forEach
                }
                Regex("""^ {0,4}(?:fun|val|const val) (\w{4,})""").find(zeile)?.let {
                    treffer.getOrPut(it.groupValues[1]) { mutableListOf() }.add(datei)
                }
            }
        }
        return treffer
    }

    /**
     * Wie oft jeder Bezeichner im ganzen Quelltext vorkommt - **einmal** gezaehlt.
     *
     * Die erste Fassung suchte je Deklaration mit einem eigenen `Regex` durch alle Dateien:
     * rund fuenfhundert Namen mal hundertvierzig Dateien. Am 3.9.2026 gemessen: **55
     * Sekunden**, mehr als die Haelfte des gesamten Testlaufs von `:app`. Eine Regel, die
     * bei jedem Bau eine Minute kostet, wird irgendwann abgeschaltet - und dann prueft sie
     * gar nichts mehr.
     *
     * Jetzt wird der Quelltext **einmal** in Bezeichner zerlegt und gezaehlt; die Pruefung
     * ist danach ein Nachschlagen.
     */
    private fun haeufigkeiten(): Map<String, Int> {
        val zaehler = mutableMapOf<String, Int>()
        val wort = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
        (Quelltext.files() + Quelltext.testFiles()).forEach { datei ->
            wort.findAll(datei.readText()).forEach { treffer ->
                zaehler[treffer.value] = (zaehler[treffer.value] ?: 0) + 1
            }
        }
        return zaehler
    }

    @Test
    fun `jede oeffentliche Stelle hat einen Aufrufer`() {
        val wieOft = haeufigkeiten()
        val ohne = deklarationen()
            .filterKeys { it !in darfBleiben }
            .filter { (name, orte) -> (wieOft[name] ?: 0) <= orte.size }
            .map { (name, orte) -> "$name (${orte.joinToString { o -> o.name }})" }
            .sorted()

        assertEquals(
            "Diese Stelle ruft niemand. Löschen - oder, wenn es sie geben muss, mit Grund " +
                "in die Liste in ToterQuelltextTest. Eine benannte Zahl ohne Aufrufer neben " +
                "derselben Zahl ohne Namen ist der gefährlichste Fall: sie sieht wie die " +
                "eine Wahrheit aus.",
            emptyList<String>(),
            ohne,
        )
    }

    @Test
    fun `die Regel findet ueberhaupt etwas`() {
        val zahl = deklarationen().size
        assertTrue("nur $zahl öffentliche Stellen gefunden - sucht die Regel noch?", zahl >= 100)
    }
}
