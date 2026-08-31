package org.biglau.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Konfiguration ist zugleich das Sicherungsformat. Sie muss also einen Umlauf
 * unveraendert ueberstehen und darf an unbekannten Feldern nicht scheitern -
 * sonst kostet ein Downgrade den Nutzer seine gesamte Belegung.
 */
class ConfigSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun `Standardkonfiguration ueberlebt den Umlauf unveraendert`() {
        val original = LauncherConfig()
        val decoded = json.decodeFromString<LauncherConfig>(
            json.encodeToString(LauncherConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `alle Aktionsarten ueberleben den Umlauf`() {
        val screen = Screen(
            id = "voll",
            name = "Alles",
            cols = 2,
            rows = 3,
            cells = listOf(
                Cell(0, 0, button = Button(ButtonAction.Action(Builtin.SOS))),
                Cell(1, 0, button = Button(ButtonAction.App("com.beispiel", "com.beispiel.Main"))),
                Cell(0, 1, button = Button(ButtonAction.Contact("Anna", "+43123", mode = ContactMode.SMS))),
                Cell(1, 1, button = Button(ButtonAction.GoToScreen("zweiter"))),
                Cell(0, 2, w = 2, button = Button(ButtonAction.None, label = "breit")),
            ),
        )
        val original = LauncherConfig(screens = listOf(screen), homeScreenId = "voll")
        val decoded = json.decodeFromString<LauncherConfig>(
            json.encodeToString(LauncherConfig.serializer(), original),
        )
        assertEquals(original, decoded)
        assertEquals(2, decoded.screens.first().cells.last().w)
    }

    @Test
    fun `unbekannte Felder werden ignoriert statt zu scheitern`() {
        val withExtra = """
            {
              "version": 1,
              "kommtErstSpaeter": true,
              "screens": [
                {"id":"home","name":"Start","cols":2,"rows":3,"cells":[],"nochNichtErfunden":42}
              ],
              "homeScreenId": "home"
            }
        """.trimIndent()
        val decoded = json.decodeFromString<LauncherConfig>(withExtra)
        assertEquals("home", decoded.homeScreenId)
        assertEquals(1, decoded.screens.size)
    }

    @Test
    fun `fehlende Bloecke fallen auf die Vorgaben zurueck`() {
        val minimal = """{"screens":[{"id":"home","name":"Start"}],"homeScreenId":"home"}"""
        val decoded = json.decodeFromString<LauncherConfig>(minimal)
        assertEquals(ThemeName.DARK, decoded.appearance.theme)
        assertEquals(LabelPosition.BOTTOM_LEFT, decoded.appearance.labelPosition)
        assertTrue("Wischen ist standardmaessig aus", !decoded.behaviour.swipeBetweenScreens)
        assertEquals(2, decoded.appearance.safeBorderPercent)
    }

    @Test
    fun `die Schema-Version wird mitgeschrieben`() {
        val text = json.encodeToString(LauncherConfig.serializer(), LauncherConfig())
        assertTrue("Version fehlt im Dokument", text.contains("\"version\": $CONFIG_VERSION"))
    }

    @Test
    fun `homeScreen faellt auf den ersten Screen zurueck wenn die Id ins Leere zeigt`() {
        val config = LauncherConfig(
            screens = listOf(Defaults.emptyScreen("a", "A")),
            homeScreenId = "gibtesnicht",
        )
        assertEquals("a", config.homeScreen.id)
    }

    @Test
    fun `eine geleerte Zelle ist von einem nie belegten Platz nicht zu unterscheiden`() {
        // "Leer" darf es nur einmal geben - sonst sehen zwei Zustaende gleich aus,
        // verhalten sich aber verschieden.
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
    fun `das dunkle Thema ist die Vorgabe`() {
        assertEquals(ThemeName.DARK, LauncherConfig().appearance.theme)
    }
}
