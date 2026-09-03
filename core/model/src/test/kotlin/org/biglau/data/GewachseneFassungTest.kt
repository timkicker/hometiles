package org.biglau.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Umzug einer *gewachsenen* Konfiguration - und zwar bei jedem Lauf.
 *
 * Daneben steht [RealConfigRoundTripTest], der dasselbe an der echten Datei eines echten
 * Telefons prüft. Der überspringt sich aber stillschweigend, solange `BIGLAU_REAL_CONFIG`
 * nicht gesetzt ist, und das war er die ganze Zeit: gemessen am 3.9.2026 war er der
 * einzige übersprungene Test im ganzen Projekt. Ein Test, der sich selbst überspringt,
 * schützt nichts.
 *
 * Deshalb liegt hier eine Abschrift derselben Mischung im Repository - drei Bildschirme,
 * ein Ordner, vierzehn Kacheln, alle Kachelarten nebeneinander. Programmnamen, Nummern
 * und PIN sind ersetzt (`tools/fassung-anonymisieren.py`); Anzahl, Lage, Farben und
 * Schalterstellungen stehen Zeichen für Zeichen wie auf dem Gerät.
 */
class GewachseneFassungTest {

    private val text: String = requireNotNull(
        javaClass.getResourceAsStream("/gewachsene-fassung.json"),
    ) { "gewachsene-fassung.json fehlt" }.readBytes().decodeToString()

    private val json = Json { ignoreUnknownKeys = true }

    private fun gelesen(): LauncherConfig =
        json.decodeFromString(LauncherConfig.serializer(), text)

    @Test
    fun `die pruefdatei ist wirklich gewachsen und nicht auf ein spielzeug geschrumpft`() {
        val config = gelesen()
        assertTrue("mindestens drei Bildschirme", config.screens.size >= 3)
        assertTrue("ein Ordner", config.screens.any { it.kind == ScreenKind.FOLDER })
        assertTrue("mindestens zwölf Kacheln", config.screens.sumOf { it.tileCount } >= 12)
        val arten = config.screens.flatMap { it.cells }.map { it.button.action::class.simpleName }.toSet()
        assertTrue("mehrere Kachelarten nebeneinander: $arten", arten.size >= 3)
    }

    @Test
    fun `in der pruefdatei steht kein echter programmname`() {
        val fremd = gelesen().screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.App)?.packageName }
            .filterNot { it.startsWith("com.example.") }
        assertEquals("Programmnamen eines echten Telefons in der Prüfdatei", emptyList<String>(), fremd)
    }

    @Test
    fun `eine gewachsene konfiguration ueberlebt den umzug`() {
        val original = gelesen()
        val angekommen = requireNotNull(ConfigTransfer.import(ConfigTransfer.export(original)))

        assertEquals(original.screens.size, angekommen.screens.size)
        assertEquals(original.screens.map { it.name }, angekommen.screens.map { it.name })
        assertEquals(original.screens.map { it.kind }, angekommen.screens.map { it.kind })
        assertEquals(
            original.screens.map { s -> s.cells.map { "${it.x},${it.y},${it.w},${it.h}" } },
            angekommen.screens.map { s -> s.cells.map { "${it.x},${it.y},${it.w},${it.h}" } },
        )
        assertEquals(
            original.screens.flatMap { it.cells }.map { it.button.action },
            angekommen.screens.flatMap { it.cells }.map { it.button.action },
        )
        assertEquals(original.appearance, angekommen.appearance)
        assertEquals(original.behaviour, angekommen.behaviour)
        assertEquals(original.security, angekommen.security)
        assertEquals(original.sos, angekommen.sos)
        assertEquals(original.contacts, angekommen.contacts)
        assertEquals(original.sms, angekommen.sms)
        assertEquals(original.homeScreenId, angekommen.homeScreenId)
        assertEquals(original.swipeOrder, angekommen.swipeOrder)
    }

    /** Was der Umzug bewusst zurücklässt, steht in ConfigTransfer.strippedForTransfer. */
    @Test
    fun `der umzug laest nur geraetegebundenes zurueck`() {
        val angekommen = requireNotNull(ConfigTransfer.import(ConfigTransfer.export(gelesen())))
        assertEquals(emptyList<String>(), angekommen.apps.recent)
        assertEquals(gelesen().phone.speedDial, angekommen.phone.speedDial)
    }
}
