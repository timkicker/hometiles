package org.biglau.data

import org.biglau.tiles.ScreenEdits
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
    fun `Ausgeschriebenes laesst sich wieder einlesen`() {
        val text = ConfigTransfer.export(sample)
        val back = ConfigTransfer.import(text)
        assertEquals(ConfigTransfer.strippedForTransfer(sample), back)
    }

    @Test
    fun `Aussehen und ausgeblendete Apps wandern mit`() {
        val back = ConfigTransfer.import(ConfigTransfer.export(sample))!!
        assertEquals(ThemeName.HIGH_CONTRAST, back.appearance.theme)
        assertEquals(1.5f, back.appearance.textScale, 0.001f)
        assertEquals(setOf("com.beispiel"), back.apps.hidden)
    }

    @Test
    fun `Widget-Kacheln wandern nicht mit`() {
        // Die Kennung stammt vom AppWidgetHost des alten Geraets. Auf dem neuen zeigt sie
        // auf nichts oder - schlimmer - auf ein fremdes Widget.
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
            "Widget-Kachel haette geleert werden muessen",
            back.screens.flatMap { it.cells }.none { it.button.action is ButtonAction.Widget },
        )
    }

    @Test
    fun `App- und Kontaktkacheln wandern sehr wohl mit`() {
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
    fun `zuletzt benutzte Apps wandern nicht mit`() {
        assertTrue(ConfigTransfer.import(ConfigTransfer.export(sample))!!.apps.recent.isEmpty())
    }

    @Test
    fun `Unsinn ergibt keine Konfiguration`() {
        assertNull(ConfigTransfer.import(""))
        assertNull(ConfigTransfer.import("das ist kein JSON"))
        assertNull(ConfigTransfer.import("[1,2,3]"))
    }

    @Test
    fun `eine leere Datei setzt nicht stillschweigend auf Werkseinstellung zurueck`() {
        // Weil jedes Feld einen Vorgabewert hat, ergaebe "{}" klaglos die Standardbelegung -
        // der Import haette die gesamte Belegung geloescht und dabei ausgesehen, als
        // haette er geklappt.
        assertNull(ConfigTransfer.import("{}"))
        assertNull(ConfigTransfer.import("""{"appearance":{"theme":"DARK"}}"""))
    }

    @Test
    fun `eine Konfiguration ohne Screen wird abgelehnt`() {
        // Ein Launcher ohne Homescreen waere ein Telefon, das nach dem Import nicht startet.
        assertNull(ConfigTransfer.import("""{"version":1,"screens":[],"homeScreenId":"home"}"""))
    }

    @Test
    fun `ein Startscreen ins Leere wird auf den ersten gebogen`() {
        val text = """
            {"version":1,
             "screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}],
             "homeScreenId":"gibtesnicht"}
        """.trimIndent()
        assertEquals("a", ConfigTransfer.import(text)!!.homeScreenId)
    }

    @Test
    fun `unbekannte Felder aus einer neueren Fassung stoeren nicht`() {
        val text = """
            {"version":99,"kommtSpaeter":true,
             "screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[],"neu":1}],
             "homeScreenId":"a"}
        """.trimIndent()
        assertEquals("a", ConfigTransfer.import(text)!!.homeScreenId)
    }

    @Test
    fun `der Dateiname traegt das Datum`() {
        // 2026-08-31, 12:00 UTC
        assertEquals("biglau-2026-08-31.json", ConfigTransfer.suggestedFileName(1788177600000L))
    }

    @Test
    fun `die Ausgabe ist lesbar formatiert`() {
        // Die Datei landet auf einem Rechner und soll sich dort ansehen lassen.
        assertTrue(ConfigTransfer.export(sample).contains("\n"))
        assertTrue(ConfigTransfer.export(sample).contains("\"version\""))
    }

    /**
     * Nach dem Einlesen traegt die Konfiguration die **eigene** Nummer.
     *
     * Am Emulator gesehen: eine Sicherung mit `version: 2` wurde eingelesen — richtig mit
     * dem Hinweis „was diese Fassung nicht kennt, blieb weg" —, aber die Zwei blieb stehen.
     * Damit haette jede spaetere Sicherung dieses Telefons behauptet, sie stamme aus einem
     * neueren BigLau, und die Warnung erschiene fuer immer.
     */
    @Test
    fun `eine Sicherung aus einer neueren Fassung bekommt die eigene Nummer`() {
        val text = ConfigTransfer.export(sample).replace("\"version\": 1", "\"version\": 2")
        assertTrue("Vorbedingung: die Datei nennt Fassung 2", ConfigTransfer.isFromNewerVersion(text))
        val gelesen = ConfigTransfer.import(text)
        assertEquals(CONFIG_VERSION, gelesen?.version)
    }
}
