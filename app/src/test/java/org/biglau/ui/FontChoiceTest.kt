package org.biglau.ui

import java.io.File
import org.biglau.Quelltext
import org.biglau.data.Appearance
import org.biglau.data.FontChoice
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: Atkinson Hyperlegible (default, shipped) / system font.
 *
 * drawn by the Braille Institute for exactly this: the letters that otherwise resemble each
 * other are pulled apart. whoever sees badly does not read smaller, they guess more often.
 */
class FontChoiceTest {

    // a default, not decoration in the settings. whoever needs it would not find it
    // otherwise.
    @Test
    fun `the default is the readable font`() {
        assertEquals(FontChoice.HYPERLEGIBLE, Appearance().font)
    }

    // shipped means shipped: no font from the network, no download on first start, no empty
    // text when there is no network.
    @Test
    fun `both cuts lie in the apk`() {
        val folder = Quelltext.resource("font")
        val files = folder.list()?.toSet().orEmpty()
        assertEquals(true, files.contains("atkinson_regular.ttf"))
        assertEquals(true, files.contains("atkinson_bold.ttf"))
    }

    // SIL Open Font License 1.1. a shipped font without its licence beside it would be a
    // legal fault in a program that calls itself GPL.
    @Test
    fun `the licence lies beside it`() {
        val licence = File("../LICENSE-Atkinson-Hyperlegible.txt")
        assertEquals(true, licence.exists())
        assertEquals(true, licence.readText().contains("SIL OPEN FONT LICENSE"))
    }

    @Test
    fun `there are exactly two possibilities`() {
        assertEquals(2, FontChoice.entries.size)
    }
}

/**
 * no fixed line heights in the typography.
 *
 * material gives `bodyLarge` a line height of 24 sp. that number stays when a place sets
 * only `fontSize` - and this app does that in around ninety places, because nearly every
 * size is computed from the cell size. at 150 percent font the second line of a heading lay
 * over the first.
 */
class TypographyLineHeightTest {

    @Test
    fun `no style prescribes a line height`() {
        for (choice in org.biglau.data.FontChoice.entries) {
            val typo = org.biglau.ui.theme.typographyFor(choice)
            val styles = listOf(
                "displayLarge" to typo.displayLarge,
                "headlineLarge" to typo.headlineLarge,
                "titleLarge" to typo.titleLarge,
                "bodyLarge" to typo.bodyLarge,
                "bodyMedium" to typo.bodyMedium,
                "labelLarge" to typo.labelLarge,
                "labelSmall" to typo.labelSmall,
            )
            val fixed = styles.filter { it.second.lineHeight != androidx.compose.ui.unit.TextUnit.Unspecified }
            assertEquals(emptyList<String>(), fixed.map { "$choice/${it.first}" })
        }
    }
}
