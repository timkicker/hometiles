package org.biglau.settings

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kein Schalter im Modell, den niemand liest.
 *
 * Vier Felder in `Behaviour` und `Security` standen seit dem ersten Tag da und wurden
 * nirgends benutzt. Ein toter Schalter ist schlimmer als ein fehlender: er steht in der
 * gesicherten Konfiguration, sieht nach einer Zusage aus und tut nichts. Aufgefallen beim
 * Abgleich von `PLAN.md` 4.4 gegen das Gebaute.
 */
class DeadSettingsTest {


    /**
     * Felder, deren Umsetzung noch aussteht. Wer eines umsetzt, streicht es hier - und wer
     * ein neues Feld anlegt, ohne es zu benutzen, bekommt hier einen roten Test.
     */
    private val nochNichtUmgesetzt = emptySet<String>()

    /**
     * Zählt **Lesezugriffe**, nicht bloße Namensgleichheit.
     *
     * Erste Fassung suchte den bloßen Namen - und `longPress` galt als benutzt, weil es
     * eine Funktion `HapticFeedback.longPress(...)` gibt. Ein Test, der so danebengreift,
     * gibt falsche Sicherheit; genau das, wogegen er antreten soll. Gezählt wird deshalb
     * nur ein Zugriff auf eine Eigenschaft: ein Punkt davor, keine Klammer danach.
     */
    private fun benutztAusserhalbDesModells(feld: String): Int {
        val zugriff = Regex("""\.$feld\b(?!\s*\()""")
        val zuweisung = Regex("""\b$feld\s*=""")
        return Quelltext.dateien()
            .filter { it.name != "Model.kt" }
            .sumOf { datei ->
                datei.readLines().count { zugriff.containsMatchIn(it) || zuweisung.containsMatchIn(it) }
            }
    }

    /**
     * Ein Feld darf auch nur im Modell selbst gelesen werden - aber dann muss die
     * Eigenschaft, die es liest, draußen ankommen. `hapticFeedback` ist so ein Fall: es
     * wird nur noch von `haptics` befragt, und `haptics` ist das, was die App verwendet.
     * Ohne diese zweite Frage könnte ein totes Feld sich hinter einem toten Getter
     * verstecken - zwei Leichen, die sich gegenseitig am Leben halten.
     */
    private fun lebendig(feld: String): Boolean {
        if (benutztAusserhalbDesModells(feld) > 0) return true
        return ableitungen(feld).any { benutztAusserhalbDesModells(it) > 0 }
    }

    /** Namen der Eigenschaften und Funktionen in Model.kt, deren Rumpf [feld] liest. */
    private fun ableitungen(feld: String): List<String> {
        val text = Quelltext.datei("org/biglau/data/Model.kt").readText()
        val kopf = Regex("""(?:val|fun) (\w+)[:(]""")
        val treffer = mutableListOf<String>()
        var name: String? = null
        for (zeile in text.lines()) {
            kopf.find(zeile.trimStart())?.let { name = it.groupValues[1] }
            if (name != null && name != feld && Regex("""\b$feld\b""").containsMatchIn(zeile)) {
                treffer += name!!
            }
        }
        return treffer.distinct()
    }

    /**
     * Zaehlt **Schreibzugriffe**: irgendwo muss ein Editor das Feld setzen.
     *
     * Das Gegenstueck zur Frage oben, und ein eigener Fehler. `Button.longPress` wurde
     * geschrieben und nie gelesen - man konnte es setzen, und es passierte nichts.
     * `swipeOrder` war andersherum: es wurde gelesen und von keiner Oberflaeche je
     * geschrieben, blieb also fuer immer leer. Beides sind Zusagen ohne Wirkung, nur von
     * verschiedenen Seiten.
     */
    private fun wirdGesetzt(feld: String): Boolean {
        val zuweisung = Regex("""\b$feld\s*=\s*[^=]""")
        return Quelltext.dateien()
            .filter { it.name != "Model.kt" && it.name != "Defaults.kt" }
            .any { datei -> datei.readLines().any { zuweisung.containsMatchIn(it) } }
    }

    /**
     * Keine Einstellungstraeger: das sind Daten, keine Schalter.
     *
     * Der Unterschied ist nicht kosmetisch. Eine Einstellung wird geaendert - deshalb muss
     * es einen Weg geben, sie zu aendern. Eine Nutzlast wird gebaut: `App("com.x", "Main")`
     * entsteht, wenn jemand eine App auf eine Kachel legt, und ihre Felder werden nie
     * einzeln umgestellt. Sie hier mitzupruefen hiesse, eine Regel aufzustellen, die
     * niemand einhalten kann.
     */
    private val nutzlast = setOf(
        "Cell", "Screen", "LaunchableApp", "SpeedDialTarget",
        // ButtonAction und seine Varianten - der Inhalt einer Kachel.
        "App", "Contact", "GoToScreen", "Folder", "Link", "Shortcut", "Widget", "Action", "Solid",
    )

    /** Alle Klassen, die Einstellungen tragen - aus Model.kt gelesen, nicht von Hand gepflegt. */
    private fun konfigKlassen(): List<String> =
        Regex("""data class (\w+)\(""")
            .findAll(Quelltext.datei("org/biglau/data/Model.kt").readText())
            .map { it.groupValues[1] }
            .filterNot { it in nutzlast }
            .toList()

    /**
     * Kein Feld in keiner Konfigurationsklasse, das niemand liest.
     *
     * Vorher gab es das je Klasse einzeln, und drei Klassen fehlten - `SosConfig`,
     * `PhoneConfig`, `ContactsConfig`. In `SosConfig` lag `callAfterSms`: ein Feld, das
     * einen Anruf nach dem Notruf ausgeloest haette, von niemandem gelesen. Die Liste der
     * Klassen kommt jetzt aus dem Modell selbst, damit eine neue Klasse nicht wieder
     * unbemerkt durchrutscht.
     */
    @Test
    fun `kein Feld bleibt ungelesen`() {
        val tot = mutableListOf<String>()
        for (klasse in konfigKlassen()) {
            val felder = Regex("""val (\w+): [\w?<>., ]+""")
                .findAll(modellAbschnitt("data class $klasse("))
                .map { it.groupValues[1] }
            for (feld in felder) {
                if (feld !in nochNichtUmgesetzt && !lebendig(feld)) tot += "$klasse.$feld"
            }
        }
        assertEquals(emptyList<String>(), tot)
    }

    /**
     * Und die Gegenfrage: kann der Nutzer das Feld ueberhaupt aendern?
     *
     * `searchNumbers` und `favouritesFirst` wurden gelesen und von keiner Oberflaeche je
     * gesetzt - sie standen auf ihrer Vorgabe und blieben dort. Die Lesepruefung sieht so
     * etwas nicht; sie sind ja gelesen.
     *
     * Gezaehlt wird eine Zuweisung **innerhalb eines `copy(`** - so wird in dieser App jede
     * Einstellung geschrieben. Ein benannter Parameter in einem Composable-Aufruf sieht
     * genauso aus wie eine Zuweisung und hat mich beim ersten Versuch getaeuscht; `copy(`
     * davor macht den Unterschied.
     */
    private fun einstellbar(feld: String): Boolean {
        val imCopy = Regex("""copy\((?:[^()]|\([^()]*\))*\b$feld\s*=""", RegexOption.DOT_MATCHES_ALL)
        val direkt = Quelltext.dateien()
            .filter { it.name != "Model.kt" }
            .any { imCopy.containsMatchIn(it.readText()) }
        if (direkt) return true
        // Oder ueber einen Setzer im Modell - `withIcons`, `withClock` und Verwandte -,
        // der draussen benutzt wird.
        return ableitungen(feld).any { benutztAusserhalbDesModells(it) > 0 }
    }

    /**
     * Felder, die niemand von Hand setzt, weil sie die App selbst pflegt.
     *
     * `recent` fuehrt Buch ueber zuletzt gestartete Apps, `version` ist die Formatnummer,
     * `pin` wird gehasht statt zugewiesen, `speedDial` geht ueber SpeedDial.assign. Sie
     * gehoeren nicht in die Oberflaeche - aber sie gehoeren benannt, sonst waere die Regel
     * eine Regel mit stiller Ausnahme.
     */
    private val vonDerAppGepflegt = setOf("recent", "version", "pin", "speedDial", "screens", "swipeOrder")

    @Test
    fun `jede Einstellung ist auch einstellbar`() {
        val fest = mutableListOf<String>()
        for (klasse in konfigKlassen()) {
            val felder = Regex("""val (\w+): [\w?<>., ]+""")
                .findAll(modellAbschnitt("data class $klasse("))
                .map { it.groupValues[1] }
            for (feld in felder) {
                if (feld in vonDerAppGepflegt || feld in nochNichtUmgesetzt) continue
                if (!einstellbar(feld)) fest += "$klasse.$feld"
            }
        }
        assertEquals(emptyList<String>(), fest)
    }

    @Test
    fun `die Ausnahmeliste bleibt kurz`() {
        // Sie ist eine Merkliste, keine Ablage. Wächst sie, ist der Plan weiter weg vom
        // Gebauten als gedacht.
        assertEquals(true, nochNichtUmgesetzt.size <= 3)
    }

    /**
     * Der Kopf einer data class, bis zur **zugehoerigen** schliessenden Klammer.
     *
     * Erste Fassung nahm die erste schliessende Klammer ueberhaupt - und
     * `listOf(Defaults.mainScreen())` in der Vorgabe von `screens` schloss den Abschnitt
     * nach zwei Feldern. Der Test lief ins Leere und meldete alles in Ordnung. Genau die
     * falsche Sicherheit, gegen die er antritt; jetzt wird die Klammertiefe gezaehlt.
     */
    private fun modellAbschnitt(kopf: String): String {
        val text = Quelltext.datei("org/biglau/data/Model.kt").readText()
        val start = text.indexOf(kopf)
        require(start >= 0) { "Kein Abschnitt $kopf in Model.kt" }
        var tiefe = 0
        for (i in start + kopf.length - 1 until text.length) {
            when (text[i]) {
                '(' -> tiefe++
                ')' -> {
                    tiefe--
                    if (tiefe == 0) return text.substring(start, i)
                }
            }
        }
        error("Klammer zu $kopf nicht gefunden")
    }
}
