package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aus jedem Bild des Editors fuehrt die Zurueck-Taste einen Schritt zurueck.
 *
 * PLAN.md 10.3.7. Der Editor ist der groesste Bildschirm der App und der einzige mit
 * eigenen Zustaenden: neunzehn Modi und daneben drei Tafeln, die sich mit `return@Box`
 * vor alles legen. Fuer die Modi gab es den Rueckweg von Anfang an. Fuer die Tafeln nicht,
 * und das faellt am Finger nicht auf, weil dort zwei grosse Knoepfe stehen.
 *
 * Am 04.09.2026 am Emulator gemessen: Ordnerkachel, Menuetaste, bearbeiten, einen Kontakt
 * gewaehlt. Es kam die Frage `Folder loeschen?` mit `Behalten` daneben. Ein Druck auf
 * Zurueck - und man stand auf dem Startbildschirm, der Editor war zu.
 *
 * Zerstoert wurde dabei nichts, der Ordner stand noch. Falsch ist trotzdem etwas: die
 * Tafeln werden aus dem Menue heraus gesetzt, `mode != Mode.MENU` war also false, und die
 * Taste beendete die Activity. Wer mit Tasten arbeitet, hat auf eine Frage geantwortet und
 * dabei ungefragt seinen Platz verloren.
 *
 * Die Regel dahinter ist kurz: **die Zurueck-Taste tut, was der Abbruchknopf auf demselben
 * Bild tut.** Steht dort `Behalten`, dann behaelt sie und bleibt. Nicht mehr und nicht
 * weniger.
 */
class EditorAusstiegTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/tiles/TileEditorActivity.kt")

    /** Der Rueckweg: die Bedingung und der Rumpf des einen BackHandler. */
    private val rueckweg: String =
        Quelltext.ausschnitt(quelle, "BackHandler(", ".safeDrawingPadding()")

    /**
     * Die Tafeln, die auf die Taste hoeren muessen. Die Sperre fehlt mit Absicht, siehe
     * unten.
     */
    private val tafeln = listOf("replacingFolder", "clearing")

    @Test
    fun `der Modus allein traegt den Rueckweg nicht`() {
        tafeln.forEach {
            assertTrue(
                "Der Rueckweg kennt $it nicht. Diese Tafel wird aus dem Menue heraus " +
                    "gesetzt; dort ist mode == Mode.MENU, und die Taste beendet dann den " +
                    "ganzen Editor statt die Frage: " + rueckweg,
                it in rueckweg,
            )
        }
    }

    @Test
    fun `die Taste raeumt die Tafel weg, nicht nur den Modus`() {
        tafeln.forEach {
            assertTrue(
                "$it wird im Rueckweg zwar erwaehnt, aber nicht zurueckgesetzt. Die Tafel " +
                    "zeichnet sich vor dem Modus; sie bliebe stehen, und die Taste taete " +
                    "sichtbar nichts.",
                Regex("""$it\s*=\s*null""").containsMatchIn(rueckweg),
            )
        }
    }

    /**
     * Die andere Seite derselben Regel.
     *
     * Die PIN-Sperre ist auch eine Vollbild-Tafel, aber ihr Abbruch ist der Weg **hinaus**.
     * Wuerde die Taste sie wegraeumen, oeffnete sie den Editor, den die PIN zuhaelt. Dass
     * `locked` hier nicht steht, ist die Entscheidung und kein Vergessen.
     */
    @Test
    fun `die Sperre hoert nicht auf die Zurueck-Taste`() {
        assertFalse(
            "Der Rueckweg fasst die Sperre an. Dann fuehrt die Zurueck-Taste an der PIN " +
                "vorbei in den Editor hinein statt aus ihm heraus.",
            "locked" in rueckweg,
        )
    }

    /**
     * Gezaehlt, damit die naechste Tafel nicht still danebensteht.
     *
     * Jedes `return@Box` ist eine Flaeche, die den Editor verdeckt. Es gibt drei: die
     * Sperre und die beiden Loeschfragen. Kommt eine vierte dazu, faellt diese Regel um und
     * fragt, was die Zurueck-Taste dort tun soll - vor dem Geraet und nicht erst darauf.
     */
    @Test
    fun `es gibt keine Tafel, die niemand bedacht hat`() {
        val verdeckend = Regex("""return@Box""").findAll(quelle).count()
        val ausgenommen = 1 // die Sperre
        assertEquals(
            "Der Editor hat $verdeckend Flaechen, die ihn ganz verdecken, bedacht sind " +
                "${tafeln.size + ausgenommen}. Fuer jede neue gehoert hier eine Antwort " +
                "hin: entweder in den Rueckweg oder ausdruecklich davon ausgenommen.",
            verdeckend,
            tafeln.size + ausgenommen,
        )
    }

    /**
     * Und die Modi selbst: von jedem fuehrt die Taste ins Menue zurueck, von dort hinaus.
     * Das stand schon vorher da; ohne diese Zeile misst der Rest davon nichts.
     */
    @Test
    fun `aus jedem Modus fuehrt die Taste ins Menue`() {
        assertTrue(
            "Der Rueckweg setzt den Modus nicht mehr auf das Menue. Dann sitzt man in " +
                "einer der neunzehn Listen fest: " + rueckweg,
            Regex("""mode\s*=\s*Mode\.MENU""").containsMatchIn(rueckweg),
        )
        assertTrue(
            "Der Rueckweg gilt nicht mehr fuer die Modi. Aus dem Menue selbst muss die " +
                "Taste den Editor beenden, aus jedem anderen Bild erst einmal nicht.",
            "mode != Mode.MENU" in rueckweg,
        )
    }
}

/**
 * Entweder alle Auswahllisten tragen eine Abbruchzeile oder keine.
 *
 * PLAN.md 10.3.7, gemessen am 04.09.2026 unmittelbar nachdem der Rueckweg stand. Der
 * Verdacht war, `Kachel verschieben` sei der einzige Zustand ohne sichtbaren Ausweg, weil
 * das Menue und die Groessenliste ihr `Fertig` haben. Ich hatte schon eine Zeile
 * `Kachel lassen, wo sie ist` eingebaut und in fuenf Sprachen uebersetzt, bevor ich
 * nachgezaehlt habe: **zehn von siebzehn** Zustaenden haben keine. Die Zeile waere die
 * einzige ihrer Art unter zehn gleichen Bildschirmen gewesen - und damit selbst ein Fehler,
 * denn sie verspricht einen Ausweg am Listenende, den die neun anderen nicht halten. Sie
 * ist wieder draussen.
 *
 * Die Trennlinie liegt woanders, und sie hat einen Grund:
 *
 * - Wo man **etwas einstellt**, muss man sagen koennen, dass man fertig ist. Das Menue, die
 *   Groessenliste, die beiden Eingabefelder: ohne die Zeile kaeme man aus einer Aenderung
 *   nicht heraus, ohne sie zurueckzunehmen.
 * - Wo man **etwas auswaehlt**, ist jede Zeile bereits eine Antwort. Der Ausweg ist die
 *   Zurueck-Taste, und die greift dank [EditorAusstiegTest] in jedem Modus.
 *
 * Eine Abbruchzeile in jeder Auswahlliste waere teuer und nicht besser: die App-Liste hat
 * dreistellig viele Eintraege, unten kaeme niemand an, und oben schoebe sie die erste
 * wirkliche Wahl nach unten. Auf drei Zoll mit grosser Schrift ist das keine Kleinigkeit.
 *
 * Diese Regel haelt die Einteilung fest. Ein neuer Modus muss hier einsortiert werden, und
 * eine halbe Loesung - eine einzelne Auswahlliste mit Abbruch - faellt um.
 */
class EditorFertigTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/tiles/TileEditorActivity.kt")

    /** Die Modi und die Flaeche, die sie zeichnen. */
    private val zweige: List<Pair<String, String>> =
        Regex("""Mode\.(\w+) -> (\w+)\s*[({]""").findAll(quelle)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

    /** Hier stellt man etwas ein und muss sagen koennen, dass man fertig ist. */
    private val mitFertig = setOf("MENU", "RESIZE", "EDIT_NUMBER", "EDIT_LINK")

    /** Hier waehlt man aus; jede Zeile ist schon eine Antwort. */
    private val nurAntworten = setOf(
        "PICK_BUILTIN", "PICK_APP", "PICK_CONTACT", "PICK_NUMBER", "PICK_MODE",
        "PICK_SHORTCUT", "PICK_WIDGET", "PICK_SCREEN", "PICK_LONG_PRESS", "MOVE",
        "PICK_COLOR", "PICK_ICON", "PICK_HUE",
    )

    /** Der Rumpf der Flaeche, die dieser Modus zeichnet. */
    private fun rumpf(komponente: String): String = Quelltext.ausschnitt(
        // Die Wachmarke gibt der letzten Funktion der Datei eine Endmarke.
        quelle + "\nprivate fun WACHE(",
        "private fun $komponente(",
        "private fun ",
    )

    private fun hatFertigzeile(komponente: String): Boolean =
        "R.string.editor_done" in rumpf(komponente)

    @Test
    fun `jeder Modus ist eingeordnet`() {
        val unbekannt = zweige.map { it.first }.filterNot { it in mitFertig || it in nurAntworten }
        assertEquals(
            "Diese Modi sind in keiner der beiden Gruppen. Fuer jeden neuen gehoert die " +
                "Frage beantwortet, ob man dort etwas einstellt (dann braucht er eine " +
                "Fertigzeile) oder etwas auswaehlt (dann traegt die Zurueck-Taste den " +
                "Ausweg): $unbekannt",
            emptyList<String>(),
            unbekannt,
        )
        assertTrue(
            "Es sind gar keine Modus-Zweige mehr zu finden. Dann misst diese Regel nichts.",
            zweige.size >= 15,
        )
    }

    @Test
    fun `wo man etwas einstellt, steht eine Fertigzeile`() {
        zweige.filter { it.first in mitFertig }.forEach { (modus, komponente) ->
            assertTrue(
                "$modus zeichnet $komponente, und dort steht keine Fertigzeile mehr. Aus " +
                    "einer Aenderung kaeme man dann nur heraus, indem man sie zuruecknimmt.",
                hatFertigzeile(komponente),
            )
        }
    }

    /**
     * Die andere Seite. Eine einzelne Auswahlliste mit Abbruchzeile ist schlimmer als keine:
     * sie lehrt, dass unten ein Ausweg steht, und neun Listen halten das nicht.
     */
    @Test
    fun `wo man auswaehlt, steht keine einzelne Ausnahme`() {
        val ausnahmen = zweige.filter { it.first in nurAntworten }
            .filter { hatFertigzeile(it.second) }
            .map { "${it.first} (${it.second})" }
        assertEquals(
            "Diese Auswahllisten haben eine Fertigzeile, die anderen nicht. Entweder alle " +
                "oder keine - sonst verspricht die eine einen Ausweg am Listenende, den " +
                "die uebrigen nicht halten: $ausnahmen",
            emptyList<String>(),
            ausnahmen,
        )
    }
}
