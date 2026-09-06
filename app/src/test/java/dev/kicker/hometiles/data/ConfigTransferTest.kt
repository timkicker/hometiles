package dev.kicker.hometiles.data

import dev.kicker.hometiles.tiles.ScreenEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTransferTest {

    private val sample = LauncherConfig(
        screens = listOf(
            Defaults.mainScreen(),
            ScreenEdits.newScreen("s2", "Zweiter", Defaults.mainScreen()),
        ),
        homeScreenId = Defaults.MAIN_ID,
        appearance = Appearance(theme = ThemeName.HIGH_CONTRAST, textScale = 1.5f),
        apps = AppsConfig(hidden = setOf("com.beispiel"), recent = listOf("a", "b")),
    )

    @Test
    fun `what was written out can be read back in`() {
        val text = ConfigTransfer.export(sample)
        val back = ConfigTransfer.import(text)
        assertEquals(ConfigTransfer.strippedForTransfer(sample), back)
    }

    @Test
    fun `appearance and hidden apps travel along`() {
        val back = ConfigTransfer.import(ConfigTransfer.export(sample))!!
        assertEquals(ThemeName.HIGH_CONTRAST, back.appearance.theme)
        assertEquals(1.5f, back.appearance.textScale, 0.001f)
        assertEquals(setOf("com.beispiel"), back.apps.hidden)
    }

    @Test
    fun `widget tiles do not travel along`() {
        // the id comes from the old device's AppWidgetHost. on the new one it points at
        // nothing or - worse - at a foreign widget.
        val withWidget = sample.copy(
            screens = listOf(
                Defaults.mainScreen().let { screen ->
                    screen.copy(
                        cells = screen.cells + Cell(
                            0, 0,
                            button = Button(ButtonAction.Widget("a/b", 42, "Uhr")),
                        ),
                    )
                },
            ),
        )
        val back = ConfigTransfer.import(ConfigTransfer.export(withWidget))!!
        assertTrue(
            "the widget tile should have been emptied",
            back.screens.flatMap { it.cells }.none { it.button.action is ButtonAction.Widget },
        )
    }

    @Test
    fun `app and contact tiles do travel along`() {
        val config = LauncherConfig(
            screens = listOf(
                Screen(
                    id = "home", name = "Start", cols = 2, rows = 1,
                    cells = listOf(
                        Cell(0, 0, button = Button(ButtonAction.App("com.chat", "com.chat.Main"))),
                        Cell(1, 0, button = Button(ButtonAction.Contact("Oma", "+43660"))),
                    ),
                ),
            ),
            homeScreenId = "home",
        )
        val back = ConfigTransfer.import(ConfigTransfer.export(config))!!
        val actions = back.screens.first().cells.map { it.button.action }
        assertTrue(actions.any { it is ButtonAction.App })
        assertTrue(actions.any { it is ButtonAction.Contact })
    }

    @Test
    fun `recently used apps do not travel along`() {
        assertTrue(ConfigTransfer.import(ConfigTransfer.export(sample))!!.apps.recent.isEmpty())
    }

    @Test
    fun `nonsense gives no configuration`() {
        assertNull(ConfigTransfer.import(""))
        assertNull(ConfigTransfer.import("das ist kein JSON"))
        assertNull(ConfigTransfer.import("[1,2,3]"))
    }

    @Test
    fun `an empty file does not silently reset to the factory setting`() {
        // since every field has a default, "{}" would give the standard arrangement
        // without complaint - the import would have deleted the whole arrangement while
        // looking as if it had worked.
        assertNull(ConfigTransfer.import("{}"))
        assertNull(ConfigTransfer.import("""{"appearance":{"theme":"DARK"}}"""))
    }

    @Test
    fun `a configuration without a screen is refused`() {
        // a launcher without a home screen would be a phone that does not start after the
        // import.
        assertNull(ConfigTransfer.import("""{"version":1,"screens":[],"homeScreenId":"home"}"""))
    }

    @Test
    fun `a home screen pointing at nothing is bent to the first one`() {
        val text = """
            {"version":1,
             "screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}],
             "homeScreenId":"gibtesnicht"}
        """.trimIndent()
        assertEquals("a", ConfigTransfer.import(text)!!.homeScreenId)
    }

    @Test
    fun `unknown fields from a newer version do not disturb`() {
        val text = """
            {"version":99,"kommtSpaeter":true,
             "screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[],"neu":1}],
             "homeScreenId":"a"}
        """.trimIndent()
        assertEquals("a", ConfigTransfer.import(text)!!.homeScreenId)
    }

    @Test
    fun `the file name carries the date`() {
        // 2026-08-31, 12:00 UTC
        assertEquals("hometiles-2026-08-31.json", ConfigTransfer.suggestedFileName(1788177600000L))
    }

    @Test
    fun `the output is formatted readably`() {
        // the file lands on a computer and should be readable there.
        assertTrue(ConfigTransfer.export(sample).contains("\n"))
        assertTrue(ConfigTransfer.export(sample).contains("\"version\""))
    }

    /**
     * after reading, the configuration carries its **own** number.
     *
     * a backup with `version: 2` was read in correctly, with the hint that what this version
     * does not know stayed out - but the two stayed. every later backup of this phone would
     * then have claimed to come from a newer HomeTiles, and the warning would appear for ever.
     */
    @Test
    fun `a backup from a newer version gets our own number`() {
        val text = ConfigTransfer.export(sample).replace("\"version\": 1", "\"version\": 2")
        assertTrue("precondition: the file names version 2", ConfigTransfer.isFromNewerVersion(text))
        val read = ConfigTransfer.import(text)
        assertEquals(CONFIG_VERSION, read?.version)
    }
}
