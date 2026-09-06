package dev.kicker.hometiles.wizard

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * in the wizard the text grows along - that step is where the size is being set.
 *
 * the explaining sentence beside the choice did **not** follow: it hung on `dpSp` and stayed
 * put while heading and rows grew. a sentence meant to show the effect and not showing it is
 * a poor proof.
 *
 * so that the growing text never pushes the buttons out, it scrolls in a field of its own and
 * the buttons stand below it - the same split the choosing steps already had.
 */
class WizardTextTest {

    private val source = Quelltext.file("dev/kicker/hometiles/wizard/WizardActivity.kt").readText()

    @Test
    fun `the explaining text follows the chosen size`() {
        assertTrue("bigSp is missing in the wizard", "bigSp(" in source)
        assertTrue(
            "the explaining text still hangs on dpSp - it would not show the change",
            "fontSize = dpSp(17f)" !in source && "fontSize = dpSp(16f)" !in source,
        )
    }

    @Test
    fun `the text scrolls so the buttons stay put`() {
        assertTrue("no scrolling field of its own for the text", "verticalScroll" in source)
        assertTrue("the field gives no room away", "weight(1f, fill = false)" in source)
    }
}
