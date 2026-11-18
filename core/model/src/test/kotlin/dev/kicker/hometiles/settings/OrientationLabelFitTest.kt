package dev.kicker.hometiles.settings

import dev.kicker.hometiles.data.Language
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * issue #1: the row for the phone's rotation setting stays one line on the jelly 2, in every
 * language, at the phone's own text size.
 *
 * the first wording, "Follow the phone's rotation setting", took two lines at 100 percent.
 * measured with the font that draws it, the way BigRow draws it: bold, 22 sp.
 *
 * here and not in `:app` because `java.awt` is missing from the android test classpath.
 */
class OrientationLabelFitTest {

    private val bold = Font.createFont(
        Font.TRUETYPE_FONT,
        File("../ui/src/main/res/font/atkinson_bold.ttf"),
    )

    private fun widthDp(text: String, sizeSp: Float): Double =
        bold.deriveFont(sizeSp).getStringBounds(text, FontRenderContext(null, true, true)).width

    // the text column of a settings row on the jelly 2, measured on the jelly2 avd on
    // 24.10.2026: 110 to 447 px at 220 dpi.
    private val columnDp = 245.0

    // the jelly 2 runs at system text size 1.35, the largest one we know a user to have.
    private val fontScale = 1.35f

    private val labels: Map<String, String> =
        File("../../app/src/main/res").listFiles { dir -> dir.name.matches(Regex("values(-..)?")) }
            .orEmpty()
            .associate { dir ->
                val text = File(dir, "strings.xml").readText()
                val label = Regex("""<string name="orientation_system">(.*?)</string>""")
                    .find(text)?.groupValues?.get(1)
                    ?: throw AssertionError("${dir.name} has no orientation_system")
                dir.name to label.replace("\\'", "'")
            }

    // a rule over zero files is green and has looked at nothing.
    @Test
    fun `every language is measured`() {
        // english plus every translation; SYSTEM is no language of its own.
        assertEquals(Language.entries.size - 1, labels.size)
    }

    @Test
    fun `the phone setting row stays one line everywhere`() {
        val tooLong = labels.mapNotNull { (language, label) ->
            val width = widthDp(label, 22f * fontScale)
            if (width > columnDp) "$language: \"$label\" is ${width.toInt()} dp" else null
        }
        assertTrue("wider than $columnDp dp:\n" + tooLong.joinToString("\n"), tooLong.isEmpty())
    }

    /** the measurement itself: the first wording has to fail it, or it measures nothing. */
    @Test
    fun `the first wording would not have fit`() {
        assertTrue(widthDp("Follow the phone's rotation setting", 22f * fontScale) > columnDp)
    }
}
