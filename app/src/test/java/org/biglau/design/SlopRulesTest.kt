package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Regeln aus PLAN.md 3.7, soweit sie sich am Quelltext prüfen lassen.
 *
 * Der Plan sagt: „Weil an dieser App maschinell weitergebaut wird, stehen die Verbote hier
 * als prüfbare Regeln und nicht als Geschmacksfrage." Eine Regel, die niemand prüft, ist
 * nach zwanzig Commits keine Regel mehr - beim ersten Durchgang standen bereits sechs
 * Abstände neben der Skala, die meisten davon frisch dazugekommen.
 */
class SlopRulesTest {

    private val quellen: List<File> =
        File("src/main/java/org/biglau").walkTopDown().filter { it.extension == "kt" }.toList()

    private fun ohneThema(): List<File> = quellen.filterNot { it.path.contains("ui/theme") }

    private fun verstoesse(muster: Regex, dateien: List<File> = quellen): List<String> =
        dateien.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                if (muster.containsMatchIn(zeile)) "${datei.name}:${index + 1}: ${zeile.trim()}" else null
            }
        }

    @Test
    fun `Farben kommen aus dem Token-System`() {
        // Ein Hexwert im Composable ist ein Fehler; die Palette steht in ui/theme.
        // Ausnahme: der Notfall-Bildschirm, der ohne Konfiguration und ohne Palette
        // auskommen muss - die könnte ja gerade das Problem sein.
        val erlaubt = quellen.filterNot {
            it.path.contains("ui/theme") || it.name == "EmergencyScreen.kt"
        }
        // Der Abdunkler unter dem Kontaktfoto ist schwarz, und zwar in jedem Thema: er
        // verdunkelt ein Foto, damit die weisse Beschriftung darauf lesbar bleibt. Ein
        // Token waere hier falsch - im hellen Thema wuerde er die Schrift verschlucken.
        val abdunklerZeile = Regex("""1f to Color\.Black\.copy""")
        // Sowohl Hexwerte als auch die eingebauten Konstanten: ein Color.Black hinter der
        // Ladeanzeige war im hellen Thema ein Fremdkoerper.
        assertEquals(
            emptyList<String>(),
            verstoesse(Regex("""Color\(0x|Color\.(Black|White|Red|Green|Blue|Gray|Yellow|Magenta|Cyan)"""), erlaubt)
                .filterNot { abdunklerZeile.containsMatchIn(it) },
        )
    }

    @Test
    fun `die App kann gar nicht ins Netz`() {
        // Die README verspricht "BigLau sendet nichts". Ohne INTERNET-Berechtigung ist das
        // keine Zusage, sondern eine Tatsache - und sie soll eine bleiben. Auf dem Telefon
        // des Nutzers steckt eine SIM mit begrenztem Datenvolumen.
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertEquals(false, manifest.contains("android.permission.INTERNET"))
        assertEquals(false, manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
    }

    @Test
    fun `keine Schlagschatten`() {
        assertEquals(emptyList<String>(), verstoesse(Regex("""\.shadow\(|shadowElevation|defaultElevation""")))
    }

    @Test
    fun `nur ein einziger Verlauf, und der macht Text lesbar`() {
        val gefunden = verstoesse(Regex("""Brush\.\w+Gradient"""))
        assertEquals("nur der Abdunkler unter dem Kontaktfoto", 1, gefunden.size)
        assertEquals(true, gefunden.single().startsWith("BigTile.kt"))
    }

    @Test
    fun `Flaechen haben genau einen Eckenradius`() {
        // Runde Formen sind erlaubt, aber nur für Anzeigen: Punkte, Abzeichen, Balken,
        // Griffe. Für Flächen - Kacheln, Zeilen, Felder, Knöpfe - gilt 12 dp, sonst
        // nichts. Ein zweiter Radius auf einer Fläche fällt sofort als Flickwerk auf.
        val radien = quellen.flatMap { datei ->
            Regex("""RoundedCornerShape\((\d+)\.dp\)""").findAll(datei.readText())
                .map { it.groupValues[1].toInt() }
        }.toSet()
        assertEquals(setOf(12), radien)
    }

    @Test
    fun `Abstaende kommen aus der Skala`() {
        // Nur Abstände, nicht Größen: eine Symbolgröße von 36 dp ist kein Abstand.
        val skala = setOf(0, 4, 8, 12, 16, 24, 32, 48)
        val muster = Regex("""(?:padding|spacedBy)\(([^)]*)\)""")
        val zahl = Regex("""(\d+)\.dp""")
        val schlechte = quellen.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                val treffer = muster.findAll(zeile)
                    .flatMap { zahl.findAll(it.groupValues[1]) }
                    .map { it.groupValues[1].toInt() }
                    .filterNot { it in skala }
                    .toList()
                if (treffer.isEmpty()) null else "${datei.name}:${index + 1}: $treffer"
            }
        }
        assertEquals(emptyList<String>(), schlechte)
    }

    @Test
    fun `keine Emoji als Symbole`() {
        // Ein Emoji kommt aus der Emoji-Schrift des Systems - eine zweite Schriftart in
        // fremder Farbe mitten in der eigenen Oberfläche. Das Blitzzeichen der Ladeanzeige
        // war genau so eines. Symbole kommen aus einem Set, einfarbig, getönt.
        val emoji = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u26A0-\u27BF\u2B00-\u2BFF\uFE0F]""")
        assertEquals(emptyList<String>(), verstoesse(emoji))
    }

    @Test
    fun `auch die Texte kommen ohne Emoji aus`() {
        val emoji = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u26A0-\u27BF\u2B00-\u2BFF\uFE0F]""")
        val treffer = listOf("values", "values-de").flatMap { verzeichnis ->
            File("src/main/res/$verzeichnis/strings.xml").readLines()
                .mapIndexedNotNull { index, zeile ->
                    if (emoji.containsMatchIn(zeile)) "$verzeichnis:${index + 1}" else null
                }
        }
        assertEquals(emptyList<String>(), treffer)
    }

    @Test
    fun `in einer Liste taucht kein Symbol zweimal auf`() {
        // Ein Symbol trägt Wissen oder es gehört weg. Zweimal dasselbe Haus - einmal für
        // "Screens", einmal für "Als Startbildschirm verwenden" - trägt keines und stiftet
        // Verwechslung. Geprüft wird pro Composable-Funktion, denn nur dort stehen die
        // Zeilen untereinander und werden verglichen.
        val doppelte = mutableListOf<String>()
        quellen.forEach { datei ->
            var funktion = datei.name
            val gesehen = mutableMapOf<String, Int>()
            datei.readLines().forEach { zeile ->
                Regex("""(?:private )?fun (\w+)\(""").find(zeile)?.let {
                    funktion = "${datei.name}#${it.groupValues[1]}"
                    gesehen.clear()
                }
                Regex("""icon = Icons\.(?:Filled|AutoMirrored\.Filled)\.(\w+)""").find(zeile)?.let {
                    val name = it.groupValues[1]
                    gesehen[name] = (gesehen[name] ?: 0) + 1
                    if (gesehen.getValue(name) == 2) doppelte += "$funktion: $name"
                }
            }
        }
        assertEquals(emptyList<String>(), doppelte)
    }

    @Test
    fun `nur Regular und Bold`() {
        val schnitte = quellen.flatMap { datei ->
            Regex("""FontWeight\.(\w+)""").findAll(datei.readText()).map { it.groupValues[1] }
        }.toSet()
        assertEquals(emptySet<String>(), schnitte - setOf("Normal", "Bold"))
    }
}
