package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was gewaehlt ist, steht nicht nur in der Farbe.
 *
 * Der Farbwaehler hatte es schon richtig und die Messung dazu aufgeschrieben: die reine
 * `selected`-Eigenschaft kommt in der Bedienungshilfen-Schnittstelle nicht an, der Zustand
 * muss zusaetzlich in den Namen. Die Symbolwahl daneben hatte es nicht - dort trug allein
 * die Flaechenfarbe die Auswahl, und BigLau ist fuer Augen gebaut, die Farben schlecht
 * unterscheiden.
 *
 * Die Regel haengt am Werkzeug, nicht an den Listen: solange `BigRow` die Auswahl ansagt,
 * sagt jede Liste sie an, die `selected` benutzt.
 */
class AuswahlAnsageTest {

    private val zeile = Quelltext.ohneKommentare("org/biglau/ui/BigRow.kt")
    private val editor = Quelltext.ohneKommentare("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `eine gewaehlte Zeile sagt es und faerbt sich nicht nur`() {
        assertTrue(
            "BigRow kennt kein selected - dann traegt die Auswahl in jeder Liste wieder " +
                "allein die Farbe.",
            zeile.contains("selected: Boolean = false"),
        )
        // Nicht auf den Wortlaut: die erste Fassung dieser Regel stand auf
        // "if (selected) palette.surfaceAccent" und fiel um, als der Schalter dazukam -
        // wieder eine Regel, die an der Form hing statt an der Sache.
        val flaeche = zeile.lines().dropWhile { !it.contains("val paint =") }.take(2).joinToString(" ")
        assertTrue(
            "BigRow faerbt bei selected nicht selbst; die Listen muessten den " +
                "Farbwechsel weiter selbst hinschreiben und koennten die Ansage " +
                "vergessen: $flaeche",
            "selected" in flaeche && "surfaceAccent" in flaeche,
        )
        assertTrue(
            "BigRow setzt bei selected keine Ansage. Gemessen am Knotenabzug des Geraets: " +
                "die selected-Eigenschaft allein kommt beim Vorlesen nicht an.",
            zeile.contains("this.selected = true") && zeile.contains("a11y_chosen"),
        )
    }

    @Test
    fun `die Ansage liegt dort, wo BigRow sie lesen kann`() {
        val gefunden = (Quelltext.texte("values") + Quelltext.texte("values-de"))
            .filter { it.readText().contains("name=\"a11y_chosen\"") }
        assertTrue(
            "a11y_chosen fehlt in einer Sprache oder liegt nicht bei :core:ui, wo BigRow " +
                "steht: ${gefunden.map { it.path }}",
            gefunden.size == 2 && gefunden.all { it.path.contains("core/ui") },
        )
    }

    @Test
    fun `das Symbolgitter zeigt und sagt, welches Symbol gilt`() {
        // Bis zur naechsten Funktion, nicht bis zu einer bestimmten: unter IconPicker
        // steht der Farbtonwaehler, und der bringt dieselben Woerter mit. Ohne die Grenze
        // waere die Regel gruen geblieben, auch wenn die Symbolwahl gar nichts sagt.
        val gitter = Quelltext.ausschnitt(editor, "fun IconPicker(", "\nprivate fun ")
        assertTrue(
            "Die Symbolwahl sagt nicht, welches Symbol gewaehlt ist - beim Vorlesen ist " +
                "das gewaehlte Feld eines von sechsundfuenfzig gleichen.",
            gitter.contains("a11y_chosen") && gitter.contains("this.selected = true"),
        )
        assertTrue(
            "Das gewaehlte Symbol ist nur an der Flaechenfarbe zu erkennen. Wer Farben " +
                "schlecht unterscheidet, sieht die Auswahl nicht.",
            gitter.contains("Modifier.border("),
        )
    }

    /**
     * Ein Schalter sagt "an" und "aus", nicht "ausgewaehlt".
     *
     * Das ist nicht dieselbe Auskunft: eine Auswahl ist eine von mehreren, ein Schalter
     * steht so oder so. Beim Vorlesen heisst das im System "an" und "aus", und BigLau soll
     * nicht seine eigene Sprache dafuer erfinden.
     */
    @Test
    fun `ein Schalter sagt seinen Zustand`() {
        assertTrue(
            "BigRow kennt keinen Schalter - dann muessten Schalter sich als " +
                "\"ausgewaehlt\" ausgeben, und das stimmt nicht.",
            zeile.contains("checked: Boolean? = null"),
        )
        assertTrue(
            "Der Schalter sagt seinen Zustand nicht an.",
            zeile.contains("a11y_on") && zeile.contains("a11y_off"),
        )
        val fehlend = (Quelltext.texte("values") + Quelltext.texte("values-de")).filterNot {
            val t = it.readText()
            "name=\"a11y_on\"" in t && "name=\"a11y_off\"" in t
        }
        assertTrue(
            "an/aus fehlt in einer Sprache: ${fehlend.map { it.path }}",
            fehlend.none { it.path.contains("core/ui") },
        )
    }

    /**
     * Ein Haken ist keine Ansage.
     *
     * Meine eigene Zaehlung von heute Nacht suchte nach der Akzentflaeche
     * (`surfaceAccent else surfaceDefault`) - und uebersah damit alle Listen, in denen die
     * Flaeche schon etwas anderes bedeutet: die Themenwahl ist in ihren eigenen Farben
     * gemalt, die Hintergrundfarben eines Screens auch, und die Symbolgroesse zeigt das
     * Symbol in der Groesse, um die es geht. Sie alle setzen stattdessen ein Haekchen - und
     * sagten nichts. Dazu kamen acht Faelle, in denen die Faerbung ueber mehrere Zeilen
     * geschrieben war und meine einzeilige Suche sie nicht sah.
     *
     * Deshalb diese Regel: wer einen Haken setzt, sagt es auch.
     */
    @Test
    fun `wo ein Haken steht, steht auch eine Ansage`() {
        val stumm = Quelltext.dateien().flatMap { datei ->
            val text = datei.readText()
            Regex("""BigRow\(""").findAll(text).mapNotNull { treffer ->
                // `mehrfach`, weil hier absichtlich am **ersten** Vorkommen geschnitten
                // wird: der Abschnitt beginnt genau an diesem Aufruf, die spaeteren
                // stehen dahinter.
                val block = Quelltext.ausschnitt(
                    text.substring(treffer.range.first),
                    "BigRow(",
                    "onClick",
                    mehrfach = true,
                )
                if ("Icons.Filled.Check" in block &&
                    "selected =" !in block &&
                    "checked =" !in block
                ) {
                    "${datei.name}:${text.substring(0, treffer.range.first).count { it == '\n' } + 1}"
                } else {
                    null
                }
            }
        }
        assertEquals(
            "Diese Zeilen zeigen einen Haken und sagen nicht, dass sie gewaehlt sind. " +
                "Vorgelesen sind sie wie jede andere:\n" + stumm.joinToString("\n"),
            emptyList<String>(),
            stumm,
        )
    }
}
