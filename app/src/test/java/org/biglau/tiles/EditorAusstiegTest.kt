package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * from every screen of the editor the back key leads one step back. `PLAN.md` 10.3.7.
 *
 * the editor has nineteen modes and beside them three panels that lay themselves over
 * everything with `return@Box`. the modes had the way back from the start, the panels did
 * not, and at the finger that goes unnoticed because two large buttons stand there.
 *
 * the panels are set from the menu, so `mode != Mode.MENU` was false and the key ended the
 * activity: anyone working by key answered a question and lost their place unasked.
 *
 * the rule behind it is short: the back key does what the cancel button on the same screen
 * does. no more and no less.
 */
class EditorAusstiegTest {

    private val quelle = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    /** the way back: the condition and the body of the one BackHandler. */
    private val rueckweg: String =
        Quelltext.cut(quelle, "BackHandler(", ".safeDrawingPadding()")

    /** the panels that must listen to the key. the lock is missing on purpose, see below. */
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
     * the other side of the same rule: the pin lock is a full-screen panel too, but its
     * cancel is the way *out*. clearing it away would open the editor the pin holds shut.
     * `locked` not standing here is the decision, not an oversight.
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
     * counted so the next panel does not stand there silently: every `return@Box` is a
     * surface that covers the editor. a fourth makes this rule fall and ask what the back
     * key should do there.
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
     * and the modes themselves: from each the key leads back to the menu, from there out.
     * without this line the rest of the rule measures nothing.
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
 * either every picker list carries a cancel row or none does. `PLAN.md` 10.3.7.
 *
 * a single one was already built and translated into five languages before counting: ten of
 * seventeen states have none. that row would have been the only one of its kind among ten
 * identical screens, promising a way out at the end of a list that the nine others do not
 * hold.
 *
 * the dividing line runs elsewhere, and it has a reason:
 *
 * - where one *sets* something, one must be able to say one is done, or a change cannot be
 *   left without undoing it.
 * - where one *picks* something, every row is already an answer, and the way out is the
 *   back key, which [EditorAusstiegTest] holds for every mode.
 *
 * a cancel row in every picker would be expensive and no better: the app list has hundreds
 * of entries, nobody would reach the bottom, and at the top it would push the first real
 * choice down.
 */
class EditorFertigTest {

    private val quelle = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    /** the modes and the surface they draw. */
    private val zweige: List<Pair<String, String>> =
        Regex("""Mode\.(\w+) -> (\w+)\s*[({]""").findAll(quelle)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

    /** here one sets something and must be able to say one is done. */
    private val mitFertig = setOf("MENU", "RESIZE", "EDIT_NUMBER", "EDIT_LINK")

    /** here one picks; every row is already an answer. */
    private val nurAntworten = setOf(
        "PICK_BUILTIN", "PICK_APP", "PICK_CONTACT", "PICK_NUMBER", "PICK_MODE",
        "PICK_SHORTCUT", "PICK_WIDGET", "PICK_SCREEN", "PICK_LONG_PRESS", "MOVE",
        "PICK_COLOR", "PICK_ICON", "PICK_HUE",
    )

    /** the body of the surface this mode draws. */
    private fun rumpf(komponente: String): String = Quelltext.cut(
        // the guard mark gives the file's last function an end mark.
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
