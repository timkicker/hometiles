package org.biglau.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the configuration is also the backup format. it must survive a round trip unchanged and
 * must not fail on unknown fields - otherwise a downgrade costs the user their whole setup.
 */
class ConfigSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `the default configuration survives the round trip unchanged`() {
        val original = LauncherConfig()
        val decoded = json.decodeFromString<LauncherConfig>(
            json.encodeToString(LauncherConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `all kinds of action survive the round trip`() {
        val screen = Screen(
            id = "full",
            name = "Everything",
            cols = 2,
            rows = 3,
            cells = listOf(
                Cell(0, 0, button = Button(ButtonAction.Action(Builtin.SOS))),
                Cell(1, 0, button = Button(ButtonAction.App("com.example", "com.example.Main"))),
                Cell(0, 1, button = Button(ButtonAction.Contact("Anna", "+43123", mode = ContactMode.SMS))),
                Cell(1, 1, button = Button(ButtonAction.GoToScreen("second"))),
                Cell(0, 2, w = 2, button = Button(ButtonAction.None, label = "wide")),
            ),
        )
        val original = LauncherConfig(screens = listOf(screen), homeScreenId = "full")
        val decoded = json.decodeFromString<LauncherConfig>(
            json.encodeToString(LauncherConfig.serializer(), original),
        )
        assertEquals(original, decoded)
        assertEquals(2, decoded.screens.first().cells.last().w)
    }

    @Test
    fun `unknown fields are ignored instead of failing`() {
        val withExtra = """
            {
              "version": 1,
              "comesLater": true,
              "screens": [
                {"id":"home","name":"Start","cols":2,"rows":3,"cells":[],"notInventedYet":42}
              ],
              "homeScreenId": "home"
            }
        """.trimIndent()
        val decoded = json.decodeFromString<LauncherConfig>(withExtra)
        assertEquals("home", decoded.homeScreenId)
        assertEquals(1, decoded.screens.size)
    }

    @Test
    fun `missing blocks fall back to the defaults`() {
        val minimal = """{"screens":[{"id":"home","name":"Start"}],"homeScreenId":"home"}"""
        val decoded = json.decodeFromString<LauncherConfig>(minimal)
        assertEquals(ThemeName.DARK, decoded.appearance.theme)
        assertEquals(LabelPosition.BOTTOM_LEFT, decoded.appearance.labelPosition)
        assertTrue("swiping is off by default", !decoded.behaviour.swipeBetweenScreens)
        assertEquals(2, decoded.appearance.safeBorderPercent)
    }

    @Test
    fun `the schema version is written along`() {
        val text = json.encodeToString(LauncherConfig.serializer(), LauncherConfig())
        assertTrue("the version is missing from the document", text.contains("\"version\": $CONFIG_VERSION"))
    }

    @Test
    fun `homeScreen falls back to the first screen when the id points nowhere`() {
        val config = LauncherConfig(
            screens = listOf(Screen(id = "a", name = "A")),
            homeScreenId = "doesnotexist",
        )
        assertEquals("a", config.homeScreen.id)
    }

    @Test
    fun `an emptied cell cannot be told from a spot never filled`() {
        // there may be only one kind of empty - otherwise two states look alike but behave
        // differently.
        val assigned = Screen(
            id = "s", name = "T", cols = 2, rows = 1,
            cells = listOf(Cell(0, 0, button = Button(ButtonAction.Action(Builtin.DIALER)))),
        )
        val emptied = assigned.copy(cells = emptyList())
        val never = Screen(id = "s", name = "T", cols = 2, rows = 1)
        assertEquals(never.freeSlots(), emptied.freeSlots())
        assertEquals(2, emptied.freeSlots().size)
    }

    @Test
    fun `the dark theme is the default`() {
        assertEquals(ThemeName.DARK, LauncherConfig().appearance.theme)
    }
}
