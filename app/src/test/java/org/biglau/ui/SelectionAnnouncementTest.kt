package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what is chosen does not stand in the colour alone.
 *
 * the bare `selected` property does not reach the accessibility interface, so the state has
 * to go into the name as well. the icon picker beside the colour picker did not do it, and
 * BigLau is built for eyes that tell colours apart badly.
 *
 * the rule hangs on the tool, not on the lists: as long as `BigRow` announces the choice,
 * every list using `selected` announces it.
 */
class SelectionAnnouncementTest {

    private val row = Quelltext.withoutComments("org/biglau/ui/BigRow.kt")
    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `a chosen row says so and does not only colour itself`() {
        assertTrue(
            "BigRow knows no selected - then the choice is carried by the colour alone " +
                "again in every list.",
            row.contains("selected: Boolean = false"),
        )
        // not on the wording: the first version stood on "if (selected) palette.surfaceAccent"
        // and fell over when the switch arrived.
        val surface = row.lines().dropWhile { !it.contains("val paint =") }.take(2).joinToString(" ")
        assertTrue(
            "BigRow does not colour on selected itself; the lists would have to write the " +
                "colour change out again and could forget the announcement: $surface",
            "selected" in surface && "surfaceAccent" in surface,
        )
        assertTrue(
            "BigRow sets no announcement on selected. measured from the device's node dump: " +
                "the selected property alone does not arrive when read aloud.",
            row.contains("this.selected = true") && row.contains("a11y_chosen"),
        )
    }

    @Test
    fun `the announcement lies where BigRow can read it`() {
        val found = (Quelltext.texts("values") + Quelltext.texts("values-de"))
            .filter { it.readText().contains("name=\"a11y_chosen\"") }
        assertTrue(
            "a11y_chosen is missing in one language or does not lie in :core:ui where BigRow " +
                "stands: ${found.map { it.path }}",
            found.size == 2 && found.all { it.path.contains("core/ui") },
        )
    }

    @Test
    fun `the icon grid shows and says which icon holds`() {
        // up to the next function, not to a named one: below IconPicker stands the tint
        // picker, which brings the same words along.
        val grid = Quelltext.cut(editor, "fun IconPicker(", "\nprivate fun ")
        assertTrue(
            "the icon picker does not say which icon is chosen - read aloud, the chosen " +
                "field is one of fifty-six alike.",
            grid.contains("a11y_chosen") && grid.contains("this.selected = true"),
        )
        assertTrue(
            "the chosen icon can only be told by its surface colour. whoever tells colours " +
                "apart badly does not see the choice.",
            grid.contains("Modifier.border("),
        )
    }

    /**
     * a switch says "on" and "off", not "chosen": a choice is one of several, a switch stands
     * one way or the other, and the system has its own words for that.
     */
    @Test
    fun `a switch says its state`() {
        assertTrue(
            "BigRow knows no switch - then switches would announce themselves as \"chosen\", " +
                "which is not true.",
            row.contains("checked: Boolean? = null"),
        )
        assertTrue(
            "the switch does not announce its state.",
            row.contains("a11y_on") && row.contains("a11y_off"),
        )
        val missing = (Quelltext.texts("values") + Quelltext.texts("values-de")).filterNot {
            val t = it.readText()
            "name=\"a11y_on\"" in t && "name=\"a11y_off\"" in t
        }
        assertTrue(
            "on/off is missing in one language: ${missing.map { it.path }}",
            missing.none { it.path.contains("core/ui") },
        )
    }

    /**
     * a tick is not an announcement.
     *
     * counting by the accent surface missed every list where the surface already means
     * something else - the theme picker paints in its own colours, so do a screen's
     * backgrounds, and the icon size shows the icon at the size in question. they all set a
     * tick instead, and said nothing.
     */
    @Test
    fun `where a tick stands, an announcement stands too`() {
        val silent = Quelltext.files().flatMap { file ->
            val text = file.readText()
            Regex("""BigRow\(""").findAll(text).mapNotNull { hit ->
                // `repeated`, because the cut is deliberately at the **first** occurrence:
                // the section starts at this very call and the later ones stand behind it.
                val block = Quelltext.cut(
                    text.substring(hit.range.first),
                    "BigRow(",
                    "onClick",
                    repeated = true,
                )
                if ("Icons.Filled.Check" in block &&
                    "selected =" !in block &&
                    "checked =" !in block
                ) {
                    "${file.name}:${text.substring(0, hit.range.first).count { it == '\n' } + 1}"
                } else {
                    null
                }
            }
        }
        assertEquals(
            "these rows show a tick and do not say that they are chosen. read aloud they are " +
                "like any other:\n" + silent.joinToString("\n"),
            emptyList<String>(),
            silent,
        )
    }
}
