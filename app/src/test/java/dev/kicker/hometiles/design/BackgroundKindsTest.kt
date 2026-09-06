package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * there are exactly two kinds of background: theme colour and own colour.
 *
 * no picture. behind a photo no contrast can be planned - it differs at every point of the
 * image - and the contrast checks in `ScreenBackgroundTest` would fall away with it. the case
 * is not even in the model: a case nobody handles is worse than one that does not exist.
 *
 * it stands in `:app` and not with the colour tests in `:core:ui` because it **reads
 * source**: the list of module roots belongs to the test source of `:app`.
 */
class BackgroundKindsTest {

    @Test
    fun `there are exactly two kinds of background`() {
        val model = Quelltext.file("dev/kicker/hometiles/data/Model.kt").readText()
        val section = Quelltext.cut(model, "sealed interface Background {", "}")
        assertEquals(true, section.contains("Theme"))
        assertEquals(true, section.contains("Solid"))
        assertEquals(false, section.contains("Image"))
    }
}
