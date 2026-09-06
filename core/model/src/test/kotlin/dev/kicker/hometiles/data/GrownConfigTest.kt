package dev.kicker.hometiles.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the move of a *grown* configuration - and on every run.
 *
 * [RealConfigRoundTripTest] does the same on a real phone's real file, but skips itself
 * silently while `HOMETILES_REAL_CONFIG` is unset, and that it was the whole time: measured on
 * 3.9.2026 it was the only skipped test in the project. a test that skips itself protects
 * nothing.
 *
 * so a copy of the same mixture lies here in the repository - three screens, one folder,
 * fourteen tiles, all tile kinds side by side. program names, numbers and pin are replaced
 * (`tools/fassung-anonymisieren.py`); count, position, colours and switch positions stand
 * character for character as they do on the device.
 */
class GrownConfigTest {

    // the resource keeps its german name: the german tool writes it, tools/README.md names it.
    private val text: String = requireNotNull(
        javaClass.getResourceAsStream("/gewachsene-fassung.json"),
    ) { "gewachsene-fassung.json is missing" }.readBytes().decodeToString()

    private val json = Json { ignoreUnknownKeys = true }

    private fun read(): LauncherConfig =
        json.decodeFromString(LauncherConfig.serializer(), text)

    @Test
    fun `the check file is really grown and not shrunk to a toy`() {
        val config = read()
        assertTrue("at least three screens", config.screens.size >= 3)
        assertTrue("one folder", config.screens.any { it.kind == ScreenKind.FOLDER })
        assertTrue("at least twelve tiles", config.screens.sumOf { it.tileCount } >= 12)
        val kinds = config.screens.flatMap { it.cells }.map { it.button.action::class.simpleName }.toSet()
        assertTrue("several tile kinds side by side: $kinds", kinds.size >= 3)
    }

    @Test
    fun `no real program name stands in the check file`() {
        val foreign = read().screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.App)?.packageName }
            .filterNot { it.startsWith("com.example.") }
        assertEquals("program names of a real phone in the check file", emptyList<String>(), foreign)
    }

    @Test
    fun `a grown configuration survives the move`() {
        val original = read()
        val arrived = requireNotNull(ConfigTransfer.import(ConfigTransfer.export(original)))

        assertEquals(original.screens.size, arrived.screens.size)
        assertEquals(original.screens.map { it.name }, arrived.screens.map { it.name })
        assertEquals(original.screens.map { it.kind }, arrived.screens.map { it.kind })
        assertEquals(
            original.screens.map { s -> s.cells.map { "${it.x},${it.y},${it.w},${it.h}" } },
            arrived.screens.map { s -> s.cells.map { "${it.x},${it.y},${it.w},${it.h}" } },
        )
        assertEquals(
            original.screens.flatMap { it.cells }.map { it.button.action },
            arrived.screens.flatMap { it.cells }.map { it.button.action },
        )
        assertEquals(original.appearance, arrived.appearance)
        assertEquals(original.behaviour, arrived.behaviour)
        assertEquals(original.security, arrived.security)
        assertEquals(original.sos, arrived.sos)
        assertEquals(original.contacts, arrived.contacts)
        assertEquals(original.sms, arrived.sms)
        assertEquals(original.homeScreenId, arrived.homeScreenId)
        assertEquals(original.swipeOrder, arrived.swipeOrder)
    }

    /** what the move deliberately leaves behind stands in ConfigTransfer.strippedForTransfer. */
    @Test
    fun `the move leaves only device-bound things behind`() {
        val arrived = requireNotNull(ConfigTransfer.import(ConfigTransfer.export(read())))
        assertEquals(emptyList<String>(), arrived.apps.recent)
        assertEquals(read().phone.speedDial, arrived.phone.speedDial)
    }
}
