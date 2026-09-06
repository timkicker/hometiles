package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the model sketch in `PLAN.md` 2.2 names the fields that really exist.
 *
 * counted once: four classes, eight discrepancies. `LauncherConfig` was missing four fields;
 * `Screen` had an `icon` that never existed while the real `kind` (`SCREEN` or `FOLDER`) was
 * missing - **the plan could not express folders at all**, although they exist; `Button`
 * named `icon`/`color` where the code has `iconName`/`colorIndex`/`colorHue`, so the freely
 * chosen hue from 3.3 did not appear in the model.
 *
 * whoever reads that section to understand the data model otherwise reads one that has been
 * wrong for weeks - and a model is the first thing one reads.
 */
class ModelInPlanTest {

    private val plan = File("../PLAN.md").readText()
    private val model = Quelltext.file("dev/kicker/hometiles/data/Model.kt").readText()

    private fun fields(text: String, cls: String): List<String> {
        val block = Regex("""data class $cls\((.*?)\n\)""", RegexOption.DOT_MATCHES_ALL)
            .find(text)
            ?: throw AssertionError("`data class $cls` not found")
        return Regex("""val (\w+):""").findAll(block.groupValues[1]).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `the four core classes name the same fields`() {
        listOf("LauncherConfig", "Screen", "Cell", "Button").forEach { cls ->
            assertEquals(
                "PLAN.md 2.2 describes $cls differently from how it is built. whoever looks " +
                    "the model up there reads a wrong one.",
                fields(model, cls),
                fields(plan, cls),
            )
        }
    }

    /**
     * and `Background` knows no image - the plan says so itself now. the first sketch had
     * `Image(uri, scale)`, and two things hung on it: a line about scaling wallpapers down
     * among the pitfalls, and the `SET_WALLPAPER` permission in the manifest.
     */
    @Test
    fun `the plan no longer promises wallpapers`() {
        assertTrue(
            "the model has Background.Image - then the plan may name it too.",
            "Image(" !in Quelltext.cut(model, "interface Background").take(400),
        )
        // only in the **code block**, not in the prose: the paragraph below explains that the
        // first sketch foresaw `Image(uri, scale)` and what hung on it. a rule forbidding
        // that too would force the plan to keep quiet about its own history - the same
        // mistake `LinksTest` made once.
        val block = Quelltext.cut(Quelltext.cut(plan, "### 2.2"), "```kotlin", "```")
        assertTrue(
            "the model sketch in PLAN.md 2.2 names a wallpaper again that does not exist.",
            "Image(" !in block,
        )
    }
}
