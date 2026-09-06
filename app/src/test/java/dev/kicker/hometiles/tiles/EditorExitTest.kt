package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.Quelltext
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
class EditorExitTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/tiles/TileEditorActivity.kt")

    /** the way back: the condition and the body of the one BackHandler. */
    private val wayBack: String =
        Quelltext.cut(source, "BackHandler(", ".safeDrawingPadding()")

    /** the panels that must listen to the key. the lock is missing on purpose, see below. */
    private val panels = listOf("replacingFolder", "clearing")

    @Test
    fun `the mode alone does not carry the way back`() {
        panels.forEach {
            assertTrue(
                "the way back does not know $it. this panel is set from the menu; there " +
                    "mode == Mode.MENU, and the key then ends the whole editor instead of " +
                    "the question: " + wayBack,
                it in wayBack,
            )
        }
    }

    @Test
    fun `the key clears the panel away, not only the mode`() {
        panels.forEach {
            assertTrue(
                "$it is mentioned in the way back but not reset. the panel draws in front " +
                    "of the mode; it would stay there and the key would visibly do nothing.",
                Regex("""$it\s*=\s*null""").containsMatchIn(wayBack),
            )
        }
    }

    /**
     * the other side of the same rule: the pin lock is a full-screen panel too, but its
     * cancel is the way *out*. clearing it away would open the editor the pin holds shut.
     * `locked` not standing here is the decision, not an oversight.
     */
    @Test
    fun `the lock does not listen to the back key`() {
        assertFalse(
            "the way back touches the lock. then the back key leads past the pin into the " +
                "editor instead of out of it.",
            "locked" in wayBack,
        )
    }

    /**
     * counted so the next panel does not stand there silently: every `return@Box` is a
     * surface that covers the editor. a fourth makes this rule fall and ask what the back
     * key should do there.
     */
    @Test
    fun `there is no panel nobody thought of`() {
        val covering = Regex("""return@Box""").findAll(source).count()
        val excepted = 1 // the lock
        assertEquals(
            "the editor has $covering surfaces that cover it entirely, thought of are " +
                "${panels.size + excepted}. every new one wants an answer here: either into " +
                "the way back or expressly excepted from it.",
            covering,
            panels.size + excepted,
        )
    }

    /**
     * and the modes themselves: from each the key leads back to the menu, from there out.
     * without this line the rest of the rule measures nothing.
     */
    @Test
    fun `from every mode the key leads to the menu`() {
        assertTrue(
            "the way back no longer sets the mode to the menu. then one sits stuck in one " +
                "of the nineteen lists: " + wayBack,
            Regex("""mode\s*=\s*Mode\.MENU""").containsMatchIn(wayBack),
        )
        assertTrue(
            "the way back no longer holds for the modes. from the menu itself the key must " +
                "end the editor, from every other picture it must not.",
            "mode != Mode.MENU" in wayBack,
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
 *   back key, which [EditorExitTest] holds for every mode.
 *
 * a cancel row in every picker would be expensive and no better: the app list has hundreds
 * of entries, nobody would reach the bottom, and at the top it would push the first real
 * choice down.
 */
class EditorDoneTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/tiles/TileEditorActivity.kt")

    /** the modes and the surface they draw. */
    private val branches: List<Pair<String, String>> =
        Regex("""Mode\.(\w+) -> (\w+)\s*[({]""").findAll(source)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

    /** here one sets something and must be able to say one is done. */
    private val withDone = setOf("MENU", "RESIZE", "EDIT_NUMBER", "EDIT_LINK")

    /** here one picks; every row is already an answer. */
    private val onlyAnswers = setOf(
        "PICK_BUILTIN", "PICK_APP", "PICK_CONTACT", "PICK_NUMBER", "PICK_MODE",
        "PICK_SHORTCUT", "PICK_WIDGET", "PICK_SCREEN", "PICK_LONG_PRESS", "MOVE",
        "PICK_COLOR", "PICK_ICON", "PICK_HUE",
    )

    /** the body of the surface this mode draws. */
    private fun body(component: String): String = Quelltext.cut(
        // the guard mark gives the file's last function an end mark.
        source + "\nprivate fun GUARD(",
        "private fun $component(",
        "private fun ",
    )

    private fun hasDoneRow(component: String): Boolean =
        "R.string.editor_done" in body(component)

    @Test
    fun `every mode is sorted into a group`() {
        val unknown = branches.map { it.first }.filterNot { it in withDone || it in onlyAnswers }
        assertEquals(
            "these modes are in neither group. for every new one the question wants an " +
                "answer: does one set something there (then it needs a done row) or pick " +
                "something (then the back key carries the way out): $unknown",
            emptyList<String>(),
            unknown,
        )
        assertTrue(
            "no mode branches can be found at all. then this rule measures nothing.",
            branches.size >= 15,
        )
    }

    @Test
    fun `where one sets something a done row stands`() {
        branches.filter { it.first in withDone }.forEach { (mode, component) ->
            assertTrue(
                "$mode draws $component, and no done row stands there any more. one could " +
                    "then leave a change only by undoing it.",
                hasDoneRow(component),
            )
        }
    }

    /**
     * the other side. a single picker list with a cancel row is worse than none: it teaches
     * that a way out stands at the bottom, and nine lists do not hold that.
     */
    @Test
    fun `where one picks there is no single exception`() {
        val exceptions = branches.filter { it.first in onlyAnswers }
            .filter { hasDoneRow(it.second) }
            .map { "${it.first} (${it.second})" }
        assertEquals(
            "these picker lists have a done row, the others do not. either all or none - " +
                "otherwise the one promises a way out at the end of a list that the rest do " +
                "not hold: $exceptions",
            emptyList<String>(),
            exceptions,
        )
    }
}
